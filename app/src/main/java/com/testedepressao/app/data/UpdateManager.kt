package com.testedepressao.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val apkUrl: String = ""
)

class UpdateManager(private val context: Context) {

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val VERSION_URL = "https://raw.githubusercontent.com/Tarrasga/teste-de-pressao/main/version.json"

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        _isChecking.value = true
        _statusMessage.value = "A verificar atualizações no GitHub..."

        try {
            val url = URL(VERSION_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                useCaches = false
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val remoteVersionCode = json.optInt("versionCode", 0)
                val remoteVersionName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val releaseNotes = json.optString("releaseNotes", "")

                val currentCode = getCurrentVersionCode()
                val hasUpdate = remoteVersionCode > currentCode

                val info = UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = remoteVersionName,
                    latestVersionCode = remoteVersionCode,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl
                )
                _updateInfo.value = info
                _statusMessage.value = if (hasUpdate) {
                    "Nova versão disponível: v$remoteVersionName"
                } else {
                    "A app já está na versão mais recente (v${getCurrentVersionName()})."
                }
                return@withContext info
            } else {
                _statusMessage.value = "Não foi possível verificar atualizações (Código ${connection.responseCode})"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Erro ao ligar ao GitHub: ${e.localizedMessage}"
        } finally {
            _isChecking.value = false
        }

        return@withContext UpdateInfo()
    }

    suspend fun downloadAndInstall(apkUrl: String) = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        _downloadProgress.value = 0f
        _statusMessage.value = "A descarregar atualização..."

        try {
            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
            }

            val fileLength = connection.contentLength
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val destFile = File(cacheDir, "TesteDePressao_update.apk")
            if (destFile.exists()) destFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileLength > 0) {
                            _downloadProgress.value = (totalBytesRead.toFloat() / fileLength).coerceIn(0f, 1f)
                        }
                    }
                    output.flush()
                }
            }

            _downloadProgress.value = 1f
            _statusMessage.value = "Download concluído! A abrir instalador..."
            installApk(destFile)
        } catch (e: Exception) {
            _statusMessage.value = "Erro no download: ${e.localizedMessage}"
        } finally {
            _isDownloading.value = false
        }
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _statusMessage.value = "Erro ao iniciar instalação: ${e.localizedMessage}"
        }
    }
}

package com.testedepressao.app.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.testedepressao.app.data.models.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class CarBluetoothManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val sendMutex = Mutex()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    // Standard SPP (Serial Port Profile) UUID for ELM327 Bluetooth devices
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        disconnect()

        try {
            // Cancel discovery to optimize connection speed and stability
            bluetoothAdapter?.cancelDiscovery()

            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()

            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream

            _connectedDeviceName.value = device.name ?: device.address
            _connectionStatus.value = ConnectionStatus.CONNECTED
            true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }

    suspend fun sendCommand(command: String, timeoutMs: Long = 800): String = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val out = outputStream ?: return@withLock "ERROR_NO_STREAM"
            val input = inputStream ?: return@withLock "ERROR_NO_STREAM"

            try {
                // 1. Drain any remaining unread bytes from previous commands to prevent prompt desynchronization
                val drainAvail = input.available()
                if (drainAvail > 0) {
                    input.read(ByteArray(drainAvail))
                }

                // 2. Write command terminated with \r
                val cmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
                out.write(cmd)
                out.flush()

                val buffer = StringBuilder()
                val startTime = System.currentTimeMillis()

                // 3. Read until prompt '>' character appears or timeout occurs
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    val avail = input.available()
                    if (avail > 0) {
                        var foundPrompt = false
                        val bytes = ByteArray(avail)
                        val readCount = input.read(bytes, 0, avail)
                        for (i in 0 until readCount) {
                            val c = bytes[i].toInt().toChar()
                            if (c == '>') {
                                foundPrompt = true
                                break
                            }
                            buffer.append(c)
                        }
                        if (foundPrompt) {
                            break
                        }
                    } else {
                        Thread.sleep(6)
                    }
                }

                cleanElmResponse(buffer.toString(), command)
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
        }
    }

    private fun cleanElmResponse(raw: String, command: String): String {
        return raw.replace(command, "")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(">", "")
            .trim()
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            outputStream = null
            socket = null
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _connectedDeviceName.value = null
        }
    }
}

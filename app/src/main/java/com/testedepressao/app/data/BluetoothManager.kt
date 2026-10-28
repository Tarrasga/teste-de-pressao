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

    suspend fun sendCommand(command: String, timeoutMs: Long = 1000): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext "ERROR_NO_STREAM"
        val input = inputStream ?: return@withContext "ERROR_NO_STREAM"

        try {
            // ELM327 expects commands terminated by carriage return
            val cmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(cmd)
            out.flush()

            val buffer = StringBuilder()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (input.available() > 0) {
                    val c = input.read().toChar()
                    // ELM327 ends responses with the prompt character '>'
                    if (c == '>') {
                        break
                    }
                    buffer.append(c)
                } else {
                    Thread.sleep(10)
                }
            }

            cleanElmResponse(buffer.toString(), command)
        } catch (e: Exception) {
            "ERROR: ${e.message}"
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

package com.testedepressao.app.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceDialog(
    devices: List<BluetoothDevice>,
    currentDeviceAddress: String?,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Dispositivos OBD2 Emparelhados")
            }
        },
        text = {
            if (devices.isEmpty()) {
                Text(
                    "Nenhum dispositivo Bluetooth emparelhado encontrado.\n\nPor favor, vá às Definições de Bluetooth do seu telemóvel e emparelhe primeiro com o seu adaptador OBD2 (geralmente chamado 'OBDII', 'Vgate' ou 'ELM327', código PIN 1234 ou 0000).",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { dev ->
                        val isSelected = dev.address == currentDeviceAddress
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectDevice(dev) },
                            color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dev.name ?: "Dispositivo Desconhecido",
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = dev.address,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = NeonCyan)
            }
        }
    )
}

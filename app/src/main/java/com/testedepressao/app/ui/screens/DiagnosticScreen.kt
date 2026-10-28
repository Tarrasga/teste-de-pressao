package com.testedepressao.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.data.models.DiagnosticTroubleCode
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.DarkSurfaceElevated
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.SuccessGreen
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber

@Composable
fun DiagnosticScreen(
    codes: List<DiagnosticTroubleCode>,
    isScanning: Boolean,
    onScan: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onScan,
                enabled = !isScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = CarbonBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = CarbonBlack,
                        strokeWidth = 2.dp
                    )
                    Text("A verificar...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Ler Erros (DTC)", fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = { showConfirmClearDialog = true },
                enabled = !isScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RaceRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Apagar Erros", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area
        if (codes.isEmpty() && !isScanning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Sem Códigos de Avaria",
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = "A centralina do motor não reportou erros ativos no momento. Clique em 'Ler Erros' para fazer uma nova verificação.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(codes) { dtc ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, RaceRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dtc.code,
                                color = RaceRed,
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = dtc.severity,
                                    color = WarningAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dtc.description,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Apagar Códigos da Centralina?") },
            text = {
                Text(
                    "Esta ação irá enviar o comando OBD Modo 04 para limpar a memória de avarias da centralina e desligar a luz do motor (Check Engine).\n\nCertifique-se de que o motor está desligado com a ignição ligada."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaceRed)
                ) {
                    Text("Sim, Apagar Erros")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

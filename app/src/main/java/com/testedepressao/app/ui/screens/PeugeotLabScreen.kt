package com.testedepressao.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedButton
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.DarkSurfaceElevated
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.PeugeotCopper
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber

@Composable
fun PeugeotLabScreen(
    consoleLogs: List<String>,
    onTestSport: () -> Unit,
    onTestCopper: () -> Unit,
    onRestoreCan: () -> Unit,
    onSendRawCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Explanatory info banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface)
                .border(1.dp, PeugeotCopper.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PeugeotCopper)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                text = "Laboratório 508 II (2020): Teste de comutação de temas de cores e injeção de comandos na rede CAN / BSI da carrinha.",
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Test Color Buttons
        Text(
            text = "TESTE DE CORES & MODOS DE CONDUÇÃO",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTestSport,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaceRed, contentColor = CarbonBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Vermelho (Sport)", fontSize = 12.sp)
            }

            Button(
                onClick = onTestCopper,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PeugeotCopper, contentColor = CarbonBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ler Ecrã (Cobre)", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRestoreCan,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp), tint = NeonCyan)
            Text("Restaurar CAN Motor (Turbo / ATSH 7DF)", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw Command Line Console
        Text(
            text = "TERMINAL CAN / UDS / ELM327",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ex: ATSH 7B0, 22 21 00, 010C", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp)
            )

            IconButton(
                onClick = {
                    if (rawInput.isNotBlank()) {
                        onSendRawCommand(rawInput)
                        rawInput = ""
                    }
                },
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Log Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "LOG DA COMUNICAÇÃO OBD:",
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(consoleLogs.reversed()) { log ->
                    val color = when {
                        log.startsWith(">>") -> NeonCyan
                        log.startsWith("<<") -> WarningAmber
                        log.contains("Sport") -> RaceRed
                        else -> TextSecondary
                    }
                    Text(
                        text = log,
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

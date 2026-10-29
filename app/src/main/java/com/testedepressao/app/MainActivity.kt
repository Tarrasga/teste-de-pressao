package com.testedepressao.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.data.models.ConnectionStatus
import com.testedepressao.app.ui.screens.AccelerationScreen
import com.testedepressao.app.ui.screens.BluetoothDeviceDialog
import com.testedepressao.app.ui.screens.DiagnosticScreen
import com.testedepressao.app.ui.screens.LiveTelemetryScreen
import com.testedepressao.app.ui.screens.PeugeotLabScreen
import com.testedepressao.app.ui.screens.SensorsScreen
import com.testedepressao.app.ui.screens.SettingsScreen
import com.testedepressao.app.ui.screens.TripComputerScreen
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.SuccessGreen
import com.testedepressao.app.ui.theme.TesteDePressaoTheme
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handled reactively
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions()

        setContent {
            TesteDePressaoTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showPeugeotLabModal by remember { mutableStateOf(false) }
    var showDiagnosticsModal by remember { mutableStateOf(false) }

    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()

    val telemetry by viewModel.telemetry.collectAsState()
    val allSensors by viewModel.allSensors.collectAsState()
    val accelerationState by viewModel.accelerationState.collectAsState()
    val dtcCodes by viewModel.dtcCodes.collectAsState()
    val isScanningDtc by viewModel.isScanningDtc.collectAsState()
    val psaConsoleLog by viewModel.psaConsoleLog.collectAsState()

    val liveTrip by viewModel.liveTrip.collectAsState()
    val tripHistory by viewModel.tripHistory.collectAsState()

    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()

    // Keep Screen On Effect
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        showPeugeotLabModal -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showPeugeotLabModal = false }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                                }
                                Text(
                                    text = "Laboratório 508",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        showDiagnosticsModal -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showDiagnosticsModal = false }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                                }
                                Text(
                                    text = "Diagnóstico DTC",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            Column {
                                Text(
                                    text = "Teste de Pressão",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDemoMode) "MODO DEMO ATIVO" else (connectedDeviceName ?: "OBD2 Desconectado"),
                                    color = if (isDemoMode) WarningAmber else if (connectionStatus == ConnectionStatus.CONNECTED) SuccessGreen else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Demo mode toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Demo",
                            color = if (isDemoMode) WarningAmber else TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarningAmber,
                                checkedTrackColor = WarningAmber.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                    }

                    // Bluetooth status pill
                    val (statusColor, statusText) = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> SuccessGreen to "OBD LIGADO"
                        ConnectionStatus.CONNECTING -> WarningAmber to "A LIGAR..."
                        ConnectionStatus.ERROR -> RaceRed to "ERRO"
                        ConnectionStatus.DISCONNECTED -> TextSecondary to "LIGAR OBD"
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .clickable { showBluetoothDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonBlack)
            )
        },
        bottomBar = {
            if (!showPeugeotLabModal && !showDiagnosticsModal) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CarbonBlack,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Speed, contentDescription = "Pressão") },
                        label = { Text("Pressão", fontSize = 10.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Viagens") },
                        label = { Text("Viagens", fontSize = 10.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Sensores") },
                        label = { Text("Sensores", fontSize = 10.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "0-100") },
                        label = { Text("0-100", fontSize = 10.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Definições") },
                        label = { Text("Definições", fontSize = 10.sp) },
                        colors = itemColors
                    )
                }
            }
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showPeugeotLabModal -> {
                    PeugeotLabScreen(
                        consoleLogs = psaConsoleLog,
                        onTestSport = { viewModel.testPsaSport() },
                        onTestCopper = { viewModel.testPsaCopper() },
                        onSendRawCommand = { viewModel.sendRawCommand(it) }
                    )
                }
                showDiagnosticsModal -> {
                    DiagnosticScreen(
                        codes = dtcCodes,
                        isScanning = isScanningDtc,
                        onScan = { viewModel.scanDtc() },
                        onClear = { viewModel.clearDtc() }
                    )
                }
                else -> {
                    when (selectedTab) {
                        0 -> LiveTelemetryScreen(
                            telemetry = telemetry,
                            onResetPeak = { viewModel.resetPeakBoost() }
                        )
                        1 -> TripComputerScreen(
                            liveTrip = liveTrip,
                            history = tripHistory,
                            onStartTrip = { viewModel.startTrip() },
                            onStopTrip = { viewModel.stopTrip() },
                            onDeleteTrip = { viewModel.deleteTrip(it) }
                        )
                        2 -> SensorsScreen(
                            sensors = allSensors
                        )
                        3 -> AccelerationScreen(
                            state = accelerationState,
                            onArmTest = { viewModel.armAccelerationTest() }
                        )
                        4 -> SettingsScreen(
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnToggle = { viewModel.setKeepScreenOn(it) },
                            sensors = allSensors,
                            onToggleSensor = { id, enabled -> viewModel.toggleSensor(id, enabled) },
                            currentVersion = viewModel.currentVersionName,
                            updateInfo = updateInfo,
                            isCheckingUpdate = isCheckingUpdate,
                            isDownloadingUpdate = isDownloadingUpdate,
                            downloadProgress = downloadProgress,
                            statusMessage = updateStatusMessage,
                            onCheckUpdate = { viewModel.checkForUpdates() },
                            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(it) },
                            onOpenDiagnostics = { showDiagnosticsModal = true },
                            onOpenPeugeotLab = { showPeugeotLabModal = true }
                        )
                    }
                }
            }
        }
    }

    if (showBluetoothDialog) {
        BluetoothDeviceDialog(
            devices = viewModel.getBondedDevices(),
            currentDeviceAddress = null,
            onSelectDevice = { device ->
                showBluetoothDialog = false
                viewModel.connectDevice(device)
            },
            onDismiss = { showBluetoothDialog = false }
        )
    }
}

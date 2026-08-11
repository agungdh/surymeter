package com.example.surymeter.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.surymeter.data.DailyUsage
import com.example.surymeter.data.NotifStyle
import com.example.surymeter.data.Settings
import com.example.surymeter.data.Speeds
import com.example.surymeter.data.Totals
import com.example.surymeter.meter.MeterUiState

private val DownGreen = Color(0xFF4CAF50)
private val UpOrange = Color(0xFFFF9800)

@Composable
fun HomeScreen(viewModel: UsageViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    var autoStarted by remember { mutableStateOf(false) }
    var canDrawOverlays by remember { mutableStateOf(AndroidSettings.canDrawOverlays(context)) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    Settings.init(context)
    var notifStyle by remember { mutableStateOf(Settings.notifStyle) }
    var useBits by remember { mutableStateOf(Settings.useBits) }
    var showLockscreen by remember { mutableStateOf(Settings.showOnLockscreen) }
    var showSignal by remember { mutableStateOf(Settings.showSignal) }
    var pauseScreenOff by remember { mutableStateOf(Settings.pauseWhenScreenOff) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = AndroidSettings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startService()
    }

    val phoneStateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val needsNotif = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (!autoStarted) {
            autoStarted = true
            if (needsNotif) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.startService()
            }
        }
        if (Build.VERSION.SDK_INT < 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
        val pm = context.getSystemService(PowerManager::class.java)
        showBatteryDialog = !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    if (showBatteryDialog) {
        BatteryDialog(
            onAllow = {
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
                showBatteryDialog = false
            },
            onDismiss = { showBatteryDialog = false }
        )
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header() }
            item { LiveSpeedCard(state) }
            item { TotalsCard(state) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Last 7 Days", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (state.days.isEmpty()) {
                            Text(
                                "No data yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            UsageBarChart(
                                days = state.days,
                                barColor = colorScheme.primary.copy(alpha = 0.35f),
                                todayColor = colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (state.days.isNotEmpty()) {
                item {
                    Text(
                        "Daily History",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.days) { day -> DailyRow(day) }
            }
            if (state.running && !canDrawOverlays) {
                item { OverlayPermissionCard(context) }
            }
            item { SettingsCard(
                notifStyle = notifStyle,
                useBits = useBits,
                showLockscreen = showLockscreen,
                showSignal = showSignal,
                pauseScreenOff = pauseScreenOff,
                onNotifStyleChange = {
                    notifStyle = it
                    Settings.notifStyle = it
                },
                onUseBitsChange = {
                    useBits = it
                    Settings.useBits = it
                },
                onShowLockscreenChange = {
                    showLockscreen = it
                    Settings.showOnLockscreen = it
                },
                onShowSignalChange = {
                    showSignal = it
                    Settings.showSignal = it
                },
                onPauseScreenOffChange = {
                    pauseScreenOff = it
                    Settings.pauseWhenScreenOff = it
                }
            ) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            val granted = Build.VERSION.SDK_INT < 33 ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                if (state.running) viewModel.stopService() else viewModel.startService()
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text(if (state.running) "Stop Metering" else "Start Metering")
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BatteryDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.example.surymeter.R.string.battery_title)) },
        text = { Text(stringResource(com.example.surymeter.R.string.battery_body)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(com.example.surymeter.R.string.battery_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.example.surymeter.R.string.battery_later))
            }
        }
    )
}

@Composable
private fun SettingsCard(
    notifStyle: NotifStyle,
    useBits: Boolean,
    showLockscreen: Boolean,
    showSignal: Boolean,
    pauseScreenOff: Boolean,
    onNotifStyleChange: (NotifStyle) -> Unit,
    onUseBitsChange: (Boolean) -> Unit,
    onShowLockscreenChange: (Boolean) -> Unit,
    onShowSignalChange: (Boolean) -> Unit,
    onPauseScreenOffChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(com.example.surymeter.R.string.settings_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))

            Text(
                stringResource(com.example.surymeter.R.string.settings_notif_style),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            val styles = listOf(
                NotifStyle.SPEED_ONLY to com.example.surymeter.R.string.notif_style_speed,
                NotifStyle.UP_DOWN to com.example.surymeter.R.string.notif_style_updown,
                NotifStyle.NETWORKS to com.example.surymeter.R.string.notif_style_networks,
                NotifStyle.FULL to com.example.surymeter.R.string.notif_style_full
            )
            styles.forEach { (style, labelRes) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = notifStyle == style, onClick = { onNotifStyleChange(style) })
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = notifStyle == style, onClick = { onNotifStyleChange(style) })
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingSwitch(
                label = stringResource(com.example.surymeter.R.string.settings_use_bits),
                summary = stringResource(com.example.surymeter.R.string.settings_use_bits_summary),
                checked = useBits,
                onCheckedChange = onUseBitsChange
            )
            SettingSwitch(
                label = stringResource(com.example.surymeter.R.string.settings_lockscreen),
                summary = stringResource(com.example.surymeter.R.string.settings_lockscreen_summary),
                checked = showLockscreen,
                onCheckedChange = onShowLockscreenChange
            )
            SettingSwitch(
                label = stringResource(com.example.surymeter.R.string.settings_signal),
                summary = stringResource(com.example.surymeter.R.string.settings_signal_summary),
                checked = showSignal,
                onCheckedChange = onShowSignalChange
            )
            SettingSwitch(
                label = stringResource(com.example.surymeter.R.string.settings_pause_screen_off),
                summary = stringResource(com.example.surymeter.R.string.settings_pause_screen_off_summary),
                checked = pauseScreenOff,
                onCheckedChange = onPauseScreenOffChange
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OverlayPermissionCard(context: android.content.Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Show speed in status bar", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Allow \"Display over other apps\" to show the current speed next to the clock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            ) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("SuryMeter", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Internet usage & bandwidth meter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveSpeedCard(state: MeterUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Live Speed", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (state.running) {
                Row {
                    NetworkSpeedColumn(
                        label = "WiFi",
                        speeds = state.speeds,
                        isMobile = false,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkSpeedColumn(
                        label = "Data",
                        speeds = state.speeds,
                        isMobile = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    "Metering is off",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NetworkSpeedColumn(
    label: String,
    speeds: Speeds,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        SpeedLine("↓", if (isMobile) speeds.mobileRx else speeds.wifiRx, DownGreen)
        Spacer(Modifier.height(2.dp))
        SpeedLine("↑", if (isMobile) speeds.mobileTx else speeds.wifiTx, UpOrange)
    }
}

@Composable
private fun SpeedLine(arrow: String, bps: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(arrow, color = color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(Format.speed(bps), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TotalsCard(state: MeterUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Total Since Install", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            TotalRow("WiFi", state.totals.wifiRx, state.totals.wifiTx)
            TotalRow("Data", state.totals.mobileRx, state.totals.mobileTx)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TotalRow("All Networks", state.totals.totalRx, state.totals.totalTx, bold = true)
        }
    }
}

@Composable
private fun TotalRow(label: String, down: Long, up: Long, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "↓ ${Format.bytes(down)}   ↑ ${Format.bytes(up)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DailyRow(day: DailyUsage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(day.date, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "WiFi ↓${Format.bytes(day.wifiRx)} ↑${Format.bytes(day.wifiTx)}  ·  " +
                        "Data ↓${Format.bytes(day.mobileRx)} ↑${Format.bytes(day.mobileTx)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Format.bytes(day.total),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

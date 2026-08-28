package lk.kiu.safewomen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.kiu.safewomen.data.model.EmergencyAlertLog
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiagnosticsScreen(viewModel: MainViewModel) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val alertLogs by viewModel.alertLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Diagnostics Console",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite
                )
                Text(
                    text = "Live telemetry & empirical evaluation logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            IconButton(onClick = { viewModel.refreshLocation() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh GPS",
                    tint = CyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GPS Signal Locator Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS SIGNAL LOCATOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = currentLocation?.provider ?: "STANDBY",
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Latitude Box
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkNavyBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "LATITUDE", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format(Locale.US, "%.5f", currentLocation?.latitude ?: 6.9271),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Longitude Box
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkNavyBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "LONGITUDE", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format(Locale.US, "%.5f", currentLocation?.longitude ?: 79.8612),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fix Precision: ±${String.format(Locale.US, "%.1f", currentLocation?.accuracy ?: 15.0f)} meters",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Outbox Dispatch Log
        Text(
            text = "SIMULATED OUTBOX DISPATCH LOG",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (alertLogs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alerts transmitted yet.\nTrigger the active SOS hold to generate an empirical log.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alertLogs) { log ->
                    DiagnosticLogItem(log)
                }
            }
        }
    }
}

@Composable
fun DiagnosticLogItem(log: EmergencyAlertLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = formatter.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (log.isSuccess) EmeraldGreen else CrimsonPrimary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.triggerType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                }
                Text(
                    text = "$timeStr (${log.totalLatencyMs}ms)",
                    fontSize = 11.sp,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.messagePayload,
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

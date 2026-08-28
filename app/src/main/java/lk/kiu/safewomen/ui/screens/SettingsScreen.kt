package lk.kiu.safewomen.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.kiu.safewomen.data.model.TriggerMode
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isProtectionActive by viewModel.isProtectionActive.collectAsState()
    val triggerMode by viewModel.triggerMode.collectAsState()
    val triggerDurationMs by viewModel.triggerDurationMs.collectAsState()
    val customSmsTemplate by viewModel.customSmsTemplate.collectAsState()

    var editableTemplate by remember(customSmsTemplate) { mutableStateOf(customSmsTemplate) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Application Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Configure background safety parameters & PIN security",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Security & PIN Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SECURITY & ACCESS CONTROL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registered Number: ${viewModel.authManager.registeredPhoneNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPinChangeDialog = true },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset PIN", color = CyanAccent, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.authManager.lockSession()
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock App", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Protection Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Master Background Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "Maintains persistent background monitoring service",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = isProtectionActive,
                    onCheckedChange = { viewModel.toggleProtection(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = CrimsonPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkNavyCardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Mode Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TRIGGER OPTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Volume Key Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Volume Key Listener",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "Trigger via physical button hold (87.9% survey preference)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    RadioButton(
                        selected = triggerMode == TriggerMode.VOLUME_BUTTON_ONLY || triggerMode == TriggerMode.HYBRID_DUAL_TRIGGER,
                        onClick = { viewModel.setTriggerMode(TriggerMode.VOLUME_BUTTON_ONLY) },
                        colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                    )
                }

                Divider(color = DarkNavyCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Screen Press Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Screen Press",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "In-app touch digitizer hold zone fallback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    RadioButton(
                        selected = triggerMode == TriggerMode.SCREEN_PRESS_ONLY,
                        onClick = { viewModel.setTriggerMode(TriggerMode.SCREEN_PRESS_ONLY) },
                        colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activation Duration Slider
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
                        text = "Activation Hold Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                    Text(
                        text = "${(triggerDurationMs / 1000.0).toInt()} Seconds",
                        color = CrimsonSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "59.6% target users selected 3–5 seconds to eliminate accidental pocket alerts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Slider(
                    value = (triggerDurationMs / 1000).toFloat(),
                    onValueChange = { seconds ->
                        viewModel.setTriggerDuration((seconds.toLong() * 1000))
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonPrimary,
                        activeTrackColor = CrimsonPrimary,
                        inactiveTrackColor = DarkNavyCardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SMS Template Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AUTO SOS SMS MESSAGE PATTERN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableTemplate,
                    onValueChange = {
                        editableTemplate = it
                        viewModel.setCustomSmsTemplate(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = DarkNavyCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use %s as placeholder where Google Maps GPS link will be dynamically embedded.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Settings Link Button
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Android Accessibility Permissions")
        }
    }

    if (showPinChangeDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            containerColor = DarkNavyCard,
            title = { Text("Reset 4-Digit PIN", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("New 4-Digit PIN", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = DarkNavyCardBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            viewModel.authManager.resetPin(newPin)
                            showPinChangeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Save PIN", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

package lk.kiu.safewomen.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.ui.viewmodel.MainViewModel
import lk.kiu.safewomen.utils.Constants

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToContacts: () -> Unit
) {
    val context = LocalContext.current
    val isProtectionActive by viewModel.isProtectionActive.collectAsState()
    val guardians by viewModel.guardians.collectAsState()
    val triggerDurationMs by viewModel.triggerDurationMs.collectAsState()
    val isTriggering by viewModel.isTriggering.collectAsState()
    val triggerProgress by viewModel.triggerProgress.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isTouching by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(targetValue = triggerProgress, label = "holdProgress")

    // Handle touch hold timer loop
    LaunchedEffect(isTouching) {
        if (isTouching && !isTriggering) {
            val totalSteps = 30
            val stepDelay = triggerDurationMs / totalSteps
            for (i in 1..totalSteps) {
                if (!isTouching) {
                    viewModel.setTriggerProgress(0f)
                    break
                }
                viewModel.setTriggerProgress(i.toFloat() / totalSteps)
                delay(stepDelay)
            }
            if (isTouching) {
                // Completed hold duration!
                vibrateDevice(context)
                viewModel.triggerEmergencyAlert(triggerType = "SCREEN_TOUCH_HOLD")
            }
        } else if (!isTouching) {
            viewModel.setTriggerProgress(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAFE Women",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextWhite
                )
                Text(
                    text = "Transit Safety Network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }

            // Protection Badge
            Surface(
                color = if (isProtectionActive) EmeraldGreen.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isProtectionActive) EmeraldGreen else AmberWarning
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isProtectionActive) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = if (isProtectionActive) EmeraldGreen else AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isProtectionActive) "Active" else "Inactive",
                        color = if (isProtectionActive) EmeraldGreen else AmberWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isProtectionActive)
                        "Background listener active. Hold volume down for ${(triggerDurationMs / 1000.0).toInt()}s or press touch zone below to trigger alert."
                    else
                        "Protection is paused. Enable background protection in settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Silent Trigger Touch Zone (Digitizer Fallback)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Silent Trigger Interface",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                Text(
                    text = "Press and hold touch zone for ${(triggerDurationMs / 1000.0).toInt()}s to trigger silent emergency reporting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Hold Zone Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CrimsonPrimary.copy(alpha = 0.3f),
                                    DarkNavyBackground
                                )
                            )
                        )
                        .border(
                            width = 3.dp,
                            color = if (isTouching) CrimsonPrimary else CrimsonPrimary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isTouching = true
                                    tryAwaitRelease()
                                    isTouching = false
                                }
                            )
                        }
                ) {
                    // Circular Progress
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(150.dp),
                        color = CrimsonPrimary,
                        strokeWidth = 6.dp,
                        trackColor = Color.Transparent
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Hold Zone",
                            tint = if (isTouching) CrimsonPrimary else TextWhite,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTriggering) "DISPATCHING..." else if (isTouching) "HOLDING..." else "HOLD ZONE",
                            color = if (isTouching) CrimsonPrimary else TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isTriggering) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Acquiring GPS & Dispatching SMS...",
                        color = CrimsonSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Emergency Quick Call Hotline Buttons (119 and 1933)
        Text(
            text = "Emergency Hotlines",
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Police 119
            Button(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Constants.POLICE_EMERGENCY_HOTLINE}"))
                    context.startActivity(dialIntent)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPolice,
                        contentDescription = "Police",
                        tint = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Police (119)",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // NTC 1933
            Button(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Constants.NTC_BUS_SAFETY_HOTLINE}"))
                    context.startActivity(dialIntent)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "NTC",
                        tint = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NTC Bus (1933)",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guardian Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyCardBorder),
            onClick = onNavigateToContacts
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = CrimsonSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Registered Guardians",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "${guardians.size} emergency contacts ready",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Manage",
                    tint = TextMuted
                )
            }
        }
    }
}

private fun vibrateDevice(context: Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(200)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

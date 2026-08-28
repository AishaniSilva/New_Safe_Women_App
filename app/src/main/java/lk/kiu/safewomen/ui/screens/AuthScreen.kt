package lk.kiu.safewomen.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.utils.AuthManager

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit
) {
    val isOnboarded = remember { authManager.isOnboardingCompleted }

    if (!isOnboarded) {
        OnboardingAuthView(
            authManager = authManager,
            onComplete = onAuthSuccess
        )
    } else {
        PinKeypadAuthView(
            authManager = authManager,
            onUnlock = onAuthSuccess
        )
    }
}

@Composable
private fun OnboardingAuthView(
    authManager: AuthManager,
    onComplete: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Shield Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CrimsonPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SAFE Women Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Register your mobile number and set your secure 4-digit safety PIN.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phone Number Input
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Your Mobile Number") },
            placeholder = { Text("07XXXXXXXX") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CrimsonPrimary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Master Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonPrimary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4-Digit PIN Input
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
            label = { Text("4-Digit Quick Unlock PIN") },
            placeholder = { Text("••••") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonPrimary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                color = CrimsonPrimary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (phoneNumber.isBlank() || password.length < 4 || pin.length != 4) {
                    errorMessage = "Please enter valid phone, password (min 4 chars), and 4-digit PIN."
                } else {
                    val success = authManager.registerUser(phoneNumber, password, pin)
                    if (success) {
                        onComplete()
                    } else {
                        errorMessage = "Registration failed. Check your inputs."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Complete Registration & Start Protection",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PinKeypadAuthView(
    authManager: AuthManager,
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var usePasswordFallback by remember { mutableStateOf(false) }
    var fallbackPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Security Notice
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SOS Volume Down Hold is ALWAYS active in background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }
        }

        // Center PIN Dots / Password Input
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (usePasswordFallback) "Enter Master Password" else "Enter Safety PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Registered: ${authManager.registeredPhoneNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = SlateLight
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!usePasswordFallback) {
                // Animated PIN Bullets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isError) CrimsonPrimary
                                    else if (isFilled) CyanAccent
                                    else SlateBorder
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Incorrect PIN. Please try again.",
                        color = CrimsonPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                OutlinedTextField(
                    value = fallbackPassword,
                    onValueChange = { fallbackPassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (authManager.validatePassword(fallbackPassword)) {
                            onUnlock()
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Unlock with Password")
                }
            }
        }

        // Bottom Numeric Keypad (If PIN mode)
        if (!usePasswordFallback) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("ALT", "0", "DEL")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardBg)
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            "ALT" -> {
                                                usePasswordFallback = true
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin
                                                    if (newPin.length == 4) {
                                                        if (authManager.validatePin(newPin)) {
                                                            onUnlock()
                                                        } else {
                                                            isError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White)
                                } else if (key == "ALT") {
                                    Icon(Icons.Default.Key, contentDescription = "Password", tint = SlateLight)
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            TextButton(
                onClick = { usePasswordFallback = false },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Switch back to PIN Keypad", color = CyanAccent)
            }
        }
    }
}

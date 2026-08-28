package lk.kiu.safewomen.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import lk.kiu.safewomen.data.model.Guardian
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.ui.viewmodel.MainViewModel

@Composable
fun ContactsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val guardians by viewModel.guardians.collectAsState()
    var showManualAddDialog by remember { mutableStateOf(false) }

    // Direct Phone Number Contact Picker Launcher
    val phonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            contactUri?.let { uri ->
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    val cursor = context.contentResolver.query(uri, projection, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                            val contactName = if (nameIdx >= 0) c.getString(nameIdx) ?: "Emergency Contact" else "Emergency Contact"
                            val rawPhone = if (phoneIdx >= 0) c.getString(phoneIdx) ?: "" else ""
                            val cleanPhone = rawPhone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

                            if (cleanPhone.isNotBlank()) {
                                viewModel.addGuardian(
                                    name = contactName,
                                    phoneNumber = cleanPhone,
                                    relationship = "Guardian",
                                    isPrimary = guardians.isEmpty()
                                )
                                Toast.makeText(
                                    context,
                                    "Added $contactName ($cleanPhone) as Emergency Guardian!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Could not import contact: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Emergency Guardians",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Active receivers for 3s Volume Down SOS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateLight
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Import from Phonebook & Manual Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    phonePickerLauncher.launch(intent)
                },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick From Contacts", color = DarkNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { showManualAddDialog = true },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Manually", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guardian Counter Badge
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Central Broadcast targets: ${guardians.size} registered guardian(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (guardians.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = SlateBorder,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Guardians Configured",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Pick From Contacts' to add trusted people.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateLight
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(guardians, key = { _, g -> g.id }) { index, guardian ->
                    GuardianItemCard(
                        index = index + 1,
                        guardian = guardian,
                        onDelete = { viewModel.deleteGuardian(guardian) }
                    )
                }
            }
        }
    }

    if (showManualAddDialog) {
        AddGuardianDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { name, phone, relationship, isPrimary ->
                viewModel.addGuardian(name, phone, relationship, isPrimary)
                showManualAddDialog = false
            }
        )
    }
}

@Composable
fun GuardianItemCard(
    index: Int,
    guardian: Guardian,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (guardian.isPrimary) CrimsonPrimary.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$index",
                            fontWeight = FontWeight.Bold,
                            color = if (guardian.isPrimary) CrimsonPrimary else CyanAccent,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = guardian.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (guardian.isPrimary) CrimsonPrimary.copy(alpha = 0.15f) else DarkNavy,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (guardian.isPrimary) "PRIMARY" else guardian.relationship.ifBlank { "GUARDIAN" },
                                color = if (guardian.isPrimary) CrimsonPrimary else CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = guardian.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateLight
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = SlateLight
                )
            }
        }
    }
}

@Composable
fun AddGuardianDialog(
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, relationship: String, isPrimary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var relationship by remember { mutableStateOf("Guardian") }
    var isPrimary by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        title = {
            Text(
                text = "Add Emergency Guardian",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = SlateLight) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = SlateBorder
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+94 / 07...)", color = SlateLight) },
                    singleLine = true,
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = SlateBorder
                    )
                )

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Father, Mother, Sister)", color = SlateLight) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = SlateBorder
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        colors = CheckboxDefaults.colors(checkedColor = CrimsonPrimary)
                    )
                    Text("Designate as Primary Emergency Node", fontSize = 13.sp, color = Color.White)
                }

                if (isError) {
                    Text(
                        text = "Please enter valid name and phone number.",
                        color = CrimsonPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, relationship, isPrimary)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Save Guardian", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateLight)
            }
        }
    )
}

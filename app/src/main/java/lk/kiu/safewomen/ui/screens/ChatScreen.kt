package lk.kiu.safewomen.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.kiu.safewomen.data.model.ChatMessage
import lk.kiu.safewomen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val quickResponses = listOf(
        "🚨 I am in distress, please track me!",
        "🚓 Police 119 has been alerted.",
        "🚌 Bus is currently in transit.",
        "✅ I have reached safely. Cancel alert."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Guardian Incident Feed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Live synchronization with registered guardians", fontSize = 12.sp, color = SlateLight)
                    }
                },
                actions = {
                    IconButton(onClick = onClearChat) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Feed", tint = SlateLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        containerColor = DarkNavy
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Live Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Forum, contentDescription = null, tint = SlateBorder, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No incident messages yet.", color = SlateLight, fontSize = 14.sp)
                                Text("Emergency SOS dispatches will appear here automatically.", color = SlateLight, fontSize = 12.sp)
                            }
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        onOpenMap = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Quick Status Chips
            Text("Quick Incident Status:", style = MaterialTheme.typography.labelSmall, color = SlateLight, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                items(quickResponses) { quickText ->
                    QuickChipItem(
                        text = quickText,
                        onSend = onSendMessage
                    )
                }
            }

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type message to guardians...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkCardBg,
                        unfocusedContainerColor = DarkCardBg
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onOpenMap: (String) -> Unit
) {
    val isSystem = message.senderRole == "SYSTEM_ALERT"
    val isPassenger = message.senderRole == "PASSENGER"

    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = timeFormatter.format(Date(message.timestamp))

    if (isSystem) {
        // Full width emergency card
        Card(
            colors = CardDefaults.cardColors(containerColor = CrimsonDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(message.senderName, fontWeight = FontWeight.Bold, color = CrimsonPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formattedTime, color = SlateLight, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(message.messageText, color = Color.White, fontSize = 13.sp)

                if (message.locationPin != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onOpenMap(message.locationPin) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Live Coordinates on Google Maps", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    } else {
        // Standard Passenger or Guardian Bubble
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isPassenger) Alignment.End else Alignment.Start
        ) {
            Text(
                text = "${message.senderName} • $formattedTime",
                style = MaterialTheme.typography.labelSmall,
                color = SlateLight,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isPassenger) 16.dp else 4.dp,
                            bottomEnd = if (isPassenger) 4.dp else 16.dp
                        )
                    )
                    .background(if (isPassenger) CrimsonPrimary else DarkCardBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.messageText,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuickChipItem(
    text: String,
    onSend: (String) -> Unit
) {
    AssistChip(
        onClick = { onSend(text) },
        label = { Text(text, fontSize = 11.sp, color = Color.White) },
        colors = AssistChipDefaults.assistChipColors(containerColor = DarkCardBg)
    )
}


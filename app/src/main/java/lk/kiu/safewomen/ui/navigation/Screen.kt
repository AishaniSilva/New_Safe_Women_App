package lk.kiu.safewomen.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Auth : Screen("auth", "Security Lock", Icons.Default.Lock)
    object Home : Screen("home", "Protection", Icons.Default.Shield)
    object Contacts : Screen("contacts", "Guardians", Icons.Default.People)
    object Chat : Screen("chat", "Incident Feed", Icons.Default.Forum)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Diagnostics : Screen("diagnostics", "Telemetry", Icons.Default.Analytics)
}

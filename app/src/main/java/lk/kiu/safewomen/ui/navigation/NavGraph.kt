package lk.kiu.safewomen.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import lk.kiu.safewomen.ui.screens.*
import lk.kiu.safewomen.ui.theme.*
import lk.kiu.safewomen.ui.viewmodel.MainViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    var isUnlocked by remember { mutableStateOf(viewModel.authManager.isSessionUnlocked) }

    if (!isUnlocked) {
        AuthScreen(
            authManager = viewModel.authManager,
            onAuthSuccess = { isUnlocked = true }
        )
        return
    }

    val items = listOf(
        Screen.Home,
        Screen.Contacts,
        Screen.Chat,
        Screen.Settings,
        Screen.Diagnostics
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkNavyCard,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextWhite,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToContacts = {
                        navController.navigate(Screen.Contacts.route)
                    }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(viewModel = viewModel)
            }
            composable(Screen.Chat.route) {
                val messages by viewModel.chatMessages.collectAsState()
                ChatScreen(
                    messages = messages,
                    onSendMessage = { text -> viewModel.sendChatMessage(text) },
                    onClearChat = { viewModel.clearChat() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(viewModel = viewModel)
            }
        }
    }
}

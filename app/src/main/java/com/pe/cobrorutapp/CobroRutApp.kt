package com.pe.cobrorutapp

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pe.cobrorutapp.core.domain.preferences.ThemeMode
import com.pe.cobrorutapp.feature.collection.CollectionScreen
import com.pe.cobrorutapp.feature.income.IncomeScreen
import com.pe.cobrorutapp.feature.workday.WorkdayScreen
import com.pe.cobrorutapp.probe.NotificationProbeEntry

private enum class Destination(val route: String, @param:StringRes val label: Int, val symbol: String) {
    WORKDAY("workday", R.string.destination_workday, "≡"),
    COLLECTION("collection", R.string.destination_collection, "S/"),
    INCOME("income", R.string.destination_income, "↓"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroRutApp(state: AppUiState, onCycleTheme: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = onCycleTheme,
                        enabled = state.preferencesReady && !state.isSaving,
                    ) {
                        Text(stringResource(when (state.themeMode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        }))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.symbol) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (state.preferencesError) {
                Text(
                    text = stringResource(R.string.preferences_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            NavHost(navController = navController, startDestination = Destination.WORKDAY.route) {
                composable(Destination.WORKDAY.route) { WorkdayScreen() }
                composable(Destination.COLLECTION.route) { CollectionScreen() }
                composable(Destination.INCOME.route) {
                    Column {
                        NotificationProbeEntry()
                        IncomeScreen(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

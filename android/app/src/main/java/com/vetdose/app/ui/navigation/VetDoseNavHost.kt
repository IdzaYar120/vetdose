package com.vetdose.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vetdose.app.R
import com.vetdose.app.ui.screens.calculate.CalculateScreen
import com.vetdose.app.ui.screens.history.HistoryScreen
import com.vetdose.app.ui.screens.result.ResultScreen
import com.vetdose.app.ui.screens.search.ProductSearchScreen
import com.vetdose.app.ui.screens.settings.SettingsScreen

private data class TopLevelDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(VetDoseDestinations.CALCULATE, R.string.nav_calculate, Icons.Default.Calculate),
    TopLevelDestination(VetDoseDestinations.HISTORY, R.string.nav_history, Icons.Default.History),
    TopLevelDestination(VetDoseDestinations.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetDoseNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle(currentRoute)) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VetDoseDestinations.CALCULATE,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(VetDoseDestinations.CALCULATE) { entry ->
                CalculateScreen(
                    onNavigateToProductSearch = { speciesId ->
                        navController.navigate(VetDoseDestinations.productSearch(speciesId))
                    },
                    onNavigateToResult = { speciesId, doseRuleId, productId, weightKg ->
                        navController.navigate(VetDoseDestinations.result(speciesId, doseRuleId, productId, weightKg))
                    },
                    onNavigateToSettings = { navController.navigate(VetDoseDestinations.SETTINGS) },
                    savedStateHandle = entry.savedStateHandle,
                )
            }
            composable(VetDoseDestinations.PRODUCT_SEARCH_ROUTE) {
                ProductSearchScreen(
                    onProductChosen = { product ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_product_id", product.id)
                        navController.popBackStack()
                    },
                )
            }
            composable(VetDoseDestinations.RESULT_ROUTE) {
                ResultScreen()
            }
            composable(VetDoseDestinations.HISTORY) {
                HistoryScreen(
                    onEntryClicked = { speciesId, doseRuleId, productId, weightKg ->
                        navController.navigate(VetDoseDestinations.result(speciesId, doseRuleId, productId, weightKg))
                    },
                )
            }
            composable(VetDoseDestinations.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun topBarTitle(route: String?): String = when (route) {
    VetDoseDestinations.CALCULATE -> stringResource(R.string.app_name)
    VetDoseDestinations.HISTORY -> stringResource(R.string.nav_history)
    VetDoseDestinations.SETTINGS -> stringResource(R.string.nav_settings)
    VetDoseDestinations.PRODUCT_SEARCH_ROUTE -> stringResource(R.string.search_title)
    VetDoseDestinations.RESULT_ROUTE -> stringResource(R.string.result_title)
    else -> stringResource(R.string.app_name)
}

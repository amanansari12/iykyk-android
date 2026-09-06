package com.amanansari.iykyk.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.amanansari.iykyk.ui.component.TopBar
import com.amanansari.iykyk.ui.screen.HomeScreen
import com.amanansari.iykyk.ui.screen.ProcessingScreen
import com.amanansari.iykyk.ui.theme.Background
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.amanansari.iykyk.ui.screen.ResultScreen
import com.amanansari.iykyk.ui.viewmodel.ProcessingViewModel

@Composable
fun NavGraph(){

    val navController = rememberNavController()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val processingViewModel: ProcessingViewModel = hiltViewModel()

    val currentDestination = currentBackStackEntry?.destination

    val title = when {
        currentDestination?.hasRoute<Home>() == true -> "HomeScreen"
        currentDestination?.hasRoute<Processing>() == true -> "Processing"
        currentDestination?.hasRoute<Results>() == true -> "Results"
        else -> ""
    }

    val isProcessingScreen =
        currentDestination?.hasRoute<Processing>() == true

    val isResultsScreen = currentDestination?.hasRoute<Results>() == true

    val showBackButton =
        currentDestination?.hasRoute<Home>() != true


    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopBar(
                title = title,
                showBackButton = showBackButton,
                onBackClick = {
                    if (isProcessingScreen || isResultsScreen) {
                        navController.navigate(Home) {
                            popUpTo(Home) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding)
        ){
            composable<Home> {
                HomeScreen(
                    onVideoSelected = { uri ->
                        navController.navigate(Processing(uri.toString()))
                    }
                )
            }

            composable<Processing> { backStackEntry ->

                val uriStr = backStackEntry.toRoute<Processing>()

                ProcessingScreen(
                    uri = uriStr.uri.toUri(),
                    viewModel = processingViewModel,
                    onCancel = {
                        navController.navigate(Home) {
                            popUpTo(Home) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onCompleted = {
                        navController.navigate(Results) {
                            // Drop Processing off the back stack so "back"
                            // from Results goes straight to Home, not to a
                            // stale completed-processing screen.
                            popUpTo<Processing> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }


            composable<Results> {
                ResultScreen(
                    viewModel = processingViewModel,
                    onCancel = {
                        navController.navigate(Home) {
                            popUpTo(Home) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },)
            }

        }

    }


}
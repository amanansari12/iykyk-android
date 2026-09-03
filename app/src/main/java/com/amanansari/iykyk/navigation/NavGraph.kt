package com.amanansari.iykyk.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun NavGraph(){

    val navController = rememberNavController()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val currentDestination = currentBackStackEntry?.destination

    val title = when {
        currentDestination?.hasRoute<Home>() == true -> "HomeScreen"
        currentDestination?.hasRoute<Results>() == true -> "Results"
        else -> ""
    }

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
                    navController.popBackStack()
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

                ProcessingScreen(uri = Uri.parse(uriStr.uri))
            }
        }

    }


}
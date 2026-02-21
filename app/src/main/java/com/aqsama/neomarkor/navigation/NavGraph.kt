package com.aqsama.neomarkor.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aqsama.neomarkor.ui.screen.DashboardScreen
import com.aqsama.neomarkor.ui.screen.EditorScreen
import com.aqsama.neomarkor.ui.screen.FileBrowserScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FileBrowser : Screen("file_browser")
    object Editor : Screen("editor/{filePath}") {
        fun createRoute(filePath: String) = "editor/${filePath.replace("/", "|")}"
    }
}

@Composable
fun NeoMarkorNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenFileBrowser = { navController.navigate(Screen.FileBrowser.route) },
                onOpenEditor = { filePath ->
                    navController.navigate(Screen.Editor.createRoute(filePath))
                }
            )
        }
        composable(Screen.FileBrowser.route) {
            FileBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenEditor = { filePath ->
                    navController.navigate(Screen.Editor.createRoute(filePath))
                }
            )
        }
        composable(Screen.Editor.route) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = encodedPath.replace("|", "/")
            EditorScreen(
                filePath = filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.aqsama.neomarkor.navigation

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aqsama.neomarkor.ui.screen.DashboardScreen
import com.aqsama.neomarkor.ui.screen.EditorScreen
import com.aqsama.neomarkor.ui.screen.FileBrowserScreen
import com.aqsama.neomarkor.ui.screen.ManageFoldersScreen
import com.aqsama.neomarkor.ui.screen.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FileBrowser : Screen("file_browser")
    object Settings : Screen("settings")
    object ManageFolders : Screen("manage_folders")
    object Editor : Screen("editor/{filePath}") {
        /**
         * Encode [filePath] with URL-safe Base64 so that SAF content:// URIs —
         * which contain characters (:, /, %) that Navigation Compose decodes when
         * extracting path arguments — survive the round-trip intact.
         */
        fun createRoute(filePath: String): String {
            val encoded = Base64.encodeToString(
                filePath.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP,
            )
            return "editor/$encoded"
        }
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
                },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenManageFolders = { navController.navigate(Screen.ManageFolders.route) },
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
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.ManageFolders.route) {
            ManageFoldersScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Editor.route) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = try {
                if (encodedPath.isEmpty()) "" else {
                    String(
                        Base64.decode(encodedPath, Base64.URL_SAFE or Base64.NO_WRAP),
                        Charsets.UTF_8,
                    )
                }
            } catch (_: IllegalArgumentException) {
                encodedPath
            }
            EditorScreen(
                filePath = filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

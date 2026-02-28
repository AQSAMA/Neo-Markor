package com.aqsama.neomarkor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.navigation.NeoMarkorNavGraph
import com.aqsama.neomarkor.ui.theme.NeoMarkorTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val storagePreferences: StoragePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by storagePreferences.observeThemeMode().collectAsState(initial = 0)
            val accentColor by storagePreferences.observeAccentColor().collectAsState(initial = 0)
            val dynamicColor by storagePreferences.observeDynamicColor().collectAsState(initial = true)

            NeoMarkorTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentColorArgb = accentColor,
            ) {
                val navController = rememberNavController()
                NeoMarkorNavGraph(navController = navController)
            }
        }
    }
}

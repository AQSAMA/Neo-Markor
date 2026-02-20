package com.aqsama.neomarkor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.aqsama.neomarkor.navigation.NeoMarkorNavGraph
import com.aqsama.neomarkor.ui.theme.NeoMarkorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeoMarkorTheme {
                val navController = rememberNavController()
                NeoMarkorNavGraph(navController = navController)
            }
        }
    }
}

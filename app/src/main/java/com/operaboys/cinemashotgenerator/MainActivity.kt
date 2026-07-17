package com.operaboys.cinemashotgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.operaboys.cinemashotgenerator.ui.MainScreen
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CinemaShotGeneratorTheme {
                MainScreen()
            }
        }
    }
}

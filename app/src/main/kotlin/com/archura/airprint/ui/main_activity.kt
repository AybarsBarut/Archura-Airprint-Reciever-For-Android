package com.archura.airprint.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.archura.airprint.ui.navigation.AirPrintNavigation
import com.archura.airprint.ui.theme.AirPrintTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirPrintTheme {
                AirPrintNavigation()
            }
        }
    }
}

package com.dramix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dramix.app.ui.navigation.AppNavigation
import com.dramix.app.ui.theme.DramixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DramixTheme {
                AppNavigation()
            }
        }
    }
}

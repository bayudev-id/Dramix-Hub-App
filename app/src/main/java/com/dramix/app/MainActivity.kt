package com.dramix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dramix.app.core.security.SecurityManager
import com.dramix.app.ui.navigation.AppNavigation
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.DramixTheme
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val securityManager: SecurityManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isCompromised = securityManager.isDeviceCompromised()
        val threats = if (isCompromised) securityManager.getDetectedThreats() else emptyList()

        setContent {
            DramixTheme {
                var showWarningDialog by remember { mutableStateOf(isCompromised) }

                AppNavigation()

                if (showWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showWarningDialog = false },
                        containerColor = MidnightCard,
                        titleContentColor = CrimsonPlay,
                        textContentColor = Slate400,
                        title = {
                            Text(text = "Peringatan Keamanan")
                        },
                        text = {
                            Text(
                                text = "Terdeteksi modifikasi lingkungan/root/hooking pada perangkat:\n" +
                                    threats.joinToString(", ") +
                                    "\n\nBeberapa fitur proteksi stream mungkin dibatasi."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { showWarningDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPlay)
                            ) {
                                Text(text = "Mengerti", color = Slate50)
                            }
                        }
                    )
                }
            }
        }
    }
}

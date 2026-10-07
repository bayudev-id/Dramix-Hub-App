package com.dramix.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@Composable
fun LicenseActivationCard(
    licenseKey: String,
    onLicenseKeyChange: (String) -> Unit,
    isActivating: Boolean,
    onActivateClick: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightCard)
            .border(1.dp, MidnightBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CrimsonPlay.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Lisensi",
                        tint = CrimsonPlay,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Aktivasi Lisensi VIP",
                    color = Slate50,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = licenseKey,
                onValueChange = onLicenseKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Masukkan Kode Lisensi (LCN-XXXX)",
                        color = Slate400,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (licenseKey.isNotBlank() && !isActivating) {
                            onActivateClick()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPlay,
                    unfocusedBorderColor = MidnightBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Slate50,
                    cursorColor = CrimsonPlay
                ),
                shape = RoundedCornerShape(8.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = CrimsonPlay,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    onActivateClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = licenseKey.isNotBlank() && !isActivating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPlay,
                    disabledContainerColor = CrimsonPlay.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Aktivasi Sekarang",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Belum punya lisensi? Dapatkan lisensi resmi via reseller atau admin.",
                color = Slate400,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

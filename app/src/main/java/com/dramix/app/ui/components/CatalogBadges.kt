package com.dramix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.GoldVip
import com.dramix.app.ui.theme.TagBadgeShape

@Composable
fun VipBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(TagBadgeShape)
            .background(GoldVip)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "VIP",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SewaBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(TagBadgeShape)
            .background(CrimsonPlay)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "SEWA",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

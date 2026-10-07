package com.dramix.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DramixShapes = Shapes(
    small = RoundedCornerShape(6.dp),   // Episode number button grid
    medium = RoundedCornerShape(8.dp),  // Catalog poster card
    large = RoundedCornerShape(16.dp), // BottomSheet & Dialogs
    extraLarge = RoundedCornerShape(24.dp)
)

val BottomSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
val EpisodeCardShape = RoundedCornerShape(8.dp)
val EpisodeNumberButtonShape = RoundedCornerShape(6.dp)
val TagBadgeShape = RoundedCornerShape(4.dp)

package com.dramix.app.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ensures minimum touch target size according to Android Accessibility guidelines (48dp x 48dp).
 */
fun Modifier.touchTargetMin(size: Dp = 48.dp): Modifier {
    return this.defaultMinSize(minWidth = size, minHeight = size)
}

object SpacingTokens {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

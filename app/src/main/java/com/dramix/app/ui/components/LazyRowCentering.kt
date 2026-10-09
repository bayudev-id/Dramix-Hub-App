package com.dramix.app.ui.components

import androidx.compose.foundation.lazy.LazyListState

/**
 * Scrolls a horizontal LazyRow so the item at [index] centers on screen.
 * Uses fixed offset -200 for consistent centering across all screen sizes.
 */
suspend fun LazyListState.animateToCentered(index: Int) {
    if (index < 0 || index >= layoutInfo.totalItemsCount) return
    animateScrollToItem(index, scrollOffset = -200)
}
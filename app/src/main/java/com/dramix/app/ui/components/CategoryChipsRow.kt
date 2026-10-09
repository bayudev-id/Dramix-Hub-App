package com.dramix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.domain.model.Category
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50

@Composable
fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onOpenCategorySheet: (() -> Unit)? = null
) {
    if (isLoading) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(5) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .size(width = 80.dp, height = 30.dp),
                    cornerRadius = 15.dp
                )
            }
        }
        return
    }

    if (categories.isEmpty()) return

    val listState = rememberLazyListState()

    // Auto-scroll to selected category (centered)
    LaunchedEffect(selectedCategoryId) {
        selectedCategoryId?.let { selected ->
            val selectedIndex = categories.indexOfFirst { it.id == selected }
            if (selectedIndex >= 0) {
                listState.animateToCentered(selectedIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id == selectedCategoryId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) CrimsonPlay.copy(alpha = 0.22f)
                        else MidnightCard
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CrimsonPlay else MidnightBorder.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.name,
                    color = if (isSelected) CrimsonPlay else Slate400,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        if (categories.size > 8 && onOpenCategorySheet != null) {
            item(key = "more_categories") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCard)
                        .border(
                            width = 1.dp,
                            color = MidnightBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onOpenCategorySheet() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Lainnya",
                            color = Slate50,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

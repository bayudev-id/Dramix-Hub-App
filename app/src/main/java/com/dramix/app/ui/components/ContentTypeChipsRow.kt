package com.dramix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.screens.home.ContentTypeOption
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.Slate400

@Composable
fun ContentTypeChipsRow(
    contentTypes: List<ContentTypeOption>,
    selectedContentType: String?,
    onContentTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (contentTypes.isEmpty()) return

    val listState = rememberLazyListState()

    // Auto-scroll to selected item (centered)
    LaunchedEffect(selectedContentType) {
        selectedContentType?.let { selected ->
            val selectedIndex = contentTypes.indexOfFirst { it.id == selected }
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
        items(contentTypes, key = { it.id }) { option ->
            val isSelected = option.id == selectedContentType

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
                    .clickable { onContentTypeSelected(option.id) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                val labelText = if (option.count > 0) "${option.label} (${option.count})" else option.label
                Text(
                    text = labelText,
                    color = if (isSelected) CrimsonPlay else Slate400,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

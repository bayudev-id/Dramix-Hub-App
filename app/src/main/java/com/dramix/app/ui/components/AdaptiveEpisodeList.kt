package com.dramix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dramix.app.domain.model.Episode
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.MidnightBorder
import com.dramix.app.ui.theme.MidnightCard
import com.dramix.app.ui.theme.Slate400
import com.dramix.app.ui.theme.Slate50
import com.dramix.app.ui.theme.TagBadgeShape

@Composable
fun AdaptiveEpisodeList(
    episodes: List<Episode>,
    activeEpisodeNumber: Int,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (episodes.isEmpty()) return

    val hasThumbnails = episodes.any { !it.cover.isNullOrBlank() }

    if (hasThumbnails) {
        // Horizontal 16:9 thumbnail carousel
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(episodes, key = { it.id }) { episode ->
                val isActive = episode.number == activeEpisodeNumber
                EpisodeThumbnailCard(
                    episode = episode,
                    isActive = isActive,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    } else {
        // 5-Column Grid with minimum 48dp touch target
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val rows = episodes.chunked(5)
            rows.forEach { rowEpisodes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowEpisodes.forEach { episode ->
                        val isActive = episode.number == activeEpisodeNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) CrimsonPlay else MidnightCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) CrimsonPlay else MidnightBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onEpisodeClick(episode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${episode.number}",
                                color = if (isActive) Color.White else Slate50,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )

                            if (episode.isVip || episode.number >= 4) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "VIP",
                                    tint = if (isActive) Color.White.copy(alpha = 0.8f) else Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                )
                            }
                        }
                    }

                    // Fill empty slots if row has fewer than 5 items
                    repeat(5 - rowEpisodes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeThumbnailCard(
    episode: Episode,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightCard)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) CrimsonPlay else MidnightBorder,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            AsyncImage(
                model = episode.cover,
                contentDescription = episode.title ?: "Episode ${episode.number}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (episode.isVip || episode.number >= 4) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(TagBadgeShape)
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIP",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = episode.title ?: "Episode ${episode.number}",
            color = if (isActive) CrimsonPlay else Slate50,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.dramix.app.domain.model

data class SearchFeedPage(
    val items: List<VideoItem>,
    val hasMore: Boolean,
    val currentPage: Int
)

package com.dramix.app.domain.model

import android.util.Base64
import org.json.JSONObject

data class LiveTvChannelMeta(
    val rawId: String,
    val title: String,
    val cover: String? = null,
    val categoryCode: String? = null,
    val channelId: String? = null,
    val categoryName: String? = null,
    val matchSchedule: String? = null,
    val matchTeams: String? = null,
    val team1: String? = null,
    val team2: String? = null,
    val tournamentName: String? = null,
    val subName: String? = null,
    val streamType: String? = null,
    val isLive: Boolean = false,
    val isPremium: Boolean = false,
    val playbackEpisodeId: String = rawId,
    val isEventMatch: Boolean = false
)

fun VideoItem.toLiveTvMeta(): LiveTvChannelMeta {
    if (id.startsWith("bittvd_")) {
        try {
            val b64Payload = id.substringAfter("bittvd_")
            val decodedBytes = Base64.decode(b64Payload, Base64.DEFAULT)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))

            val cc = json.optString("cc").trim()
            val cid = json.optString("cid").trim()
            val desc = json.optString("d").trim()
            val name = json.optString("n").trim().ifEmpty { title }
            val subName = json.optString("ns").trim()
            val tech = json.optString("j").trim()
            val catName = json.optString("cn").trim()
            val isLive = json.optBoolean("lv", false)
            val isPremium = json.optBoolean("pr", false)

            val resolvedEpisodeId = if (cc.isNotEmpty() && cid.isNotEmpty()) {
                "bittv:$cc:$cid"
            } else {
                id
            }

            var schedule: String? = null
            var teams: String? = null

            if (desc.contains(" - ")) {
                val parts = desc.split(" - ", limit = 2)
                schedule = parts[0].trim().ifEmpty { null }
                teams = parts[1].trim().ifEmpty { null }
            } else if (desc.isNotEmpty()) {
                // If no date separator, it could be a category name like "News/Berita" or "Entertainment"
                teams = null
            }

            var team1: String? = null
            var team2: String? = null
            if (teams != null && teams.contains(" vs ", ignoreCase = true)) {
                val vsSplit = teams.split(Regex(" (?i)vs "), limit = 2)
                team1 = vsSplit.getOrNull(0)?.trim()?.ifEmpty { null }
                team2 = vsSplit.getOrNull(1)?.trim()?.ifEmpty { null }
            }

            val isEvent = cc.equals("EV", ignoreCase = true) ||
                catName.equals("Events", ignoreCase = true) ||
                teams != null

            val techDisplay = when (tech.lowercase()) {
                "ts" -> "TS"
                "dash-clearkey" -> "ClearKey"
                "dash" -> "DASH"
                "hls" -> "HLS"
                "drm_rcti_plus" -> "RCTI+ DRM"
                "rcti_plus" -> "RCTI+"
                else -> tech.uppercase().ifEmpty { null }
            }

            return LiveTvChannelMeta(
                rawId = id,
                title = name,
                cover = cover,
                categoryCode = cc.ifEmpty { null },
                channelId = cid.ifEmpty { null },
                categoryName = catName.ifEmpty { null },
                matchSchedule = schedule ?: views?.ifEmpty { null },
                matchTeams = teams,
                team1 = team1,
                team2 = team2,
                tournamentName = subName.ifEmpty { null },
                subName = if (desc.isNotEmpty() && teams == null) desc else subName.ifEmpty { null },
                streamType = techDisplay,
                isLive = isLive || score.equals("LIVE", ignoreCase = true),
                isPremium = isPremium || isVip,
                playbackEpisodeId = resolvedEpisodeId,
                isEventMatch = isEvent
            )
        } catch (_: Exception) {
            // Fallthrough to standard mapping
        }
    }

    // Fallback for standard VideoItem
    val isLiveScore = score.equals("LIVE", ignoreCase = true)
    return LiveTvChannelMeta(
        rawId = id,
        title = title,
        cover = cover,
        categoryCode = null,
        channelId = null,
        categoryName = tags.firstOrNull(),
        matchSchedule = views?.ifEmpty { null } ?: if (isLiveScore) "LIVE" else null,
        matchTeams = null,
        team1 = null,
        team2 = null,
        tournamentName = episodeInfo,
        subName = episodeInfo ?: tags.firstOrNull(),
        streamType = null,
        isLive = isLiveScore,
        isPremium = isVip || tags.any { it.equals("VIP", ignoreCase = true) },
        playbackEpisodeId = id,
        isEventMatch = false
    )
}

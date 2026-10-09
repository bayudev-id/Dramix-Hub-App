package com.dramix.app.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig

class PlayerPreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "dramix_player_preferences"
        private const val KEY_VIDEO_ZOOM = "pref_video_zoom"
        private const val KEY_PLAYBACK_SPEED = "pref_playback_speed"
        private const val KEY_AUTO_NEXT = "pref_auto_next"
        private const val KEY_PREFERRED_QUALITY = "pref_preferred_quality"
        private const val KEY_PREFERRED_SUBTITLE_ID = "pref_preferred_subtitle_id"

        // Subtitle style keys
        private const val KEY_SUB_FONT_FAMILY = "pref_sub_font_family"
        private const val KEY_SUB_OUTLINE_STYLE = "pref_sub_outline_style"
        private const val KEY_SUB_FONT_SIZE_PX = "pref_sub_font_size_px"
        private const val KEY_SUB_POSITION_PERCENT = "pref_sub_position_percent"
        private const val KEY_SUB_BG_OPACITY = "pref_sub_bg_opacity"
        private const val KEY_SUB_TEXT_COLOR = "pref_sub_text_color"
        private const val KEY_SUB_BASE_BG_COLOR = "pref_sub_base_bg_color"
    }

    fun getVideoZoom(): String = prefs.getString(KEY_VIDEO_ZOOM, "100%") ?: "100%"

    fun saveVideoZoom(zoom: String) {
        prefs.edit().putString(KEY_VIDEO_ZOOM, zoom).apply()
    }

    fun getPlaybackSpeed(): Float = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)

    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }

    fun isAutoNext(): Boolean = prefs.getBoolean(KEY_AUTO_NEXT, false)

    fun saveAutoNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_NEXT, enabled).apply()
    }

    fun getPreferredQuality(): String? = prefs.getString(KEY_PREFERRED_QUALITY, null)

    fun savePreferredQuality(quality: String?) {
        prefs.edit().putString(KEY_PREFERRED_QUALITY, quality).apply()
    }

    fun getPreferredSubtitleId(): String? = prefs.getString(KEY_PREFERRED_SUBTITLE_ID, null)

    fun savePreferredSubtitleId(subId: String?) {
        prefs.edit().putString(KEY_PREFERRED_SUBTITLE_ID, subId).apply()
    }

    fun getSubtitleStyle(isFullscreen: Boolean = false): SubtitleStyleConfig {
        val prefix = if (isFullscreen) "pref_sub_fs_" else "pref_sub_pt_"
        val defaultSize = if (isFullscreen) 28 else 20
        return SubtitleStyleConfig(
            fontSizePx = prefs.getInt(prefix + "font_size_px", prefs.getInt(KEY_SUB_FONT_SIZE_PX, defaultSize)),
            positionPercent = prefs.getInt(prefix + "position_percent", prefs.getInt(KEY_SUB_POSITION_PERCENT, 10)),
            backgroundOpacityPercent = prefs.getInt(prefix + "bg_opacity", prefs.getInt(KEY_SUB_BG_OPACITY, 50)),
            backgroundPaddingPx = prefs.getInt(prefix + "bg_padding_px", 16),
            lineSpacingPx = prefs.getInt(prefix + "line_spacing_px", 4),
            textColor = prefs.getLong(prefix + "text_color", prefs.getLong(KEY_SUB_TEXT_COLOR, 0xFFFFFFFFL)),
            baseBackgroundColor = prefs.getLong(prefix + "base_bg_color", prefs.getLong(KEY_SUB_BASE_BG_COLOR, 0xFF000000L)),
            fontFamily = prefs.getString(prefix + "font_family", prefs.getString(KEY_SUB_FONT_FAMILY, "Comic Sans MS")) ?: "Comic Sans MS",
            outlineStyle = prefs.getString(prefix + "outline_style", prefs.getString(KEY_SUB_OUTLINE_STYLE, "Thin")) ?: "Thin"
        )
    }

    fun saveSubtitleStyle(style: SubtitleStyleConfig, isFullscreen: Boolean = false) {
        val prefix = if (isFullscreen) "pref_sub_fs_" else "pref_sub_pt_"
        prefs.edit()
            .putInt(prefix + "font_size_px", style.fontSizePx)
            .putInt(prefix + "position_percent", style.positionPercent)
            .putInt(prefix + "bg_opacity", style.backgroundOpacityPercent)
            .putInt(prefix + "bg_padding_px", style.backgroundPaddingPx)
            .putInt(prefix + "line_spacing_px", style.lineSpacingPx)
            .putLong(prefix + "text_color", style.textColor)
            .putLong(prefix + "base_bg_color", style.baseBackgroundColor)
            .putString(prefix + "font_family", style.fontFamily)
            .putString(prefix + "outline_style", style.outlineStyle)
            .putInt(KEY_SUB_FONT_SIZE_PX, style.fontSizePx)
            .putInt(KEY_SUB_POSITION_PERCENT, style.positionPercent)
            .putInt(KEY_SUB_BG_OPACITY, style.backgroundOpacityPercent)
            .putLong(KEY_SUB_TEXT_COLOR, style.textColor)
            .putLong(KEY_SUB_BASE_BG_COLOR, style.baseBackgroundColor)
            .putString(KEY_SUB_FONT_FAMILY, style.fontFamily)
            .putString(KEY_SUB_OUTLINE_STYLE, style.outlineStyle)
            .apply()
    }
}

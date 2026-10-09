package com.dramix.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig
import com.dramix.app.ui.screens.player_vod.SubtitleUiModel
import com.dramix.app.ui.theme.CrimsonPlay

private enum class SettingsMenuScreen {
    MAIN,
    SPEED,
    SUBTITLES,
    QUALITY,
    SUBTITLE_STYLE,
    SUBTITLE_FONT,
    SUBTITLE_OUTLINE
}

private val MenuBackground = Color(0xF2101012)
private val MenuBorder = Color(0xFF26262A)
private val MutedText = Color(0xFF9E9EA6)
private val ChevronTint = Color(0xFF7E7E86)
private val DividerColor = Color(0xFF222226)

/**
 * In-player settings popover menu matching OTT player reference design.
 * Anchored to the bottom-right above player controls with scrollable content in portrait mode.
 */
@Composable
fun PlayerSettingsMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    qualities: List<String>,
    selectedQuality: String?,
    onSelectQuality: (String) -> Unit,
    subtitles: List<SubtitleUiModel>,
    selectedSubtitleId: String,
    onSelectSubtitle: (String) -> Unit,
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    isAutoNext: Boolean = false,
    onToggleAutoNext: (Boolean) -> Unit = {},
    videoZoom: String = "100%",
    onUpdateZoom: (Int) -> Unit = {},
    subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    onSelectFontFamily: (String) -> Unit = {},
    onSelectOutlineStyle: (String) -> Unit = {},
    onUpdateFontSize: (Int) -> Unit = {},
    onUpdatePosition: (Int) -> Unit = {},
    onUpdateBgOpacity: (Int) -> Unit = {},
    onSetBgOpacity: (Int) -> Unit = {},
    onUpdateLineSpacing: (Int) -> Unit = {},
    onUpdateBgPadding: (Int) -> Unit = {},
    onUpdateTextColor: (Long) -> Unit = {},
    onUpdateBgColor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(SettingsMenuScreen.MAIN) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            currentScreen = SettingsMenuScreen.MAIN
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val isPortraitPlayer = maxHeight < 320.dp
            val menuMaxHeight = if (isPortraitPlayer) {
                (maxHeight - 44.dp).coerceAtLeast(140.dp)
            } else {
                280.dp
            }

            // Compact floating menu card anchored to bottom end
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 44.dp, end = 12.dp)
                    .widthIn(min = 205.dp, max = 235.dp)
                    .heightIn(max = menuMaxHeight)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(MenuBackground)
                    .border(BorderStroke(1.dp, MenuBorder), RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent tap propagation */ }
                    )
            ) {
                when (currentScreen) {
                    SettingsMenuScreen.MAIN -> {
                        MainMenuContent(
                            currentSpeed = playbackSpeed,
                            onNavigateSpeed = { currentScreen = SettingsMenuScreen.SPEED },
                            subtitles = subtitles,
                            selectedSubtitleId = selectedSubtitleId,
                            onNavigateSubtitles = { currentScreen = SettingsMenuScreen.SUBTITLES },
                            onNavigateSubtitleStyle = { currentScreen = SettingsMenuScreen.SUBTITLE_STYLE },
                            selectedQuality = selectedQuality,
                            onNavigateQuality = { currentScreen = SettingsMenuScreen.QUALITY },
                            videoZoom = videoZoom,
                            onUpdateZoom = onUpdateZoom,
                            isAutoNext = isAutoNext,
                            onToggleAutoNext = onToggleAutoNext
                        )
                    }

                    SettingsMenuScreen.QUALITY -> {
                        QualitySubMenuContent(
                            qualities = qualities,
                            selectedQuality = selectedQuality,
                            onSelectQuality = { q ->
                                onSelectQuality(q)
                            },
                            onBack = { currentScreen = SettingsMenuScreen.MAIN }
                        )
                    }

                    SettingsMenuScreen.SUBTITLES -> {
                        SubtitlesSubMenuContent(
                            subtitles = subtitles,
                            selectedSubtitleId = selectedSubtitleId,
                            onSelectSubtitle = { sId ->
                                onSelectSubtitle(sId)
                            },
                            onBack = { currentScreen = SettingsMenuScreen.MAIN }
                        )
                    }

                    SettingsMenuScreen.SPEED -> {
                        SpeedSubMenuContent(
                            currentSpeed = playbackSpeed,
                            onSelectSpeed = { spd ->
                                onSelectSpeed(spd)
                            },
                            onBack = { currentScreen = SettingsMenuScreen.MAIN }
                        )
                    }

                    SettingsMenuScreen.SUBTITLE_STYLE -> {
                        SubtitleStyleSubMenuContent(
                            subtitleStyle = subtitleStyle,
                            onNavigateFont = { currentScreen = SettingsMenuScreen.SUBTITLE_FONT },
                            onNavigateOutline = { currentScreen = SettingsMenuScreen.SUBTITLE_OUTLINE },
                            onUpdateFontSize = onUpdateFontSize,
                            onUpdatePosition = onUpdatePosition,
                            onUpdateBgOpacity = onUpdateBgOpacity,
                            onUpdateLineSpacing = onUpdateLineSpacing,
                            onUpdateBgPadding = onUpdateBgPadding,
                            onUpdateTextColor = onUpdateTextColor,
                            onUpdateBgColor = onUpdateBgColor,
                            onBack = { currentScreen = SettingsMenuScreen.MAIN }
                        )
                    }

                    SettingsMenuScreen.SUBTITLE_FONT -> {
                        FontSubMenuContent(
                            currentFont = subtitleStyle.fontFamily,
                            onSelectFont = { f -> onSelectFontFamily(f) },
                            onBack = { currentScreen = SettingsMenuScreen.SUBTITLE_STYLE }
                        )
                    }

                    SettingsMenuScreen.SUBTITLE_OUTLINE -> {
                        OutlineSubMenuContent(
                            currentOutline = subtitleStyle.outlineStyle,
                            onSelectOutline = { o -> onSelectOutlineStyle(o) },
                            onBack = { currentScreen = SettingsMenuScreen.SUBTITLE_STYLE }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Image 3: Main Settings Menu (Scrollable in portrait)
 */
@Composable
private fun MainMenuContent(
    currentSpeed: Float,
    onNavigateSpeed: () -> Unit,
    subtitles: List<SubtitleUiModel>,
    selectedSubtitleId: String,
    onNavigateSubtitles: () -> Unit,
    onNavigateSubtitleStyle: () -> Unit,
    selectedQuality: String?,
    onNavigateQuality: () -> Unit,
    videoZoom: String,
    onUpdateZoom: (Int) -> Unit,
    isAutoNext: Boolean,
    onToggleAutoNext: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        // 1. Play Speed
        val speedText = if (currentSpeed == 1.0f) "Normal" else "${currentSpeed}x"
        MenuItemRow(
            icon = Icons.Default.PlayCircleOutline,
            title = "Play Speed",
            value = speedText,
            onClick = onNavigateSpeed
        )

        // 2. Subtitles
        val activeSubLabel = subtitles.find { it.id.equals(selectedSubtitleId, ignoreCase = true) }?.label ?: "Off"
        MenuItemRow(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "Subtitles",
            value = activeSubLabel,
            onClick = onNavigateSubtitles
        )

        // 3. Subtitle Style
        MenuItemRow(
            icon = Icons.Default.Tune,
            title = "Subtitle Style",
            value = "",
            onClick = onNavigateSubtitleStyle
        )

        // 4. Quality
        MenuItemRow(
            icon = Icons.Default.Videocam,
            title = "Quality",
            value = selectedQuality ?: "Auto",
            onClick = onNavigateQuality
        )

        // 5. Video Zoom: Stepper langsung di menu utama (kelipatan 5%, min 100%)
        val zoomNum = videoZoom.removeSuffix("%").trim().toIntOrNull() ?: 100
        StepperSettingRow(
            label = "Video Zoom",
            valueText = "${zoomNum}%",
            onDecrement = { onUpdateZoom(-5) },
            onIncrement = { onUpdateZoom(5) },
            canDecrement = zoomNum > 100,
            canIncrement = zoomNum < 300
        )

        // 6. Auto Next
        MenuToggleRow(
            icon = Icons.Default.SkipNext,
            title = "Auto Next",
            isChecked = isAutoNext,
            onCheckedChange = onToggleAutoNext
        )
    }
}

/**
 * Image 4: Quality Sub-menu
 */
@Composable
private fun QualitySubMenuContent(
    qualities: List<String>,
    selectedQuality: String?,
    onSelectQuality: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubMenuHeader(title = "Quality", onBack = onBack)

        val list = if (qualities.isEmpty()) listOf("Auto") else qualities
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            list.forEach { q ->
                val isSelected = selectedQuality?.equals(q, ignoreCase = true) == true ||
                    (selectedQuality == null && q.equals("auto", ignoreCase = true))
                SubMenuItemRow(
                    label = q,
                    isSelected = isSelected,
                    onClick = { onSelectQuality(q) }
                )
            }
        }
    }
}

/**
 * Image 5: Subtitles Sub-menu
 */
@Composable
private fun SubtitlesSubMenuContent(
    subtitles: List<SubtitleUiModel>,
    selectedSubtitleId: String,
    onSelectSubtitle: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubMenuHeader(title = "Subtitles", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            subtitles.forEach { sub ->
                val isSelected = sub.id.equals(selectedSubtitleId, ignoreCase = true)
                SubMenuItemRow(
                    label = sub.label,
                    isSelected = isSelected,
                    onClick = { onSelectSubtitle(sub.id) }
                )
            }
        }
    }
}

/**
 * Play Speed Sub-menu
 */
@Composable
private fun SpeedSubMenuContent(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubMenuHeader(title = "Play Speed", onBack = onBack)

        val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            speedOptions.forEach { spd ->
                val label = if (spd == 1.0f) "Normal" else "${spd}x"
                val isSelected = currentSpeed == spd
                SubMenuItemRow(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onSelectSpeed(spd) }
                )
            }
        }
    }
}

@Composable
private fun SubtitleStyleSubMenuContent(
    subtitleStyle: SubtitleStyleConfig,
    onNavigateFont: () -> Unit,
    onNavigateOutline: () -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onUpdatePosition: (Int) -> Unit,
    onUpdateBgOpacity: (Int) -> Unit,
    onUpdateLineSpacing: (Int) -> Unit,
    onUpdateBgPadding: (Int) -> Unit,
    onUpdateTextColor: (Long) -> Unit,
    onUpdateBgColor: (Long) -> Unit,
    onBack: () -> Unit
) {
    val composeFontFamily = when {
        subtitleStyle.fontFamily.lowercase().contains("comic") -> {
            try {
                FontFamily(Font(com.dramix.app.R.font.comic_bold, FontWeight.Bold))
            } catch (_: Exception) {
                FontFamily.Cursive
            }
        }
        subtitleStyle.fontFamily.lowercase().contains("georgia") || subtitleStyle.fontFamily.lowercase().contains("serif") -> FontFamily.Serif
        subtitleStyle.fontFamily.lowercase().contains("courier") || subtitleStyle.fontFamily.lowercase().contains("monospace") -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val composeFontWeight = if (subtitleStyle.fontFamily.lowercase().contains("gemuk") || subtitleStyle.fontFamily.lowercase().contains("impact") || subtitleStyle.fontFamily.lowercase().contains("comic")) {
        FontWeight.Bold
    } else {
        FontWeight.SemiBold
    }

    val outlineStrokeWidth = when (subtitleStyle.outlineStyle.lowercase().trim()) {
        "thin" -> 1.2f
        "medium" -> 2.2f
        "thick" -> 3.5f
        else -> 0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SubMenuHeader(title = "Subtitle Style", onBack = onBack)

        // 1. Live Subtitle Preview Box (tanpa bayangan luar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF08080A))
                .border(BorderStroke(0.8.dp, Color(0xFF26262C)), RoundedCornerShape(6.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            SubtitleOverlay(
                text = "It's always the season of love",
                style = subtitleStyle.copy(fontSizePx = 16, positionPercent = 0),
                modifier = Modifier.wrapContentSize()
            )
        }

        // 2. Navigation to Font Sub-menu [Image 2]
        SubMenuNavigationRow(
            label = "Font",
            value = subtitleStyle.fontFamily,
            onClick = onNavigateFont
        )

        // 3. Navigation to Outline Sub-menu [Image 3]
        SubMenuNavigationRow(
            label = "Outline",
            value = subtitleStyle.outlineStyle,
            onClick = onNavigateOutline
        )

        // 4. Font Size Stepper (kelipatan 2px, 12px sampai 80px)
        StepperSettingRow(
            label = "Font Size",
            valueText = "${subtitleStyle.fontSizePx}px",
            onDecrement = { onUpdateFontSize(-2) },
            onIncrement = { onUpdateFontSize(2) },
            canDecrement = subtitleStyle.fontSizePx > 12,
            canIncrement = subtitleStyle.fontSizePx < 80
        )

        // 5. Position Stepper (min 0% sampai 50% kelipatan 2%)
        StepperSettingRow(
            label = "Position",
            valueText = "${subtitleStyle.positionPercent}%",
            onDecrement = { onUpdatePosition(-2) },
            onIncrement = { onUpdatePosition(2) },
            canDecrement = subtitleStyle.positionPercent > 0,
            canIncrement = subtitleStyle.positionPercent < 50
        )

        // 6. Background Stepper (di bawah position, kelipatan 5% - preset dihapus sesuai Image 1)
        StepperSettingRow(
            label = "Background",
            valueText = "${subtitleStyle.backgroundOpacityPercent}%",
            onDecrement = { onUpdateBgOpacity(-5) },
            onIncrement = { onUpdateBgOpacity(5) },
            canDecrement = subtitleStyle.backgroundOpacityPercent > 0,
            canIncrement = subtitleStyle.backgroundOpacityPercent < 100
        )

        // Line Spacing Stepper
        StepperSettingRow(
            label = "Line Spacing",
            valueText = "${subtitleStyle.lineSpacingPx}px",
            onDecrement = { onUpdateLineSpacing(-1) },
            onIncrement = { onUpdateLineSpacing(1) },
            canDecrement = subtitleStyle.lineSpacingPx > 0,
            canIncrement = subtitleStyle.lineSpacingPx < 20
        )

        // Background Padding Stepper
        StepperSettingRow(
            label = "Background Padding",
            valueText = "${subtitleStyle.backgroundPaddingPx}px",
            onDecrement = { onUpdateBgPadding(-1) },
            onIncrement = { onUpdateBgPadding(1) },
            canDecrement = subtitleStyle.backgroundPaddingPx > 8,
            canIncrement = subtitleStyle.backgroundPaddingPx < 48
        )

        // 7. Text Color Picker
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Text Color",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val colors = listOf(
                    0xFFFFFFFFL to "White",
                    0xFFFFEB3BL to "Yellow",
                    0xFF00E5FFL to "Cyan",
                    0xFF00DF81L to "Green"
                )
                colors.forEach { (cVal, _) ->
                    val isSelected = subtitleStyle.textColor == cVal
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(cVal))
                            .border(
                                width = if (isSelected) 2.dp else 0.8.dp,
                                color = if (isSelected) CrimsonPlay else Color(0xFF404048),
                                shape = CircleShape
                            )
                            .clickable { onUpdateTextColor(cVal) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (cVal == 0xFFFFFFFFL) Color.Black else Color.White)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * Image 2: Font Sub-menu
 */
@Composable
private fun FontSubMenuContent(
    currentFont: String,
    onSelectFont: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SubMenuHeader(title = "Font", onBack = onBack)

        val fontOptions = listOf(
            "Arial",
            "Comic Sans MS",
            "Georgia",
            "Courier",
            "Verdana",
            "Impact (Gemuk)"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            fontOptions.forEach { font ->
                val isSelected = currentFont.equals(font, ignoreCase = true)
                SubMenuItemRow(
                    label = font,
                    isSelected = isSelected,
                    onClick = { onSelectFont(font) }
                )
            }
        }
    }
}

/**
 * Image 3: Outline Sub-menu
 */
@Composable
private fun OutlineSubMenuContent(
    currentOutline: String,
    onSelectOutline: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SubMenuHeader(title = "Outline", onBack = onBack)

        val outlineOptions = listOf(
            "None",
            "Thin",
            "Medium",
            "Thick"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            outlineOptions.forEach { outline ->
                val isSelected = currentOutline.equals(outline, ignoreCase = true)
                SubMenuItemRow(
                    label = outline,
                    isSelected = isSelected,
                    onClick = { onSelectOutline(outline) }
                )
            }
        }
    }
}

@Composable
private fun SubMenuNavigationRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = MutedText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ChevronTint,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun StepperSettingRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1A1E))
                .border(0.8.dp, Color(0xFF2E2E36), RoundedCornerShape(4.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            // Decrement Button (<)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(enabled = canDecrement, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Kurang",
                    tint = if (canDecrement) Color.White else Color(0xFF55555C),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Value Text (e.g. "28px" or "10%")
            Text(
                text = valueText,
                color = CrimsonPlay,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Increment Button (>)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(enabled = canIncrement, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Tambah",
                    tint = if (canIncrement) Color.White else Color(0xFF55555C),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SubMenuHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Kembali",
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    HorizontalDivider(color = DividerColor, thickness = 0.8.dp)
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    value: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = MutedText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ChevronTint,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun SubMenuItemRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CrimsonPlay,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = CrimsonPlay,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Spacer(modifier = Modifier.width(23.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MenuToggleRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isChecked) "On" else "Off",
            color = MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isChecked) CrimsonPlay else Color(0xFF383842))
                .padding(2.dp),
            contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

package com.dramix.app

import androidx.compose.ui.graphics.Color
import com.dramix.app.ui.theme.CrimsonPlay
import com.dramix.app.ui.theme.PureBlack
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun verify_oled_cinema_color_tokens() {
        assertEquals(Color(0xFF000000), PureBlack)
        assertEquals(Color(0xFFE11D48), CrimsonPlay)
    }
}

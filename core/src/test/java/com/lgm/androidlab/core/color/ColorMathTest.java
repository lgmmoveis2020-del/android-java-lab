package com.lgm.androidlab.core.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ColorMathTest {

    @ParameterizedTest(name = "hsv({0}, {1}, {2}) = {3}")
    @CsvSource({
            "0,   1, 1, FFFF0000",
            "60,  1, 1, FFFFFF00",
            "120, 1, 1, FF00FF00",
            "180, 1, 1, FF00FFFF",
            "240, 1, 1, FF0000FF",
            "300, 1, 1, FFFF00FF",
            "360, 1, 1, FFFF0000",
            "-120, 1, 1, FF0000FF",
            "0,   0, 1, FFFFFFFF",
            "0,   0, 0, FF000000",
            "0,   1, 0.5, FF800000",
            "200, 0.5, 0.5, FF406A80",
    })
    void hsvToArgb(float h, float s, float v, String expectedHex) {
        assertEquals(expectedHex, String.format("%08X", ColorMath.hsvToArgb(h, s, v)));
    }

    @Test
    @DisplayName("canais são extraídos e remontados corretamente")
    void channels() {
        int color = ColorMath.argb(0x80, 0x12, 0x34, 0x56);
        assertEquals(0x80123456, color);
        assertEquals(0x80, ColorMath.alpha(color));
        assertEquals(0x12, ColorMath.red(color));
        assertEquals(0x34, ColorMath.green(color));
        assertEquals(0x56, ColorMath.blue(color));
        assertEquals(0xFF123456, ColorMath.withAlpha(color, 255));
        assertEquals(0x00FFFFFF, ColorMath.argb(-5, 300, 999, 256) & 0x00FFFFFF);
        assertEquals(0, ColorMath.alpha(ColorMath.argb(-5, 300, 999, 256)));
    }

    @Test
    @DisplayName("lerp interpola e satura t em [0, 1]")
    void lerp() {
        int black = ColorMath.argb(255, 0, 0, 0);
        int white = ColorMath.argb(255, 255, 255, 255);
        assertEquals(black, ColorMath.lerp(black, white, 0));
        assertEquals(white, ColorMath.lerp(black, white, 1));
        assertEquals(white, ColorMath.lerp(black, white, 7));
        assertEquals(black, ColorMath.lerp(black, white, -1));
        int mid = ColorMath.lerp(black, white, 0.5f);
        assertEquals(128, ColorMath.red(mid));
        assertEquals(255, ColorMath.alpha(mid));
    }

    @Test
    @DisplayName("luminância cresce do preto ao branco")
    void luminance() {
        assertEquals(0.0, ColorMath.luminance(0xFF000000), 1e-9);
        assertEquals(1.0, ColorMath.luminance(0xFFFFFFFF), 1e-9);
        assertTrue(ColorMath.luminance(0xFF00FF00) > ColorMath.luminance(0xFF0000FF), "verde é mais claro que azul");
    }
}

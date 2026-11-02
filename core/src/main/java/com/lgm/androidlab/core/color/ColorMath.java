package com.lgm.androidlab.core.color;

/**
 * Conversões de cor independentes do Android (o pacote android.graphics não existe na JVM).
 * Cores são inteiros ARGB como em {@code android.graphics.Color}.
 */
public final class ColorMath {

    private ColorMath() {
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * HSV → ARGB opaco.
     *
     * @param hue        0..360 (valores fora são normalizados)
     * @param saturation 0..1
     * @param value      0..1
     */
    public static int hsvToArgb(float hue, float saturation, float value) {
        float h = ((hue % 360f) + 360f) % 360f;
        float s = clamp01(saturation);
        float v = clamp01(value);

        float c = v * s;
        float hPrime = h / 60f;
        float x = c * (1 - Math.abs(hPrime % 2 - 1));
        float m = v - c;

        float r;
        float g;
        float b;
        if (hPrime < 1) {
            r = c; g = x; b = 0;
        } else if (hPrime < 2) {
            r = x; g = c; b = 0;
        } else if (hPrime < 3) {
            r = 0; g = c; b = x;
        } else if (hPrime < 4) {
            r = 0; g = x; b = c;
        } else if (hPrime < 5) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        return argb(255, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    /** Interpola linearmente cada canal entre duas cores ({@code t} em 0..1). */
    public static int lerp(int from, int to, float t) {
        float k = clamp01(t);
        return argb(
                Math.round(alpha(from) + (alpha(to) - alpha(from)) * k),
                Math.round(red(from) + (red(to) - red(from)) * k),
                Math.round(green(from) + (green(to) - green(from)) * k),
                Math.round(blue(from) + (blue(to) - blue(from)) * k));
    }

    /** Devolve a mesma cor com outro alfa (0..255). */
    public static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    /** Luminância relativa aproximada (0 = preto, 1 = branco), útil para escolher texto claro/escuro. */
    public static double luminance(int color) {
        return (0.2126 * red(color) + 0.7152 * green(color) + 0.0722 * blue(color)) / 255.0;
    }

    private static int clamp(int channel) {
        return channel < 0 ? 0 : Math.min(255, channel);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(1, v);
    }
}

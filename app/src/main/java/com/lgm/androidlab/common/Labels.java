package com.lgm.androidlab.common;

import androidx.annotation.NonNull;

import java.util.Locale;

/** Formatação de rótulos derivados de enums e números. */
public final class Labels {

    private Labels() {
    }

    /** {@code "GOSPER_GUN"} vira {@code "Gosper Gun"}. */
    @NonNull
    public static String prettify(@NonNull String enumName) {
        StringBuilder sb = new StringBuilder(enumName.length());
        boolean capitalize = true;
        for (char c : enumName.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                capitalize = true;
            } else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /** 1234567 vira "1.234.567" no locale atual. */
    @NonNull
    public static String groupDigits(long value) {
        return String.format(Locale.getDefault(), "%,d", value);
    }

    /** Milissegundos legíveis: "12 ms" ou "1,25 s". */
    @NonNull
    public static String duration(double millis) {
        if (millis < 1000) {
            return String.format(Locale.getDefault(), "%.0f ms", millis);
        }
        return String.format(Locale.getDefault(), "%.2f s", millis / 1000.0);
    }
}

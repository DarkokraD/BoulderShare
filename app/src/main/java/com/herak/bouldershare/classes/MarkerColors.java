package com.herak.bouldershare.classes;

import java.util.Locale;

/** Opaque RGB only, so every marker remains visible and exportable. */
public final class MarkerColors {
    private MarkerColors() { }
    public static Integer parseHex(String input) {
        if (input == null) return null;
        String value = input.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("[0-9a-fA-F]{6}")) return null;
        return 0xff000000 | Integer.parseInt(value, 16);
    }
    public static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xffffff); }
}

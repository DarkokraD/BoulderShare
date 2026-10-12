package com.herak.bouldershare.classes;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

/** Global appearance preferences; a renderer always receives an immutable snapshot. */
public final class MarkerSettings {
    private MarkerSettings() { }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static MarkerAppearance load(Context context) {
        Map<String, ?> values = preferences(context).getAll();
        MarkerAppearance defaults = MarkerAppearance.defaults();
        return new MarkerAppearance(number(values, "marker_stroke", defaults.strokeWidth),
                number(values, "marker_border", defaults.borderWidth),
                number(values, "marker_padding", defaults.autoPadding),
                color(values, "marker_start_color", defaults.startColor),
                color(values, "marker_regular_color", defaults.regularColor),
                color(values, "marker_top_color", defaults.topColor));
    }

    private static float number(Map<String, ?> values, String key, float fallback) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }
    private static int color(Map<String, ?> values, String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : fallback;
    }

    public static void save(Context context, MarkerAppearance appearance) {
        preferences(context).edit().putFloat("marker_stroke", appearance.strokeWidth)
                .putFloat("marker_border", appearance.borderWidth).putFloat("marker_padding", appearance.autoPadding)
                .putInt("marker_start_color", appearance.startColor)
                .putInt("marker_regular_color", appearance.regularColor)
                .putInt("marker_top_color", appearance.topColor).apply();
    }
}

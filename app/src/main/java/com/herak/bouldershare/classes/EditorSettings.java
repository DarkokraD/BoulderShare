package com.herak.bouldershare.classes;

import android.content.Context;
import android.content.SharedPreferences;

/** Editor presentation preferences, separate from route and marker geometry. */
public final class EditorSettings {
    private static final String PANEL_EXPANDED = "editor_panel_expanded";
    private static final String SHOW_INSTRUCTIONS = "show_editor_instructions";

    private EditorSettings() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static boolean isPanelExpanded(Context context) {
        return preferences(context).getBoolean(PANEL_EXPANDED, true);
    }

    public static void setPanelExpanded(Context context, boolean expanded) {
        preferences(context).edit().putBoolean(PANEL_EXPANDED, expanded).apply();
    }

    public static boolean showInstructions(Context context) {
        return preferences(context).getBoolean(SHOW_INSTRUCTIONS, true);
    }

    public static void setShowInstructions(Context context, boolean show) {
        preferences(context).edit().putBoolean(SHOW_INSTRUCTIONS, show).apply();
    }
}

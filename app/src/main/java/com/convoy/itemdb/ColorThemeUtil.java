package com.convoy.itemdb;

import android.graphics.Color;

public final class ColorThemeUtil {
    private ColorThemeUtil() {}

    public static int parseOrDefault(String colorHex, String fallbackHex) {
        try {
            return Color.parseColor(colorHex == null || colorHex.trim().isEmpty() ? fallbackHex : colorHex);
        } catch (IllegalArgumentException ignored) {
            return Color.parseColor(fallbackHex);
        }
    }

    public static int blendTowardWhite(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (int) (Color.red(color) + ((255 - Color.red(color)) * amount));
        int g = (int) (Color.green(color) + ((255 - Color.green(color)) * amount));
        int b = (int) (Color.blue(color) + ((255 - Color.blue(color)) * amount));
        return Color.rgb(r, g, b);
    }

    public static int blendTowardBlack(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (int) (Color.red(color) * (1f - amount));
        int g = (int) (Color.green(color) * (1f - amount));
        int b = (int) (Color.blue(color) * (1f - amount));
        return Color.rgb(r, g, b);
    }

    public static int darken(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (int) (Color.red(color) * (1f - amount));
        int g = (int) (Color.green(color) * (1f - amount));
        int b = (int) (Color.blue(color) * (1f - amount));
        return Color.rgb(r, g, b);
    }

    public static int idealTextColor(int backgroundColor) {
        double luminance = (0.299 * Color.red(backgroundColor))
                + (0.587 * Color.green(backgroundColor))
                + (0.114 * Color.blue(backgroundColor));
        return luminance >= 160 ? Color.BLACK : Color.WHITE;
    }
}

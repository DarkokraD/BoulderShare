package com.herak.bouldershare.classes;

import com.herak.bouldershare.enums.HoldType;
import java.util.Objects;

/** Immutable marker dimensions in oriented, loaded photo pixels, independent of zoom. */
public final class MarkerAppearance {
    public final float strokeWidth, borderWidth, autoPadding;
    public final int startColor, regularColor, topColor;

    public MarkerAppearance(float strokeWidth, float borderWidth, float autoPadding,
                            int startColor, int regularColor, int topColor) {
        this.strokeWidth = bounded(strokeWidth, 1, 20, 3);
        this.borderWidth = bounded(borderWidth, 0, 10, 1);
        this.autoPadding = bounded(autoPadding, 0, 20, 2);
        this.startColor = startColor | 0xff000000;
        this.regularColor = regularColor | 0xff000000;
        this.topColor = topColor | 0xff000000;
    }

    public static MarkerAppearance defaults() {
        return new MarkerAppearance(3, 1, 2, 0xff60e697, 0xffff695b, 0xffeb74f5);
    }

    private static float bounded(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public int colorFor(HoldType type) {
        return type == HoldType.START_HOLD ? startColor : type == HoldType.TOP_HOLD ? topColor : regularColor;
    }

    /** Both strokes share a centerline; the widest stroke begins at the clear disk. */
    public float centerlineRadius(float clearRadius, float photoScale) {
        return clearRadius + (strokeWidth / 2 + borderWidth) * photoScale;
    }

    public float outerRadius(float clearRadius, float photoScale) {
        return clearRadius + (strokeWidth + 2 * borderWidth) * photoScale;
    }

    public int fittedClearRadius(float detectedRadius, double sourcePerReferencePixel) {
        if (!Float.isFinite(detectedRadius) || detectedRadius <= 0
                || !Double.isFinite(sourcePerReferencePixel) || sourcePerReferencePixel <= 0) {
            throw new IllegalArgumentException("Fit dimensions must be positive and finite");
        }
        return Math.max(1, (int) Math.ceil(((double) detectedRadius + autoPadding) / sourcePerReferencePixel));
    }

    public boolean sameRenderingAs(MarkerAppearance other) {
        return strokeWidth == other.strokeWidth && borderWidth == other.borderWidth
                && startColor == other.startColor && regularColor == other.regularColor && topColor == other.topColor;
    }

    @Override public boolean equals(Object value) {
        if (!(value instanceof MarkerAppearance)) return false;
        MarkerAppearance other = (MarkerAppearance) value;
        return sameRenderingAs(other) && autoPadding == other.autoPadding;
    }
    @Override public int hashCode() {
        return Objects.hash(strokeWidth, borderWidth, autoPadding, startColor, regularColor, topColor);
    }
}

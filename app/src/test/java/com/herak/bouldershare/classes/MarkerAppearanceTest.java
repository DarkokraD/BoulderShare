package com.herak.bouldershare.classes;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarkerAppearanceTest {
    @Test public void strokesRemainOutsideDetectedHoldAtEveryScaleAndReferenceAspect() {
        for (float border : new float[]{0, 1, 10}) {
            MarkerAppearance appearance = new MarkerAppearance(20, border, 2, 0, 0, 0);
            for (float referenceScale : new float[]{.5f, 1, 2}) {
                int clear = appearance.fittedClearRadius(30.3f, referenceScale);
                for (float displayScale : new float[]{.25f, 1, 4}) {
                    float innerEdge = appearance.centerlineRadius(clear * referenceScale * displayScale, displayScale)
                            - (appearance.strokeWidth / 2 + appearance.borderWidth) * displayScale;
                    assertTrue(innerEdge + .0001f >= (30.3f + 2) * displayScale);
                }
            }
        }
    }
    @Test public void zeroPaddingIsExactAndBadDimensionsAreBounded() {
        MarkerAppearance appearance = new MarkerAppearance(Float.NaN, -10, 0, 0, 0, 0);
        assertEquals(3, appearance.strokeWidth, 0); assertEquals(0, appearance.borderWidth, 0);
        assertEquals(30, appearance.fittedClearRadius(30, 1));
        assertEquals(0xff000000, appearance.startColor);
        appearance = new MarkerAppearance(999, 999, Float.POSITIVE_INFINITY, 0, 0, 0);
        assertEquals(20, appearance.strokeWidth, 0); assertEquals(10, appearance.borderWidth, 0);
        assertEquals(2, appearance.autoPadding, 0);
    }
    @Test public void hexAcceptsOpaqueRgbOnly() {
        assertEquals(Integer.valueOf(0xff12abef), MarkerColors.parseHex("#12abEF"));
        assertEquals(Integer.valueOf(0xff123456), MarkerColors.parseHex("123456"));
        assertEquals("#12ABEF", MarkerColors.hex(0xff12abef));
        for (String invalid : new String[]{"", "#ABC", "#00123456", "#GGGGGG", "1234567", "-12345"}) {
            assertNull(MarkerColors.parseHex(invalid));
        }
    }
}

package com.herak.bouldershare.classes.smart;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MaskHoldFitterTest {
    @Test public void fitsOnlyTheTappedComponent() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 8, 10, 14, 16);
        fill(mask, 40, 25, 24, 35, 34);
        HoldFit fit = fullImage(mask, 40, 40, 10, 12).fit;
        assertNotNull(fit);
        assertEquals(11, fit.x, .001f);
        assertEquals(13, fit.y, .001f);
        assertEquals((float) Math.hypot(3, 3), fit.radius, .001f);
    }

    @Test public void transformsNonSquareMaskPixelsAndCropOffsetToSourceCoordinates() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 8, 10, 14, 16);
        HoldFit fit = MaskHoldFitter.fit(mask, 40, 40, 100, 200, 80, 120,
                500, 600, 120, 236).fit;
        assertNotNull(fit);
        assertEquals(122, fit.x, .001f);
        assertEquals(239, fit.y, .001f);
        assertEquals((float) Math.hypot(6, 9), fit.radius, .001f);
    }

    @Test public void enclosingCircleContainsEveryCornerOfAnIrregularMask() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 10, 10, 14, 25);
        fill(mask, 40, 10, 22, 25, 25);
        HoldFit fit = fullImage(mask, 40, 40, 12, 12).fit;
        assertNotNull(fit);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                if (mask[y * 40 + x] == 0) continue;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        assertTrue(Math.hypot(x + dx - fit.x, y + dy - fit.y) <= fit.radius);
                    }
                }
            }
        }
    }

    @Test public void diagonalConnectionPreservesWholeHoldOutline() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 10, 10, 14, 14);
        fill(mask, 40, 14, 14, 18, 18);
        HoldFit fit = fullImage(mask, 40, 40, 11, 11).fit;
        assertNotNull(fit);
        assertEquals(14, fit.x, .001f);
        assertEquals(14, fit.y, .001f);
    }

    @Test public void rejectsCropTruncatedComponentAndRequestsLargerCrop() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 0, 10, 5, 16);
        MaskHoldFitter.Result result = MaskHoldFitter.fit(mask, 40, 40, 100, 100,
                40, 40, 500, 500, 102, 112);
        assertNull(result.fit);
        assertTrue(result.touchesArtificialBoundary);
    }

    @Test public void allowsComponentTouchingRealPhotoBoundary() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 0, 10, 5, 16);
        MaskHoldFitter.Result result = fullImage(mask, 40, 40, 2, 12);
        assertNotNull(result.fit);
        assertFalse(result.touchesArtificialBoundary);
    }

    @Test public void rejectsNoiseAndWallSizedMasks() {
        float[] mask = new float[40 * 40];
        mask[12 * 40 + 12] = 1;
        assertNull(fullImage(mask, 40, 40, 12, 12).fit);
        Arrays.fill(mask, 1);
        assertNull(fullImage(mask, 40, 40, 12, 12).fit);
    }

    @Test public void rejectsUncertainOrNonfiniteSeedRatherThanJumpingToNearbyHold() {
        float[] mask = new float[40 * 40];
        fill(mask, 40, 10, 10, 16, 16);
        for (float value : new float[] { .69f, Float.NaN, Float.POSITIVE_INFINITY, 1.1f }) {
            mask[12 * 40 + 12] = value;
            assertNull(fullImage(mask, 40, 40, 12, 12).fit);
        }
    }

    @Test public void invalidMaskOrSeedCannotProduceFit() {
        float[] mask = new float[40 * 40];
        assertNull(fullImage(new float[2], 40, 40, 10, 10).fit);
        assertNull(fullImage(mask, 40, 40, -1, 10).fit);
        assertNull(fullImage(mask, 40, 40, 40, 10).fit);
        assertNull(fullImage(mask, 40, 40, Float.NaN, 10).fit);
        assertNull(MaskHoldFitter.fit(mask, 40, 40, 490, 0, 40, 40,
                500, 500, 495, 10).fit);
    }

    @Test(expected = IllegalArgumentException.class)
    public void holdFitCannotCarryInvalidGeometry() {
        new HoldFit(1, 2, Float.NaN);
    }

    private static MaskHoldFitter.Result fullImage(float[] mask, int width, int height,
                                                   float x, float y) {
        return MaskHoldFitter.fit(mask, width, height, 0, 0, width, height,
                width, height, x, y);
    }

    private static void fill(float[] mask, int width, int left, int top, int right, int bottom) {
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) mask[y * width + x] = 1;
        }
    }
}

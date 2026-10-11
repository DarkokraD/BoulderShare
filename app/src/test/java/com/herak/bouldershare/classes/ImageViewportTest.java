package com.herak.bouldershare.classes;

import org.junit.Test;
import static org.junit.Assert.*;

public class ImageViewportTest {
    @Test public void movingPinchFocusFromFitPreservesTheOriginalPhotoAnchor() {
        ImageViewport viewport = new ImageViewport();
        viewport.configure(600, 400, 1200, 800);
        float anchorX = viewport.normalizedX(200), anchorY = viewport.normalizedY(150);
        viewport.scaleAndPan(2, 200, 150, 250, 150);
        assertEquals(-150, viewport.getLeft(), .0001f);
        assertEquals(anchorX, viewport.normalizedX(250), .0001f);
        assertEquals(anchorY, viewport.normalizedY(150), .0001f);
    }

    @Test public void movingPinchFocusFromClampedEdgeUsesOnlyTheFinalBounds() {
        ImageViewport viewport = new ImageViewport();
        viewport.configure(600, 400, 1200, 800);
        viewport.zoomAt(2, 300, 200);
        viewport.pan(10000, 0);
        assertEquals(0, viewport.getLeft(), .0001f);
        float anchorX = viewport.normalizedX(100), anchorY = viewport.normalizedY(150);
        viewport.scaleAndPan(2, 100, 150, 150, 150);
        assertEquals(-50, viewport.getLeft(), .0001f);
        assertEquals(anchorX, viewport.normalizedX(150), .0001f);
        assertEquals(anchorY, viewport.normalizedY(150), .0001f);
    }

    @Test public void zoomPreservesPhotoPointUnderFingersAndHasBounds() {
        ImageViewport viewport = new ImageViewport();
        viewport.configure(600, 400, 1200, 800);
        viewport.zoomAt(2, 200, 150);
        assertEquals(1f / 3, viewport.normalizedX(200), .0001f);
        assertEquals(.375f, viewport.normalizedY(150), .0001f);
        assertEquals(2, viewport.getZoom(), .0001f);
        viewport.zoomAt(100, 300, 200);
        assertEquals(8, viewport.getZoom(), .0001f);
        viewport.zoomAt(.0001f, 300, 200);
        assertEquals(1, viewport.getZoom(), .0001f);
        assertEquals(0, viewport.getLeft(), .0001f);
    }
    @Test public void panCannotExposeEdgesAndSmallImageRemainsCentered() {
        ImageViewport viewport = new ImageViewport();
        viewport.configure(600, 600, 1200, 400);
        viewport.zoomAt(2, 300, 300);
        viewport.pan(10000, 10000);
        assertEquals(0, viewport.getLeft(), .0001f);
        assertEquals(100, viewport.getTop(), .0001f);
        viewport.pan(-10000, -10000);
        assertEquals(-600, viewport.getLeft(), .0001f);
        assertEquals(100, viewport.getTop(), .0001f);
    }
    @Test public void normalizedStateRestoresBeforePhotoLoadsAndClampsAfterRotation() {
        ImageViewport original = new ImageViewport();
        original.configure(600, 400, 1200, 800);
        original.zoomAt(3, 300, 200); original.pan(-200, -60);
        ImageViewport restored = new ImageViewport();
        restored.restore(original.getZoom(), original.getCenterX(), original.getCenterY());
        restored.configure(400, 600, 1200, 800);
        assertEquals(3, restored.getZoom(), .0001f);
        assertEquals(original.getCenterX(), restored.getCenterX(), .0001f);
        assertEquals(original.getCenterY(), restored.getCenterY(), .0001f);
        assertTrue(restored.getLeft() <= 0);
        assertTrue(restored.getLeft() + restored.getImageWidth() >= 400);
        restored.reset();
        assertEquals(.5f, restored.getCenterX(), .0001f);
        assertEquals(.5f, restored.getCenterY(), .0001f);
        assertEquals(0, restored.getLeft(), .0001f);
    }
}

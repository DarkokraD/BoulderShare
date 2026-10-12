package com.herak.bouldershare.classes.smart;

import com.google.mediapipe.tasks.vision.interactivesegmenter.Stroke;

import org.junit.Test;

import static org.junit.Assert.*;

public class TapStrokeTest {
    @Test public void promptIsCenteredOnTapAndHasUsefulLength() {
        Stroke stroke = MediaPipeHoldEstimator.tapStroke(.5f, .25f);
        assertEquals(Stroke.BrushMode.POSITIVE, stroke.brushMode());
        assertTrue(stroke.completed());
        assertEquals(2, stroke.points().size());
        assertEquals(.475f, stroke.points().get(0).x(), .000001f);
        assertEquals(.525f, stroke.points().get(1).x(), .000001f);
        assertEquals(.25f, stroke.points().get(0).y(), .000001f);
        assertEquals(.25f, stroke.points().get(1).y(), .000001f);
    }

    @Test public void promptShiftsInwardAtEdgesAndStillContainsTheTap() {
        for (float x : new float[] {0, .001f, .01f, .99f, Math.nextDown(1f)}) {
            Stroke stroke = MediaPipeHoldEstimator.tapStroke(x, .5f);
            float start = stroke.points().get(0).x(), end = stroke.points().get(1).x();
            assertTrue(start >= 0);
            assertTrue(end < 1);
            assertTrue(start <= x && end >= x);
            assertEquals(.05f, end - start, .000001f);
        }
    }
}

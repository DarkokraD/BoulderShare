package com.herak.bouldershare;

import com.herak.bouldershare.classes.Hold;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ExampleUnitTest {
    @Test public void portraitResizeAndExportKeepMarkersAligned() {
        Hold marker = new Hold(300, 450, 24);
        assertEquals(150f, marker.projectedX(400, 800), .001f);
        assertEquals(225f, marker.projectedY(600, 1200), .001f);
        assertEquals(12f, marker.projectedRadius(400, 600, 800, 1200), .001f);
        assertEquals(900f, marker.projectedX(2400, 800), .001f);
        assertEquals(1350f, marker.projectedY(3600, 1200), .001f);
        assertEquals(72f, marker.projectedRadius(2400, 3600, 800, 1200), .001f);
    }

    @Test public void landscapeReferenceDoesNotDependOnEditorOrientation() {
        Hold marker = new Hold(900, 200, 40);
        assertEquals(450f, marker.projectedX(600, 1200), .001f);
        assertEquals(100f, marker.projectedY(400, 800), .001f);
        assertEquals(20f, marker.projectedRadius(600, 400, 1200, 800), .001f);
    }
}

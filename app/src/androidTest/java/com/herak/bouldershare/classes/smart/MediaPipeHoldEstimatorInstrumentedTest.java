package com.herak.bouldershare.classes.smart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter;
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenterOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;

import static org.junit.Assert.*;

/** Real packaged-model/runtime checks; one wall fixture cannot establish general accuracy. */
@RunWith(AndroidJUnit4.class)
public class MediaPipeHoldEstimatorInstrumentedTest {
    @Test public void packagedCpuModelProducesLocalizedFiniteConfidenceMask() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap image = syntheticImage();
        BaseOptions base = BaseOptions.builder().setModelAssetPath(MediaPipeHoldEstimator.MODEL_ASSET)
                .setDelegate(Delegate.CPU).build();
        try (InteractiveSegmenter segmenter = InteractiveSegmenter.createFromOptions(context,
                InteractiveSegmenterOptions.builder().setBaseOptions(base).build());
             MPImage input = new BitmapImageBuilder(image).build()) {
            segmenter.setImage(input);
            try (MPImage mask = segmenter.segment(Collections.singletonList(
                    MediaPipeHoldEstimator.tapStroke(.5f, .5f)))) {
                assertTrue(mask.getWidth() > 0);
                assertTrue(mask.getHeight() > 0);
                ByteBuffer bytes = ByteBufferExtractor.extract(mask).duplicate()
                        .order(ByteOrder.nativeOrder());
                bytes.rewind();
                assertEquals(mask.getWidth() * mask.getHeight() * Float.BYTES, bytes.remaining());
                FloatBuffer values = bytes.asFloatBuffer();
                int foreground = 0;
                while (values.hasRemaining()) {
                    float confidence = values.get();
                    assertTrue(Float.isFinite(confidence));
                    assertTrue(confidence >= 0 && confidence <= 1);
                    if (confidence >= MaskHoldFitter.CONFIDENCE_THRESHOLD) foreground++;
                }
                int count = mask.getWidth() * mask.getHeight();
                assertTrue("Model must identify an object rather than an empty mask", foreground > count * .02);
                assertTrue("Model must identify the object rather than the whole background", foreground < count * .20);
                assertTrue(values.get(mask.getHeight() / 2 * mask.getWidth() + mask.getWidth() / 2) >= .7f);
            }
        } finally {
            if (!image.isRecycled()) image.recycle();
        }
    }

    @Test public void estimatorFitsWholeSyntheticHoldWithoutRecyclingOriginal() {
        Bitmap image = syntheticImage();
        MediaPipeHoldEstimator estimator = new MediaPipeHoldEstimator(
                ApplicationProvider.getApplicationContext());
        try {
            HoldFit fit = estimator.estimate(image, 128, 128);
            assertNotNull("A clear synthetic hold must produce a usable fit", fit);
            assertFalse(image.isRecycled());
            assertEquals(Color.RED, image.getPixel(128, 128));
            assertEquals(128, fit.x, 4);
            assertEquals(128, fit.y, 4);
            assertTrue(fit.radius >= 32 && fit.radius <= 45);
            assertEncloses(fit, new float[][] {{93, 128}, {163, 128}, {128, 93}, {128, 163}}, 3);
        } finally {
            estimator.close();
            estimator.close();
            image.recycle();
        }
    }

    @Test public void estimatorFitsDistinctRealWallHoldsInSourceCoordinates() throws Exception {
        Bitmap wall;
        try (InputStream stream = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open("wall-stuttgart.jpg")) {
            wall = BitmapFactory.decodeStream(stream);
        }
        assertNotNull(wall);
        try (MediaPipeHoldEstimator estimator = new MediaPipeHoldEstimator(
                ApplicationProvider.getApplicationContext())) {
            // Approximate visible outline extrema from the attributed wall fixture. These
            // check full-hold enclosure and localization, not a pixel-perfect golden mask.
            HoldFit yellow = estimator.estimate(wall, 390, 164);
            assertPlausible(yellow, 392, 164, 20, 42);
            assertEncloses(yellow, new float[][] {{370, 170}, {408, 154}, {395, 141}, {383, 187}}, 3);

            HoldFit red = estimator.estimate(wall, 214, 45);
            assertPlausible(red, 214, 45, 12, 25);
            assertEncloses(red, new float[][] {{201, 45}, {227, 46}, {214, 33}, {215, 59}}, 3);

            HoldFit purple = estimator.estimate(wall, 535, 70);
            assertPlausible(purple, 545, 68, 18, 35);
            assertEncloses(purple, new float[][] {{524, 70}, {564, 66}, {545, 57}, {551, 79}}, 3);
            assertFalse(wall.isRecycled());
        } finally { wall.recycle(); }
    }

    private static void assertPlausible(HoldFit fit, float x, float y, float minRadius, float maxRadius) {
        assertNotNull("A visible, separate hold must produce a localized fit", fit);
        assertEquals(x, fit.x, 6);
        assertEquals(y, fit.y, 6);
        assertTrue("Circle must fit the hold rather than a bolt or wall", fit.radius >= minRadius && fit.radius <= maxRadius);
    }

    private static void assertEncloses(HoldFit fit, float[][] points, float tolerance) {
        for (float[] point : points) {
            assertTrue("Circle must enclose the visible hold outline",
                    Math.hypot(point[0] - fit.x, point[1] - fit.y) <= fit.radius + tolerance);
        }
    }

    private static Bitmap syntheticImage() {
        Bitmap image = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.LTGRAY);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        new Canvas(image).drawCircle(128, 128, 35, paint);
        return image;
    }
}

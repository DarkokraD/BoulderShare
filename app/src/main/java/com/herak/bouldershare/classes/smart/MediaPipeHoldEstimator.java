package com.herak.bouldershare.classes.smart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter;
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenterOptions;
import com.google.mediapipe.tasks.vision.interactivesegmenter.Stroke;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;

/** Point-prompt segmentation with owned crops and conservative, source-coordinate circle fitting. */
public final class MediaPipeHoldEstimator implements HoldEstimator {
    public static final String MODEL_ASSET = "magic_touch_v2_int8.task";
    private static final int INITIAL_CROP_SIZE = 256;
    private static final int MAX_CROP_SIZE = 1024;
    private final Context context;
    private InteractiveSegmenter segmenter;
    private boolean closed;

    /** Cheap construction; loading the model and native runtime happens on the estimate worker. */
    public MediaPipeHoldEstimator(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public HoldFit estimate(Bitmap image, float sourceX, float sourceY) {
        if (closed) throw new IllegalStateException("Estimator is closed");
        if (image == null || image.isRecycled() || !Float.isFinite(sourceX)
                || !Float.isFinite(sourceY) || sourceX < 0 || sourceY < 0
                || sourceX >= image.getWidth() || sourceY >= image.getHeight()) return null;
        if (segmenter == null) {
            BaseOptions base = BaseOptions.builder().setModelAssetPath(MODEL_ASSET)
                    .setDelegate(Delegate.CPU).build();
            segmenter = InteractiveSegmenter.createFromOptions(context,
                    InteractiveSegmenterOptions.builder().setBaseOptions(base).build());
        }
        for (int cropSize = INITIAL_CROP_SIZE; cropSize <= MAX_CROP_SIZE; cropSize *= 2) {
            MaskHoldFitter.Result result = estimateCrop(image, sourceX, sourceY, cropSize);
            if (!result.touchesArtificialBoundary || cropSize == MAX_CROP_SIZE
                    || (image.getWidth() <= cropSize && image.getHeight() <= cropSize)) {
                return result.fit;
            }
        }
        return null;
    }

    private MaskHoldFitter.Result estimateCrop(Bitmap image, float sourceX, float sourceY,
                                              int cropSize) {
        int width = Math.min(cropSize, image.getWidth());
        int height = Math.min(cropSize, image.getHeight());
        int left = Math.max(0, Math.min(image.getWidth() - width, Math.round(sourceX - width * .5f)));
        int top = Math.max(0, Math.min(image.getHeight() - height, Math.round(sourceY - height * .5f)));
        // BitmapImageBuilder owns and recycles its bitmap on close. Always copy, including
        // whole-image crops: Bitmap.createBitmap(source, ...) can return the original bitmap.
        Bitmap crop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            new Canvas(crop).drawBitmap(image, new Rect(left, top, left + width, top + height),
                    new Rect(0, 0, width, height), null);
            try (MPImage input = new BitmapImageBuilder(crop).build()) {
                segmenter.setImage(input);
                Stroke tap = tapStroke((sourceX - left) / width, (sourceY - top) / height);
                try (MPImage mask = segmenter.segment(Collections.singletonList(tap))) {
                    int maskWidth = mask.getWidth(), maskHeight = mask.getHeight();
                    if (maskWidth <= 0 || maskHeight <= 0 || maskWidth > 2048 || maskHeight > 2048) {
                        return new MaskHoldFitter.Result(null, false);
                    }
                    ByteBuffer buffer = ByteBufferExtractor.extract(mask).duplicate()
                            .order(ByteOrder.nativeOrder());
                    buffer.rewind();
                    int count = maskWidth * maskHeight;
                    if (buffer.remaining() != count * Float.BYTES) {
                        return new MaskHoldFitter.Result(null, false);
                    }
                    float[] confidence = new float[count];
                    buffer.asFloatBuffer().get(confidence);
                    return MaskHoldFitter.fit(confidence, maskWidth, maskHeight,
                            left, top, width, height, image.getWidth(), image.getHeight(),
                            sourceX, sourceY);
                }
            }
        } finally {
            if (!crop.isRecycled()) crop.recycle();
        }
    }

    /**
     * MagicTouch v2 consumes a rasterized stroke: a single point (or one-pixel line) produces
     * an ineffective prompt. A short line centered on the tap selects the intended object.
     * Shift the full five-percent stroke inward at edges, keeping the tap on the line.
     */
    static Stroke tapStroke(float normalizedX, float normalizedY) {
        float maximum = Math.nextDown(1f);
        float startX = Math.max(0f, Math.min(maximum - .05f, normalizedX - .025f));
        return Stroke.builder().setBrushMode(Stroke.BrushMode.POSITIVE)
                .setPoints(Arrays.asList(NormalizedKeypoint.create(startX, normalizedY),
                        NormalizedKeypoint.create(startX + .05f, normalizedY)))
                .setCompleted(true).build();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (segmenter != null) {
            segmenter.close();
            segmenter = null;
        }
    }
}

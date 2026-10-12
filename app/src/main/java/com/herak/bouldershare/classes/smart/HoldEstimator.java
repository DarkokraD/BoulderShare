package com.herak.bouldershare.classes.smart;

import android.graphics.Bitmap;

/** All operations, including close, belong to a single background worker. */
public interface HoldEstimator extends AutoCloseable {
    /** Returns null when the tap cannot be fitted reliably. Never modifies or recycles image. */
    HoldFit estimate(Bitmap image, float sourceX, float sourceY);

    @Override
    void close();
}

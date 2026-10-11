package com.herak.bouldershare.classes;

/** Photo-only viewport, independent from persisted marker reference coordinates. */
public final class ImageViewport {
    private float width, height, fitWidth, fitHeight;
    private float zoom = 1f, centerX = .5f, centerY = .5f;

    public void configure(float width, float height, float imageWidth, float imageHeight) {
        this.width = width; this.height = height;
        float fit = Math.min(width / imageWidth, height / imageHeight);
        fitWidth = imageWidth * fit; fitHeight = imageHeight * fit;
        clamp();
    }

    public void restore(float zoom, float centerX, float centerY) {
        this.zoom = Float.isFinite(zoom) ? Math.max(1, Math.min(8, zoom)) : 1;
        this.centerX = Float.isFinite(centerX) ? centerX : .5f;
        this.centerY = Float.isFinite(centerY) ? centerY : .5f;
        clamp();
    }

    public void reset() { restore(1, .5f, .5f); }
    public float getZoom() { return zoom; }
    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getFitWidth() { return fitWidth; }
    public float getFitHeight() { return fitHeight; }
    public float getImageWidth() { return fitWidth * zoom; }
    public float getImageHeight() { return fitHeight * zoom; }
    public float getLeft() { return width / 2 - centerX * getImageWidth(); }
    public float getTop() { return height / 2 - centerY * getImageHeight(); }
    public float normalizedX(float screenX) { return (screenX - getLeft()) / getImageWidth(); }
    public float normalizedY(float screenY) { return (screenY - getTop()) / getImageHeight(); }

    public void zoomAt(float factor, float focusX, float focusY) {
        scaleAndPan(factor, focusX, focusY, focusX, focusY);
    }

    /** Anchor before changing zoom, then move that photo point under the new focus. */
    public void scaleAndPan(float factor, float previousFocusX, float previousFocusY,
                            float currentFocusX, float currentFocusY) {
        if (fitWidth <= 0 || fitHeight <= 0 || !Float.isFinite(factor) || factor <= 0) return;
        float anchorX = normalizedX(previousFocusX), anchorY = normalizedY(previousFocusY);
        zoom = Math.max(1, Math.min(8, zoom * factor));
        centerX = anchorX - (currentFocusX - width / 2) / getImageWidth();
        centerY = anchorY - (currentFocusY - height / 2) / getImageHeight();
        clamp();
    }

    public void pan(float dx, float dy) {
        if (getImageWidth() <= 0 || getImageHeight() <= 0) return;
        centerX -= dx / getImageWidth(); centerY -= dy / getImageHeight();
        clamp();
    }

    private void clamp() {
        if (fitWidth <= 0 || fitHeight <= 0) return;
        centerX = clampCenter(centerX, width, getImageWidth());
        centerY = clampCenter(centerY, height, getImageHeight());
    }
    private static float clampCenter(float center, float viewport, float image) {
        if (image <= viewport) return .5f;
        float inset = viewport / (image * 2);
        return Math.max(inset, Math.min(1 - inset, center));
    }
}

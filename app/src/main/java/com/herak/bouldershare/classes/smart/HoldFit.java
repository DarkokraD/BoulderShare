package com.herak.bouldershare.classes.smart;

/** An immutable enclosing circle of mask pixels, without cosmetic padding or marker strokes. */
public final class HoldFit {
    public final float x;
    public final float y;
    public final float radius;

    public HoldFit(float x, float y, float radius) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(radius)
                || radius <= 0) {
            throw new IllegalArgumentException("A hold fit must have finite geometry and positive radius");
        }
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getRadius() { return radius; }
}

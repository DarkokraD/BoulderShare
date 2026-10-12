package com.herak.bouldershare.classes.smart;

/** Pure geometry: keep only the confident component containing the tap, then enclose its pixels. */
final class MaskHoldFitter {
    static final float CONFIDENCE_THRESHOLD = .70f;

    static final class Result {
        final HoldFit fit;
        final boolean touchesArtificialBoundary;

        Result(HoldFit fit, boolean touchesArtificialBoundary) {
            this.fit = fit;
            this.touchesArtificialBoundary = touchesArtificialBoundary;
        }
    }

    private MaskHoldFitter() { }

    static Result fit(float[] confidence, int maskWidth, int maskHeight,
                      int cropLeft, int cropTop, int cropWidth, int cropHeight,
                      int imageWidth, int imageHeight, float sourceX, float sourceY) {
        if (maskWidth <= 0 || maskHeight <= 0 || maskWidth > 2048 || maskHeight > 2048
                || confidence == null || confidence.length != maskWidth * maskHeight
                || imageWidth <= 0 || imageHeight <= 0 || cropWidth <= 0 || cropHeight <= 0
                || cropLeft < 0 || cropTop < 0 || cropLeft + cropWidth > imageWidth
                || cropTop + cropHeight > imageHeight || !Float.isFinite(sourceX)
                || !Float.isFinite(sourceY) || sourceX < cropLeft || sourceY < cropTop
                || sourceX >= cropLeft + cropWidth || sourceY >= cropTop + cropHeight) {
            return new Result(null, false);
        }
        int seedX = (int) ((sourceX - cropLeft) * maskWidth / cropWidth);
        int seedY = (int) ((sourceY - cropTop) * maskHeight / cropHeight);
        int seed = seedY * maskWidth + seedX;
        if (!foreground(confidence[seed])) return new Result(null, false);

        // Each pixel enters the queue once; disconnected holds can never influence the fit.
        boolean[] visited = new boolean[confidence.length];
        int[] component = new int[confidence.length];
        int count = 1;
        component[0] = seed;
        visited[seed] = true;
        int minX = seedX, maxX = seedX, minY = seedY, maxY = seedY;
        boolean artificialBoundary = false;
        for (int head = 0; head < count; head++) {
            int pixel = component[head];
            int x = pixel % maskWidth, y = pixel / maskWidth;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            if ((x == 0 && cropLeft > 0)
                    || (y == 0 && cropTop > 0)
                    || (x == maskWidth - 1 && cropLeft + cropWidth < imageWidth)
                    || (y == maskHeight - 1 && cropTop + cropHeight < imageHeight)) {
                artificialBoundary = true;
            }
            // Eight-connected pixels keep diagonal outlines together.
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= maskWidth || ny >= maskHeight) continue;
                    int neighbor = ny * maskWidth + nx;
                    if (!visited[neighbor] && foreground(confidence[neighbor])) {
                        visited[neighbor] = true;
                        component[count++] = neighbor;
                    }
                }
            }
        }
        if (artificialBoundary) return new Result(null, true);

        float scaleX = (float) cropWidth / maskWidth;
        float scaleY = (float) cropHeight / maskHeight;
        double area = count * (double) scaleX * scaleY;
        // A wall-sized mask and isolated noise are safer left as ordinary manual markers.
        if (area < 16 || count > confidence.length * .50
                || area > (double) imageWidth * imageHeight * .35) {
            return new Result(null, false);
        }
        float centerX = cropLeft + (minX + maxX + 1) * .5f * scaleX;
        float centerY = cropTop + (minY + maxY + 1) * .5f * scaleY;
        double radiusSquared = 0;
        for (int i = 0; i < count; i++) {
            int x = component[i] % maskWidth, y = component[i] / maskWidth;
            // Enclose pixel corners, including non-square pixels and irregular/concave masks.
            double dx = Math.max(Math.abs(cropLeft + x * scaleX - centerX),
                    Math.abs(cropLeft + (x + 1) * scaleX - centerX));
            double dy = Math.max(Math.abs(cropTop + y * scaleY - centerY),
                    Math.abs(cropTop + (y + 1) * scaleY - centerY));
            radiusSquared = Math.max(radiusSquared, dx * dx + dy * dy);
        }
        // Appearance padding is applied once by the editor, after detection.
        double exactRadius = Math.sqrt(radiusSquared);
        float radius = (float) exactRadius;
        if (radius < exactRadius) radius = Math.nextUp(radius);
        if (!Float.isFinite(radius) || radius > Math.hypot(imageWidth, imageHeight) * .30) {
            return new Result(null, false);
        }
        return new Result(new HoldFit(centerX, centerY, radius), false);
    }

    private static boolean foreground(float value) {
        return Float.isFinite(value) && value >= CONFIDENCE_THRESHOLD && value <= 1;
    }
}

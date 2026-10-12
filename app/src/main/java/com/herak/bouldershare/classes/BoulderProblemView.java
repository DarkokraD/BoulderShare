package com.herak.bouldershare.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.VisibleForTesting;

import com.herak.bouldershare.classes.smart.HoldEstimator;
import com.herak.bouldershare.classes.smart.HoldFit;
import com.herak.bouldershare.classes.smart.MediaPipeHoldEstimator;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.enums.HoldType;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Photo navigation never changes the persisted marker reference space or export. */
public class BoulderProblemView extends View {
    private final MainActivity activity;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF imageRect = new RectF();
    private final RectF displayedRect = new RectF();
    private final ImageViewport viewport = new ImageViewport();
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final GestureDetector gestures;
    private final ScaleGestureDetector scales;
    private final int touchSlop;
    private final float minimumHitRadius;
    private BoulderProblemInfo info;
    private Bitmap bitmap;
    private HoldType currentType = HoldType.START_HOLD;
    private Hold downHold, selectedForResize;
    private PendingTap pendingTap;
    private PendingTap deferredDoubleTap;
    private Hold doubleTapTarget;
    private boolean doubleTapPending;
    private int activePointer = MotionEvent.INVALID_POINTER_ID;
    private float downX, downY, lastX, lastY, grabX, grabY, scaleFocusX, scaleFocusY;
    private boolean dragging, panning, suppressSinglePointer, released, loadFailed, choosingResize;
    private OnHoldResizeRequestedListener resizeListener;
    private final HoldEstimator holdEstimator;
    private final ThreadPoolExecutor smartExecutor = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2));
    private final ArrayList<SmartRequest> smartRequests = new ArrayList<>();
    private long smartGeneration;
    private boolean smartMarkingEnabled, smartUnavailable;
    private SmartMarkingStatus smartStatus = SmartMarkingStatus.IDLE;
    private MarkerAppearance markerAppearance;
    private OnSmartMarkingStatusListener smartStatusListener;

    public enum SmartMarkingStatus { IDLE, DETECTING, FITTED, MANUAL_FALLBACK, UNAVAILABLE }
    public interface OnSmartMarkingStatusListener {
        void onSmartMarkingStatusChanged(SmartMarkingStatus status);
    }

    public interface OnHoldResizeRequestedListener { void onHoldResizeRequested(Hold hold); }
    private static class PendingTap {
        final Hold location, target;
        PendingTap(Hold location, Hold target) { this.location = location; this.target = target; }
    }

    public BoulderProblemView(Context context) {
        this(context, new MediaPipeHoldEstimator(context.getApplicationContext()));
    }

    @VisibleForTesting
    public BoulderProblemView(Context context, HoldEstimator estimator) {
        super(context);
        holdEstimator = estimator;
        markerAppearance = MarkerSettings.load(context);
        activity = (MainActivity) context;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        minimumHitRadius = 24 * getResources().getDisplayMetrics().density;
        setSaveEnabled(true);
        info = activity.getmBoulderProblemInfo();
        if (info == null) info = new BoulderProblemInfo();
        if (info.getHolds() == null) info.setHolds(new ArrayList<>());
        bitmap = activity.getmBoulderBitmap();
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent event) {
                if (!canEdit() || suppressSinglePointer || dragging || panning || !inside(event.getX(), event.getY())) return true;
                if (choosingResize) {
                    Hold selected = downHold;
                    if (selected != null && info.getHolds().contains(selected)) {
                        choosingResize = false;
                        cancelSmartRequest(selected);
                        selectedForResize = selected;
                        pendingTap = null;
                        if (resizeListener != null) resizeListener.onHoldResizeRequested(selected);
                    }
                } else pendingTap = new PendingTap(point(event.getX(), event.getY()), downHold);
                return true;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent event) { flushPendingSingleTap(); return true; }
            @Override public boolean onDoubleTap(MotionEvent event) {
                if (!canEdit() || suppressSinglePointer || choosingResize || selectedForResize != null) return true;
                deferredDoubleTap = pendingTap;
                pendingTap = null;
                doubleTapTarget = downHold;
                doubleTapPending = true;
                return true;
            }
            @Override public boolean onDoubleTapEvent(MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_UP && doubleTapPending) {
                    Hold existing = doubleTapTarget;
                    if (canEdit() && !suppressSinglePointer && !dragging && !panning
                            && existing != null && info.getHolds().contains(existing)) {
                        cancelSmartRequest(existing);
                        currentType = HoldType.values()[(existing.getType().ordinal() + 1) % HoldType.values().length];
                        existing.setType(currentType);
                        changed();
                    }
                    clearDeferredDoubleTap();
                }
                return true;
            }
        });
        gestures.setIsLongpressEnabled(false);
        scales = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                scaleFocusX = detector.getFocusX(); scaleFocusY = detector.getFocusY();
                return isEnabled() && !released;
            }
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if (!isEnabled() || released) return false;
                float focusX = detector.getFocusX(), focusY = detector.getFocusY();
                viewport.scaleAndPan(detector.getScaleFactor(), scaleFocusX, scaleFocusY, focusX, focusY);
                scaleFocusX = focusX; scaleFocusY = focusY;
                invalidate();
                return true;
            }
        });
        scales.setQuickScaleEnabled(false);
        scales.setStylusScaleEnabled(false);
        setContentDescription(context.getString(R.string.editor_accessibility));
        if (bitmap == null && info.getInputBitmapUri() != null) {
            final Context application = context.getApplicationContext();
            final android.net.Uri input = info.getInputBitmapUri();
            imageExecutor.execute(() -> {
                try {
                    Bitmap loaded = ImageFiles.load(application, input);
                    post(() -> {
                        if (released || activity.isFinishing() || activity.isDestroyed()) return;
                        bitmap = loaded;
                        activity.setmBoulderBitmap(loaded);
                        updateImageRect();
                        invalidate();
                    });
                } catch (IOException | RuntimeException error) {
                    post(() -> {
                        if (released || activity.isFinishing() || activity.isDestroyed()) return;
                        loadFailed = true;
                        invalidate();
                        new MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.photo_unavailable).setMessage(R.string.relink_photo_message)
                                .setPositiveButton(R.string.choose_photo, (dialog, which) -> activity.requestPhotoRelink())
                                .setNegativeButton(R.string.cancel, (dialog, which) -> activity.changeFragment(MainActivity.FRAGMENT_TYPE.MAIN_FRAGMENT))
                                .setOnCancelListener(dialog -> activity.changeFragment(MainActivity.FRAGMENT_TYPE.MAIN_FRAGMENT)).show();
                    });
                }
            });
        }
    }

    public BoulderProblemInfo getBoulderProblemInfo() { return info; }
    public void setBoulderProblemInfo(BoulderProblemInfo value) {
        cancelAllSmartRequests(); cancelHoldResize(); info = value; invalidate();
    }
    public boolean isImageReady() { return bitmap != null && !imageRect.isEmpty(); }
    private boolean canEdit() { return isEnabled() && !released && isImageReady(); }

    /** A completed tap retains the exact hit selected at its original pointer down. */
    public void flushPendingSingleTap() {
        PendingTap tap = pendingTap;
        pendingTap = null;
        if (tap == null || !canEdit() || choosingResize || selectedForResize != null) return;
        if (tap.target != null) {
            cancelSmartRequest(tap.target);
            if (!info.getHolds().remove(tap.target)) return;
        } else {
            addHold(tap.location, true);
        }
        changed(); performClick();
    }

    private void addHold(Hold hold, boolean estimate) {
        hold.setType(currentType);
        hold.setCircleRadius(Math.max(1, Math.round(info.getReferenceHeight() / 25f)));
        info.getHolds().add(hold);
        if (estimate) startSmartRequest(hold);
    }

    public boolean isSmartMarkingEnabled() { return smartMarkingEnabled; }
    public MarkerAppearance getMarkerAppearance() { return markerAppearance; }
    public void setMarkerAppearance(MarkerAppearance appearance) {
        if (markerAppearance.equals(appearance)) return;
        cancelAllSmartRequests();
        markerAppearance = appearance;
        invalidate();
    }
    public void setSmartMarkingEnabled(boolean enabled) {
        if (smartMarkingEnabled == enabled) return;
        smartMarkingEnabled = enabled;
        cancelAllSmartRequests();
        if (enabled && smartUnavailable) setSmartStatus(SmartMarkingStatus.UNAVAILABLE);
    }
    public void setOnSmartMarkingStatusListener(OnSmartMarkingStatusListener listener) {
        smartStatusListener = released ? null : listener;
        if (smartStatusListener != null) smartStatusListener.onSmartMarkingStatusChanged(smartStatus);
    }
    private void setSmartStatus(SmartMarkingStatus status) {
        smartStatus = status;
        if (smartStatusListener != null) smartStatusListener.onSmartMarkingStatusChanged(status);
    }
    private void startSmartRequest(Hold hold) {
        if (!smartMarkingEnabled || !canEdit() || !isAttachedToWindow()) return;
        if (smartUnavailable) { setSmartStatus(SmartMarkingStatus.UNAVAILABLE); return; }
        SmartRequest request = new SmartRequest(this, hold);
        smartRequests.add(request);
        try {
            smartExecutor.execute(request);
            setSmartStatus(SmartMarkingStatus.DETECTING);
        } catch (RejectedExecutionException busy) {
            smartRequests.remove(request);
            setSmartStatus(smartRequests.isEmpty() ? SmartMarkingStatus.MANUAL_FALLBACK : SmartMarkingStatus.DETECTING);
        }
    }
    private void cancelSmartRequest(Hold hold) {
        for (int i = smartRequests.size() - 1; i >= 0; i--) {
            SmartRequest request = smartRequests.get(i);
            if (request.hold == hold) {
                request.cancelled.set(true); smartExecutor.remove(request); smartRequests.remove(i);
            }
        }
        if (smartRequests.isEmpty() && smartStatus == SmartMarkingStatus.DETECTING) setSmartStatus(SmartMarkingStatus.IDLE);
    }
    private void cancelAllSmartRequests() {
        smartGeneration++;
        for (SmartRequest request : smartRequests) {
            request.cancelled.set(true); smartExecutor.remove(request);
        }
        smartRequests.clear();
        setSmartStatus(SmartMarkingStatus.IDLE);
    }
    private void completeSmartRequest(SmartRequest request, HoldFit fit, boolean unavailable) {
        if (!smartRequests.remove(request)) return;
        boolean valid = !request.cancelled.get() && request.generation == smartGeneration
                && smartMarkingEnabled && canEdit() && isAttachedToWindow()
                && info == request.model && bitmap == request.image
                && java.util.Objects.equals(info.getInputBitmapUri(), request.uri)
                && info.getReferenceWidth() == request.referenceWidth && info.getReferenceHeight() == request.referenceHeight
                && info.getHolds().contains(request.hold) && request.hold.getX() == request.x
                && request.hold.getY() == request.y && request.hold.getCircleRadius() == request.radius
                && request.hold.getType() == request.type;
        if (!valid) {
            if (smartRequests.isEmpty() && smartStatus == SmartMarkingStatus.DETECTING) setSmartStatus(SmartMarkingStatus.IDLE);
            return;
        }
        SmartMarkingStatus outcome = SmartMarkingStatus.MANUAL_FALLBACK;
        if (unavailable) {
            smartUnavailable = true;
            cancelAllSmartRequests();
            outcome = SmartMarkingStatus.UNAVAILABLE;
        } else if (fit != null && Float.isFinite(fit.x) && Float.isFinite(fit.y)
                && Float.isFinite(fit.radius) && fit.radius > 0 && fit.radius <= Math.max(bitmap.getWidth(), bitmap.getHeight())
                && fit.x >= 0 && fit.x <= bitmap.getWidth() && fit.y >= 0 && fit.y <= bitmap.getHeight()) {
            float scaleX = bitmap.getWidth() / (float) request.referenceWidth;
            float scaleY = bitmap.getHeight() / (float) request.referenceHeight;
            request.hold.setX(fit.x / scaleX); request.hold.setY(fit.y / scaleY);
            request.hold.setCircleRadius(markerAppearance.fittedClearRadius(fit.radius,
                    Math.min(bitmap.getWidth() / (double) request.referenceWidth,
                            bitmap.getHeight() / (double) request.referenceHeight)));
            changed();
            outcome = SmartMarkingStatus.FITTED;
        }
        setSmartStatus(smartRequests.isEmpty() ? outcome : SmartMarkingStatus.DETECTING);
    }
    /** The worker holds a weak view reference so a native call cannot retain a destroyed activity. */
    private static final class SmartRequest implements Runnable {
        private static final Handler MAIN = new Handler(Looper.getMainLooper());
        final WeakReference<BoulderProblemView> owner;
        final HoldEstimator estimator;
        final Hold hold;
        final BoulderProblemInfo model;
        final Bitmap image;
        final android.net.Uri uri;
        final int referenceWidth, referenceHeight, radius;
        final float x, y;
        final HoldType type;
        final long generation;
        final AtomicBoolean cancelled = new AtomicBoolean();
        SmartRequest(BoulderProblemView view, Hold hold) {
            owner = new WeakReference<>(view); estimator = view.holdEstimator;
            this.hold = hold; model = view.info; image = view.bitmap; uri = model.getInputBitmapUri();
            referenceWidth = model.getReferenceWidth(); referenceHeight = model.getReferenceHeight();
            x = hold.getX(); y = hold.getY(); radius = hold.getCircleRadius(); type = hold.getType();
            generation = view.smartGeneration;
        }
        @Override public void run() {
            if (cancelled.get()) return;
            HoldFit result = null;
            boolean unavailable = false;
            try {
                result = estimator.estimate(image, x * image.getWidth() / referenceWidth,
                        y * image.getHeight() / referenceHeight);
            } catch (RuntimeException | LinkageError failure) { unavailable = true; }
            final HoldFit fit = result;
            final boolean failed = unavailable;
            MAIN.post(() -> {
                BoulderProblemView view = owner.get();
                if (view != null) view.completeSmartRequest(this, fit, failed);
            });
        }
    }

    public void resetViewport() { flushPendingSingleTap(); cancelGestures(); viewport.reset(); invalidate(); }
    public void setOnHoldResizeRequestedListener(OnHoldResizeRequestedListener listener) { resizeListener = listener; }
    public boolean isChoosingHoldToResize() { return choosingResize; }
    public boolean requestHoldResize() {
        flushPendingSingleTap(); cancelAllSmartRequests(); cancelGestures(); selectedForResize = null;
        choosingResize = canEdit() && !info.getHolds().isEmpty();
        return choosingResize;
    }
    public void cancelHoldResize() {
        choosingResize = false; selectedForResize = null; cancelGestures(); invalidate();
    }
    public void resizeHold(Hold hold, int radius) {
        if (hold != selectedForResize || !canEdit() || !info.getHolds().contains(hold)) return;
        cancelSmartRequest(hold);
        selectedForResize = null;
        int bounded = Math.max(1, radius);
        if (hold.getCircleRadius() != bounded) { hold.setCircleRadius(bounded); changed(); }
    }

    private void cancelTapRecognition() {
        pendingTap = null;
        clearDeferredDoubleTap();
        if (gestures == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        try { gestures.onTouchEvent(cancel); } finally { cancel.recycle(); }
    }
    private void clearDeferredDoubleTap() {
        deferredDoubleTap = null; doubleTapTarget = null; doubleTapPending = false;
    }

    /** A second press may be a drag rather than a double tap; never apply its deletion/type action. */
    private void recoverFirstTapForDrag() {
        if (!doubleTapPending) return;
        PendingTap first = deferredDoubleTap;
        clearDeferredDoubleTap();
        if (first != null && first.target == null) {
            addHold(first.location, false);
            changed();
            downHold = hit(downX, downY);
            if (downHold != null) {
                Hold location = point(downX, downY);
                grabX = location.getX() - downHold.getX(); grabY = location.getY() - downHold.getY();
            }
        }
    }
    private void clearPointerState() {
        activePointer = MotionEvent.INVALID_POINTER_ID; downHold = null;
        dragging = panning = false;
    }
    private void cancelGestures() {
        cancelTapRecognition(); clearPointerState(); suppressSinglePointer = false;
        if (scales == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        try { scales.onTouchEvent(cancel); } finally { cancel.recycle(); }
    }
    @Override public void setEnabled(boolean enabled) {
        if (!enabled) { cancelAllSmartRequests(); choosingResize = false; selectedForResize = null; cancelGestures(); }
        super.setEnabled(enabled);
    }

    private void updateImageRect() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        viewport.configure(getWidth(), getHeight(), bitmap.getWidth(), bitmap.getHeight());
        float width = viewport.getFitWidth(), height = viewport.getFitHeight();
        imageRect.set((getWidth() - width) / 2, (getHeight() - height) / 2,
                (getWidth() + width) / 2, (getHeight() + height) / 2);
        if (info.getReferenceWidth() <= 0 || info.getReferenceHeight() <= 0) {
            if (info.getHolds().isEmpty()) info.setReferenceSize(bitmap.getWidth(), bitmap.getHeight());
            else info.setReferenceSize(Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
        }
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { updateImageRect(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getContext().getString(loadFailed ? R.string.photo_unavailable : R.string.image_loading),
                    getWidth() / 2f, getHeight() / 2f, paint);
            if (loadFailed) canvas.drawText(getContext().getString(R.string.choose_photo),
                    getWidth() / 2f, getHeight() / 2f + paint.getTextSize() * 1.8f, paint);
            return;
        }
        updateImageRect();
        displayedRect.set(viewport.getLeft(), viewport.getTop(),
                viewport.getLeft() + viewport.getImageWidth(), viewport.getTop() + viewport.getImageHeight());
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(bitmap, null, displayedRect, paint);
        canvas.save(); canvas.clipRect(displayedRect); canvas.translate(displayedRect.left, displayedRect.top);
        drawAnnotations(canvas, info, displayedRect.width(), displayedRect.height(),
                bitmap.getWidth(), bitmap.getHeight(), markerAppearance); canvas.restore();
    }
    public Canvas drawOnCustomCanvas(Canvas canvas) {
        if (bitmap != null) drawAnnotations(canvas, info, canvas.getWidth(), canvas.getHeight(),
                bitmap.getWidth(), bitmap.getHeight(), markerAppearance);
        return canvas;
    }
    /** Viewport navigation is deliberately absent from exported full-photo coordinates. */
    public static Bitmap render(Bitmap source, BoulderProblemInfo snapshot) {
        return render(source, snapshot, MarkerAppearance.defaults());
    }
    public static Bitmap render(Bitmap source, BoulderProblemInfo snapshot, MarkerAppearance appearance) {
        Bitmap output = source.copy(Bitmap.Config.ARGB_8888, true);
        drawAnnotations(new Canvas(output), snapshot, output.getWidth(), output.getHeight(),
                source.getWidth(), source.getHeight(), appearance); return output;
    }
    private static void drawAnnotations(Canvas canvas, BoulderProblemInfo info, float width, float height,
                                        float sourceWidth, float sourceHeight, MarkerAppearance appearance) {
        if (info == null || info.getReferenceWidth() <= 0 || info.getReferenceHeight() <= 0) return;
        Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
        marker.setStyle(Paint.Style.STROKE);
        float photoScale = Math.min(width / sourceWidth, height / sourceHeight);
        if (info.getHolds() != null) for (Hold hold : info.getHolds()) {
            float x = hold.projectedX(width, info.getReferenceWidth());
            float y = hold.projectedY(height, info.getReferenceHeight());
            float radius = hold.projectedRadius(width, height, info.getReferenceWidth(), info.getReferenceHeight());
            float centerline = appearance.centerlineRadius(radius, photoScale);
            if (appearance.borderWidth > 0) {
                marker.setColor(Color.argb(160, 0, 0, 0));
                marker.setStrokeWidth((appearance.strokeWidth + 2 * appearance.borderWidth) * photoScale);
                canvas.drawCircle(x, y, centerline, marker);
            }
            marker.setColor(appearance.colorFor(hold.getType()));
            marker.setStrokeWidth(appearance.strokeWidth * photoScale);
            canvas.drawCircle(x, y, centerline, marker);
        }
        String text = title(info);
        if (!text.isEmpty()) {
            Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
            float textSize = Math.max(12, height / 45f), padding = textSize * .6f;
            label.setTextSize(textSize);
            java.util.List<String> lines = new ArrayList<>();
            for (String paragraph : text.split("\\n")) {
                String remaining = paragraph;
                while (!remaining.isEmpty()) {
                    int count = label.breakText(remaining, true, Math.max(1, width - padding * 2), null);
                    count = Math.max(1, count);
                    lines.add(remaining.substring(0, count));
                    remaining = remaining.substring(count);
                }
            }
            label.setColor(Color.argb(185, 20, 29, 34));
            canvas.drawRect(0, 0, width, padding * 2 + lines.size() * textSize * 1.25f, label);
            label.setColor(Color.WHITE);
            label.setTextAlign(Paint.Align.CENTER);
            float y = padding + textSize;
            for (String line : lines) { canvas.drawText(line, width / 2, y, label); y += textSize * 1.25f; }
        }
    }

    private static String title(BoulderProblemInfo info) {
        String text = info.getName() == null ? "" : info.getName().trim();
        if (info.getGrade() != null && !info.getGrade().isEmpty()) text += (text.isEmpty() ? "" : " · ") + info.getGrade();
        if (info.getAuthor() != null && !info.getAuthor().isEmpty()) text += (text.isEmpty() ? "" : " · ") + info.getAuthor();
        if (info.getComment() != null && !info.getComment().isEmpty()) text += (text.isEmpty() ? "" : "\n") + info.getComment();
        return text;
    }

    private boolean inside(float x, float y) {
        return bitmap != null && x >= viewport.getLeft() && y >= viewport.getTop()
                && x <= viewport.getLeft() + viewport.getImageWidth()
                && y <= viewport.getTop() + viewport.getImageHeight();
    }
    private Hold point(float x, float y) {
        return new Hold(viewport.normalizedX(x) * info.getReferenceWidth(),
                viewport.normalizedY(y) * info.getReferenceHeight());
    }
    private Hold hit(float x, float y) {
        if (!inside(x, y)) return null;
        Hold nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Hold hold : info.getHolds()) {
            float hx = viewport.getLeft() + hold.projectedX(viewport.getImageWidth(), info.getReferenceWidth());
            float hy = viewport.getTop() + hold.projectedY(viewport.getImageHeight(), info.getReferenceHeight());
            float photoScale = Math.min(viewport.getImageWidth() / bitmap.getWidth(),
                    viewport.getImageHeight() / bitmap.getHeight());
            float radius = Math.max(minimumHitRadius, markerAppearance.outerRadius(
                    hold.projectedRadius(viewport.getImageWidth(), viewport.getImageHeight(),
                            info.getReferenceWidth(), info.getReferenceHeight()), photoScale));
            float dx = x - hx, dy = y - hy, distance = dx * dx + dy * dy;
            if (distance <= radius * radius && distance < nearestDistance) { nearest = hold; nearestDistance = distance; }
        }
        return nearest;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (loadFailed && isEnabled()) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) { performClick(); activity.requestPhotoRelink(); }
            return true;
        }
        if (!canEdit()) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            suppressSinglePointer = false; dragging = panning = false;
            activePointer = event.getPointerId(0);
            downX = lastX = event.getX(); downY = lastY = event.getY();
            downHold = hit(downX, downY);
            if (downHold != null) {
                cancelSmartRequest(downHold);
                Hold location = point(downX, downY);
                grabX = location.getX() - downHold.getX(); grabY = location.getY() - downHold.getY();
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            suppressSinglePointer = true; clearPointerState(); cancelTapRecognition();
        }
        scales.onTouchEvent(event);
        if (suppressSinglePointer) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                clearPointerState(); suppressSinglePointer = false;
            }
            return true;
        }
        int index = event.findPointerIndex(activePointer);
        if (index < 0) { cancelGestures(); return true; }
        float x = event.getX(index), y = event.getY(index);
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - downX, dy = y - downY;
            if (!dragging && !panning && dx * dx + dy * dy > touchSlop * touchSlop) {
                recoverFirstTapForDrag();
                dragging = downHold != null && !choosingResize && selectedForResize == null;
                panning = !dragging;
                cancelTapRecognition();
            }
            if (dragging && info.getHolds().contains(downHold)) {
                Hold location = point(x, y);
                float nx = Math.max(0, Math.min(info.getReferenceWidth(), location.getX() - grabX));
                float ny = Math.max(0, Math.min(info.getReferenceHeight(), location.getY() - grabY));
                if (nx != downHold.getX() || ny != downHold.getY()) {
                    downHold.setX(nx); downHold.setY(ny); changed();
                }
            } else if (panning) { viewport.pan(x - lastX, y - lastY); invalidate(); }
            lastX = x; lastY = y;
        }
        if (!dragging && !panning) {
            gestures.onTouchEvent(event);
            // GestureDetector cancels its prior confirmation timer on a new down.
            // A recognized double tap cleared pendingTap in onDoubleTap; otherwise
            // the prior completed tap must survive the new drag/pan/pinch sequence.
            if (action == MotionEvent.ACTION_DOWN && pendingTap != null) flushPendingSingleTap();
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            clearPointerState();
            if (action == MotionEvent.ACTION_CANCEL) cancelGestures();
        }
        return true;
    }

    /** Freeze marker geometry before any activity, view, save, or share snapshot. */
    public void prepareForSnapshot() {
        flushPendingSingleTap(); cancelAllSmartRequests();
    }

    @Override protected Parcelable onSaveInstanceState() {
        prepareForSnapshot();
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.zoom = viewport.getZoom(); state.centerX = viewport.getCenterX(); state.centerY = viewport.getCenterY();
        return state;
    }
    @Override protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) { super.onRestoreInstanceState(state); return; }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.getSuperState());
        viewport.restore(saved.zoom, saved.centerX, saved.centerY); invalidate();
    }
    public static class SavedState extends BaseSavedState {
        float zoom, centerX, centerY;
        SavedState(Parcelable parent) { super(parent); }
        SavedState(Parcel source) { super(source); zoom = source.readFloat(); centerX = source.readFloat(); centerY = source.readFloat(); }
        @Override public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags); destination.writeFloat(zoom); destination.writeFloat(centerX); destination.writeFloat(centerY);
        }
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override public SavedState createFromParcel(Parcel source) { return new SavedState(source); }
            @Override public SavedState[] newArray(int size) { return new SavedState[size]; }
        };
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private void changed() { activity.setHasUnsavedChanges(true); activity.setSaveIconVisibility(true); invalidate(); }
    public Bitmap getBitmap() { return bitmap == null ? null : render(bitmap, info, markerAppearance); }
    @Override protected void onDetachedFromWindow() {
        if (released) { super.onDetachedFromWindow(); return; }
        released = true;
        smartStatusListener = null; resizeListener = null;
        cancelAllSmartRequests(); cancelHoldResize(); imageExecutor.shutdownNow();
        // Native inference and close must run serially; shutdownNow could interrupt inference.
        final HoldEstimator closingEstimator = holdEstimator;
        smartExecutor.execute(() -> {
            try { closingEstimator.close(); } catch (RuntimeException | LinkageError ignored) { }
        });
        smartExecutor.shutdown();
        super.onDetachedFromWindow();
    }
}

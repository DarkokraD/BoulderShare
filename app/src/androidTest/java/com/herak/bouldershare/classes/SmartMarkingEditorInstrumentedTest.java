package com.herak.bouldershare.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.smart.HoldEstimator;
import com.herak.bouldershare.classes.smart.HoldFit;
import com.herak.bouldershare.enums.HoldType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Delayed fake results exercise editor races without relying on model predictions. */
@RunWith(AndroidJUnit4.class)
public class SmartMarkingEditorInstrumentedTest {
    private ActivityScenario<MainActivity> scenario;
    private SharedPreferences preferences;
    private boolean hadFirstStart, firstStart;
    private Bitmap source;
    private BoulderProblemInfo info;
    private BoulderProblemView editor;
    private ControlledEstimator estimator;
    private ThreadPoolExecutor worker;
    private final AtomicReference<BoulderProblemView.SmartMarkingStatus> status = new AtomicReference<>();

    @Before public void openEditor() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        hadFirstStart = preferences.contains("firstStart"); firstStart = preferences.getBoolean("firstStart", true);
        preferences.edit().putBoolean("firstStart", false).commit();
        source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888); source.eraseColor(Color.BLACK);
        info = new BoulderProblemInfo(); info.setInputBitmapUri(Uri.parse("file:///smart-mark-test"));
        info.setReferenceSize(600, 400); info.setHolds(new ArrayList<>());
        estimator = new ControlledEstimator();
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            activity.setmBoulderProblemInfo(info); activity.setmBoulderBitmap(source);
            activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
            activity.getSupportFragmentManager().executePendingTransactions();
            View old = activity.findViewById(R.id.boulderProblemView);
            ViewGroup parent = (ViewGroup) old.getParent();
            int index = parent.indexOfChild(old);
            ViewGroup.LayoutParams parameters = old.getLayoutParams();
            parent.removeView(old);
            editor = new BoulderProblemView(activity, estimator);
            editor.setId(R.id.boulderProblemView);
            parent.addView(editor, index, parameters);
            editor.setOnSmartMarkingStatusListener(status::set);
        });
        Field executor = BoulderProblemView.class.getDeclaredField("smartExecutor");
        executor.setAccessible(true); worker = (ThreadPoolExecutor) executor.get(editor);
        withEditor(view -> assertTrue(view.isAttachedToWindow()));
    }

    @After public void closeEditor() throws Exception {
        if (estimator != null) estimator.release.countDown();
        if (scenario != null) scenario.close();
        if (estimator != null) assertTrue("Estimator closes after inference", estimator.closed.await(10, TimeUnit.SECONDS));
        if (source != null) source.recycle();
        if (preferences != null) {
            SharedPreferences.Editor restore = preferences.edit();
            if (hadFirstStart) restore.putBoolean("firstStart", firstStart); else restore.remove("firstStart");
            restore.commit();
        }
    }

    private void withEditor(Consumer<BoulderProblemView> action) {
        scenario.onActivity(activity -> {
            editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
            editor.layout(0, 0, 600, 400); action.accept(editor);
        });
    }
    private Hold addPending() throws Exception {
        withEditor(view -> {
            view.setSmartMarkingEnabled(true); tap(view, 300, 200); view.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertEquals(16, info.getHolds().get(0).getCircleRadius());
            assertEquals(BoulderProblemView.SmartMarkingStatus.DETECTING, status.get());
        });
        assertTrue(estimator.started.await(10, TimeUnit.SECONDS));
        withEditor(BoulderProblemView::resetViewport);
        return info.getHolds().get(0);
    }
    private void releaseAndDrain() {
        estimator.release.countDown();
        long deadline = SystemClock.uptimeMillis() + 10000;
        while ((worker.getActiveCount() != 0 || !worker.getQueue().isEmpty()) && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(5);
        }
        assertEquals(0, worker.getActiveCount()); assertTrue(worker.getQueue().isEmpty());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test public void defaultOffDoesNotInvokeEstimatorAndListenerGetsCurrentStatus() {
        withEditor(view -> {
            assertFalse(view.isSmartMarkingEnabled());
            assertEquals(BoulderProblemView.SmartMarkingStatus.IDLE, status.get());
            tap(view, 300, 200); view.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size());
        });
        assertEquals(0, estimator.calls.get());
    }
    @Test public void delayedSuccessRefinesOriginalHoldInSourceCoordinates() throws Exception {
        Hold hold = addPending(); releaseAndDrain();
        withEditor(view -> {
            assertSame(hold, info.getHolds().get(0));
            assertEquals(320, hold.getX(), .001f); assertEquals(210, hold.getY(), .001f);
            assertEquals(32, hold.getCircleRadius());
            assertEquals(BoulderProblemView.SmartMarkingStatus.FITTED, status.get());
            assertTrue(((MainActivity) view.getContext()).isHasUnsavedChanges());
        });
    }
    @Test public void changingAppearanceCancelsLateFitsAndPaddingAffectsNextFit() throws Exception {
        Hold hold = addPending();
        withEditor(view -> view.setMarkerAppearance(new MarkerAppearance(8, 0, 6, Color.RED, Color.GREEN, Color.BLUE)));
        releaseAndDrain();
        assertEquals(16, hold.getCircleRadius());
        withEditor(view -> { tap(view, 450, 300); view.flushPendingSingleTap(); });
        releaseAndDrain();
        withEditor(view -> assertEquals(36, info.getHolds().get(1).getCircleRadius()));
    }
    @Test public void zoomedTapAndNavigationKeepSourceCoordinatesAndFullPhotoExport() throws Exception {
        withEditor(view -> {
            view.setSmartMarkingEnabled(true);
            BoulderProblemView.SavedState viewport = new BoulderProblemView.SavedState(View.BaseSavedState.EMPTY_STATE);
            viewport.zoom = 2; viewport.centerX = .5f; viewport.centerY = .5f;
            view.onRestoreInstanceState(viewport);
            tap(view, 400, 260); view.flushPendingSingleTap();
            view.resetViewport(); // Navigation must not invalidate a source-space result.
        });
        assertTrue(estimator.started.await(10, TimeUnit.SECONDS)); releaseAndDrain();
        withEditor(view -> {
            assertEquals(350, estimator.seedX, .001f); assertEquals(230, estimator.seedY, .001f);
            Hold hold = info.getHolds().get(0); assertEquals(370, hold.getX(), .001f);
            assertEquals(240, hold.getY(), .001f);
            Bitmap export = view.getBitmap();
            try { assertEquals(600, export.getWidth()); assertEquals(400, export.getHeight());
                assertNotEquals(Color.BLACK, export.getPixel(405, 240));
            } finally { export.recycle(); }
        });
    }
    @Test public void legacyReferenceAspectConvertsCircleUsingMinimumScale() throws Exception {
        info.setReferenceSize(300, 400);
        Hold hold = addPending(); releaseAndDrain();
        withEditor(view -> {
            assertEquals(160, hold.getX(), .001f); assertEquals(210, hold.getY(), .001f);
            assertEquals(32, hold.getCircleRadius());
            assertEquals(32, hold.projectedRadius(600, 400, 300, 400), .001f);
        });
    }
    @Test public void pointerDownCancelsBeforeGrabOffsetEvenIfNoDragOccurs() throws Exception {
        Hold hold = addPending();
        withEditor(view -> send(view, SystemClock.uptimeMillis(), 0, MotionEvent.ACTION_DOWN, 305, 200));
        releaseAndDrain();
        assertEquals(300, hold.getX(), .001f); assertEquals(200, hold.getY(), .001f); assertEquals(16, hold.getCircleRadius());
    }
    @Test public void immediateDragCannotBeOverwrittenByLateResult() throws Exception {
        Hold hold = addPending();
        withEditor(view -> drag(view, 305, 200, 405, 260)); releaseAndDrain();
        assertEquals(400, hold.getX(), .001f); assertEquals(260, hold.getY(), .001f); assertEquals(16, hold.getCircleRadius());
    }
    @Test public void deletedMarkerIsNeverRecreated() throws Exception {
        addPending(); withEditor(view -> { tap(view, 300, 200); view.flushPendingSingleTap(); }); releaseAndDrain();
        assertTrue(info.getHolds().isEmpty());
    }
    @Test public void resizeSelectionCancelsEvenWhenDialogIsCancelled() throws Exception {
        Hold hold = addPending();
        withEditor(view -> { assertTrue(view.requestHoldResize()); tap(view, 300, 200); view.cancelHoldResize(); });
        releaseAndDrain(); assertEquals(16, hold.getCircleRadius()); assertEquals(300, hold.getX(), .001f);
    }
    @Test public void doubleTapTypeChangeSurvivesLateResult() throws Exception {
        Hold hold = addPending();
        withEditor(view -> {
            long down = SystemClock.uptimeMillis();
            send(view, down, 0, MotionEvent.ACTION_DOWN, 300, 200); send(view, down, 20, MotionEvent.ACTION_UP, 300, 200);
            send(view, down, 100, MotionEvent.ACTION_DOWN, 300, 200); send(view, down, 120, MotionEvent.ACTION_UP, 300, 200);
        });
        releaseAndDrain(); assertEquals(HoldType.REGULAR_HOLD, hold.getType()); assertEquals(16, hold.getCircleRadius());
    }
    @Test public void switchingOffAndOnDoesNotReviveCancelledResult() throws Exception {
        Hold hold = addPending();
        withEditor(view -> { view.setSmartMarkingEnabled(false); view.setSmartMarkingEnabled(true); });
        releaseAndDrain(); assertEquals(16, hold.getCircleRadius()); assertEquals(300, hold.getX(), .001f);
    }
    @Test public void disabledSaveBoundaryFreezesGeometry() throws Exception {
        Hold hold = addPending(); withEditor(view -> view.setEnabled(false)); releaseAndDrain();
        assertEquals(16, hold.getCircleRadius()); assertEquals(300, hold.getX(), .001f);
    }
    @Test public void snapshotFlushesPendingTapAndFreezesLateResult() throws Exception {
        withEditor(view -> {
            view.setSmartMarkingEnabled(true); tap(view, 300, 200); view.prepareForSnapshot();
            assertEquals(1, info.getHolds().size());
        });
        releaseAndDrain(); assertEquals(16, info.getHolds().get(0).getCircleRadius());
    }
    @Test public void replacingModelCancelsOldHoldResult() throws Exception {
        Hold hold = addPending();
        BoulderProblemInfo replacement = new BoulderProblemInfo(); replacement.setHolds(new ArrayList<>());
        replacement.setReferenceSize(600, 400);
        withEditor(view -> view.setBoulderProblemInfo(replacement)); releaseAndDrain();
        assertEquals(16, hold.getCircleRadius()); assertTrue(replacement.getHolds().isEmpty());
    }
    @Test public void detachDropsResultClearsListenerAndClosesOnInferenceWorker() throws Exception {
        Hold hold = addPending(); AtomicInteger callbacks = new AtomicInteger();
        withEditor(view -> {
            view.setOnSmartMarkingStatusListener(value -> callbacks.incrementAndGet());
            ((ViewGroup) view.getParent()).removeView(view);
        });
        int afterDetach = callbacks.get(); releaseAndDrain();
        assertTrue(estimator.closed.await(10, TimeUnit.SECONDS));
        assertEquals(afterDetach, callbacks.get()); assertEquals(16, hold.getCircleRadius());
        assertSame(estimator.inferenceThread, estimator.closeThread);
    }
    @Test public void nullFitKeepsManualMarker() throws Exception {
        estimator.noFit = true; Hold hold = addPending(); releaseAndDrain();
        assertEquals(16, hold.getCircleRadius()); assertEquals(BoulderProblemView.SmartMarkingStatus.MANUAL_FALLBACK, status.get());
    }
    @Test public void missingNativeLibraryDoesNotCrashOrRepeatedlyRetry() throws Exception {
        estimator.unavailable = true; Hold hold = addPending(); releaseAndDrain();
        assertEquals(16, hold.getCircleRadius()); assertEquals(BoulderProblemView.SmartMarkingStatus.UNAVAILABLE, status.get());
        withEditor(view -> { tap(view, 100, 100); view.flushPendingSingleTap(); });
        assertEquals(1, estimator.calls.get()); assertEquals(2, info.getHolds().size());
    }
    @Test public void rapidTapsHaveBoundedQueueAndKeepManualMarkersForOverflow() throws Exception {
        addPending();
        withEditor(view -> {
            float[][] points = {{60, 60}, {180, 60}, {420, 60}, {540, 60}, {60, 330}, {180, 330}, {420, 330}, {540, 330}};
            for (float[] point : points) {
                tap(view, point[0], point[1]); view.flushPendingSingleTap(); view.resetViewport();
            }
            assertTrue(worker.getQueue().size() <= 2); assertEquals(9, info.getHolds().size());
        });
        releaseAndDrain(); assertTrue(estimator.calls.get() <= 3);
        int fitted = 0, manual = 0;
        for (Hold hold : info.getHolds()) { if (hold.getCircleRadius() == 32) fitted++; else if (hold.getCircleRadius() == 16) manual++; }
        assertEquals(3, fitted); assertEquals(6, manual);
    }
    @Test public void secondPressDragRecoveryNeverStartsInference() {
        withEditor(view -> {
            view.setSmartMarkingEnabled(true);
            long down = SystemClock.uptimeMillis();
            send(view, down, 0, MotionEvent.ACTION_DOWN, 300, 200); send(view, down, 20, MotionEvent.ACTION_UP, 300, 200);
            send(view, down, 100, MotionEvent.ACTION_DOWN, 300, 200);
            send(view, down, 120, MotionEvent.ACTION_MOVE, 400, 260); send(view, down, 140, MotionEvent.ACTION_UP, 400, 260);
            view.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertEquals(400, info.getHolds().get(0).getX(), .001f);
        });
        assertEquals(0, estimator.calls.get());
    }

    private static void tap(BoulderProblemView view, float x, float y) {
        long now = SystemClock.uptimeMillis(); send(view, now, 0, MotionEvent.ACTION_DOWN, x, y);
        send(view, now, 20, MotionEvent.ACTION_UP, x, y);
    }
    private static void drag(BoulderProblemView view, float fromX, float fromY, float toX, float toY) {
        long now = SystemClock.uptimeMillis(); send(view, now, 0, MotionEvent.ACTION_DOWN, fromX, fromY);
        send(view, now, 20, MotionEvent.ACTION_MOVE, toX, toY); send(view, now, 30, MotionEvent.ACTION_UP, toX, toY);
    }
    private static void send(BoulderProblemView view, long down, long offset, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, down + offset, action, x, y, 0);
        try { view.onTouchEvent(event); } finally { event.recycle(); }
    }

    private static final class ControlledEstimator implements HoldEstimator {
        final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), closed = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean noFit, unavailable;
        volatile float seedX, seedY;
        volatile Thread inferenceThread, closeThread;
        @Override public HoldFit estimate(Bitmap image, float x, float y) {
            inferenceThread = Thread.currentThread(); seedX = x; seedY = y;
            calls.incrementAndGet(); started.countDown();
            try { if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release inference"); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            if (unavailable) throw new UnsatisfiedLinkError("Synthetic missing native library");
            return noFit ? null : new HoldFit(x + 20, y + 10, 30);
        }
        @Override public void close() { closeThread = Thread.currentThread(); closed.countDown(); }
    }
}

package com.herak.bouldershare.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.enums.HoldType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BoulderGestureInstrumentedTest {
    private ActivityScenario<MainActivity> scenario;
    private SharedPreferences preferences;
    private boolean hadFirstStart, firstStart;
    private Bitmap source;
    private File photo;
    private BoulderProblemInfo info;

    @Before public void openEditor() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        hadFirstStart = preferences.contains("firstStart"); firstStart = preferences.getBoolean("firstStart", true);
        preferences.edit().putBoolean("firstStart", false).commit();
        source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888); source.eraseColor(Color.BLACK);
        photo = File.createTempFile("gesture_test_", ".jpg", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(photo)) { source.compress(Bitmap.CompressFormat.JPEG, 100, output); }
        info = new BoulderProblemInfo(); info.setInputBitmapUri(Uri.fromFile(photo));
        info.setReferenceSize(600, 400); info.setHolds(new ArrayList<>());
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            activity.setmBoulderProblemInfo(info); activity.setmBoulderBitmap(source);
            activity.setHasUnsavedChanges(false); activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
            activity.getSupportFragmentManager().executePendingTransactions();
        });
    }
    @After public void closeEditor() {
        if (scenario != null) scenario.close();
        if (source != null) source.recycle();
        if (photo != null) photo.delete();
        SharedPreferences.Editor restore = preferences.edit();
        if (hadFirstStart) restore.putBoolean("firstStart", firstStart); else restore.remove("firstStart");
        restore.commit();
    }
    private void withEditor(Consumer<BoulderProblemView> test) {
        scenario.onActivity(activity -> {
            BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
            editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
            editor.layout(0, 0, 600, 400); assertTrue(editor.isImageReady()); test.accept(editor);
        });
    }

    @Test public void pinchOverHoldZoomsWithoutResizingMovingOrDirtying() {
        Hold hold = new Hold(300, 200, 8); info.getHolds().add(hold);
        withEditor(editor -> {
            pinch(editor);
            assertTrue(state(editor).zoom > 1);
            assertEquals(8, hold.getCircleRadius()); assertEquals(300, hold.getX(), .001f);
            assertEquals(200, hold.getY(), .001f); assertEquals(1, info.getHolds().size());
            assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
            editor.flushPendingSingleTap(); assertEquals(1, info.getHolds().size());
        });
    }

    @Test public void immediateDragUsesGrabOffsetAndTapJitterDoesNotMoveHold() {
        Hold hold = new Hold(300, 200, 8); info.getHolds().add(hold);
        withEditor(editor -> {
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 305, 200);
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 405, 260);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 405, 260);
            assertEquals(400, hold.getX(), .001f); assertEquals(260, hold.getY(), .001f);
            assertEquals(1, info.getHolds().size()); editor.flushPendingSingleTap(); assertEquals(1, info.getHolds().size());
            ((MainActivity) editor.getContext()).setHasUnsavedChanges(false);
            editor.resetViewport();
            float jitter = Math.max(1, ViewConfiguration.get(editor.getContext()).getScaledTouchSlop() / 4f);
            tap(editor, 400, 260, jitter);
            assertEquals(400, hold.getX(), .001f); assertEquals(260, hold.getY(), .001f);
            assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
            editor.flushPendingSingleTap(); assertTrue(info.getHolds().isEmpty());
        });
    }

    @Test public void emptyDragPansAndPointerTransitionsCannotCreateOrMoveMarkers() {
        withEditor(editor -> {
            pinch(editor);
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 50, 50);
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 150, 120);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 150, 120);
            editor.flushPendingSingleTap(); assertTrue(info.getHolds().isEmpty());
            assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
            editor.resetViewport();
            Hold hold = new Hold(300, 200, 12); info.getHolds().add(hold);
            down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 10, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0, 1}, 300, 200, 400, 200);
            send(editor, down, 20, MotionEvent.ACTION_POINTER_UP, new int[]{0, 1}, 300, 200, 400, 200);
            send(editor, down, 30, MotionEvent.ACTION_MOVE, new int[]{1}, 450, 300);
            send(editor, down, 40, MotionEvent.ACTION_UP, new int[]{1}, 450, 300);
            editor.flushPendingSingleTap(); assertEquals(300, hold.getX(), .001f); assertEquals(200, hold.getY(), .001f);
            assertEquals(1, info.getHolds().size()); assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
        });
    }

    @Test public void zoomedAddAndDragExportTheFullPhotoInReferenceCoordinates() {
        withEditor(editor -> {
            pinch(editor);
            BoulderProblemView.SavedState viewport = state(editor);
            tap(editor, 400, 260, 0); editor.flushPendingSingleTap();
            Hold hold = info.getHolds().get(0);
            assertEquals(300 + 100 / viewport.zoom, hold.getX(), .01f);
            assertEquals(200 + 60 / viewport.zoom, hold.getY(), .01f);
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 400, 260);
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 460, 300);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 460, 300);
            assertEquals(300 + 160 / viewport.zoom, hold.getX(), .01f);
            assertEquals(200 + 100 / viewport.zoom, hold.getY(), .01f);
            Bitmap export = editor.getBitmap();
            try {
                assertEquals(600, export.getWidth()); assertEquals(400, export.getHeight());
                int pixel = export.getPixel(Math.round(hold.getX() + hold.getCircleRadius()), Math.round(hold.getY()));
                assertTrue(Color.green(pixel) > Color.red(pixel));
                assertEquals(Color.BLACK, source.getPixel(Math.round(hold.getX()), Math.round(hold.getY())));
            } finally { export.recycle(); }
        });
    }

    @Test public void nearestHitAndDelayedTapKeepTheOriginalTarget() {
        Hold first = new Hold(294, 200, 2), nearer = new Hold(310, 200, 2);
        info.getHolds().add(first); info.getHolds().add(nearer);
        withEditor(editor -> {
            tap(editor, 307, 200, 0);
            nearer.setX(500); nearer.setY(350);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertSame(first, info.getHolds().get(0));
        });
    }

    @Test public void completedTapSurvivesAnImmediateIndependentPanGesture() {
        withEditor(editor -> {
            tap(editor, 20, 20, 0);
            assertTrue(info.getHolds().isEmpty());
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 580, 380);
            assertEquals(1, info.getHolds().size());
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 500, 300);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 500, 300);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size());
            assertEquals(20, info.getHolds().get(0).getX(), .001f);
            assertEquals(20, info.getHolds().get(0).getY(), .001f);
        });
    }

    @Test public void tapEmptyThenImmediateSameSpotDragAddsAndMovesExactlyOneHold() {
        withEditor(editor -> {
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 20, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            send(editor, down, 80, MotionEvent.ACTION_DOWN, new int[]{0}, 302, 202);
            assertTrue(info.getHolds().isEmpty());
            send(editor, down, 100, MotionEvent.ACTION_MOVE, new int[]{0}, 420, 280);
            send(editor, down, 120, MotionEvent.ACTION_UP, new int[]{0}, 420, 280);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size());
            assertEquals(418, info.getHolds().get(0).getX(), .001f);
            assertEquals(278, info.getHolds().get(0).getY(), .001f);
            assertEquals(HoldType.START_HOLD, info.getHolds().get(0).getType());
        });
    }

    @Test public void secondPressDragPreservesExistingHoldTypeAndDoesNotDeleteIt() {
        Hold hold = new Hold(300, 200, 12); hold.setType(HoldType.REGULAR_HOLD); info.getHolds().add(hold);
        withEditor(editor -> {
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 20, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            send(editor, down, 80, MotionEvent.ACTION_DOWN, new int[]{0}, 305, 200);
            assertEquals(HoldType.REGULAR_HOLD, hold.getType());
            send(editor, down, 100, MotionEvent.ACTION_MOVE, new int[]{0}, 405, 260);
            send(editor, down, 120, MotionEvent.ACTION_UP, new int[]{0}, 405, 260);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertSame(hold, info.getHolds().get(0));
            assertEquals(400, hold.getX(), .001f); assertEquals(260, hold.getY(), .001f);
            assertEquals(HoldType.REGULAR_HOLD, hold.getType());
        });
    }

    @Test public void stationaryDoubleTapChangesTypeOnlyOnSecondReleaseWithoutDeletion() {
        Hold hold = new Hold(300, 200, 12); hold.setType(HoldType.REGULAR_HOLD); info.getHolds().add(hold);
        withEditor(editor -> {
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 20, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            send(editor, down, 80, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            assertEquals(HoldType.REGULAR_HOLD, hold.getType());
            assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
            send(editor, down, 100, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertEquals(HoldType.TOP_HOLD, hold.getType());
            assertTrue(((MainActivity) editor.getContext()).isHasUnsavedChanges());
        });
    }

    @Test public void secondPointerCancelsDeferredDoubleTapWithoutTypeChangeOrDeletion() {
        Hold hold = new Hold(300, 200, 12); hold.setType(HoldType.REGULAR_HOLD); info.getHolds().add(hold);
        withEditor(editor -> {
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 20, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            send(editor, down, 80, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 90, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0, 1}, 300, 200, 400, 200);
            send(editor, down, 100, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0, 1}, 300, 200, 400, 200);
            send(editor, down, 110, MotionEvent.ACTION_UP, new int[]{0}, 300, 200);
            editor.flushPendingSingleTap();
            assertEquals(1, info.getHolds().size()); assertEquals(HoldType.REGULAR_HOLD, hold.getType());
            assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
        });
    }

    @Test public void resizeSelectionAllowsNavigationAndNeverMovesOrAddsHolds() {
        Hold hold = new Hold(300, 200, 8); info.getHolds().add(hold);
        AtomicReference<Hold> selected = new AtomicReference<>();
        withEditor(editor -> {
            editor.setOnHoldResizeRequestedListener(selected::set);
            assertTrue(editor.requestHoldResize());
            tap(editor, 50, 50, 0); assertTrue(editor.isChoosingHoldToResize());
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 300, 200);
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 450, 280);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 450, 280);
            pinch(editor); editor.resetViewport();
            assertEquals(300, hold.getX(), .001f); assertEquals(200, hold.getY(), .001f);
            assertEquals(1, info.getHolds().size()); assertFalse(((MainActivity) editor.getContext()).isHasUnsavedChanges());
            tap(editor, 300, 200, 0); assertSame(hold, selected.get());
            assertFalse(editor.isChoosingHoldToResize());
            editor.cancelHoldResize(); editor.resizeHold(hold, 80);
            assertEquals(8, hold.getCircleRadius());
        });
    }

    @Test public void recreationRestoresViewportEvenWhenBitmapLoadsAsynchronously() {
        AtomicReference<BoulderProblemView.SavedState> before = new AtomicReference<>();
        withEditor(editor -> {
            pinch(editor);
            long down = SystemClock.uptimeMillis();
            send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 40, 40);
            send(editor, down, 20, MotionEvent.ACTION_MOVE, new int[]{0}, 110, 40);
            send(editor, down, 30, MotionEvent.ACTION_UP, new int[]{0}, 110, 40);
            before.set(state(editor));
            ((MainActivity) editor.getContext()).setmBoulderBitmap(null);
        });
        scenario.recreate();
        long deadline = SystemClock.uptimeMillis() + 10000;
        boolean[] ready = {false};
        while (!ready[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> { BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView); ready[0] = editor != null && editor.isImageReady(); });
            if (!ready[0]) SystemClock.sleep(30);
        }
        assertTrue(ready[0]);
        withEditor(editor -> {
            BoulderProblemView.SavedState after = state(editor);
            assertEquals(before.get().zoom, after.zoom, .001f);
            assertEquals(before.get().centerX, after.centerX, .001f);
            assertEquals(before.get().centerY, after.centerY, .001f);
            assertTrue(info.getHolds().isEmpty());
        });
    }

    private static BoulderProblemView.SavedState state(BoulderProblemView editor) {
        SparseArray<Parcelable> state = new SparseArray<>(); editor.saveHierarchyState(state);
        return (BoulderProblemView.SavedState) state.get(editor.getId());
    }
    private static void tap(BoulderProblemView editor, float x, float y, float jitter) {
        long down = SystemClock.uptimeMillis();
        send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, x, y);
        if (jitter != 0) send(editor, down, 10, MotionEvent.ACTION_MOVE, new int[]{0}, x + jitter, y);
        send(editor, down, 20, MotionEvent.ACTION_UP, new int[]{0}, x + jitter, y);
    }
    private static void pinch(BoulderProblemView editor) {
        ViewConfiguration configuration = ViewConfiguration.get(editor.getContext());
        float minimumSpan = Build.VERSION.SDK_INT >= 29 ? configuration.getScaledMinimumScalingSpan()
                : 200 * editor.getResources().getDisplayMetrics().density;
        // Span thresholds use device pixels, even though these tests lay out a small editor.
        float span = Math.max(160, minimumSpan + configuration.getScaledTouchSlop() * 3f);
        long down = SystemClock.uptimeMillis();
        send(editor, down, 0, MotionEvent.ACTION_DOWN, new int[]{0}, 250, 200);
        send(editor, down, 10, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new int[]{0, 1}, 250, 200, 350, 200);
        send(editor, down, 30, MotionEvent.ACTION_MOVE, new int[]{0, 1}, 300 - span / 2, 200, 300 + span / 2, 200);
        send(editor, down, 60, MotionEvent.ACTION_MOVE, new int[]{0, 1}, 300 - span * .65f, 200, 300 + span * .65f, 200);
        send(editor, down, 90, MotionEvent.ACTION_MOVE, new int[]{0, 1}, 300 - span * .9f, 200, 300 + span * .9f, 200);
        send(editor, down, 100, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new int[]{0, 1}, 300 - span * .9f, 200, 300 + span * .9f, 200);
        send(editor, down, 110, MotionEvent.ACTION_UP, new int[]{0}, 300 - span * .9f, 200);
        assertTrue("Pinch must cross the device's minimum span " + minimumSpan, state(editor).zoom > 1);
    }
    private static void send(BoulderProblemView editor, long down, long offset, int action, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[ids.length];
        for (int index = 0; index < ids.length; index++) {
            properties[index] = new MotionEvent.PointerProperties(); properties[index].id = ids[index];
            properties[index].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordinates[index] = new MotionEvent.PointerCoords(); coordinates[index].x = xy[index * 2];
            coordinates[index].y = xy[index * 2 + 1]; coordinates[index].pressure = 1; coordinates[index].size = 1;
        }
        MotionEvent event = MotionEvent.obtain(down, down + offset, action, ids.length, properties, coordinates,
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { editor.onTouchEvent(event); } finally { event.recycle(); }
    }
}

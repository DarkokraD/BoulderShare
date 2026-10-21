package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.EditorSettings;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.enums.HoldType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EditorControlsInstrumentedTest {
    private ActivityScenario<MainActivity> scenario;
    private SharedPreferences preferences;
    private final Map<String, Boolean> previousPreferences = new HashMap<>();
    private Bitmap source;
    private File photo;

    @Before public void openEditor() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        for (String key : new String[]{"firstStart", "smart_marking", "editor_panel_expanded", "show_editor_instructions"}) {
            previousPreferences.put(key, preferences.contains(key) ? preferences.getBoolean(key, true) : null);
        }
        preferences.edit().putBoolean("firstStart", false).putBoolean("smart_marking", false)
                .putBoolean("editor_panel_expanded", true).putBoolean("show_editor_instructions", true).commit();
        source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        photo = File.createTempFile("editor_controls_", ".jpg", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(photo)) {
            source.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            BoulderProblemInfo info = new BoulderProblemInfo();
            info.setInputBitmapUri(Uri.fromFile(photo));
            info.setReferenceSize(600, 400);
            info.setHolds(new ArrayList<>());
            info.getHolds().add(new Hold(300, 200, 20));
            activity.setmBoulderProblemInfo(info);
            activity.setmBoulderBitmap(source);
            activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
            activity.getSupportFragmentManager().executePendingTransactions();
            activity.setHasUnsavedChanges(false);
        });
    }

    @After public void closeEditor() {
        if (scenario != null) scenario.close();
        if (source != null) source.recycle();
        if (photo != null) photo.delete();
        SharedPreferences.Editor restore = preferences.edit();
        for (Map.Entry<String, Boolean> entry : previousPreferences.entrySet()) {
            if (entry.getValue() == null) restore.remove(entry.getKey());
            else restore.putBoolean(entry.getKey(), entry.getValue());
        }
        restore.commit();
    }

    @Test public void collapsedPanelPersistsWithoutDirtyingRouteAndKeepsAnAccessibleHandle() {
        scenario.onActivity(activity -> {
            activity.findViewById(R.id.legend_top).performClick();
            activity.findViewById(R.id.action_toggle_editor_controls).performClick();
            BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
            assertFalse(editor.isAssigningHoldType());
            assertEquals(HoldType.TOP_HOLD, editor.getCurrentHoldType());
            assertFalse(EditorSettings.isPanelExpanded(activity));
            assertFalse(activity.isHasUnsavedChanges());
        });
        scenario.recreate();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            assertEquals(View.GONE, activity.findViewById(R.id.editor_controls_content).getVisibility());
            View handle = activity.findViewById(R.id.action_toggle_editor_controls);
            assertTrue(handle.isShown());
            assertTrue(handle.isClickable());
            assertNotNull(handle.getContentDescription());
            assertTrue(handle.getHeight() >= Math.round(48 * activity.getResources().getDisplayMetrics().density));
            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                assertEquals(Math.round(56 * activity.getResources().getDisplayMetrics().density),
                        activity.findViewById(R.id.editor_controls).getWidth());
            }
            BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
            assertEquals(HoldType.TOP_HOLD, editor.getCurrentHoldType());
            assertFalse(editor.isAssigningHoldType());
            assertFalse(activity.isHasUnsavedChanges());
            handle.performClick();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.editor_controls_content).getVisibility());
            assertTrue(activity.findViewById(R.id.legend_top).isSelected());
            assertTrue(EditorSettings.isPanelExpanded(activity));
        });
    }

    @Test public void instructionPreferenceUpdatesWhenReturningToEditor() {
        scenario.onActivity(activity -> assertEquals(View.VISIBLE,
                activity.findViewById(R.id.editor_gesture_hint).getVisibility()));
        scenario.moveToState(Lifecycle.State.STARTED);
        EditorSettings.setShowInstructions(ApplicationProvider.getApplicationContext(), false);
        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(activity -> {
            assertEquals(View.GONE, activity.findViewById(R.id.editor_gesture_hint).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.smart_marking_status).getVisibility());
            assertTrue(activity.findViewById(R.id.action_resize_hold).isShown());
            activity.findViewById(R.id.legend_regular).performClick();
            assertEquals(View.GONE, activity.findViewById(R.id.editor_gesture_hint).getVisibility());
            assertTrue(activity.findViewById(R.id.legend_regular).isSelected());
            assertFalse(activity.isHasUnsavedChanges());
        });
        scenario.moveToState(Lifecycle.State.STARTED);
        EditorSettings.setShowInstructions(ApplicationProvider.getApplicationContext(), true);
        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(activity -> assertEquals(View.VISIBLE,
                activity.findViewById(R.id.editor_gesture_hint).getVisibility()));
    }

    @Test public void legendAssignsExistingHoldAndAddsNewHoldThenOrdinaryEditingResumes() {
        withEditor(editor -> {
            MainActivity activity = (MainActivity) editor.getContext();
            Hold target = editor.getBoulderProblemInfo().getHolds().get(0);
            activity.findViewById(R.id.legend_top).performClick();
            assertTrue(activity.findViewById(R.id.legend_top).isSelected());
            assertTrue(activity.findViewById(R.id.legend_top).isActivated());
            assertFalse(activity.isHasUnsavedChanges());
            tap(editor, 300, 200);
            editor.flushPendingSingleTap();
            assertEquals(HoldType.TOP_HOLD, target.getType());
            assertEquals(1, editor.getBoulderProblemInfo().getHolds().size());
            assertFalse(editor.isAssigningHoldType());
            assertTrue(activity.isHasUnsavedChanges());
            activity.findViewById(R.id.legend_regular).performClick();
            tap(editor, 100, 100);
            editor.flushPendingSingleTap();
            assertEquals(2, editor.getBoulderProblemInfo().getHolds().size());
            assertEquals(HoldType.REGULAR_HOLD, editor.getBoulderProblemInfo().getHolds().get(1).getType());
            assertFalse(editor.isAssigningHoldType());
            editor.cancelHoldResize(); // Reset recognition before a separate ordinary tap.
            tap(editor, 300, 200);
            editor.flushPendingSingleTap();
            assertFalse(editor.getBoulderProblemInfo().getHolds().contains(target));
        });
    }

    @Test public void selectedTypeCancelsPendingAssignmentAndSameTypeAssignmentStaysClean() {
        withEditor(editor -> {
            MainActivity activity = (MainActivity) editor.getContext();
            Hold target = editor.getBoulderProblemInfo().getHolds().get(0);
            activity.findViewById(R.id.legend_start).performClick();
            tap(editor, 300, 200);
            editor.flushPendingSingleTap();
            assertEquals(HoldType.START_HOLD, target.getType());
            assertFalse(activity.isHasUnsavedChanges());
            activity.findViewById(R.id.legend_top).performClick();
            tap(editor, 300, 200);
            activity.findViewById(R.id.legend_top).performClick();
            editor.flushPendingSingleTap();
            assertFalse(editor.isAssigningHoldType());
            assertEquals(HoldType.START_HOLD, target.getType());
            assertFalse(activity.isHasUnsavedChanges());
            activity.findViewById(R.id.legend_regular).performClick();
            tap(editor, 300, 200);
            activity.findViewById(R.id.action_resize_hold).performClick();
            editor.flushPendingSingleTap();
            assertTrue(editor.isChoosingHoldToResize());
            assertFalse(editor.isAssigningHoldType());
            assertEquals(HoldType.START_HOLD, target.getType());
            assertFalse(activity.isHasUnsavedChanges());
            activity.findViewById(R.id.action_toggle_editor_controls).performClick();
            assertFalse(editor.isChoosingHoldToResize());
            editor.flushPendingSingleTap();
            assertEquals(1, editor.getBoulderProblemInfo().getHolds().size());
        });
    }

    @Test public void doubleTapUpdatesLegendAndAssignmentDragCannotMoveOrRetypeHold() {
        withEditor(editor -> {
            MainActivity activity = (MainActivity) editor.getContext();
            long down = SystemClock.uptimeMillis();
            send(editor, down, down, MotionEvent.ACTION_DOWN, 300, 200);
            send(editor, down, down + 10, MotionEvent.ACTION_UP, 300, 200);
            send(editor, down + 60, down + 60, MotionEvent.ACTION_DOWN, 300, 200);
            send(editor, down + 60, down + 70, MotionEvent.ACTION_UP, 300, 200);
            Hold target = editor.getBoulderProblemInfo().getHolds().get(0);
            assertEquals(HoldType.REGULAR_HOLD, target.getType());
            assertTrue(activity.findViewById(R.id.legend_regular).isSelected());
            assertFalse(activity.findViewById(R.id.legend_start).isSelected());
            activity.setHasUnsavedChanges(false);
            activity.findViewById(R.id.legend_top).performClick();
            down = SystemClock.uptimeMillis();
            send(editor, down, down, MotionEvent.ACTION_DOWN, 300, 200);
            send(editor, down, down + 20, MotionEvent.ACTION_MOVE, 420, 260);
            send(editor, down, down + 40, MotionEvent.ACTION_UP, 420, 260);
            editor.flushPendingSingleTap();
            assertEquals(300, target.getX(), .001f);
            assertEquals(200, target.getY(), .001f);
            assertEquals(HoldType.REGULAR_HOLD, target.getType());
            assertFalse(activity.isHasUnsavedChanges());
            editor.setEnabled(false);
            assertFalse(editor.isAssigningHoldType());
            assertEquals(HoldType.TOP_HOLD, editor.getCurrentHoldType());
        });
    }

    @Test public void doubleTapDuringExplicitAssignmentAppliesTypeOnceWithoutDeletion() {
        withEditor(editor -> {
            MainActivity activity = (MainActivity) editor.getContext();
            activity.findViewById(R.id.legend_top).performClick();
            long down = SystemClock.uptimeMillis();
            send(editor, down, down, MotionEvent.ACTION_DOWN, 300, 200);
            send(editor, down, down + 10, MotionEvent.ACTION_UP, 300, 200);
            send(editor, down + 60, down + 60, MotionEvent.ACTION_DOWN, 300, 200);
            send(editor, down + 60, down + 70, MotionEvent.ACTION_UP, 300, 200);
            editor.flushPendingSingleTap();
            assertEquals(1, editor.getBoulderProblemInfo().getHolds().size());
            assertEquals(HoldType.TOP_HOLD, editor.getBoulderProblemInfo().getHolds().get(0).getType());
            assertEquals(HoldType.TOP_HOLD, editor.getCurrentHoldType());
            assertFalse(editor.isAssigningHoldType());
            assertTrue(activity.findViewById(R.id.legend_top).isSelected());
        });
    }

    @Test public void collapseKeepsCompletedOrdinaryTapsAndCancelsExplicitAssignment() {
        withEditor(editor -> {
            MainActivity activity = (MainActivity) editor.getContext();
            View toggle = activity.findViewById(R.id.action_toggle_editor_controls);
            tap(editor, 100, 100);
            toggle.performClick();
            assertEquals("Completed ordinary addition survives immediate collapse", 2,
                    editor.getBoulderProblemInfo().getHolds().size());
            assertTrue(activity.isHasUnsavedChanges());
            toggle.performClick();
            activity.setHasUnsavedChanges(false);
            tap(editor, 100, 100);
            toggle.performClick();
            assertEquals("Completed ordinary removal survives immediate collapse", 1,
                    editor.getBoulderProblemInfo().getHolds().size());
            assertTrue(activity.isHasUnsavedChanges());
            toggle.performClick();
            activity.setHasUnsavedChanges(false);
            activity.findViewById(R.id.legend_top).performClick();
            tap(editor, 300, 200);
            toggle.performClick();
            editor.flushPendingSingleTap();
            assertEquals(HoldType.START_HOLD, editor.getBoulderProblemInfo().getHolds().get(0).getType());
            assertFalse(editor.isAssigningHoldType());
            assertFalse(activity.isHasUnsavedChanges());
        });
    }

    private void withEditor(Consumer<BoulderProblemView> action) {
        scenario.onActivity(activity -> {
            BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
            editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
            editor.layout(0, 0, 600, 400);
            action.accept(editor);
        });
    }

    private static void tap(BoulderProblemView editor, float x, float y) {
        long down = SystemClock.uptimeMillis();
        send(editor, down, down, MotionEvent.ACTION_DOWN, x, y);
        send(editor, down, down + 10, MotionEvent.ACTION_UP, x, y);
    }

    private static void send(BoulderProblemView editor, long down, long time, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, time, action, x, y, 0);
        try { editor.onTouchEvent(event); } finally { event.recycle(); }
    }
}

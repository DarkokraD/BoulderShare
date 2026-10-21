package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.slider.Slider;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.Hold;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HoldResizeInstrumentedTest {
    @Test public void bottomPanelPreviewsOnlySelectedHoldAndApplyCommitsItsSize() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        boolean hadFirstStart = preferences.contains("firstStart");
        boolean firstStart = preferences.getBoolean("firstStart", true);
        preferences.edit().putBoolean("firstStart", false).commit();
        Bitmap source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.BLACK);
        BoulderProblemInfo info = new BoulderProblemInfo();
        info.setReferenceSize(600, 400);
        info.setHolds(new ArrayList<>());
        Hold target = new Hold(300, 200, 37);
        Hold other = new Hold(100, 100, 24);
        info.getHolds().add(target);
        info.getHolds().add(other);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setmBoulderProblemInfo(info);
                activity.setmBoulderBitmap(source);
                activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
                activity.getSupportFragmentManager().executePendingTransactions();
                activity.setHasUnsavedChanges(false);
            });
            selectTarget(scenario);
            Bitmap[] original = new Bitmap[1];
            scenario.onActivity(activity -> {
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                View panel = activity.findViewById(R.id.hold_size_panel);
                assertSame(editor.getRootView(), panel.getRootView());
                assertEquals(View.GONE, activity.findViewById(R.id.editor_controls).getVisibility());
                assertTrue("Resize panel stays below the photo", panel.getTop() >=
                        activity.findViewById(R.id.rlBoulderFragment).getBottom());
                original[0] = drawEditor(editor);
            });
            onView(withId(R.id.hold_size)).perform(dragSliderToMinimum(original[0]));
            scenario.onActivity(activity -> {
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                Bitmap preview = drawEditor(editor);
                Bitmap export = editor.getBitmap();
                try {
                    assertFalse("Slider redraws the marker immediately", original[0].sameAs(preview));
                    assertTrue("Unapplied preview stays out of exports", original[0].sameAs(export));
                    for (int y = 60; y < 140; y++) for (int x = 60; x < 140; x++) {
                        assertEquals("Other hold is unchanged", original[0].getPixel(x, y), preview.getPixel(x, y));
                    }
                    assertEquals(37, target.getCircleRadius());
                    assertFalse(activity.isHasUnsavedChanges());
                } finally { preview.recycle(); export.recycle(); }
            });
            onView(withId(R.id.hold_size_cancel)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(37, target.getCircleRadius());
                assertEquals(2, info.getHolds().size());
                assertFalse(activity.isHasUnsavedChanges());
                Bitmap cancelled = drawEditor(activity.findViewById(R.id.boulderProblemView));
                try { assertTrue("Cancel restores the picture", original[0].sameAs(cancelled)); }
                finally { cancelled.recycle(); original[0].recycle(); }
                assertEquals(View.GONE, activity.findViewById(R.id.hold_size_panel).getVisibility());
            });

            // Merely opening the control and applying must not round the existing size.
            selectTarget(scenario);
            onView(withId(R.id.hold_size_apply)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(37, target.getCircleRadius());
                assertFalse(activity.isHasUnsavedChanges());
            });

            selectTarget(scenario);
            onView(withId(R.id.hold_size)).perform(setSliderValue(100));
            scenario.onActivity(activity -> activity.getOnBackPressedDispatcher().onBackPressed());
            scenario.onActivity(activity -> {
                assertEquals(37, target.getCircleRadius());
                assertFalse(activity.isHasUnsavedChanges());
                assertEquals(View.GONE, activity.findViewById(R.id.hold_size_panel).getVisibility());
                assertNotNull(activity.findViewById(R.id.boulderProblemView));
            });

            // Rotation must discard an unapplied preview without changing the route.
            selectTarget(scenario);
            onView(withId(R.id.hold_size)).perform(setSliderValue(100));
            int[] originalOrientation = new int[1];
            scenario.onActivity(activity -> originalOrientation[0] = activity.getResources().getConfiguration().orientation);
            int oppositeOrientation = originalOrientation[0] == Configuration.ORIENTATION_LANDSCAPE
                    ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            rotate(scenario, oppositeOrientation);
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(37, activity.getmBoulderProblemInfo().getHolds().get(0).getCircleRadius());
                assertFalse(activity.isHasUnsavedChanges());
                assertEquals(View.GONE, activity.findViewById(R.id.hold_size_panel).getVisibility());
                activity.setmBoulderProblemInfo(info);
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                editor.setBoulderProblemInfo(info);
            });

            selectTarget(scenario);
            onView(withId(R.id.hold_size)).perform(setSliderValue(0));
            onView(withId(R.id.hold_size_apply)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(1, target.getCircleRadius());
                assertEquals(24, other.getCircleRadius());
                assertEquals(2, info.getHolds().size());
                assertTrue(activity.isHasUnsavedChanges());
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                assertFalse(editor.isChoosingHoldToResize());
            });
            rotate(scenario, originalOrientation[0]);
        } finally {
            source.recycle();
            SharedPreferences.Editor restore = preferences.edit();
            if (hadFirstStart) restore.putBoolean("firstStart", firstStart);
            else restore.remove("firstStart");
            restore.commit();
        }
    }

    private static void rotate(ActivityScenario<MainActivity> scenario, int orientation) {
        scenario.onActivity(activity -> activity.setRequestedOrientation(
                orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        long deadline = SystemClock.uptimeMillis() + 5000;
        boolean[] rotated = {false};
        while (!rotated[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> rotated[0] = activity.getResources().getConfiguration().orientation == orientation);
            if (!rotated[0]) SystemClock.sleep(30);
        }
        assertTrue("Editor did not rotate", rotated[0]);
    }

    private static Bitmap drawEditor(BoulderProblemView editor) {
        editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        editor.layout(0, 0, 600, 400);
        Bitmap image = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        editor.draw(new Canvas(image));
        return image;
    }

    private static ViewAction dragSliderToMinimum(Bitmap original) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isAssignableFrom(Slider.class); }
            @Override public String getDescription() { return "Preview marker while dragging size slider"; }
            @Override public void perform(UiController uiController, View view) {
                long down = SystemClock.uptimeMillis();
                MotionEvent start = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN,
                        view.getWidth() / 2f, view.getHeight() / 2f, 0);
                MotionEvent move = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_MOVE,
                        0, view.getHeight() / 2f, 0);
                MotionEvent end = MotionEvent.obtain(down, down + 40, MotionEvent.ACTION_UP,
                        0, view.getHeight() / 2f, 0);
                try {
                    view.dispatchTouchEvent(start);
                    view.dispatchTouchEvent(move);
                    Bitmap dragging = drawEditor(view.getRootView().findViewById(R.id.boulderProblemView));
                    try { assertFalse("Marker previews before the finger lifts", original.sameAs(dragging)); }
                    finally { dragging.recycle(); }
                    view.dispatchTouchEvent(end);
                } finally { start.recycle(); move.recycle(); end.recycle(); }
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private static void selectTarget(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
            editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
            editor.layout(0, 0, 600, 400);
            activity.findViewById(R.id.action_resize_hold).performClick();
            assertTrue(editor.isChoosingHoldToResize());
            long down = SystemClock.uptimeMillis();
            MotionEvent start = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 300, 200, 0);
            MotionEvent end = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_UP, 300, 200, 0);
            try { editor.onTouchEvent(start); editor.onTouchEvent(end); }
            finally { start.recycle(); end.recycle(); }
            assertFalse(editor.isChoosingHoldToResize());
        });
    }

    private static ViewAction setSliderValue(float value) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isAssignableFrom(Slider.class); }
            @Override public String getDescription() { return "Set marker size slider"; }
            @Override public void perform(UiController uiController, View view) {
                ((Slider) view).setValue(value);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}

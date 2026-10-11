package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HoldResizeInstrumentedTest {
    @Test public void resizeControlSelectsOneHoldAndOnlyApplyChangesItsSize() {
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
            onView(withId(R.id.hold_size)).inRoot(isDialog()).perform(setSliderValue(0));
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(37, target.getCircleRadius());
                assertEquals(2, info.getHolds().size());
                assertFalse(activity.isHasUnsavedChanges());
            });

            // Merely opening the control and applying must not round the existing size.
            selectTarget(scenario);
            onView(withText(R.string.apply)).inRoot(isDialog()).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(37, target.getCircleRadius());
                assertFalse(activity.isHasUnsavedChanges());
            });

            selectTarget(scenario);
            onView(withId(R.id.hold_size)).inRoot(isDialog()).perform(setSliderValue(0));
            onView(withText(R.string.apply)).inRoot(isDialog()).perform(click());
            scenario.onActivity(activity -> {
                assertEquals(1, target.getCircleRadius());
                assertEquals(24, other.getCircleRadius());
                assertEquals(2, info.getHolds().size());
                assertTrue(activity.isHasUnsavedChanges());
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                assertFalse(editor.isChoosingHoldToResize());
            });
        } finally {
            source.recycle();
            SharedPreferences.Editor restore = preferences.edit();
            if (hadFirstStart) restore.putBoolean("firstStart", firstStart);
            else restore.remove("firstStart");
            restore.commit();
        }
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

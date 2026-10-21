package com.herak.bouldershare;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.KeyEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.slider.Slider;
import com.herak.bouldershare.classes.*;
import com.herak.bouldershare.enums.HoldType;
import com.herak.bouldershare.fragments.MarkerColorDialog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MarkerSettingsInstrumentedTest {
    private Context context;
    private MarkerAppearance previous;
    @Before public void defaults() {
        context = ApplicationProvider.getApplicationContext(); previous = MarkerSettings.load(context);
        MarkerSettings.save(context, MarkerAppearance.defaults());
    }
    @After public void restore() { MarkerSettings.save(context, previous); }

    @Test public void instructionPreferenceSurvivesRecreationAndMarkerDefaults() {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        boolean hadPreference = preferences.contains("show_editor_instructions");
        boolean original = EditorSettings.showInstructions(context);
        EditorSettings.setShowInstructions(context, true);
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.show_editor_instructions)).perform(scrollTo(), click());
            assertFalse(EditorSettings.showInstructions(context));
            scenario.recreate();
            onView(withId(R.id.show_editor_instructions)).perform(scrollTo()).check(matches(not(isChecked())));
            onView(withId(R.id.marker_defaults)).perform(scrollTo(), click());
            assertFalse(EditorSettings.showInstructions(context));
            onView(withId(R.id.show_editor_instructions)).perform(scrollTo()).check(matches(not(isChecked())));
        } finally {
            if (hadPreference) EditorSettings.setShowInstructions(context, original);
            else preferences.edit().remove("show_editor_instructions").commit();
        }
    }

    @Test public void settingsPersistAndInvalidHexSurvivesRecreationWithoutCommitting() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                Slider stroke = activity.findViewById(R.id.marker_stroke);
                stroke.requestFocus();
                stroke.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertEquals(3.5f, MarkerSettings.load(activity).strokeWidth, 0);
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(3.5f,
                    ((Slider) activity.findViewById(R.id.marker_stroke)).getValue(), 0));
            onView(withId(R.id.marker_start_color)).perform(scrollTo(), click());
            onView(withId(R.id.marker_color_hex)).perform(scrollTo(), replaceText("#ZZ1234"), closeSoftKeyboard());
            onView(withText(R.string.apply)).check(matches(not(isEnabled())));
            scenario.recreate();
            onView(withId(R.id.marker_color_hex)).check(matches(withText("#ZZ1234")));
            onView(withText(R.string.apply)).check(matches(not(isEnabled())));
            onView(withText(R.string.cancel)).perform(click());
            assertEquals(MarkerAppearance.defaults().startColor, MarkerSettings.load(context).startColor);
            onView(withId(R.id.marker_start_color)).perform(scrollTo(), click());
            onView(withId(R.id.marker_color_hex)).perform(scrollTo(), replaceText("#123456"), closeSoftKeyboard());
            onView(withText(R.string.apply)).perform(click());
            assertEquals(0xff123456, MarkerSettings.load(context).startColor);
            scenario.recreate();
            onView(withId(R.id.marker_start_color)).check(matches(withText("Start · #123456")));
        }
    }

    @Test public void rgbPickerChangesHexAndCancelLeavesPreferencesAlone() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.marker_regular_color)).perform(scrollTo(), click());
            scenario.onActivity(activity -> {
                MarkerColorDialog fragment = (MarkerColorDialog) activity.getSupportFragmentManager().findFragmentByTag("marker_color");
                Slider red = fragment.requireDialog().findViewById(R.id.marker_color_red);
                red.requestFocus();
                red.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
            });
            onView(withId(R.id.marker_color_hex)).check(matches(withText("#FE695B")));
            onView(withText(R.string.cancel)).perform(click());
            assertEquals(0xffff695b, MarkerSettings.load(context).regularColor);
        }
    }

    @Test public void maxStrokeAndBorderLeaveClearDiskUntouchedAndZeroBorderUsesTypeColors() {
        Bitmap source = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888); source.eraseColor(Color.WHITE);
        BoulderProblemInfo info = new BoulderProblemInfo(); info.setReferenceSize(300, 200);
        info.setHolds(new ArrayList<>()); info.getHolds().add(new Hold(150, 100, 20));
        Bitmap export = BoulderProblemView.render(source, info, new MarkerAppearance(20, 10, 2, Color.RED, Color.GREEN, Color.BLUE));
        try {
            for (int y = 82; y <= 118; y++) for (int x = 132; x <= 168; x++) {
                if (Math.hypot(x - 150, y - 100) <= 18) assertEquals(Color.WHITE, export.getPixel(x, y));
            }
            assertEquals(Color.RED, export.getPixel(190, 100));
            assertNotEquals(Color.WHITE, export.getPixel(173, 100));
        } finally { export.recycle(); }
        info.getHolds().clear();
        HoldType[] types = HoldType.values();
        for (int i = 0; i < 3; i++) {
            Hold hold = new Hold(50 + i * 100, 100, 20); hold.setType(types[i]); info.getHolds().add(hold);
        }
        MarkerAppearance colors = new MarkerAppearance(10, 0, 0, Color.RED, Color.GREEN, Color.BLUE);
        export = BoulderProblemView.render(source, info, colors);
        try {
            for (int i = 0; i < 3; i++) {
                assertEquals(Color.WHITE, export.getPixel(68 + i * 100, 100));
                assertEquals(colors.colorFor(types[i]), export.getPixel(75 + i * 100, 100));
            }
        } finally { export.recycle(); source.recycle(); }
    }
}

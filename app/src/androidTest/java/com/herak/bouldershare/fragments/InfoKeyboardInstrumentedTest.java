package com.herak.bouldershare.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Layout;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ScrollView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InfoKeyboardInstrumentedTest {
    @Test public void nextReachesNotesAndSavesWhileKeyboardOpenInPortrait() {
        exerciseKeyboard(Configuration.ORIENTATION_PORTRAIT);
    }

    @Test public void nextReachesNotesAndSavesWhileKeyboardOpenInLandscape() {
        exerciseKeyboard(Configuration.ORIENTATION_LANDSCAPE);
    }

    private void exerciseKeyboard(int orientation) {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences firstRun = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences authorPrefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        boolean hadFirstRun = firstRun.contains("firstStart");
        boolean oldFirstRun = firstRun.getBoolean("firstStart", true);
        boolean hadAuthor = authorPrefs.contains("username");
        String oldAuthor = authorPrefs.getString("username", "");
        firstRun.edit().putBoolean("firstStart", false).commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            try {
            rotate(scenario, orientation);
            scenario.onActivity(activity -> {
                BoulderProblemInfo info = new BoulderProblemInfo("Original author", "Original name", "Original notes", "6a");
                info.setHolds(new ArrayList<>());
                activity.setmBoulderProblemInfo(info);
                activity.setHasUnsavedChanges(false);
                InfoFragment.newInstance(null, null).show(activity.getSupportFragmentManager(), "notes_test");
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            showNameKeyboard(scenario);
            dialogView(R.id.etInfoProblemName).perform(pressImeActionButton());
            scenario.onActivity(activity -> assertTrue(field(activity, R.id.etInfoAuthor).hasFocus()));
            dialogView(R.id.etInfoAuthor).perform(pressImeActionButton());
            awaitKeyboard(scenario);
            assertEditableAboveKeyboard(scenario);
            scenario.onActivity(activity -> {
                EditText notes = field(activity, R.id.etInfoComment);
                assertTrue("Author Next focuses Notes", notes.hasFocus());
                notes.setText("");
            });
            dialogView(R.id.etInfoComment).perform(typeTextIntoFocusedView("Reach left"),
                    pressKey(KeyEvent.KEYCODE_ENTER), typeTextIntoFocusedView("Match then finish"));
            assertEditableAboveKeyboard(scenario);
            scenario.onActivity(activity -> {
                assertEquals("Reach left\nMatch then finish", field(activity, R.id.etInfoComment).getText().toString());
                assertEquals("Original notes", activity.getmBoulderProblemInfo().getComment());
                assertFalse("Typing stays a draft", activity.isHasUnsavedChanges());
                ((NumberPicker) dialog(activity).findViewById(R.id.pickerGrade)).setValue(3);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals("Reach left\nMatch then finish", field(activity, R.id.etInfoComment).getText().toString());
                assertEquals(3, ((NumberPicker) dialog(activity).findViewById(R.id.pickerGrade)).getValue());
                assertEquals("Original notes", activity.getmBoulderProblemInfo().getComment());
                assertFalse(activity.isHasUnsavedChanges());
            });
            awaitDialogFocus(scenario);
            dialogView(R.id.etInfoComment).perform(scrollTo(), click());
            scenario.onActivity(activity -> ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(field(activity, R.id.etInfoComment), InputMethodManager.SHOW_IMPLICIT));
            awaitKeyboard(scenario);
            assertEditableAboveKeyboard(scenario);
            // Even a short landscape viewport can scroll to Save with the IME open.
            dialogView(R.id.info_done).perform(scrollTo());
            assertActionAboveKeyboard(scenario, R.id.info_done);
            dialogView(R.id.info_done).perform(click());
            scenario.onActivity(activity -> {
                assertEquals("Reach left\nMatch then finish", activity.getmBoulderProblemInfo().getComment());
                assertTrue(activity.isHasUnsavedChanges());
                activity.setHasUnsavedChanges(false);
                InfoFragment.newInstance(null, null).show(activity.getSupportFragmentManager(), "notes_test");
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            showNameKeyboard(scenario);
            dialogView(R.id.etInfoProblemName).perform(pressImeActionButton());
            dialogView(R.id.etInfoAuthor).perform(pressImeActionButton());
            awaitKeyboard(scenario);
            assertEditableAboveKeyboard(scenario);
            scenario.onActivity(activity -> field(activity, R.id.etInfoComment).setText("Discard this draft"));
            dialogView(R.id.info_cancel).perform(scrollTo());
            assertActionAboveKeyboard(scenario, R.id.info_cancel);
            dialogView(R.id.info_cancel).perform(click());
            scenario.onActivity(activity -> {
                assertEquals("Reach left\nMatch then finish", activity.getmBoulderProblemInfo().getComment());
                assertFalse("Cancel keeps the existing dirty state", activity.isHasUnsavedChanges());
            });
            } catch (RuntimeException | AssertionError failure) {
                captureFailure(scenario, orientation);
                throw failure;
            }
        } finally {
            SharedPreferences.Editor restoreFirst = firstRun.edit();
            if (hadFirstRun) restoreFirst.putBoolean("firstStart", oldFirstRun); else restoreFirst.remove("firstStart");
            restoreFirst.commit();
            SharedPreferences.Editor restoreAuthor = authorPrefs.edit();
            if (hadAuthor) restoreAuthor.putString("username", oldAuthor); else restoreAuthor.remove("username");
            restoreAuthor.commit();
        }
    }

    private static Dialog dialog(MainActivity activity) {
        InfoFragment fragment = (InfoFragment) activity.getSupportFragmentManager().findFragmentByTag("notes_test");
        assertNotNull(fragment);
        return fragment.requireDialog();
    }

    private static EditText field(MainActivity activity, int id) { return dialog(activity).findViewById(id); }

    private static ViewInteraction dialogView(int id) { return onView(withId(id)).inRoot(isDialog()); }

    private static void awaitDialogFocus(ActivityScenario<MainActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        boolean[] ready = {false};
        while (!ready[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                View decor = dialog(activity).getWindow().getDecorView();
                ready[0] = decor.isAttachedToWindow() && decor.hasWindowFocus() && !decor.isLayoutRequested();
            });
            if (!ready[0]) SystemClock.sleep(30);
        }
        assertTrue("Dialog is attached, laid out, and has window focus", ready[0]);
    }

    private static void showNameKeyboard(ActivityScenario<MainActivity> scenario) {
        awaitDialogFocus(scenario);
        dialogView(R.id.etInfoProblemName).perform(scrollTo(), click());
        scenario.onActivity(activity -> ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(field(activity, R.id.etInfoProblemName), InputMethodManager.SHOW_IMPLICIT));
        awaitKeyboard(scenario);
    }

    private static void awaitKeyboard(ActivityScenario<MainActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        boolean[] visible = {false};
        while (!visible[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(dialog(activity).getWindow().getDecorView());
                visible[0] = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
            });
            if (!visible[0]) SystemClock.sleep(30);
        }
        assertTrue("Test requires the real soft keyboard to be visible", visible[0]);
        SystemClock.sleep(250); // Let the keyboard's resize animation finish.
    }

    private static void assertEditableAboveKeyboard(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            Dialog dialog = dialog(activity);
            View decor = dialog.getWindow().getDecorView();
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
            assertNotNull(insets);
            assertTrue(insets.isVisible(WindowInsetsCompat.Type.ime()));
            Rect visible = new Rect();
            decor.getWindowVisibleDisplayFrame(visible);
            EditText notes = dialog.findViewById(R.id.etInfoComment);
            // Measure the selected text line independently of Material's focused
            // rectangle, which can include the whole field and its counter.
            Layout layout = notes.getLayout();
            assertNotNull(layout);
            int offset = Math.max(0, notes.getSelectionEnd());
            int line = layout.getLineForOffset(offset);
            int x = Math.round(layout.getPrimaryHorizontal(offset)) + notes.getTotalPaddingLeft() - notes.getScrollX();
            Rect caret = new Rect(x, layout.getLineTop(line) + notes.getTotalPaddingTop() - notes.getScrollY(),
                    x + 1, layout.getLineBottom(line) + notes.getTotalPaddingTop() - notes.getScrollY());
            int[] location = new int[2];
            notes.getLocationOnScreen(location);
            caret.offset(location[0], location[1]);
            ScrollView scroll = dialog.findViewById(R.id.info_scroll);
            scroll.getLocationOnScreen(location);
            Rect viewport = new Rect();
            assertTrue(scroll.getGlobalVisibleRect(viewport));
            assertEquals("The scroll viewport fits inside the dialog", scroll.getHeight(), viewport.height());
            assertTrue("Notes caret is inside the scroll viewport", caret.top >= location[1]);
            assertTrue("Notes caret is above the keyboard", caret.bottom <= visible.bottom);
            assertTrue("Notes caret is inside the scroll viewport", caret.bottom <= location[1] + scroll.getHeight());
            Rect fieldVisible = new Rect();
            assertTrue("Focused Notes has a visible input area", notes.getGlobalVisibleRect(fieldVisible));
            assertTrue("The input viewport has room for a line of text", scroll.getHeight() >= notes.getLineHeight());
        });
    }

    private static void assertActionAboveKeyboard(ActivityScenario<MainActivity> scenario, int id) {
        scenario.onActivity(activity -> {
            Dialog dialog = dialog(activity);
            View decor = dialog.getWindow().getDecorView();
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
            assertNotNull(insets);
            assertTrue("Scrolling to an action keeps the real keyboard open", insets.isVisible(WindowInsetsCompat.Type.ime()));
            Rect visible = new Rect();
            decor.getWindowVisibleDisplayFrame(visible);
            View action = dialog.findViewById(id);
            int[] location = new int[2];
            action.getLocationOnScreen(location);
            assertTrue("Dialog action is above the keyboard", location[1] + action.getHeight() <= visible.bottom);
            assertTrue("Dialog action is below the top of the screen", location[1] >= visible.top);
            assertTrue("Action can be tapped", action.getGlobalVisibleRect(new Rect()));
        });
    }

    private static void captureFailure(ActivityScenario<MainActivity> scenario, int orientation) {
        String label = orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait";
        try {
            scenario.onActivity(activity -> {
                Dialog dialog = dialog(activity);
                View decor = dialog.getWindow().getDecorView();
                Rect frame = new Rect();
                decor.getWindowVisibleDisplayFrame(frame);
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                StringBuilder report = new StringBuilder("NOTES FAILURE ").append(label)
                        .append(" frame=").append(frame).append(" decor=").append(decor.getWidth()).append('x').append(decor.getHeight())
                        .append(" IME=").append(insets == null ? "null" : insets.getInsets(WindowInsetsCompat.Type.ime()));
                for (int id : new int[]{R.id.info_scroll, R.id.etInfoProblemName, R.id.etInfoAuthor,
                        R.id.etInfoComment, R.id.pickerGrade, R.id.info_done, R.id.info_cancel}) {
                    View view = dialog.findViewById(id);
                    int[] position = new int[2];
                    view.getLocationOnScreen(position);
                    Rect visible = new Rect();
                    boolean displayed = view.getGlobalVisibleRect(visible);
                    report.append('\n').append(activity.getResources().getResourceEntryName(id))
                            .append(" screen=").append(position[0]).append(',').append(position[1])
                            .append(" size=").append(view.getWidth()).append('x').append(view.getHeight())
                            .append(" measured=").append(view.getMeasuredHeight()).append(" scrollY=").append(view.getScrollY())
                            .append(" displayed=").append(displayed).append(" visible=").append(visible)
                            .append(" focus=").append(view.hasFocus());
                    if (view instanceof EditText) {
                        EditText edit = (EditText) view;
                        report.append(" selection=").append(edit.getSelectionEnd())
                                .append(" paddingTop=").append(edit.getTotalPaddingTop())
                                .append(" lineHeight=").append(edit.getLineHeight());
                    }
                }
                System.out.println(report);
            });
        } catch (Exception | AssertionError diagnosticFailure) {
            System.out.println("NOTES FAILURE geometry diagnostics=" + diagnosticFailure);
        }
        try {
            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            if (screenshot != null) {
                Context context = ApplicationProvider.getApplicationContext();
                File output = new File(context.getExternalFilesDir(null), "notes-failure-" + label + ".png");
                try (FileOutputStream stream = new FileOutputStream(output)) {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream);
                } finally { screenshot.recycle(); }
                System.out.println("NOTES FAILURE screenshot=" + output);
            }
        } catch (Exception | AssertionError diagnosticFailure) {
            System.out.println("NOTES FAILURE screenshot diagnostics=" + diagnosticFailure);
        }
    }

    private static void rotate(ActivityScenario<MainActivity> scenario, int orientation) {
        MainActivity[] original = {null};
        boolean[] needsRotation = {false};
        scenario.onActivity(activity -> {
            original[0] = activity;
            needsRotation[0] = activity.getResources().getConfiguration().orientation != orientation;
            activity.setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });
        long deadline = SystemClock.uptimeMillis() + 5000;
        boolean[] rotated = {false};
        while (!rotated[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> rotated[0] = activity.getResources().getConfiguration().orientation == orientation
                    && (!needsRotation[0] || activity != original[0]) && activity.hasWindowFocus());
            if (!rotated[0]) SystemClock.sleep(30);
        }
        assertTrue("Activity did not rotate", rotated[0]);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}

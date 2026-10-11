package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.PopupMenu;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.ImageFiles;
import com.herak.bouldershare.data.BoulderContract;
import com.herak.bouldershare.data.BoulderRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EditorSaveInstrumentedTest {
    @Test public void tapThenImmediateSaveIncludesAddAndRemoveBeforeConfirmation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        boolean hadFirstStart = preferences.contains("firstStart");
        boolean firstStart = preferences.getBoolean("firstStart", true);
        preferences.edit().putBoolean("firstStart", false).commit();
        Bitmap source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.BLACK);
        File input = photo(context, source);
        BoulderProblemInfo info = route(Uri.fromFile(input));
        Uri addedOutput = null;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setmBoulderProblemInfo(info);
                activity.setmBoulderBitmap(source);
                activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
                activity.getSupportFragmentManager().executePendingTransactions();
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                editor.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
                editor.layout(0, 0, 600, 400);
                assertTrue(editor.isImageReady());
                tap(editor, 300, 200);
                assertTrue(info.getHolds().isEmpty()); // Single-tap confirmation is still delayed.
                save(activity, editor);
                assertEquals(1, info.getHolds().size()); // Save must flush the completed tap.
            });
            awaitSave(scenario);
            SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout() + 50);
            assertEquals(1, BoulderRepository.load(context, info.getId()).getHolds().size());
            addedOutput = info.getFinalBitmapUri();
            scenario.onActivity(activity -> {
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                float fitScale = Math.min(editor.getWidth() / 600f, editor.getHeight() / 400f);
                tap(editor, (editor.getWidth() - 600 * fitScale) / 2 + 300 * fitScale,
                        (editor.getHeight() - 400 * fitScale) / 2 + 200 * fitScale);
                save(activity, editor);
                assertTrue(info.getHolds().isEmpty());
            });
            awaitSave(scenario);
            SystemClock.sleep(ViewConfiguration.getDoubleTapTimeout() + 50);
            scenario.onActivity(activity -> {
                assertTrue(info.getHolds().isEmpty());
                assertFalse(activity.isHasUnsavedChanges());
            });
            assertTrue(BoulderRepository.load(context, info.getId()).getHolds().isEmpty());
        } finally {
            deleteRoute(context, info);
            if (addedOutput != null) context.getContentResolver().delete(addedOutput, null, null);
            input.delete(); source.recycle();
            SharedPreferences.Editor restore = preferences.edit();
            if (hadFirstStart) restore.putBoolean("firstStart", firstStart); else restore.remove("firstStart");
            restore.commit();
        }
    }

    @Test public void sharingDeletedExportRegeneratesAndUpdatesTheSameRoute() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap source = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.BLUE);
        File input = photo(context, source);
        BoulderProblemInfo info = route(Uri.fromFile(input));
        info.setReferenceSize(120, 80);
        info.setFinalBitmapUri(ImageFiles.export(context, source));
        BoulderFragment.SaveModel model = new BoulderFragment.SaveModel();
        AtomicReference<BoulderFragment.SaveResult> result = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Observer<BoulderFragment.SaveResult> observer = value -> {
            if (value != null && !value.pending) { result.set(value); completed.countDown(); }
        };
        Uri original = info.getFinalBitmapUri();
        try {
            BoulderRepository.save(context, info);
            long originalId = info.getId();
            assertEquals(1, context.getContentResolver().delete(original, null, null));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                model.result.observeForever(observer);
                model.start(context, BoulderProblemInfo.fromBundle(info.toBundle()), source, false, true);
            });
            assertTrue("Share recovery did not finish", completed.await(15, TimeUnit.SECONDS));
            BoulderFragment.SaveResult recovered = result.get();
            assertNotNull(recovered); assertFalse(recovered.error); assertTrue(recovered.saved);
            assertEquals(Long.valueOf(originalId), recovered.info.getId());
            assertNotEquals(original, recovered.info.getFinalBitmapUri());
            assertNotNull(recovered.shareUri);
            BoulderProblemInfo loaded = BoulderRepository.load(context, originalId);
            assertEquals(recovered.info.getFinalBitmapUri(), loaded.getFinalBitmapUri());
            try (InputStream stream = context.getContentResolver().openInputStream(recovered.shareUri)) {
                assertNotNull(stream); assertTrue(stream.read() != -1);
            }
            context.getContentResolver().delete(recovered.shareUri, null, null);
            info.setFinalBitmapUri(recovered.info.getFinalBitmapUri());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                model.result.removeObserver(observer); model.onCleared();
            });
            deleteRoute(context, info);
            input.delete(); source.recycle();
        }
    }

    private static void tap(BoulderProblemView editor, float x, float y) {
        long down = SystemClock.uptimeMillis();
        MotionEvent start = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent end = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_UP, x, y, 0);
        try { editor.onTouchEvent(start); editor.onTouchEvent(end); }
        finally { start.recycle(); end.recycle(); }
    }

    private static void save(MainActivity activity, BoulderProblemView editor) {
        MenuItem item = new PopupMenu(activity, editor).getMenu().add(0, R.id.action_save, 0, "Save");
        BoulderFragment fragment = (BoulderFragment) activity.getSupportFragmentManager().findFragmentById(R.id.flayoutMainActivity);
        assertNotNull(fragment); assertTrue(fragment.onOptionsItemSelected(item));
    }

    private static void awaitSave(ActivityScenario<MainActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        boolean[] finished = {false};
        while (!finished[0] && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                BoulderFragment fragment = (BoulderFragment) activity.getSupportFragmentManager().findFragmentById(R.id.flayoutMainActivity);
                finished[0] = fragment != null && !fragment.isBusy()
                        && activity.getmBoulderProblemInfo().getId() != null && !activity.isHasUnsavedChanges();
            });
            if (!finished[0]) SystemClock.sleep(30);
        }
        assertTrue("Save did not finish successfully", finished[0]);
    }

    private static BoulderProblemInfo route(Uri input) {
        BoulderProblemInfo info = new BoulderProblemInfo();
        info.setInputBitmapUri(input); info.setHolds(new ArrayList<>());
        return info;
    }

    private static File photo(Context context, Bitmap bitmap) throws Exception {
        File input = File.createTempFile("save_regression_", ".jpg", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(input)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output));
        }
        return input;
    }

    private static void deleteRoute(Context context, BoulderProblemInfo info) {
        if (info.getId() != null) context.getContentResolver().delete(
                BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(info.getId()), null, null);
        if (info.getFinalBitmapUri() != null) context.getContentResolver().delete(info.getFinalBitmapUri(), null, null);
    }
}

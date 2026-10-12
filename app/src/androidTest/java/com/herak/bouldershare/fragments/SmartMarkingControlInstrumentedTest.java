package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SmartMarkingControlInstrumentedTest {
    @Test public void bundledDetectionDoesNotGrantInternetAccess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo info = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_PERMISSIONS);
        if (info.requestedPermissions != null) {
            for (String permission : info.requestedPermissions) {
                assertNotEquals("android.permission.INTERNET", permission);
            }
        }
    }

    @Test public void smartMarkingIsOptionalAndPreferenceSurvivesRecreation() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        boolean hadFirstStart = preferences.contains("firstStart");
        boolean firstStart = preferences.getBoolean("firstStart", true);
        boolean hadSmartMarking = preferences.contains("smart_marking");
        boolean smartMarking = preferences.getBoolean("smart_marking", false);
        preferences.edit().putBoolean("firstStart", false).remove("smart_marking").commit();
        Bitmap source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BoulderProblemInfo info = new BoulderProblemInfo();
                info.setReferenceSize(600, 400);
                info.setHolds(new ArrayList<>());
                activity.setmBoulderProblemInfo(info);
                activity.setmBoulderBitmap(source);
                activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
                activity.getSupportFragmentManager().executePendingTransactions();
                MaterialSwitch control = activity.findViewById(R.id.smart_marking);
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                assertTrue(control.isChecked());
                assertTrue(editor.isSmartMarkingEnabled());
                control.setChecked(false);
                assertFalse(editor.isSmartMarkingEnabled());
                assertFalse(preferences.getBoolean("smart_marking", true));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                MaterialSwitch control = activity.findViewById(R.id.smart_marking);
                BoulderProblemView editor = activity.findViewById(R.id.boulderProblemView);
                assertNotNull(control);
                assertFalse(control.isChecked());
                assertFalse(editor.isSmartMarkingEnabled());
                control.setChecked(true);
                assertTrue(editor.isSmartMarkingEnabled());
                assertTrue(preferences.getBoolean("smart_marking", false));
            });
        } finally {
            source.recycle();
            SharedPreferences.Editor restore = preferences.edit();
            if (hadFirstStart) restore.putBoolean("firstStart", firstStart);
            else restore.remove("firstStart");
            if (hadSmartMarking) restore.putBoolean("smart_marking", smartMarking);
            else restore.remove("smart_marking");
            restore.commit();
        }
    }
}

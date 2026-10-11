package com.herak.bouldershare;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.data.BoulderContract;
import com.herak.bouldershare.data.BoulderDbHelper;
import com.herak.bouldershare.data.BoulderRepository;
import com.herak.bouldershare.enums.HoldType;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test public void versionThreeMigrationPreservesRoutesAndLegacyHoldEncoding() {
        Context context = ApplicationProvider.getApplicationContext();
        try (SQLiteDatabase db = SQLiteDatabase.create(null);
             BoulderDbHelper helper = new BoulderDbHelper(context)) {
            db.execSQL("CREATE TABLE boulder_problem (_id INTEGER PRIMARY KEY, name TEXT)");
            db.execSQL("CREATE TABLE holds (_id INTEGER PRIMARY KEY, boulder_id INTEGER, hold_type INTEGER, coord_x REAL, coord_y REAL, circle_radius INTEGER)");
            db.execSQL("INSERT INTO boulder_problem VALUES (17, 'Original route')");
            db.execSQL("INSERT INTO holds VALUES (3, 17, 'TOP_HOLD', 123, 456, 24)");
            helper.onUpgrade(db, 3, 4);
            try (Cursor route = db.rawQuery("SELECT * FROM boulder_problem WHERE _id = 17", null)) {
                assertTrue(route.moveToFirst());
                assertEquals("Original route", route.getString(route.getColumnIndexOrThrow("name")));
                assertEquals(0, route.getInt(route.getColumnIndexOrThrow("reference_width")));
                assertEquals(0, route.getInt(route.getColumnIndexOrThrow("reference_height")));
            }
            try (Cursor holds = db.rawQuery("SELECT * FROM holds WHERE boulder_id = 17", null)) {
                assertTrue(holds.moveToFirst());
                assertEquals("TOP_HOLD", holds.getString(holds.getColumnIndexOrThrow("hold_type")));
                assertEquals(123f, holds.getFloat(holds.getColumnIndexOrThrow("coord_x")), .001f);
            }
        }
    }

    @Test public void updatingSecondRouteDoesNotOverwriteFirstAndItemDeleteIsScoped() {
        Context context = ApplicationProvider.getApplicationContext();
        BoulderProblemInfo first = route("First test route"), second = route("Second test route");
        try {
            BoulderRepository.save(context, first);
            BoulderRepository.save(context, second);
            second.setName("Updated second test route");
            second.getHolds().get(0).setType(HoldType.TOP_HOLD);
            BoulderRepository.save(context, second);
            assertEquals("First test route", BoulderRepository.load(context, first.getId()).getName());
            BoulderProblemInfo loaded = BoulderRepository.load(context, second.getId());
            assertEquals("Updated second test route", loaded.getName());
            assertEquals(1, loaded.getHolds().size());
            assertEquals(HoldType.TOP_HOLD, loaded.getHolds().get(0).getType());
            assertEquals(1200, loaded.getReferenceWidth());
            try (Cursor filtered = context.getContentResolver().query(BoulderContract.BoulderProblemInfoEntry.CONTENT_URI,
                    null, "_id = ?", new String[]{Long.toString(second.getId())}, null)) {
                assertNotNull(filtered); assertEquals(1, filtered.getCount());
            }
            context.getContentResolver().delete(BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(second.getId()), null, null);
            assertNull(BoulderRepository.load(context, second.getId()));
            assertNotNull(BoulderRepository.load(context, first.getId()));
            try (Cursor markers = context.getContentResolver().query(BoulderContract.HoldsEntry.buildHoldsOfBoulderProblem(second.getId()), null, null, null, null)) {
                assertNotNull(markers); assertEquals(0, markers.getCount());
            }
        } finally {
            for (BoulderProblemInfo info : new BoulderProblemInfo[]{first, second})
                if (info.getId() != null) context.getContentResolver().delete(
                        BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(info.getId()), null, null);
        }
    }

    @Test public void editorStateRoundTripPreservesUnsavedMarkerAndReferenceSize() {
        BoulderProblemInfo info = route("Unsaved route");
        BoulderProblemInfo restored = BoulderProblemInfo.fromBundle(info.toBundle());
        assertNull(restored.getId());
        assertEquals(info.getInputBitmapUri(), restored.getInputBitmapUri());
        assertEquals(1200, restored.getReferenceWidth());
        assertEquals(800, restored.getReferenceHeight());
        assertEquals(450f, restored.getHolds().get(0).getX(), .001f);
        assertNotSame(info.getHolds().get(0), restored.getHolds().get(0));
    }

    @Test public void legacyAndMalformedHoldTypesAreReadable() {
        assertEquals(HoldType.TOP_HOLD, BoulderRepository.readHoldType("TOP_HOLD"));
        assertEquals(HoldType.TOP_HOLD, BoulderRepository.readHoldType("2"));
        assertEquals(HoldType.REGULAR_HOLD, BoulderRepository.readHoldType(null));
        assertEquals(HoldType.REGULAR_HOLD, BoulderRepository.readHoldType("unknown"));
        assertEquals(HoldType.REGULAR_HOLD, BoulderRepository.readHoldType("999"));
    }

    private static BoulderProblemInfo route(String name) {
        BoulderProblemInfo info = new BoulderProblemInfo("Test", name, "", "6A");
        info.setInputBitmapUri(Uri.parse("file:///test/input.jpg"));
        info.setFinalBitmapUri(Uri.parse("content://test/output.jpg"));
        info.setReferenceSize(1200, 800);
        ArrayList<Hold> holds = new ArrayList<>();
        holds.add(new Hold(450, 200, 24));
        info.setHolds(holds);
        return info;
    }
}

package com.herak.bouldershare.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.enums.HoldType;

import java.util.ArrayList;
import java.util.List;

/** Atomic route persistence; called from a background executor. */
public final class BoulderRepository {
    private BoulderRepository() { }

    public static BoulderProblemInfo load(Context context, long id) {
        try (Cursor row = context.getContentResolver().query(
                BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(id), null, null, null, null)) {
            if (row == null || !row.moveToFirst()) return null;
            BoulderProblemInfo info = new BoulderProblemInfo(string(row, "author"), string(row, "name"),
                    string(row, "comment"), string(row, "grade"));
            info.setId(id);
            String input = string(row, "input_bitmap_uri"), output = string(row, "final_bitmap_uri");
            if (input != null) info.setInputBitmapUri(Uri.parse(input));
            if (output != null) info.setFinalBitmapUri(Uri.parse(output));
            info.setReferenceSize(row.getInt(row.getColumnIndexOrThrow("reference_width")),
                    row.getInt(row.getColumnIndexOrThrow("reference_height")));
            List<Hold> holds = new ArrayList<>();
            try (Cursor markers = context.getContentResolver().query(
                    BoulderContract.HoldsEntry.buildHoldsOfBoulderProblem(id), null, null, null, "_id ASC")) {
                if (markers != null) while (markers.moveToNext()) {
                    Hold hold = new Hold(markers.getFloat(markers.getColumnIndexOrThrow("coord_x")),
                            markers.getFloat(markers.getColumnIndexOrThrow("coord_y")),
                            markers.getInt(markers.getColumnIndexOrThrow("circle_radius")));
                    hold.setId(markers.getLong(markers.getColumnIndexOrThrow("_id")));
                    hold.setBoulderId(id);
                    // Versions 1-3 declared this INTEGER but stored enum names; preserve both encodings.
                    String type = string(markers, "hold_type");
                    hold.setType(readHoldType(type));
                    holds.add(hold);
                }
            }
            info.setHolds(holds);
            return info;
        }
    }

    public static long save(Context context, BoulderProblemInfo info) {
        if (info.getInputBitmapUri() == null || info.getFinalBitmapUri() == null)
            throw new IllegalArgumentException("A route needs its original photo and exported image");
        long id;
        try (BoulderDbHelper helper = new BoulderDbHelper(context.getApplicationContext())) {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("author", info.getAuthor()); values.put("name", info.getName());
                values.put("comment", info.getComment()); values.put("grade", info.getGrade());
                values.put("input_bitmap_uri", info.getInputBitmapUri().toString());
                values.put("final_bitmap_uri", info.getFinalBitmapUri().toString());
                values.put("reference_width", info.getReferenceWidth());
                values.put("reference_height", info.getReferenceHeight());
                if (info.getId() != null && db.update("boulder_problem", values, "_id = ?",
                        new String[]{Long.toString(info.getId())}) > 0) id = info.getId();
                else id = db.insertOrThrow("boulder_problem", null, values);
                db.delete("holds", "boulder_id = ?", new String[]{Long.toString(id)});
                if (info.getHolds() != null) for (Hold hold : info.getHolds()) {
                    ContentValues marker = new ContentValues();
                    marker.put("boulder_id", id); marker.put("coord_x", hold.getX());
                    marker.put("coord_y", hold.getY()); marker.put("circle_radius", hold.getCircleRadius());
                    marker.put("hold_type", hold.getType().name());
                    db.insertOrThrow("holds", null, marker);
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        info.setId(id);
        context.getContentResolver().notifyChange(BoulderContract.BoulderProblemInfoEntry.CONTENT_URI, null);
        context.getContentResolver().notifyChange(BoulderContract.HoldsEntry.CONTENT_URI, null);
        return id;
    }

    private static String string(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    public static HoldType readHoldType(String value) {
        if (value == null) return HoldType.REGULAR_HOLD;
        try { return HoldType.valueOf(value); }
        catch (IllegalArgumentException unknownName) {
            try {
                int ordinal = Integer.parseInt(value);
                if (ordinal >= 0 && ordinal < HoldType.values().length) return HoldType.values()[ordinal];
            } catch (NumberFormatException ignored) { }
            return HoldType.REGULAR_HOLD;
        }
    }
}

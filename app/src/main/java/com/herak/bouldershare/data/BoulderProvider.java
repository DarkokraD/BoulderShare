package com.herak.bouldershare.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/** Private database provider. URI constraints are always combined with caller selection. */
public class BoulderProvider extends ContentProvider {
    private static final int PROBLEMS = 1, PROBLEM = 2, HOLDS = 3, HOLD = 4;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        MATCHER.addURI(BoulderContract.CONTENT_AUTHORITY, "boulder", PROBLEMS);
        MATCHER.addURI(BoulderContract.CONTENT_AUTHORITY, "boulder/#", PROBLEM);
        MATCHER.addURI(BoulderContract.CONTENT_AUTHORITY, "holds", HOLDS);
        MATCHER.addURI(BoulderContract.CONTENT_AUTHORITY, "holds/#", HOLD);
    }
    private BoulderDbHelper helper;

    @Override public boolean onCreate() {
        helper = new BoulderDbHelper(getContext());
        return true;
    }

    private String table(Uri uri) {
        switch (MATCHER.match(uri)) {
            case PROBLEMS: case PROBLEM: return BoulderContract.BoulderProblemInfoEntry.TABLE_NAME;
            case HOLDS: case HOLD: return BoulderContract.HoldsEntry.TABLE_NAME;
            default: throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private String constraint(Uri uri) {
        int match = MATCHER.match(uri);
        if (match == PROBLEM || match == HOLD) return "_id = " + ContentUris.parseId(uri);
        if (match == HOLDS) {
            String problem = uri.getQueryParameter(BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID);
            if (problem != null) return BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + " = " + Long.parseLong(problem);
        }
        return null;
    }

    private String selection(Uri uri, String selection) {
        String constraint = constraint(uri);
        if (constraint == null) return selection;
        return selection == null || selection.isEmpty() ? constraint : "(" + constraint + ") AND (" + selection + ")";
    }

    @Override public String getType(Uri uri) {
        switch (MATCHER.match(uri)) {
            case PROBLEMS: return BoulderContract.BoulderProblemInfoEntry.CONTENT_TYPE;
            case PROBLEM: return BoulderContract.BoulderProblemInfoEntry.CONTENT_ITEM_TYPE;
            case HOLDS: return BoulderContract.HoldsEntry.CONTENT_TYPE;
            case HOLD: return BoulderContract.HoldsEntry.CONTENT_ITEM_TYPE;
            default: throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        Cursor cursor = helper.getReadableDatabase().query(table(uri), projection,
                selection(uri, selection), args, null, null, order);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        long id = helper.getWritableDatabase().insertOrThrow(table(uri), null, values);
        notifyChanges();
        return ContentUris.withAppendedId(MATCHER.match(uri) == HOLDS || MATCHER.match(uri) == HOLD
                ? BoulderContract.HoldsEntry.CONTENT_URI : BoulderContract.BoulderProblemInfoEntry.CONTENT_URI, id);
    }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        int count = helper.getWritableDatabase().update(table(uri), values, selection(uri, selection), args);
        if (count > 0) notifyChanges();
        return count;
    }

    @Override public int delete(Uri uri, String selection, String[] args) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        db.beginTransaction();
        try {
            String where = selection(uri, selection);
            if (MATCHER.match(uri) == PROBLEMS || MATCHER.match(uri) == PROBLEM) {
                // Old installations did not enable foreign keys; delete child rows explicitly.
                db.delete(BoulderContract.HoldsEntry.TABLE_NAME, "boulder_id IN (SELECT _id FROM boulder_problem"
                        + (where == null ? "" : " WHERE " + where) + ")", args);
            }
            count = db.delete(table(uri), where, args);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        if (count > 0) notifyChanges();
        return count;
    }

    @Override public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues value : values) db.insertOrThrow(table(uri), null, value);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        if (values.length > 0) notifyChanges();
        return values.length;
    }

    private void notifyChanges() {
        getContext().getContentResolver().notifyChange(BoulderContract.BoulderProblemInfoEntry.CONTENT_URI, null);
        getContext().getContentResolver().notifyChange(BoulderContract.HoldsEntry.CONTENT_URI, null);
    }
}

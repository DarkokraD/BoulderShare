package com.herak.bouldershare.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by darko on 18.8.2017..
 */

public class BoulderProvider extends ContentProvider{

    static final int BOULDER_PROBLEM_INFO = 100;
    static final int BOULDER_PROBLEM_INFOS = 101;
    static final int HOLDS = 200;
    static final int HOLDS_FROM_BOULDER_PROBLEM = 201;

    private static final SQLiteQueryBuilder sBoulderProblemQueryBuilder;
    private static final SQLiteQueryBuilder sHoldQueryBuilder;

    static{
        sBoulderProblemQueryBuilder = new SQLiteQueryBuilder();
        sHoldQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sBoulderProblemQueryBuilder.setTables(
                BoulderContract.BoulderProblemInfoEntry.TABLE_NAME
//                        + " INNER JOIN " +
//                        WeatherContract.LocationEntry.TABLE_NAME +
//                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
//                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
//                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
//                        "." + WeatherContract.LocationEntry._ID
        );
        sHoldQueryBuilder.setTables(
                BoulderContract.HoldsEntry.TABLE_NAME);
    }

    private BoulderDbHelper mOpenHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = BoulderContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, BoulderContract.PATH_BOULDER_PROBLEM_INFO + "/#", BOULDER_PROBLEM_INFO);
        matcher.addURI(authority, BoulderContract.PATH_HOLDS, HOLDS);
        matcher.addURI(authority, BoulderContract.PATH_HOLDS + "/#", HOLDS_FROM_BOULDER_PROBLEM);
        matcher.addURI(authority, BoulderContract.PATH_BOULDER_PROBLEM_INFO, BOULDER_PROBLEM_INFOS);


        return matcher;
    }

    @Override
    public boolean onCreate(){
        mOpenHelper = new BoulderDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case BOULDER_PROBLEM_INFO:
                return BoulderContract.BoulderProblemInfoEntry.CONTENT_ITEM_TYPE;
            case BOULDER_PROBLEM_INFOS:
                return BoulderContract.BoulderProblemInfoEntry.CONTENT_TYPE;
            case HOLDS:
                return BoulderContract.HoldsEntry.CONTENT_ITEM_TYPE;
            case HOLDS_FROM_BOULDER_PROBLEM:
                return BoulderContract.HoldsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }


    private static final String sHoldsWithBoulderProblemIdSelection =
            BoulderContract.HoldsEntry.TABLE_NAME +
                    "." + BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + " = ?";

    private static final String sBoulderProblemWithIdSelection =
            BoulderContract.BoulderProblemInfoEntry.TABLE_NAME +
                    "." + BoulderContract.BoulderProblemInfoEntry._ID + " = ?";


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case BOULDER_PROBLEM_INFO:
            {
                retCursor = getBoulderProblemInfo(uri, projection, sortOrder);
                break;
            }
            case BOULDER_PROBLEM_INFOS:
            {
                retCursor = getBoulderProblemInfos(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
//            case HOLDS: {
//                retCursor = getHoldWithId(uri, projection, sortOrder);
//                break;
//            }
            // "weather"
            case HOLDS_FROM_BOULDER_PROBLEM: {
                retCursor = getHoldsWithBoulderProblemId(uri, projection, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    private Cursor getHoldsWithBoulderProblemId(Uri uri, String[] projection, String sortOrder) {
        String id = BoulderContract.HoldsEntry.getBoulderProblemIdFromUri(uri);
        return sHoldQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sHoldsWithBoulderProblemIdSelection,
                new String[]{id},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getBoulderProblemInfo(Uri uri, String[] projection, String sortOrder) {
        String id = BoulderContract.BoulderProblemInfoEntry.getBoulderProblemIdFromUri(uri);

        return sBoulderProblemQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sBoulderProblemWithIdSelection,
                new String[]{id},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getBoulderProblemInfos(Uri uri, String[] projection, String sortOrder) {
        String id = BoulderContract.BoulderProblemInfoEntry.getBoulderProblemIdFromUri(uri);

        return sBoulderProblemQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case HOLDS: {

                long _id = db.insert(BoulderContract.HoldsEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = BoulderContract.HoldsEntry.buildHoldsUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case BOULDER_PROBLEM_INFO:
            case BOULDER_PROBLEM_INFOS:
                {

                long _id = db.insert(BoulderContract.BoulderProblemInfoEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HOLDS:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(BoulderContract.HoldsEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Student: Start by getting a writable database
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // Student: Use the uriMatcher to match the WEATHER and LOCATION URI's we are going to
        // handle.  If it doesn't match these, throw an UnsupportedOperationException.
        if(null == selection) selection = "1";
        switch (match) {
            case HOLDS: {
                rowsDeleted = db.delete(BoulderContract.HoldsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case BOULDER_PROBLEM_INFO: {
                rowsDeleted = db.delete(BoulderContract.BoulderProblemInfoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if(rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;

    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case HOLDS: {
                rowsUpdated = db.update(BoulderContract.HoldsEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case BOULDER_PROBLEM_INFO: {
                rowsUpdated = db.update(BoulderContract.BoulderProblemInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if(rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

}

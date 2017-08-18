package com.herak.bouldershare.data;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import static android.Manifest.permission_group.LOCATION;

/**
 * Created by darko on 18.8.2017..
 */

public class BoulderProvider extends ContentProvider{

    static final int BOULDER_PROBLEM_INFO = 100;
    static final int HOLDS = 200;
    static final int HOLDS_FROM_BOULDER_PROBLEM = 201;

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
        matcher.addURI(authority, BoulderContract.PATH_BOULDER_PROBLEM_INFO, BOULDER_PROBLEM_INFO);
        matcher.addURI(authority, BoulderContract.PATH_HOLDS, HOLDS);
        matcher.addURI(authority, BoulderContract.PATH_HOLDS + "/#", HOLDS_FROM_BOULDER_PROBLEM);


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
            case HOLDS:
                return BoulderContract.HoldsEntry.CONTENT_ITEM_TYPE;
            case HOLDS_FROM_BOULDER_PROBLEM:
                return BoulderContract.HoldsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

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
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case HOLDS: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case HOLDS_FROM_BOULDER_PROBLEM: {
                retCursor = null;
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

}

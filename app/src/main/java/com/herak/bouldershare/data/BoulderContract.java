package com.herak.bouldershare.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by darko on 16.5.2017..
 */

public final class BoulderContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.herak.bouldershare";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_BOULDER_PROBLEM_INFO = "info";
    public static final String PATH_HOLDS = "holds";

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private BoulderContract() {}

    /* Inner class that defines the table contents */
    public static class BoulderProblemInfoEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOULDER_PROBLEM_INFO).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOULDER_PROBLEM_INFO;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOULDER_PROBLEM_INFO;

        public static Uri buildBoulderProblemInfoUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static final String TABLE_NAME = "boulder_problem";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_COMMENT = "comment";
        public static final String COLUMN_INPUTBITMAPURI = "input_bitmap_uri";
        public static final String COLUMN_GRADE = "grade";
    }

    public static class HoldsEntry implements BaseColumns{

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HOLDS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HOLDS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HOLDS;

        public static Uri buildHoldsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildHoldsOfBoulderProblem(long boulder_id){
            return CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(COLUMN_BOULDER_PROBLEM_ID, Long.toString(boulder_id))
                    .build();
        };

        public static final String TABLE_NAME = "holds";
        public static final String COLUMN_BOULDER_PROBLEM_ID = "boulder_id";
        public static final String COLUMN_COORD_X = "coord_x";
        public static final String COLUMN_COORD_Y = "coord_y";
        //Hold type (start, normal or top)
        public static final String COLUMN_HOLD_TYPE = "hold_type";
        public static final String COLUMN_CIRCLE_RADIUS = "circle_radius";
    }

}

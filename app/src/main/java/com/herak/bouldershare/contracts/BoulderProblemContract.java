package com.herak.bouldershare.contracts;

import android.provider.BaseColumns;

/**
 * Created by darko on 16.5.2017..
 */

public final class BoulderProblemContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private BoulderProblemContract() {}

    /* Inner class that defines the table contents */
    public static class BoulderProblemInfoEntry implements BaseColumns {
        public static final String TABLE_NAME = "boulder_problem";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_COMMENT = "comment";
        public static final String COLUMN_INPUTBITMAPURI = "input_bitmap_uri";
        public static final String COLUMN_GRADE = "grade";
    }

    public static class HoldsEntry implements BaseColumns{
        public static final String TABLE_NAME = "holds";
        public static final String COLUMN_BOULDER_PROBLEM_ID = "boulder_id";
        public static final String COLUMN_COORD_X = "coord_x";
        public static final String COLUMN_COORD_Y = "coord_y";
        //Hold type (start, normal or top)
        public static final String COLUMN_HOLD_TYPE = "hold_type";
        public static final String COLUMN_CIRCLE_RADIUS = "circle_radius";
    }

}

package com.herak.bouldershare.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.herak.bouldershare.contracts.BoulderProblemContract;

/**
 * Created by darko on 20.5.2017..
 */

public class BoulderDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "boulder.db";

    public BoulderDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String  SQL_CREATE_BOULDER_TABLE = "CREATE TABLE " + BoulderProblemContract.BoulderProblemInfoEntry.TABLE_NAME + " (" +
                BoulderProblemContract.BoulderProblemInfoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BoulderProblemContract.BoulderProblemInfoEntry.COLUMN_AUTHOR + " TEXT, " +
                BoulderProblemContract.BoulderProblemInfoEntry.COLUMN_INPUTBITMAPURI + " TEXT NOT NULL, " +
                BoulderProblemContract.BoulderProblemInfoEntry.COLUMN_NAME + " TEXT, " +
                BoulderProblemContract.BoulderProblemInfoEntry.COLUMN_COMMENT + " TEXT, " +
                BoulderProblemContract.BoulderProblemInfoEntry.COLUMN_GRADE + " TEXT)";

        final String  SQL_CREATE_HOLDS_TABLE = "CREATE TABLE " + BoulderProblemContract.HoldsEntry.TABLE_NAME + " (" +
                BoulderProblemContract.HoldsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BoulderProblemContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + " INTEGER NOT NULL, " +
                BoulderProblemContract.HoldsEntry.COLUMN_COORD_X + " REAL NOT NULL, " +
                BoulderProblemContract.HoldsEntry.COLUMN_COORD_Y + " REAL NOT NULL, " +
                BoulderProblemContract.HoldsEntry.COLUMN_CIRCLE_RADIUS + " INTEGER NOT NULL, " +
                BoulderProblemContract.HoldsEntry.COLUMN_HOLD_TYPE + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + BoulderProblemContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + ") REFERENCES " +
                BoulderProblemContract.BoulderProblemInfoEntry.TABLE_NAME + " (" + BoulderProblemContract.BoulderProblemInfoEntry._ID + ")" +
                ");";

        db.execSQL(SQL_CREATE_BOULDER_TABLE);
        db.execSQL(SQL_CREATE_HOLDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

// This code would delete the tables and recreate them. Use alter table to add columns instead
        //https://www.sqlite.org/lang_altertable.html
//        db.execSQL("DROP TABLE IF EXISTS " + BoulderProblemContract.BoulderProblemInfoEntry.TABLE_NAME);
//        db.execSQL("DROP TABLE IF EXISTS " + BoulderProblemContract.HoldsEntry.TABLE_NAME);
//        onCreate(db);
    }
}

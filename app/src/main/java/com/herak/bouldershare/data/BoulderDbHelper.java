package com.herak.bouldershare.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by darko on 20.5.2017..
 */

public class BoulderDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;

    static final String DATABASE_NAME = "boulder.db";

    public BoulderDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String  SQL_CREATE_BOULDER_TABLE = "CREATE TABLE " + BoulderContract.BoulderProblemInfoEntry.TABLE_NAME + " (" +
                BoulderContract.BoulderProblemInfoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_AUTHOR + " TEXT, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_INPUTBITMAPURI + " TEXT NOT NULL, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI + " TEXT, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_NAME + " TEXT, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_COMMENT + " TEXT, " +
                BoulderContract.BoulderProblemInfoEntry.COLUMN_GRADE + " TEXT)";

        final String  SQL_CREATE_HOLDS_TABLE = "CREATE TABLE " + BoulderContract.HoldsEntry.TABLE_NAME + " (" +
                BoulderContract.HoldsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + " INTEGER NOT NULL, " +
                BoulderContract.HoldsEntry.COLUMN_COORD_X + " REAL NOT NULL, " +
                BoulderContract.HoldsEntry.COLUMN_COORD_Y + " REAL NOT NULL, " +
                BoulderContract.HoldsEntry.COLUMN_CIRCLE_RADIUS + " INTEGER NOT NULL, " +
                BoulderContract.HoldsEntry.COLUMN_HOLD_TYPE + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID + ") REFERENCES " +
                BoulderContract.BoulderProblemInfoEntry.TABLE_NAME + " (" + BoulderContract.BoulderProblemInfoEntry._ID + ")" +
                ");";

        db.execSQL(SQL_CREATE_BOULDER_TABLE);
        db.execSQL(SQL_CREATE_HOLDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            final String SQL_ALTER_BOULDER_TABLE = "ALTER TABLE " + BoulderContract.BoulderProblemInfoEntry.TABLE_NAME
//                    + " ADD COLUMN " + BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI + ";";
//        db.execSQL(SQL_ALTER_BOULDER_TABLE);

// This code would delete the tables and recreate them. Use alter table to add columns instead
        //https://www.sqlite.org/lang_altertable.html
//        db.execSQL("DROP TABLE IF EXISTS " + BoulderContract.BoulderProblemInfoEntry.TABLE_NAME);
//        db.execSQL("DROP TABLE IF EXISTS " + BoulderContract.HoldsEntry.TABLE_NAME);
//        onCreate(db);
    }
}

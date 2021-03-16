/**
 * 
 */
package com.vi.androiddemo;

import java.util.ArrayList;

import com.vi.androiddemo.FaceDbContract.FaceEntry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author hongbing
 *
 */
public class FaceDbHelper extends SQLiteOpenHelper {
    
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "FaceDataBase.db";
    
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FaceEntry.TABLE_NAME + "(" + 
            FaceEntry._ID + " INTEGER PRIMARY KEY," + 
            FaceEntry.FACE_COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
            FaceEntry.FACE_COLUMN_FEATURES + TEXT_TYPE + ")";
    
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FaceEntry.TABLE_NAME;

    /**
     * @param context
     * @param name
     * @param factory
     * @param version
     */
    public FaceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);    
        Log.e("DB", "databased created");
    }    

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.e("DB", "Face table created");
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    
    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new face
    public void insertFace(String name, String features) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(FaceEntry.FACE_COLUMN_NAME, name); // Contact Name
        values.put(FaceEntry.FACE_COLUMN_FEATURES, features); // Contact Phone
    
        //Inserting Row
        db.insert(FaceEntry.TABLE_NAME, null, values);
        db.close(); // Closing database connection
    }
    
    // Getting All Faces
    public ArrayList<FaceRecord> getFaces() {
        ArrayList<FaceRecord> faceList = new ArrayList<FaceRecord>();
    
        // Select All Query
        String selectQuery = "SELECT  * FROM " + FaceEntry.TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {            
            do {
                FaceRecord face = new FaceRecord();
                face._id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(FaceEntry._ID)));
                face._name = cursor.getString(cursor.getColumnIndex(FaceEntry.FACE_COLUMN_NAME));
                face._features = cursor.getString(cursor.getColumnIndex(FaceEntry.FACE_COLUMN_FEATURES));
                
                // Adding contact to list
                faceList.add(face);
            } while (cursor.moveToNext());
        }

        // return contact list
        cursor.close();
        db.close();
        return faceList;        
    }    
    
    public String getFaceNameByID(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =  db.rawQuery( "SELECT * FROM " + FaceEntry.TABLE_NAME + " WHERE " + FaceEntry._ID + " = " + id, null);
        
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(FaceEntry.FACE_COLUMN_NAME));
        }
        return "";
     }     
}

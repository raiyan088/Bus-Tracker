package com.rr.bubtbustracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;
import com.rr.bubtbustracker.model.TripLocation;

import java.util.ArrayList;

public class TripDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bus_trip.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_TRIP = "locations";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_LNG = "lng";
    private static final String COLUMN_TIME = "time";

    public TripDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRIP + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_LAT + " REAL," +
                COLUMN_LNG + " REAL," +
                COLUMN_TIME + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIP);
        onCreate(db);
    }

    public void insertLocation(double lat, double lng, long time) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_LAT, lat);
            values.put(COLUMN_LNG, lng);
            values.put(COLUMN_TIME, time);
            db.insert(TABLE_TRIP, null, values);
        } catch (Exception e) {}
    }

    public ArrayList<TripLocation> getAllLocationsAsList() {
        ArrayList<TripLocation> locations = new ArrayList<>();

        try {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRIP + " ORDER BY " + COLUMN_ID + " ASC", null);
            while (cursor.moveToNext()) {
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                locations.add(new TripLocation(lat, lng, time));
            }
            cursor.close();
        } catch (Exception e) {}

        return locations;
    }

    public ArrayList<LatLng> getAllLocationsAsLatLng() {
        ArrayList<LatLng> locations = new ArrayList<>();

        try {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRIP + " ORDER BY " + COLUMN_ID + " ASC", null);
            while (cursor.moveToNext()) {
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                locations.add(new LatLng(lat, lng));
            }
            cursor.close();
        } catch (Exception e) {}

        return locations;
    }

    public void clearTrip() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRIP, null, null);
    }
}



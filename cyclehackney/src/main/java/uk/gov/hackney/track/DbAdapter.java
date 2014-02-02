package uk.gov.hackney.track;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
  private static final int DATABASE_VERSION = 21;

  public static final String K_TRIP_ROWID = "_id";
  public static final String K_TRIP_PURP = "purp";
  public static final String K_TRIP_START = "start";
  public static final String K_TRIP_END = "endtime";
  public static final String K_TRIP_FANCYSTART = "fancystart";
  public static final String K_TRIP_FANCYINFO = "fancyinfo";
  public static final String K_TRIP_NOTE = "note";
  public static final String K_TRIP_AGE = "age";
  public static final String K_TRIP_GENDER = "gender";
  public static final String K_TRIP_DISTANCE = "distance";
  public static final String K_TRIP_STATUS = "status";

  public static final String K_POINT_ROWID = "_id";
  public static final String K_POINT_TRIP  = "trip";
  public static final String K_POINT_TIME  = "time";
  public static final String K_POINT_LAT   = "lat";
  public static final String K_POINT_LGT   = "lgt";
  public static final String K_POINT_ACC   = "acc";
  public static final String K_POINT_ALT   = "alt";
  public static final String K_POINT_SPEED = "speed";

  private static final String TAG = "DbAdapter";
  private static final String TABLE_CREATE_TRIPS = "create table trips "
    + "(_id integer primary key autoincrement, purp text, start double, endtime double, "
    + "fancystart text, fancyinfo text, distance float, note text, age text, gender text, "
      + "status integer);";

  private static final String TABLE_CREATE_COORDS = "create table coords "
    + "(_id integer primary key autoincrement, "
      + "trip integer, lat int, lgt int, "
      + "time double, acc float, alt double, speed float);";

  private static final String DATABASE_NAME = "data";
  private static final String DATA_TABLE_TRIPS = "trips";
  private static final String DATA_TABLE_COORDS = "coords";

  private final Context context_;
  private DatabaseHelper dbHelper_;
  private SQLiteDatabase db_;

  private static class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(TABLE_CREATE_TRIPS);
      db.execSQL(TABLE_CREATE_COORDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_TRIPS);
      db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_COORDS);
      onCreate(db);
    }
  } // DatabaseHelper

  public static int unfinishedTrip(final Context context) {
    final DbAdapter db = new DbAdapter(context.getApplicationContext());
    db.openReadOnly();

    Cursor c = null;
    try {
      c = db.db_.query(DATA_TABLE_TRIPS,
                new String[]{ K_TRIP_ROWID },
                K_TRIP_STATUS + "=" + TripData.STATUS_RECORDING_COMPLETE,
                null, null, null, null);
      if (c.getCount() != 0) {
        c.moveToFirst();
        return c.getInt(c.getColumnIndex(K_TRIP_ROWID));
      } // if ...
    } finally {
      c.close();
      db.close();
    }

    return -1;
  } // availableForUpload

  public DbAdapter(final Context ctx) {
    context_ = ctx;
  }

  public DbAdapter open() throws SQLException {
    dbHelper_ = new DatabaseHelper(context_);
    db_ = dbHelper_.getWritableDatabase();
    return this;
  }

  public DbAdapter openReadOnly() throws SQLException {
    dbHelper_ = new DatabaseHelper(context_);
    db_ = dbHelper_.getReadableDatabase();
    return this;
  }

  public void close() {
    dbHelper_.close();
  }

  // #### Coordinate table methods ####
  public boolean addCoordToTrip(long tripid, CyclePoint pt) {
    boolean success = true;

    // Add the latest point
    ContentValues rowValues = new ContentValues();
    rowValues.put(K_POINT_TRIP, tripid);
    rowValues.put(K_POINT_LAT, pt.getLatitudeE6());
    rowValues.put(K_POINT_LGT, pt.getLongitudeE6());
    rowValues.put(K_POINT_TIME, pt.time);
    rowValues.put(K_POINT_ACC, pt.accuracy);
    rowValues.put(K_POINT_ALT, pt.altitude);
    rowValues.put(K_POINT_SPEED, pt.speed);

    success = success && (db_.insert(DATA_TABLE_COORDS, null, rowValues) > 0);

    // And update the trip stats
    rowValues = new ContentValues();
    rowValues.put(K_TRIP_END, pt.time);

    success = success && (db_.update(DATA_TABLE_TRIPS, rowValues, K_TRIP_ROWID + "=" + tripid, null) > 0);

    return success;
  }

  public boolean deleteAllCoordsForTrip(long tripid) {
    return db_.delete(DATA_TABLE_COORDS, K_POINT_TRIP + "=" + tripid, null) > 0;
  }

  public Cursor fetchAllCoordsForTrip(long tripid) {
    try {
      Cursor mCursor = db_.query(true, DATA_TABLE_COORDS, new String[] {
          K_POINT_LAT, K_POINT_LGT, K_POINT_TIME,
          K_POINT_ACC, K_POINT_ALT, K_POINT_SPEED },
          K_POINT_TRIP + "=" + tripid,
          null, null, null, K_POINT_TIME, null);

      if (mCursor != null) {
        mCursor.moveToFirst();
      }
      return mCursor;
    } catch (Exception e) {
      //Log.v("GOT!",e.toString());
      return null;
    }
  }

  // #### Trip table methods ####

  /**
   * Create a new trip using the data provided. If the trip is successfully
   * created return the new rowId for that trip, otherwise return a -1 to
   * indicate failure.
   */
  private long createTrip(String purp, double starttime, String fancystart,
                         String note) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(K_TRIP_PURP, purp);
    initialValues.put(K_TRIP_START, starttime);
    initialValues.put(K_TRIP_FANCYSTART, fancystart);
    initialValues.put(K_TRIP_NOTE, note);
    initialValues.put(K_TRIP_STATUS, TripData.STATUS_RECORDING);

    return db_.insert(DATA_TABLE_TRIPS, null, initialValues);
  }

  public long createTrip() {
    return createTrip("", System.currentTimeMillis(), "", "");
  }

  /**
   * Delete the trip with the given rowId
   *
   * @param rowId
   *            id of note to delete
   * @return true if deleted, false otherwise
   */
  public boolean deleteTrip(long rowId) {
    return db_.delete(DATA_TABLE_TRIPS, K_TRIP_ROWID + "=" + rowId, null) > 0;
  }

  /**
   * Return a Cursor over the list of all notes in the database
   *
   * @return Cursor over all trips
   */
  public Cursor fetchAllTrips() {
    Cursor c = db_.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID,
        K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART, K_TRIP_NOTE, K_TRIP_FANCYINFO },
        null, null, null, null, "-" + K_TRIP_START);
    if (c != null && c.getCount()>0) {
      c.moveToFirst();
    }
    return c;
  }

  public Cursor fetchUnsentTrips() {
    Cursor c = db_.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID },
        K_TRIP_STATUS + "=" + TripData.STATUS_COMPLETE_UNSENT,
        null, null, null, null);
    if (c != null && c.getCount()>0) {
      c.moveToFirst();
    }
    return c;
  }

  /**
   * Return a Cursor positioned at the trip that matches the given rowId
   *
   * @param rowId id of trip to retrieve
   * @return Cursor positioned to matching trip, if found
   * @throws SQLException if trip could not be found/retrieved
   */
  public Cursor fetchTrip(long rowId) throws SQLException {
    Cursor mCursor = db_.query(true, DATA_TABLE_TRIPS, new String[] {
        K_TRIP_ROWID, K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART,
        K_TRIP_NOTE, K_TRIP_AGE, K_TRIP_GENDER, K_TRIP_STATUS, K_TRIP_END,
        K_TRIP_FANCYINFO, K_TRIP_DISTANCE },
        K_TRIP_ROWID + "=" + rowId,

        null, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  public boolean updateTrip(long tripid,
                            String purp,
                            double starttime,
                            String fancystart,
                            String fancyinfo,
                            String note,
                            String age,
                            String gender,
                            float distance) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(K_TRIP_PURP, purp);
    initialValues.put(K_TRIP_START, starttime);
    initialValues.put(K_TRIP_FANCYSTART, fancystart);
    initialValues.put(K_TRIP_NOTE, note);
    initialValues.put(K_TRIP_AGE, age);
    initialValues.put(K_TRIP_GENDER, gender);
    initialValues.put(K_TRIP_FANCYINFO, fancyinfo);
    initialValues.put(K_TRIP_DISTANCE, distance);

    return db_.update(DATA_TABLE_TRIPS,
                      initialValues,
                      K_TRIP_ROWID + "=" + tripid, null) > 0;
  }

  public boolean updateTripStatus(long tripid, int tripStatus) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(K_TRIP_STATUS, tripStatus);

    return db_.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "="
        + tripid, null) > 0;
  }
}

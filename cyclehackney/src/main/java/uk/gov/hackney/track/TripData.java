package uk.gov.hackney.track;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class TripData {
  private long tripid;
  private double startTime_ = 0;
  private double endTime_ = 0;
  private int status;
  private float distance;
  private String purp_;
  private String info_;
  private String fancystart_;
  private List<GeoPoint> gpspoints;
  private String note_;
  private String age_;
  private String gender_;

  private DbAdapter mDb;

  public static int STATUS_RECORDING = 0;
  public static int STATUS_RECORDING_COMPLETE = 5;
  public static int STATUS_COMPLETE_UNSENT = 1;
  public static int STATUS_COMPLETE = 2;
  public static int STATUS_COMPLETE_FAILED = 3;

  public static TripData createTrip(Context c) {
    TripData t = new TripData(c.getApplicationContext(), 0);
    t.createTripInDatabase(c);
    t.initializeData();
    return t;
  }

  public static TripData fetchTrip(Context c, long tripid) {
    TripData t = new TripData(c.getApplicationContext(), tripid);
    t.populateDetails();
    return t;
  }

  public TripData(Context ctx, long tripid) {
    Context context = ctx.getApplicationContext();
    this.tripid = tripid;
    mDb = new DbAdapter(context);
  }

  private void initializeData() {
    startTime_ = System.currentTimeMillis();
    endTime_ = System.currentTimeMillis();
    distance = 0;

    purp_ = fancystart_ = info_ = "";

    gpspoints = new ArrayList<GeoPoint>();

    updateTrip();
  }

  // Get lat/long extremes, etc, from trip record
  private void populateDetails() {
    mDb.openReadOnly();

    Cursor tripdetails = mDb.fetchTrip(tripid);
    startTime_ = tripdetails.getDouble(tripdetails.getColumnIndex("start"));
    status =  tripdetails.getInt(tripdetails.getColumnIndex("status"));
    endTime_ = tripdetails.getDouble(tripdetails.getColumnIndex("endtime"));
    distance = tripdetails.getFloat(tripdetails.getColumnIndex("distance"));

    purp_ = tripdetails.getString(tripdetails.getColumnIndex("purp"));
    fancystart_ = tripdetails.getString(tripdetails.getColumnIndex("fancystart"));
    info_ = tripdetails.getString(tripdetails.getColumnIndex("fancyinfo"));
    note_ = tripdetails.getString(tripdetails.getColumnIndex("note"));
    age_ = tripdetails.getString(tripdetails.getColumnIndex("age"));
    gender_ = tripdetails.getString(tripdetails.getColumnIndex("gender"));

    tripdetails.close();
    mDb.close();

    loadJourney();
  }

  private void loadJourney() {
    // Otherwise, we need to query DB and build points from scratch.
    gpspoints = new ArrayList<GeoPoint>();

    mDb.openReadOnly();

    Cursor points = mDb.fetchAllCoordsForTrip(tripid);
    int COL_LAT = points.getColumnIndex("lat");
    int COL_LGT = points.getColumnIndex("lgt");
    int COL_TIME = points.getColumnIndex("time");
    int COL_ACC  = points.getColumnIndex(DbAdapter.K_POINT_ACC);

    while (!points.isAfterLast()) {
      int lat = points.getInt(COL_LAT);
      int lgt = points.getInt(COL_LGT);
      double time = points.getDouble(COL_TIME);
      float acc = (float) points.getDouble(COL_ACC);

      gpspoints.add(new CyclePoint(lat, lgt, time));

      points.moveToNext();
    } // while
    points.close();
    mDb.close();
  } // loadJourney

  private void createTripInDatabase(Context c) {
    mDb.open();
    tripid = mDb.createTrip();
    mDb.close();
  }

  void dropTrip() {
    mDb.open();
    mDb.deleteAllCoordsForTrip(tripid);
    mDb.deleteTrip(tripid);
    mDb.close();
  }

  public long id() { return tripid; }
  public boolean dataAvailable() { return gpspoints.size() != 0; }
  public GeoPoint startLocation() { return gpspoints.get(0); }
  public GeoPoint endLocation() { return gpspoints.get(gpspoints.size()-1); }
  public BoundingBoxE6 boundingBox() {
    int lathigh = Integer.MIN_VALUE;
    int lgthigh = Integer.MIN_VALUE;
    int latlow = Integer.MAX_VALUE;
    int lgtlow = Integer.MAX_VALUE;

    for(GeoPoint gp : gpspoints) {
      lathigh = Math.max(gp.getLatitudeE6(), lathigh);
      latlow = Math.min(gp.getLatitudeE6(), latlow);
      lgthigh = Math.max(gp.getLongitudeE6(), lgthigh);
      lgtlow = Math.min(gp.getLongitudeE6(), lgtlow);
    }

    return new BoundingBoxE6(lathigh, lgtlow, latlow, lgthigh);
  }
	public Iterable<GeoPoint> journey() { return gpspoints;	}
  public double startTime() { return startTime_; }
  public double endTime() { return endTime_; }
  public double elapsed() {
    if(status == STATUS_RECORDING)
      return System.currentTimeMillis() - startTime_;
    return endTime_ - startTime_;
  } // elapsed
  public float distanceTravelled() {
    return (0.0006212f * distance);
  } // distanceTravelled
  public String notes() { return note_; }
  public String purpose() { return purp_; }
  public String info() { return info_; }
  public String fancyStart() { return fancystart_; }
  public String age() { return age_; }
  public String gender() { return gender_; }

  public void addPointNow(Location loc) {
    int lat = (int)(loc.getLatitude() * 1E6);
    int lgt = (int)(loc.getLongitude() * 1E6);

    float accuracy = loc.getAccuracy();
    double altitude = loc.getAltitude();
    float speed = loc.getSpeed();

    CyclePoint pt = new CyclePoint(lat, lgt, loc.getTime(), accuracy, altitude, speed);

    if (gpspoints.size() > 1) {
      GeoPoint gp = gpspoints.get(gpspoints.size()-1);

      float segmentDistance = gp.distanceTo(pt);
      if (segmentDistance == 0)
        return; // we haven't gone anywhere

      distance += segmentDistance;
    } // if ...

    gpspoints.add(pt);

    endTime_ = loc.getTime();

    mDb.open();
    mDb.addCoordToTrip(tripid, pt);
    mDb.updateTrip(tripid, "", startTime_, "", "", "", "", "", distance);
    mDb.close();

    return;
  } // addPointNow

  public void recordingStopped() { updateTripStatus(STATUS_RECORDING_COMPLETE); }
  public void metaDataComplete() { updateTripStatus(STATUS_COMPLETE_UNSENT);}
  public void successfullyUploaded() { updateTripStatus(STATUS_COMPLETE); }
  public void uploadFailed() { updateTripStatus(STATUS_COMPLETE_FAILED); }

  private void updateTripStatus(int tripStatus) {
    mDb.open();
    mDb.updateTripStatus(tripid, tripStatus);
    mDb.close();
  }

  private void updateTrip() { updateTrip("","","","","",""); }
  public void updateTrip(String purpose,
                         String fancyStart,
                         String fancyInfo,
                         String notes,
                         String age,
                         String gender) {
    // Save the trip details to the phone database. W00t!
    mDb.open();
    mDb.updateTrip(tripid, purpose, startTime_, fancyStart, fancyInfo, notes, age, gender, distance);
    mDb.close();

    purp_ = purpose;
    note_ = notes;
    age_ = age;
    gender_ = gender;
  } // updateTrip

} // TripData

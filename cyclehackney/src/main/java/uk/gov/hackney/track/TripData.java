package uk.gov.hackney.track;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class TripData {
  long tripid;
  private double startTime = 0;
  private double endTime = 0;
  int lathigh, lgthigh, latlow, lgtlow;
  private int status;
  private float distance;
  String purp, fancystart, info;
  private List<GeoPoint> gpspoints;

  private DbAdapter mDb;

  public static int STATUS_RECORDING = 0;
  public static int STATUS_COMPLETE_UNSENT = 1;
  public static int STATUS_COMPLETE = 2;

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
    startTime = System.currentTimeMillis();
    endTime = System.currentTimeMillis();
    distance = 0;

    lathigh = (int) (-100 * 1E6);
    latlow = (int) (100 * 1E6);
    lgtlow = (int) (180 * 1E6);
    lgthigh = (int) (-180 * 1E6);
    purp = fancystart = info = "";

    gpspoints = new ArrayList<GeoPoint>();

    updateTrip();
  }

  // Get lat/long extremes, etc, from trip record
  private void populateDetails() {
    mDb.openReadOnly();

    Cursor tripdetails = mDb.fetchTrip(tripid);
    startTime = tripdetails.getDouble(tripdetails.getColumnIndex("start"));
    lathigh = tripdetails.getInt(tripdetails.getColumnIndex("lathi"));
    latlow =  tripdetails.getInt(tripdetails.getColumnIndex("latlo"));
    lgthigh = tripdetails.getInt(tripdetails.getColumnIndex("lgthi"));
    lgtlow =  tripdetails.getInt(tripdetails.getColumnIndex("lgtlo"));
    status =  tripdetails.getInt(tripdetails.getColumnIndex("status"));
    endTime = tripdetails.getDouble(tripdetails.getColumnIndex("endtime"));
    distance = tripdetails.getFloat(tripdetails.getColumnIndex("distance"));

    purp = tripdetails.getString(tripdetails.getColumnIndex("purp"));
    fancystart = tripdetails.getString(tripdetails.getColumnIndex("fancystart"));
    info = tripdetails.getString(tripdetails.getColumnIndex("fancyinfo"));

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

  public boolean dataAvailable() { return gpspoints.size() != 0; }
  public GeoPoint startLocation() { return gpspoints.get(0); }
  public GeoPoint endLocation() { return gpspoints.get(gpspoints.size()-1); }
  public BoundingBoxE6 boundingBox() { return new BoundingBoxE6(lathigh, lgtlow, latlow, lgthigh); }
	public Iterable<GeoPoint> journey() { return gpspoints;	}
  public double startTime() { return startTime; }
  public double elapsed() {
    if(status == STATUS_RECORDING)
      return System.currentTimeMillis() - startTime;
    return endTime - startTime;
  } // elapsed
  public float distanceTravelled() {
    return (0.0006212f * distance);
  } // distanceTravelled


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

    endTime = loc.getTime();

    latlow = Math.min(latlow, lat);
    lathigh = Math.max(lathigh, lat);
    lgtlow = Math.min(lgtlow, lgt);
    lgthigh = Math.max(lgthigh, lgt);

    mDb.open();
    mDb.addCoordToTrip(tripid, pt);
    mDb.updateTrip(tripid, "", startTime, "", "", "", lathigh, latlow, lgthigh, lgtlow, distance);
    mDb.close();

    return;
  } // addPointNow

  public void recordingStopped() {
    updateTripStatus(STATUS_COMPLETE_UNSENT);
    updateTrip();
  }

  private void updateTripStatus(int tripStatus) {
    mDb.open();
    mDb.updateTripStatus(tripid, tripStatus);
    mDb.close();
  }

  public void updateTrip() { updateTrip("","","",""); }
  public void updateTrip(String purpose, String fancyStart, String fancyInfo, String notes) {
    // Save the trip details to the phone database. W00t!
    mDb.open();
    mDb.updateTrip(tripid, purpose,	startTime, fancyStart, fancyInfo, notes,
        lathigh, latlow, lgthigh, lgtlow, distance);
    mDb.close();
  }

  public void upload() {
    updateTripStatus(STATUS_COMPLETE);
  } // upload
} // TripData

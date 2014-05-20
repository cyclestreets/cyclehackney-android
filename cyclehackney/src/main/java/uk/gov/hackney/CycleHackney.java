package uk.gov.hackney;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.PhotoMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.PhotoUploadFragment;
import net.cyclestreets.fragments.R;

import java.util.ArrayList;
import java.util.List;

import uk.gov.hackney.track.DbAdapter;
import uk.gov.hackney.track.IRecordService;
import uk.gov.hackney.track.RecordingActivity;
import uk.gov.hackney.track.RecordingService;
import uk.gov.hackney.track.SaveTrip;
import uk.gov.hackney.track.TripData;
import uk.gov.hackney.track.TripDataUploader;

public class CycleHackney extends MainTabbedActivity {
  public static void start(final Context context) {
    final Intent fi = new Intent(context, CycleHackney.class);
    context.startActivity(fi);
  } // start

  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    isAlreadyActive(this);

    uploadLeftOverTrips();
  } // onCreate

  private void isAlreadyActive(final Activity activity) {
    // check to see if already recording here
    Intent rService = new Intent(activity, RecordingService.class);
    ServiceConnection sc = new ServiceConnection() {
      public void onServiceDisconnected(ComponentName name) {}
      public void onServiceConnected(ComponentName name, IBinder service) {
        IRecordService rs = (IRecordService)service;
        int state = rs.getState();
        if (state == RecordingService.STATE_RECORDING) {
          activity.startActivity(new Intent(activity, RecordingActivity.class));
          activity.finish();
        } else {
          int unfinishedTrip = DbAdapter.unfinishedTrip(activity);
          if (unfinishedTrip != -1) {
            SaveTrip.start(activity, unfinishedTrip);
            activity.finish();
          }
        }

        activity.unbindService(this); // race?  this says we no longer care
      }
    };
    // This needs to block until the onServiceConnected (above) completes.
    // Thus, we can check the recording status before continuing on.
    activity.bindService(rService, sc, Context.BIND_AUTO_CREATE);
  } // isAlreadyActive

  private void uploadLeftOverTrips() {
    final List<Integer> trips = DbAdapter.unUploadedTrips(this);
    if (trips.size() == 0)
      return;

    final List<TripData> tripData = new ArrayList<TripData>();
    for (int id : trips)
      tripData.add(TripData.fetchTrip(this, id));

    TripDataUploader.upload(this, tripData);
  } // uploadLeftOverTrips

  protected void addTabs(final TabHost tabHost) {
    addTab("Journey Log", R.drawable.ic_menu_live_ride, LogJourneyFragment.class);
    //addTab("Route Map", R.drawable.ic_tab_planroute, RouteMapFragment.class);
    addTab("Upload", R.drawable.ic_tab_photoupload, PhotoUploadFragment.class);
    addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs
} // class CycleHackney

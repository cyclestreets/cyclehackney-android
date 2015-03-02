package uk.gov.hackney;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.PhotoMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.PhotoUploadFragment;
import net.cyclestreets.fragments.R;

import java.util.ArrayList;
import java.util.List;

import net.cyclestreets.track.DbAdapter;
import net.cyclestreets.track.SaveTrip;
import net.cyclestreets.track.Tracker;
import net.cyclestreets.track.TrackerStatusListener;
import net.cyclestreets.track.TripData;
import net.cyclestreets.track.TripDataUploader;

public class CycleHackney extends MainTabbedActivity {
  public static void start(final Context context) {
    final Intent fi = new Intent(context, CycleHackney.class);
    context.startActivity(fi);
  } // start

  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    isAlreadyActive(this);

    Tracker.uploadLeftOverTrips(this);
  } // onCreate

  private void isAlreadyActive(final Activity activity) {
    Tracker.checkStatus(this, new TrackerStatusListener() {
      public void recordingActive() {
        HackneyRecordingActivity.start(activity);
        activity.finish();
      } // recordingActive
      public void unsavedTrip() {
        SaveTrip.startWithUnsaved(activity);
        activity.finish();
      } // unsavedTrip
    });
  } // isAlreadyActive

  protected void addTabs(final TabHost tabHost) {
    addTab("Journey Log", R.drawable.ic_tab_navigate, LogJourneyFragment.class);
    //addTab("Route Map", R.drawable.ic_tab_planroute, RouteMapFragment.class);
    addTab("Upload", R.drawable.ic_tab_photoupload, PhotoUploadFragment.class);
    addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs
} // class CycleHackney

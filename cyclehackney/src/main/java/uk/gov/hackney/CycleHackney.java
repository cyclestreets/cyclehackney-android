package uk.gov.hackney;

import android.os.Bundle;
import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.PhotoMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.fragments.R;

import uk.gov.hackney.track.RecordingService;

public class CycleHackney extends MainTabbedActivity
{
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    RecordingService.isAlreadyActive(this);
  } // onCreate

  protected void addTabs(final TabHost tabHost) {
    addTab("Journey Log", R.drawable.ic_menu_live_ride, LogJourneyFragment.class);
    //addTab("Route Map", R.drawable.ic_tab_planroute, RouteMapFragment.class);
    addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs
} // class CycleHackney

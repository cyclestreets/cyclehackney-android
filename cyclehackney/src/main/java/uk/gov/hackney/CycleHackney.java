package uk.gov.hackney;

import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.RouteMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.fragments.R;

public class CycleHackney extends MainTabbedActivity
{
  protected void addTabs(final TabHost tabHost) {
    addTab("Route Map", R.drawable.ic_tab_planroute, RouteMapFragment.class);
    //addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs
} // class CycleHackney

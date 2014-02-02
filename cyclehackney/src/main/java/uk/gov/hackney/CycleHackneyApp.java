package uk.gov.hackney;

import android.app.Application;
import android.content.DialogInterface;

import net.cyclestreets.CycleStreetsAppSupport;
import net.cyclestreets.util.MessageBox;

import uk.gov.hackney.track.DbAdapter;

public class CycleHackneyApp extends Application {
  @Override
  public void onCreate() {
    super.onCreate();

    CycleStreetsAppSupport.initialise(this);
  } // onCreate
} // CycleStreetsApp

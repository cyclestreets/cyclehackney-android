package uk.gov.hackney;

import android.app.Application;
import net.cyclestreets.CycleStreetsAppSupport;

public class CycleHackneyApp extends Application {
  @Override
  public void onCreate() {
    super.onCreate();

    CycleStreetsAppSupport.initialise(this);
  } // onCreate
} // CycleStreetsApp

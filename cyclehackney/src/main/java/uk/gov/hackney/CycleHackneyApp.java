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

    /*if (DbAdapter.availableForUpload(this)) {
      MessageBox.YesNo(this,
          "You have trips that need to be uploaded.  Do you want to upload them now?",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
          }
      );
    } */
  } // onCreate
} // CycleStreetsApp

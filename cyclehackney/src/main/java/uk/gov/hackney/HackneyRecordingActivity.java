package uk.gov.hackney;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.PhotoMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.PhotoUploadFragment;
import net.cyclestreets.fragments.R;

import uk.gov.hackney.track.RecordingFragment;

public class HackneyRecordingActivity extends MainTabbedActivity {
  public static void start(final Context context) {
    final Intent fi = new Intent(context, HackneyRecordingActivity.class);
    context.startActivity(fi);
  } // start

  protected void addTabs(final TabHost tabHost) {
    addTab("Recorder", R.drawable.ic_tab_navigate, RecordingFragment.class);
    addTab("Upload", R.drawable.ic_tab_photoupload, PhotoUploadFragment.class);
    addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs

  @Override
  public void onPause() {
    super.onPause();

    final SharedPreferences.Editor edit = prefs().edit();
    edit.putString("TAB", "Recorder");
    edit.commit();
  } // onResume
} // class CycleHackney

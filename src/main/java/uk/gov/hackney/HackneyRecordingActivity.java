package uk.gov.hackney;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TabHost;

import net.cyclestreets.MainTabbedActivity;
import net.cyclestreets.PhotoMapFragment;
import net.cyclestreets.AboutFragment;
import net.cyclestreets.PhotoUploadFragment;
import net.cyclestreets.fragments.R;

public class HackneyRecordingActivity extends MainTabbedActivity {
  public static void start(final Context context) {
    final Intent fi = new Intent(context, HackneyRecordingActivity.class);
    context.startActivity(fi);
  } // start

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    resetFirstTab();
  } // onCreate

  @Override
  public void onNewIntent(final Intent intent) {
    super.onNewIntent(intent);

    resetFirstTab();
  } // onNewIntent

  @Override
  protected void addTabs(final TabHost tabHost) {
    addTab("Recorder", R.drawable.ic_tab_navigate, HackneyRecordingFragment.class);
    addTab("Upload", R.drawable.ic_tab_photoupload, PhotoUploadFragment.class);
    addTab("Photomap", R.drawable.ic_tab_photomap, PhotoMapFragment.class);
    addTab("About", R.drawable.ic_tab_more, AboutFragment.class);
  } // addTabs

  private void resetFirstTab() {
    final SharedPreferences.Editor edit = prefs().edit();
    edit.putString("TAB", "Recorder");
    edit.commit();
  } // resetFirstTab
} // class CycleHackney

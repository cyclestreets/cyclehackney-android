package uk.gov.hackney;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.cyclestreets.util.MessageBox;

public class LogJourneyFragment extends Fragment implements View.OnClickListener {
  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_log_journey, container, false);

    final Button startButton = (Button)view.findViewById(R.id.ButtonStart);
    startButton.setOnClickListener(this);

    return view;
  } // onCreateView

  public void onClick(final View v) {
    // Before we go to record, check GPS status
    final LocationManager manager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      alertNoGps();
      return;
    }

    //startActivity(i);
  } // onClick

  private void alertNoGps() {
    MessageBox.YesNo(getActivity(),
        "Your phone's GPS is disabled. Cycle Hackney needs GPS to determine your location.\n\nGo to System Settings now to enable GPS?",
        new DialogInterface.OnClickListener() {
          public void onClick(final DialogInterface dialog, final int id) {
            final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(toLaunch);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, 0);
          }
        });
  } // alertNoGps
} // LogJourneyFragment

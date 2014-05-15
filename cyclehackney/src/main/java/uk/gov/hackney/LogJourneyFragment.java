package uk.gov.hackney;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.cyclestreets.util.MessageBox;

import java.sql.SQLException;

import uk.gov.hackney.track.DbAdapter;
import uk.gov.hackney.track.RecordingActivity;
import uk.gov.hackney.track.ShowJourney;

public class LogJourneyFragment extends Fragment implements View.OnClickListener {
  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_log_journey, container, false);

    final Button startButton = (Button)view.findViewById(R.id.ButtonStart);
    startButton.setOnClickListener(this);

    // Not first run - set up the list view of saved trips
    final ListView listSavedTrips = (ListView)view.findViewById(R.id.ListSavedTrips);
    populateList(listSavedTrips);

    return view;
  } // onCreateView

  public void onClick(final View v) {
    // Before we go to record, check GPS status
    final LocationManager manager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      alertNoGps();
      return;
    }

    startActivity(new Intent(getActivity(), RecordingActivity.class));
    getActivity().finish();
  } // onClick

  private void alertNoGps() {
    MessageBox.YesNo(getActivity(),
        "Your phone's GPS is disabled. Cycle Hackney needs GPS to determine your location.\n\nGo to System Settings now to enable GPS?",
        new DialogInterface.OnClickListener() {
          public void onClick(final DialogInterface dialog, final int id) {
            final ComponentName toLaunch = new ComponentName("com.android.settings", "com.android.settings.SecuritySettings");
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(toLaunch);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, 0);
          }
        });
  } // alertNoGps

  /////////////////////////////////////////
  private void populateList(final ListView lv) {
    // Get list from the real phone database. W00t!
    final DbAdapter database = new DbAdapter(getActivity());
    database.open();

    final float distance = database.totalDistance();
    final Cursor allTrips = database.fetchAllTrips();

    SimpleCursorAdapter sca = new SimpleCursorAdapter(getActivity(),
        R.layout.twolinelist, allTrips,
        new String[] { "purp", "fancystart", "fancyinfo"},
        new int[] {R.id.TextView01, R.id.TextView03, R.id.TextInfo}
    );

    lv.setAdapter(sca);
    final TextView counter = (TextView)lv.getRootView().findViewById(R.id.TextViewPreviousTrips);
    final TextView total = (TextView)lv.getRootView().findViewById(R.id.TextViewTotalDistance);

    final double distanceMiles = distance * 0.0006212f;
    final int calories = (int)((distanceMiles * 49) - 1.69);
    total.setText(distance != 0 ? String.format("Total distance: %1.1f miles\nCalories burnt: %d kcal", distanceMiles, calories) : "");

    final int numtrips = allTrips.getCount();
    switch (numtrips) {
      case 0:
        counter.setText("No saved trips.");
        break;
      case 1:
        counter.setText("1 saved trip:");
        break;
      default:
        counter.setText("" + numtrips + " saved trips:");
    }
    database.close();

    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
        final Intent i = new Intent(getActivity(), ShowJourney.class);
        i.putExtra("showtrip", id);
        startActivity(i);
      } // onItemClick
    });
  } // populateList
} // LogJourneyFragment

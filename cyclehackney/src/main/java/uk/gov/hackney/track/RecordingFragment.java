package uk.gov.hackney.track;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.cyclestreets.util.MessageBox;
import net.cyclestreets.views.CycleMapView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import uk.gov.hackney.CycleHackney;
import uk.gov.hackney.R;

public class RecordingFragment extends Fragment
    implements View.OnClickListener, ServiceConnection, RecordingService.RecordingListener {
  private IRecordService rs_;
  private TripData trip_;

  private Button finishButton_;
  private TextView txtDistance_;
  private TextView txtDuration_;
  private TextView txtCurSpeed_;

  private CycleMapView mapView_;

  private final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");

  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    View rootView = inflater.inflate(R.layout.journey_in_progress, null);

    mapView_ = new CycleMapView(getActivity(), getClass().getName());
    mapView_.hideLocationButton();
    mapView_.lockOnLocation();
    mapView_.getController().setZoom(16);

    final RelativeLayout v = (RelativeLayout)rootView.findViewById(R.id.mapholder);
    v.addView(mapView_,
        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
            RelativeLayout.LayoutParams.FILL_PARENT));

    txtDistance_ = (TextView)rootView.findViewById(R.id.journey_distance);
    txtDuration_ = (TextView)rootView.findViewById(R.id.journey_time);
    txtCurSpeed_ = (TextView)rootView.findViewById(R.id.journey_speed);

    finishButton_ = (Button)rootView.findViewById(R.id.ButtonFinished);

    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    final Intent rService = new Intent(getActivity(), RecordingService.class);
    getActivity().startService(rService);
    getActivity().bindService(rService, this, Context.BIND_AUTO_CREATE);

    // Finish button
    finishButton_.setOnClickListener(this);

    return rootView;
  } // onCreateView

  @Override
  public void updateStatus(float spdCurrent, float spdMax) {
    txtCurSpeed_.setText(String.format("%1.1f mph", spdCurrent));

    txtDistance_.setText(String.format("%1.1f miles", trip_.distanceTravelled()));

    mapView_.invalidate();
  } // updateStatus

  @Override
  public void updateTimer(long elapsedMS) {
    txtDuration_.setText(sdf.format(elapsedMS));
  } // updateTimer

  @Override
  public void riderHasStopped() {
    finishTrip();
  } // riderHasStopped


  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    rs_ = (IRecordService)service;

    trip_ = rs_.startRecording();
    getActivity().setTitle("Cycle Hackney - Recording...");
    mapView_.overlayPushTop(JourneyOverlay.InProgressJourneyOverlay(getActivity(), mapView_, trip_));

    rs_.setListener(this);
  } // onServiceConnected

  @Override
  public void onServiceDisconnected(ComponentName name) {
    rs_ = null;
  } // onServiceDisconnected

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onClick(final View v) {
    confirmFinishTrip();
  } // onClick

  private void confirmFinishTrip () {
    MessageBox.YesNo(getActivity(),
        "Are you sure you want to stop recording?",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            finishTrip();
          }
        }
    );
  } // confirmFinishTrip

  private void finishTrip() {
    // If we have points, go to the save-trip activity
    if (trip_.dataAvailable()) {
      rs_.finishRecording();

      SaveTrip.start(getActivity(), trip_.id());
    } else {
      rs_.cancelRecording();

      // Otherwise, cancel and go back to main screen
      Toast.makeText(getActivity().getBaseContext(), "No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

      CycleHackney.start(getActivity());
    } // if ...

    getActivity().finish();
  } // finishedTrip

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onPause() {
    super.onPause();
    mapView_.onPause();
  } // onPause

  @Override
  public void onResume() {
    super.onResume();
    mapView_.onResume();
  } // onResume

  @Override
  public void onDestroy() {
    getActivity().unbindService(this);
    super.onDestroy();
  }

} // class RecordingFragment

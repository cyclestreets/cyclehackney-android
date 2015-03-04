package uk.gov.hackney;

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

import net.cyclestreets.routing.Journey;
import net.cyclestreets.track.TrackListener;
import net.cyclestreets.track.Tracker;
import net.cyclestreets.track.TrackerControl;
import net.cyclestreets.util.MessageBox;
import net.cyclestreets.views.CycleMapView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import net.cyclestreets.track.JourneyOverlay;
import net.cyclestreets.track.SaveTrip;
import net.cyclestreets.track.TripData;

public class HackneyRecordingFragment extends Fragment
    implements View.OnClickListener, TrackListener {
  private TrackerControl control_;

  private Button finishButton_;
  private TextView txtDistance_;
  private TextView txtDuration_;
  private TextView txtCurSpeed_;

  private CycleMapView mapView_;
  private JourneyOverlay journeyOverlay_;

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
    journeyOverlay_ = null;

    final RelativeLayout v = (RelativeLayout)rootView.findViewById(R.id.mapholder);
    v.addView(mapView_,
        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
            RelativeLayout.LayoutParams.FILL_PARENT));

    txtDistance_ = (TextView)rootView.findViewById(R.id.journey_distance);
    txtDuration_ = (TextView)rootView.findViewById(R.id.journey_time);
    txtCurSpeed_ = (TextView)rootView.findViewById(R.id.journey_speed);

    finishButton_ = (Button)rootView.findViewById(R.id.ButtonFinished);

    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    if (control_ == null)
      control_ = Tracker.create(getActivity(), this);
    control_.start();

    getActivity().setTitle("Cycle Hackney - Recording...");

    // Finish button
    finishButton_.setOnClickListener(this);

    return rootView;
  } // onCreateView

  ///////////////////////////////////////////////
  @Override
  public void started(TripData trip) {
  } // started

  @Override
  public void updateStatus(float currentMph, TripData trip) {
    long millisecondsElapsed = trip.secondsElapsed() * 1000L;
    txtDuration_.setText(sdf.format(millisecondsElapsed));

    txtCurSpeed_.setText(String.format("%1.1f mph", currentMph));

    txtDistance_.setText(String.format("%1.1f miles", trip.distanceTravelled()));

    updateOverlay(trip);
  } // updateStatus

  @Override
  public void riderHasStopped(final TripData trip) {
    finishTrip();
  } // riderHasStopped

  @Override
  public void completed(final TripData trip) {
    SaveTrip.start(getActivity(), trip.id());
    getActivity().finish();
  } // completed

  @Override
  public void abandoned(final TripData trip) {
    Toast.makeText(getActivity().getBaseContext(), "No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();
    CycleHackney.start(getActivity());
    getActivity().finish();
  } // abandoned

  /////////////////////////////////////////////////////////////////////////////
  private void updateOverlay(TripData trip) {
    if (journeyOverlay_ == null) {
      journeyOverlay_ = JourneyOverlay.InProgressJourneyOverlay(getActivity(), mapView_, trip);
      mapView_.overlayPushTop(journeyOverlay_);
    } // ...

    journeyOverlay_.update(trip);
  } // addJourneyOverlay

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onClick(final View v) {
    confirmFinishTrip();
  } // onClick

  private void confirmFinishTrip () {
    MessageBox.YesNo(getActivity(),
        "Finish trip?",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            finishTrip();
          }
        }
    );
  } // confirmFinishTrip

  private void finishTrip() {
    control_.stop();
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
    if (control_ != null)
      control_.stop();
    super.onDestroy();
  } // onDestroy
} // class RecordingFragment

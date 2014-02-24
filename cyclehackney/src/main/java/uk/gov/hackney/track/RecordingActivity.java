package uk.gov.hackney.track;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.cyclestreets.views.CycleMapView;

import uk.gov.hackney.CycleHackney;
import uk.gov.hackney.R;

public class RecordingActivity extends Activity
    implements View.OnClickListener, ServiceConnection {
  private IRecordService rs_;
  private TripData trip_;

  private Button finishButton_;
  private TextView txtDistance;
  private TextView txtDuration;
  private TextView txtCurSpeed;

  private CycleMapView mapView_;

  private final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");

  private Timer timer_;
  private final Handler handler_ = new Handler();
  private final Runnable updateTimer_ = new Runnable() {
    public void run() {
      updateTimer();
    }
  };

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.journey_in_progress);

    mapView_ = new CycleMapView(this, getClass().getName());
    mapView_.hideLocationButton();
    mapView_.lockOnLocation();
    mapView_.getController().setZoom(16);

    final RelativeLayout v = (RelativeLayout)findViewById(R.id.mapholder);
    v.addView(mapView_,
        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
            RelativeLayout.LayoutParams.FILL_PARENT));

    txtDistance = (TextView)findViewById(R.id.journey_distance);
    txtDuration = (TextView)findViewById(R.id.journey_time);
    txtCurSpeed = (TextView)findViewById(R.id.journey_speed);

    finishButton_ = (Button)findViewById(R.id.ButtonFinished);

    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    final Intent rService = new Intent(this, RecordingService.class);
    startService(rService);
    bindService(rService, this, Context.BIND_AUTO_CREATE);

    // Finish button
    finishButton_.setOnClickListener(this);
  } // onCreate

  public void updateStatus(float spdCurrent, float spdMax) {
    txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));

    txtDistance.setText(String.format("%1.1f miles", trip_.distanceTravelled()));

    mapView_.invalidate();
  } // updateStatus

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    rs_ = (IRecordService)service;

    trip_ = rs_.startRecording();
    setTitle("Cycle Hackney - Recording...");
    mapView_.overlayPushTop(JourneyOverlay.InProgressJourneyOverlay(this, mapView_, trip_));

    rs_.setListener(this);
  } // onServiceConnected

  @Override
  public void onServiceDisconnected(ComponentName name) {
    rs_ = null;
  } // onServiceDisconnected

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onClick(final View v) {
    finishTrip();
  } // onClick

  private void finishTrip() {
    // If we have points, go to the save-trip activity
    if (trip_.dataAvailable()) {
      rs_.finishRecording();

      SaveTrip.start(this, trip_.id());
    } else {
      rs_.cancelRecording();

      // Otherwise, cancel and go back to main screen
      Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

      CycleHackney.start(this);
    } // if ...

    finish();
  } // finishedTrip

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onPause() {
    super.onPause();
    mapView_.onPause();
    stopTimer();
  } // onPause

  @Override
  public void onResume() {
    super.onResume();
    mapView_.onResume();
    startTimer();
  } // onResume

  @Override
  protected void onDestroy() {
    unbindService(this);
    super.onDestroy();
  }

  /////////////////////////////////////////////////////////////////////////////
  private void updateTimer() {
    if (trip_ == null)
      return;

    txtDuration.setText(sdf.format(trip_.elapsedMS()));
    if (rs_.hasRiderStopped())
      finishTrip();
  } // updateTimer

  private void startTimer() {
    timer_ = new Timer();
    timer_.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        handler_.post(updateTimer_);
      }
    }, 0, 1000);  // every second
  } // startTimer

  private void stopTimer() {
    if (timer_ != null)
      timer_.cancel();
  } // stopTimer
} // class RecordingActivity

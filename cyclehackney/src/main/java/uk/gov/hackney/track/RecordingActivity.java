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
import android.widget.TextView;
import android.widget.Toast;

import uk.gov.hackney.CycleHackney;
import uk.gov.hackney.R;

public class RecordingActivity extends Activity
    implements View.OnClickListener, ServiceConnection {
  private IRecordService rs_;
  private TripData trip_;
  private float curDistance_;

  private Button finishButton_;
  private TextView txtStat;
  private TextView txtDistance;
  private TextView txtDuration;
  private TextView txtCurSpeed;
  private TextView txtMaxSpeed;
  private TextView txtAvgSpeed;

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

    setContentView(R.layout.recording);

    txtStat =     (TextView)findViewById(R.id.TextRecordStats);
    txtDistance = (TextView)findViewById(R.id.TextDistance);
    txtDuration = (TextView)findViewById(R.id.TextDuration);
    txtCurSpeed = (TextView)findViewById(R.id.TextSpeed);
    txtMaxSpeed = (TextView)findViewById(R.id.TextMaxSpeed);
    txtAvgSpeed = (TextView)findViewById(R.id.TextAvgSpeed);

    finishButton_ = (Button)findViewById(R.id.ButtonFinished);

    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    final Intent rService = new Intent(this, RecordingService.class);
    startService(rService);
    bindService(rService, this, Context.BIND_AUTO_CREATE);

    // Finish button
    finishButton_.setOnClickListener(this);
  } // onCreate

  public void updateStatus(int points, float distance, float spdCurrent, float spdMax) {
    curDistance_ = distance;

    txtStat.setText((points>0) ? points + " data points received..." : "Waiting for GPS fix...");
    txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));
    txtMaxSpeed.setText(String.format("Max Speed: %1.1f mph", spdMax));

    float miles = 0.0006212f * distance;
    txtDistance.setText(String.format("%1.1f miles", miles));
  } // updateStatus

  private void cancelRecording() {
    rs_.cancelRecording();
  } // cancelRecording

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    rs_ = (IRecordService)service;

    trip_ = rs_.startRecording();
    setTitle("Cycle Hackney - Recording...");

    rs_.setListener(this);
  } // onServiceConnected

  @Override
  public void onServiceDisconnected(ComponentName name) {
    rs_ = null;
  } // onServiceDisconnected

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onClick(final View v) {
    Intent fi;
    // If we have points, go to the save-trip activity
    if (trip_.numpoints > 0) {
      // Save trip so far (points and extent, but no purpose or notes)
      fi = new Intent(this, SaveTrip.class);
      trip_.updateTrip("","","","");
    } else {
      // Otherwise, cancel and go back to main screen
      Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

      cancelRecording();

      fi = new Intent(this, CycleHackney.class);
      fi.putExtra("keep", true);
    } // if ...

    // Either way, activate next task, and then kill this task
    startActivity(fi);
    finish();
  } // onClick

  /////////////////////////////////////////////////////////////////////////////
  @Override
  public void onPause() {
    super.onPause();
    stopTimer();
  } // onPause

  @Override
  public void onResume() {
    super.onResume();

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

    double dd = System.currentTimeMillis() - trip_.startTime;

    txtDuration.setText(sdf.format(dd));

    double avgSpeed = 3600.0 * 0.6212 * curDistance_ / dd;
    txtAvgSpeed.setText(String.format("%1.1f mph", avgSpeed));
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
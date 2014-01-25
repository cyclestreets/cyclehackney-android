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

public class RecordingActivity extends Activity {
  private Intent fi;
  private TripData trip;
  private boolean isRecording = false;
  private Button finishButton;
  private Timer timer;
  private float curDistance;

  private TextView txtStat;
  private TextView txtDistance;
  private TextView txtDuration;
  private TextView txtCurSpeed;
  private TextView txtMaxSpeed;
  private TextView txtAvgSpeed;

  private final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");

  // Need handler for callbacks to the UI thread
  private final Handler mHandler = new Handler();
  private final Runnable mUpdateTimer = new Runnable() {
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

    finishButton = (Button)findViewById(R.id.ButtonFinished);

    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    // Query the RecordingService to figure out what to do.
    Intent rService = new Intent(this, RecordingService.class);
    startService(rService);
    ServiceConnection sc = new ServiceConnection() {
      public void onServiceDisconnected(ComponentName name) {}
      public void onServiceConnected(ComponentName name, IBinder service) {
        IRecordService rs = (IRecordService) service;

        switch (rs.getState()) {
          case RecordingService.STATE_IDLE:
            trip = TripData.createTrip(RecordingActivity.this);
            rs.startRecording(trip);
            isRecording = true;
            RecordingActivity.this.setTitle("Cycle Hackney - Recording...");
            break;
          case RecordingService.STATE_RECORDING:
            long id = rs.getCurrentTrip();
            trip = TripData.fetchTrip(RecordingActivity.this, id);
            isRecording = true;
            RecordingActivity.this.setTitle("Cycle Hackney - Recording...");
            break;
          case RecordingService.STATE_FULL:
            // Should never get here, right?
            break;
        }
        rs.setListener(RecordingActivity.this);
        unbindService(this);
      }
    };
    bindService(rService, sc, Context.BIND_AUTO_CREATE);

    // Finish button
    finishButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // If we have points, go to the save-trip activity
        if (trip.numpoints > 0) {
          // Save trip so far (points and extent, but no purpose or notes)
          fi = new Intent(RecordingActivity.this, SaveTrip.class);
          trip.updateTrip("","","","");
        }
        // Otherwise, cancel and go back to main screen
        else {
          Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();

          cancelRecording();

          // Go back to main screen
          fi = new Intent(RecordingActivity.this, CycleHackney.class);
          fi.putExtra("keep", true);
        }

        // Either way, activate next task, and then kill this task
        startActivity(fi);
        RecordingActivity.this.finish();
      }
    });
  }

  public void updateStatus(int points, float distance, float spdCurrent, float spdMax) {
    this.curDistance = distance;

    //TODO: check task status before doing this?
    if (points>0) {
      txtStat.setText(""+points+" data points received...");
    } else {
      txtStat.setText("Waiting for GPS fix...");
    }
    txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));
    txtMaxSpeed.setText(String.format("Max Speed: %1.1f mph", spdMax));

    float miles = 0.0006212f * distance;
    txtDistance.setText(String.format("%1.1f miles", miles));
  }

  void setListener() {
    Intent rService = new Intent(this, RecordingService.class);
    ServiceConnection sc = new ServiceConnection() {
      public void onServiceDisconnected(ComponentName name) {}
      public void onServiceConnected(ComponentName name, IBinder service) {
        IRecordService rs = (IRecordService) service;
        unbindService(this);
      }
    };
    // This should block until the onServiceConnected (above) completes, but doesn't
    bindService(rService, sc, Context.BIND_AUTO_CREATE);
  }

  void cancelRecording() {
    Intent rService = new Intent(this, RecordingService.class);
    ServiceConnection sc = new ServiceConnection() {
      public void onServiceDisconnected(ComponentName name) {}
      public void onServiceConnected(ComponentName name, IBinder service) {
        IRecordService rs = (IRecordService) service;
        rs.cancelRecording();
        unbindService(this);
      }
    };
    // This should block until the onServiceConnected (above) completes.
    bindService(rService, sc, Context.BIND_AUTO_CREATE);
  }

  // onResume is called whenever this activity comes to foreground.
  // Use a timer to update the trip duration.
  @Override
  public void onResume() {
    super.onResume();

    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        mHandler.post(mUpdateTimer);
      }
    }, 0, 1000);  // every second
  }

  void updateTimer() {
    if (trip != null && isRecording) {
      double dd = System.currentTimeMillis() - trip.startTime;

      txtDuration.setText(sdf.format(dd));

      double avgSpeed = 3600.0 * 0.6212 * this.curDistance / dd;
      txtAvgSpeed.setText(String.format("%1.1f mph", avgSpeed));
    }
  }

  // Don't do pointless UI updates if the activity isn't being shown.
  @Override
  public void onPause() {
    super.onPause();
    if (timer != null) timer.cancel();
  }
}

package uk.gov.hackney.track;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import uk.gov.hackney.R;

public class RecordingService extends Service implements LocationListener {
  private static int updateDistance = 5;  // metres
  private static int updateTime = 5000;    // milliseconds
  private static final int NOTIFICATION_ID = 1;

  private RecordingActivity recordActivity;
  private LocationManager locationManager_ = null;

  // Bike bell variables
  private static int BELL_FIRST_INTERVAL = 20;
  private static int BELL_NEXT_INTERVAL = 5;
  private Timer timer;
  private SoundPool soundpool;
  private int bikebell;
  final Handler mHandler = new Handler();
  final Runnable mRemindUser = new Runnable() {
    public void run() { remindUser(); }
  };

  float curSpeed, maxSpeed;
  TripData trip;

  public final static int STATE_IDLE = 0;
  public final static int STATE_RECORDING = 1;
  public final static int STATE_FULL = 3;

  int state = STATE_IDLE;
  private final MyServiceBinder myServiceBinder = new MyServiceBinder();

  // ---SERVICE methods - required! -----------------
  @Override
  public void onCreate() {
    super.onCreate();
    soundpool = new SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
    bikebell = soundpool.load(this.getBaseContext(), R.raw.bikebell,1);
    locationManager_ = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
  } // onCreate

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (timer!=null) {
      timer.cancel();
      timer.purge();
    }
  }

  @Override
  public IBinder onBind(final Intent intent) {
    return myServiceBinder;
  }
  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    return Service.START_STICKY;
  } // onStartCommand

  private class MyServiceBinder extends Binder implements IRecordService {
    public int getState() {
      return state;
    }
    public TripData startRecording() {
      return RecordingService.this.startRecording();
    }
    public void cancelRecording() {
      RecordingService.this.cancelRecording();
    }
    public long finishRecording() {
      return RecordingService.this.finishRecording();
    }
    public void reset() {
      RecordingService.this.state = STATE_IDLE;
    }
    public void setListener(RecordingActivity ra) {
      RecordingService.this.recordActivity = ra;
      notifyListeners();
    }
  } // class MyServiceBinder

  // ---end SERVICE methods -------------------------

  public TripData startRecording() {
    if (state == STATE_RECORDING)
      return this.trip;

    startForeground(NOTIFICATION_ID, createNotification("Recording ...", Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT));

    this.state = STATE_RECORDING;
    this.trip = TripData.createTrip(RecordingService.this);

    curSpeed = maxSpeed = 0.0f;

    // Start listening for GPS updates!
    locationManager_.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTime, updateDistance, this);

    // Set up timer for bike bell
    if (timer != null) {
      timer.cancel(); timer.purge();
    }
    timer = new Timer();
    timer.schedule (new TimerTask() {
      @Override public void run() {
        mHandler.post(mRemindUser);
      }
    }, BELL_FIRST_INTERVAL*60000, BELL_NEXT_INTERVAL*60000);

    return trip;
  }

  public long finishRecording() {
    this.state = STATE_FULL;

    clearUp();

    trip.recordingStopped();

    return trip.tripid;
  }

  public void cancelRecording() {
    if (trip != null)
      trip.dropTrip();

    clearUp();

    this.state = STATE_IDLE;
  } // cancelRecording

  private void clearUp() {
    locationManager_.removeUpdates(this);

    clearNotifications();

    if (timer != null) {
      timer.cancel();
      timer.purge();
    }

    stopForeground(true);
  } // clearUp

  // LocationListener implementation:
  @Override
  public void onLocationChanged(Location loc) {
    updateTripStats(loc);
    trip.addPointNow(loc);
    notifyListeners();
  }

  private void updateTripStats(Location newLocation) {
    final float spdConvert = 2.2369f;

    // Stats should only be updated if accuracy is decent
    if (newLocation.getAccuracy() > 20)
      return;

    // Speed data is sometimes awful, too:
    curSpeed = newLocation.getSpeed() * spdConvert;
    if (curSpeed < 60.0f)
      maxSpeed = Math.max(maxSpeed, curSpeed);
  } // updateTripStats


  @Override
  public void onProviderDisabled(String arg0) {
  }

  @Override
  public void onProviderEnabled(String arg0) {
  }

  @Override
  public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
  }
  // END LocationListener implementation:

  private NotificationManager nm() {
    return (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
  } // nm

  private Notification createNotification(final String tickerText,
                                          final int flags) {
    final Notification notification = new Notification(R.drawable.icon25, tickerText, System.currentTimeMillis());
    notification.flags = flags;
    final Intent notificationIntent = new Intent(this, RecordingActivity.class);
    final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    notification.setLatestEventInfo(this, "Cycle Hackney - Recording", "Tap to see your ongoing trip", contentIntent);
    return notification;
  } // createNotification

  private void showNotification(final String tickerText,
                                final int flags) {
    final Notification notification = createNotification(tickerText, flags);
    nm().notify(NOTIFICATION_ID, notification);
  } // showNotification

  public void remindUser() {
    soundpool.play(bikebell, 1.0f, 1.0f, 1, 0, 1.0f);

    int minutes = (int) (trip.elapsed() / 60000);
    String tickerText = String.format("Still recording (%d min)", minutes);

    showNotification(tickerText, Notification.FLAG_ONGOING_EVENT);
  } // remindUser

  private void clearNotifications() {
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    mNotificationManager.cancel(NOTIFICATION_ID);
  } // clearNotifications

  void notifyListeners() {
    if (recordActivity != null) {
      recordActivity.updateStatus(curSpeed, maxSpeed);
    }
  } // notifyListeners
} // RecordingService

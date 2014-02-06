package uk.gov.hackney.track;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings.System;

import net.cyclestreets.api.ApiClient;
import net.cyclestreets.util.ListFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import uk.gov.hackney.R;

import uk.gov.hackney.CycleHackney;

public class TripDataUploader extends AsyncTask<Void, Void, Boolean> {
  private int NOTIFICATION_ID = 1;

  public static void upload(final Context context, final TripData tripData) {
    upload(context, ListFactory.list(tripData));
  }

  public static void upload(final Context context, final List<TripData> tripData) {
    final TripDataUploader tdu = new TripDataUploader(context, tripData);
    tdu.execute();
  }

  private Context context_;
  private List<TripData> tripData_;

  private TripDataUploader(final Context context, final List<TripData> tripData) {
    context_ = context;
    tripData_ = tripData;
  } // UploadDataTask

  protected Boolean doInBackground(Void... p) {
    for (final TripData td : tripData_) {
      try  {
        notification("Uploading trip ...");

        final byte[] resultBytes = ApiClient.postApiRaw("/v2/gpstracks.add",
                       "username", "",
                       "password", "",
                       "version", 1,
                       "notes", td.notes(),
                       "purpose", td.purpose(),
                       "start", td.startTime(),
                       "end", td.endTime(),
                       "user", userAsJSON(td),
                       "userAge", td.age(),
                       "userGender", td.gender(),
                       "coords", coordsAsJSON(td),
                       "device", deviceId(),
                       "format", "atlanta"
            );
        final JSONObject result = parse(resultBytes);

        if (result.has("error") && result.getString("error").length() != 0)
          throw new RuntimeException("Poop");

        td.successfullyUploaded();
        cancelNotification();
      } catch (final Exception e) {
        td.uploadFailed();
        warning("Upload failed.");
      }
    } // for ...
    return true;
  } // doInBackground

  private String deviceId() {
    String androidId = System.getString(context_.getContentResolver(), System.ANDROID_ID);
    String androidBase = "androidDeviceId-";

    if (androidId == null) { // This happens when running in the Emulator
      final String emulatorId = "android-RunningAsTestingDeleteMe";
      return emulatorId;
    }
    String deviceId = androidBase.concat(androidId);
    return deviceId;
  } // deviceId

  private JSONObject parse(final byte[] result) throws Exception {
    final String s = new String(result, "UTF-8");
    return new JSONObject(s);
  } // parse

  private static final String TRIP_COORDS_TIME = "rec";
  private static final String TRIP_COORDS_LAT = "lat";
  private static final String TRIP_COORDS_LON = "lon";
  private static final String TRIP_COORDS_ALT = "alt";
  private static final String TRIP_COORDS_SPEED = "spd";
  private static final String TRIP_COORDS_HACCURACY = "hac";
  private static final String TRIP_COORDS_VACCURACY = "vac";

  private String coordsAsJSON(final TripData tripData) throws JSONException {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    JSONObject tripCoords = new JSONObject();

    for(CyclePoint cp : tripData.journey()) {
      JSONObject coord = new JSONObject();

      coord.put(TRIP_COORDS_TIME, cp.time);
      coord.put(TRIP_COORDS_LAT, cp.getLatitudeE6()/1e6);
      coord.put(TRIP_COORDS_LON, cp.getLongitudeE6()/1e6);
      coord.put(TRIP_COORDS_ALT, cp.getAltitude());
      coord.put(TRIP_COORDS_SPEED, cp.speed);
      coord.put(TRIP_COORDS_HACCURACY, cp.accuracy);
      coord.put(TRIP_COORDS_VACCURACY, cp.accuracy);

      tripCoords.put(coord.getString(TRIP_COORDS_TIME), coord);
    }

    return tripCoords.toString();
  } // coordsAsJSON

  private String userAsJSON(final TripData tripData) throws JSONException {
    JSONObject user = new JSONObject();
    user.put("userAge", tripData.age());
    user.put("userGender", tripData.gender());
    return user.toString();
  }

  private void notification(final String text) {
    showNotification(text, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
  } // notification

  private void warning(final String text) {
    showNotification(text, Notification.FLAG_AUTO_CANCEL);
  }

  private void showNotification(final String text, final int flags) {
    final NotificationManager nm = nm();
    final Notification notification = createNotification(text, flags);
    nm.notify(NOTIFICATION_ID, notification);
  }

  private Notification createNotification(final String text, final int flags) {
    final Notification notification = new Notification(R.drawable.icon25, text, java.lang.System.currentTimeMillis());
    notification.flags = flags;
    final Intent notificationIntent = new Intent(context_, CycleHackney.class);
    final PendingIntent contentIntent = PendingIntent.getActivity(context_, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    notification.setLatestEventInfo(context_.getApplicationContext(), "Cycle Hackney", text, contentIntent);
    return notification;
  }

  private void cancelNotification() {
    nm().cancel(NOTIFICATION_ID);
  } // cancelNotification

  private NotificationManager nm() {
    return (NotificationManager)context_.getSystemService(Context.NOTIFICATION_SERVICE);
  } // nm
} // UploadDataTask

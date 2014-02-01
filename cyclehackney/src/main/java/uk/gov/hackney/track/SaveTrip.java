package uk.gov.hackney.track;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import uk.gov.hackney.CycleHackney;
import uk.gov.hackney.R;

public class SaveTrip extends Activity
    implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
  public static void start(final Context context, final long tripid) {
    final Intent fi = new Intent(context, SaveTrip.class);
    fi.putExtra("showtrip", tripid);
    context.startActivity(fi);
  } // start

  private final Map<Integer, ToggleButton> purpButtons = new HashMap<Integer,ToggleButton>();
  private final Map <Integer, String> purpDescriptions = new HashMap<Integer, String>();
  private TripData trip_;
  private String purpose_;

  // Set up the purpose buttons to be one-click only
  private void preparePurposeButtons() {
    purpButtons.put(R.id.ToggleCommute, (ToggleButton)findViewById(R.id.ToggleCommute));
    purpButtons.put(R.id.ToggleSchool,  (ToggleButton)findViewById(R.id.ToggleSchool));
    purpButtons.put(R.id.ToggleWorkRel, (ToggleButton)findViewById(R.id.ToggleWorkRel));
    purpButtons.put(R.id.ToggleExercise,(ToggleButton)findViewById(R.id.ToggleExercise));
    purpButtons.put(R.id.ToggleSocial,  (ToggleButton)findViewById(R.id.ToggleSocial));
    purpButtons.put(R.id.ToggleShopping,(ToggleButton)findViewById(R.id.ToggleShopping));
    purpButtons.put(R.id.ToggleErrand,  (ToggleButton)findViewById(R.id.ToggleErrand));
    purpButtons.put(R.id.ToggleOther,   (ToggleButton)findViewById(R.id.ToggleOther));

    purpDescriptions.put(R.id.ToggleCommute,
        "<b>Commute:</b> this bike trip was primarily to get between home and your main workplace.");
    purpDescriptions.put(R.id.ToggleSchool,
        "<b>School:</b> this bike trip was primarily to go to or from school or college.");
    purpDescriptions.put(R.id.ToggleWorkRel,
        "<b>Work-Related:</b> this bike trip was primarily to go to or from a business related meeting, function, or work-related errand for your job.");
    purpDescriptions.put(R.id.ToggleExercise,
        "<b>Exercise:</b> this bike trip was primarily for exercise, or biking for the sake of biking.");
    purpDescriptions.put(R.id.ToggleSocial,
        "<b>Social:</b> this bike trip was primarily for going to or from a social activity, e.g. at a friend's house, the park, a restaurant, the movies.");
    purpDescriptions.put(R.id.ToggleShopping,
        "<b>Shopping:</b> this bike trip was primarily to purchase or bring home goods or groceries.");
    purpDescriptions.put(R.id.ToggleErrand,
        "<b>Errand:</b> this bike trip was primarily to attend to personal business such as banking, a doctor  visit, going to the gym, etc.");
    purpDescriptions.put(R.id.ToggleOther,
        "<b>Other:</b> if none of the other reasons applied to this trip, you can enter comments below to tell us more.");

    for (Entry<Integer, ToggleButton> e: purpButtons.entrySet())
      e.getValue().setOnCheckedChangeListener(this);
  } // preparePurposeButtons

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.save);

    final Bundle cmds = getIntent().getExtras();
    final long journeyId = cmds.getLong("showtrip");
    trip_ = TripData.fetchTrip(this, journeyId);

    // Set up trip purpose buttons
    purpose_ = "";
    preparePurposeButtons();

    // Discard btn
    final Button btnDiscard = (Button)findViewById(R.id.ButtonDiscard);
    btnDiscard.setOnClickListener(this);

    // Submit btn
    final Button btnSubmit = (Button)findViewById(R.id.ButtonSubmit);
    btnSubmit.setOnClickListener(this);
    btnSubmit.setEnabled(false);

    // Don't pop up the soft keyboard until user clicks!
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  } // onCreate

  public void onClick(final View v) {
    if(v.getId() == R.id.ButtonDiscard)
      discardTrip();

    if(v.getId() == R.id.ButtonSubmit)
      uploadTrip();
  } // onClick

  private void discardTrip() {
    Toast.makeText(getBaseContext(), "Trip discarded.",	Toast.LENGTH_SHORT).show();

    trip_.dropTrip();

    CycleHackney.start(this);
    finish();
  } // discardTrip

  private void uploadTrip() {
    if (purpose_.equals("")) {
      // Oh no!  No trip purpose!
      Toast.makeText(getBaseContext(), "You must select a trip purpose before submitting! Choose from the purposes above.", Toast.LENGTH_SHORT).show();
      return;
    }

    EditText notes = (EditText)findViewById(R.id.NotesField);

    String fancyStartTime = DateFormat.getInstance().format(trip_.startTime());

    // "3.5 miles in 26 minutes"
    SimpleDateFormat sdf = new SimpleDateFormat("m");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String minutes = sdf.format(trip_.elapsed());
    String fancyEndInfo = String.format("%1.1f miles, %s minutes.  %s",
        trip_.distanceTravelled(),
        minutes,
        notes.getEditableText().toString());

    // Save the trip details to the phone database. W00t!
    trip_.updateTrip(purpose_,
        fancyStartTime,
        fancyEndInfo,
        notes.getEditableText().toString());

    TripDataUploader.upload(this, trip_);

    CycleHackney.start(this);
    finish();
  } // uploadTrip

  @Override
  public void onCheckedChanged(CompoundButton v, boolean isChecked) {
    if (!isChecked)
      return;

    for (Entry<Integer, ToggleButton> e: purpButtons.entrySet())
      e.getValue().setChecked(false);

    v.setChecked(true);
    purpose_ = v.getText().toString();
    ((TextView)findViewById(R.id.TextPurpDescription)).setText(
       Html.fromHtml(purpDescriptions.get(v.getId())));

    final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
    btnSubmit.setEnabled(true);
  } // onCheckedChanged
} // SaveTrip

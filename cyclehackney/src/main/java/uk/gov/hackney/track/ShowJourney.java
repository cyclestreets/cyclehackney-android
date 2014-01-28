package uk.gov.hackney.track;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import net.cyclestreets.views.CycleMapView;

public class ShowJourney extends Activity {
	private CycleMapView mapView_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    mapView_ = new CycleMapView(this, getClass().getName(), false);

    setContentView(mapView_);
  } // onCreate

  @Override
  public void onResume() {
    super.onResume();
    mapView_.onResume();
  } // onResume

  @Override
  public void onPause() {
    super.onPause();
    mapView_.onPause();
  } // onPause
} // ShowJourney

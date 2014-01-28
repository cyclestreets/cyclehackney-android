package uk.gov.hackney.track;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;

import net.cyclestreets.routing.Route;
import net.cyclestreets.routing.Segment;
import net.cyclestreets.routing.Segments;
import net.cyclestreets.views.CycleMapView;

import org.osmdroid.api.IProjection;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.Iterator;

public class ShowJourney extends Activity {
	private CycleMapView mapView_;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    mapView_ = new CycleMapView(this, getClass().getName(), false);

    setContentView(mapView_);

    final Bundle cmds = getIntent().getExtras();
    final long journeyId = cmds.getLong("showtrip");
    final TripData trip = TripData.fetchTrip(this, journeyId);

    mapView_.overlayPushTop(new JourneyOverlay(this, trip));
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

  static private class JourneyOverlay extends Overlay {
    static private int ROUTE_COLOUR = 0x80ff00ff;

    private TripData trip_;
    private final Paint rideBrush_;
    private Path ridePath_;
    private int zoomLevel_ = -1;

    public JourneyOverlay(final Context context, final TripData tripData) {
      super(context);

      trip_ = tripData;

      rideBrush_ = createBrush(ROUTE_COLOUR);
    } // PathOverlay

    private Paint createBrush(int colour) {
      final Paint brush = new Paint();

      brush.setColor(colour);
      brush.setStrokeWidth(2.0f);
      brush.setStyle(Paint.Style.STROKE);
      brush.setStrokeWidth(10.0f);

      return brush;
    } // createBrush

    @Override
    public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
      if (shadow)
        return;

      if(zoomLevel_ != mapView.getZoomLevel() && !mapView.isAnimating()) {
        ridePath_ = null;
        zoomLevel_ = mapView.getProjection().getZoomLevel();
      } // if ...

      if(ridePath_ == null)
        drawSegments(mapView.getProjection());

      canvas.drawPath(ridePath_, rideBrush_);
    } // draw

    private Path newPath() {
      final Path path = new Path();
      path.rewind();
      return path;
    } // newPath

    private void drawSegments(final IProjection projection) {
      ridePath_ = newPath();

      Point screenPoint = new Point();

      boolean first = true;
      for(final GeoPoint gp : trip_.journey()) {
        screenPoint = projection.toPixels(gp, screenPoint);

        if(first) {
          ridePath_.moveTo(screenPoint.x, screenPoint.y);
          first = false;
        } else
          ridePath_.lineTo(screenPoint.x, screenPoint.y);
      } // for ...
    } // drawSegments
  } // JourneyOverlay
} // ShowJourney

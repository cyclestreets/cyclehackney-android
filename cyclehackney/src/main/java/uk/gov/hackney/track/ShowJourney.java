package uk.gov.hackney.track;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

import uk.gov.hackney.R;
import net.cyclestreets.views.CycleMapView;

import org.osmdroid.api.IProjection;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

public class ShowJourney extends Activity {
	private CycleMapView mapView_;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

    setContentView(R.layout.completed_journey);

    mapView_ = new CycleMapView(this, getClass().getName(), false);
    final RelativeLayout v = (RelativeLayout)findViewById(R.id.mapholder);
    v.addView(mapView_,
              new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
                                              RelativeLayout.LayoutParams.FILL_PARENT));

    final Bundle cmds = getIntent().getExtras();
    final long journeyId = cmds.getLong("showtrip");
    final TripData trip = TripData.fetchTrip(this, journeyId);

    setText(R.id.journey_info, trip.info);
    setText(R.id.journey_purpose, trip.purp);
    setText(R.id.journey_start, trip.fancystart);

    // zoomToBoundingBox works better if setZoom first
    mapView_.getController().setZoom(14);
    mapView_.overlayPushTop(new JourneyOverlay(this, mapView_, trip));
  } // onCreate

  private void setText(final int id, final String text) {
    final TextView tv = (TextView)findViewById(id);
    tv.setText(text);
  } // setText

  static private class JourneyOverlay extends Overlay {
    static private int ROUTE_COLOUR = 0x80ff00ff;

    private final CycleMapView mapView_;
    private boolean initial_ = true;

    private final TripData trip_;
    private final Paint rideBrush_;
    private Path ridePath_;
    private int zoomLevel_ = -1;
    private final BitmapDrawable greenWisp_;
    private final BitmapDrawable redWisp_;
    private final Matrix canvasTransform_ = new Matrix();
    private final float[] transformValues_ = new float[9];
    private final Matrix bitmapTransform_ = new Matrix();
    private final Paint bitmapPaint_ = new Paint();

    public JourneyOverlay(final Context context,
                          final CycleMapView mapView,
                          final TripData tripData) {
      super(context);

      mapView_ = mapView;
      trip_ = tripData;

      rideBrush_ = createBrush(ROUTE_COLOUR);

      final Resources res = context.getResources();
      greenWisp_ = (BitmapDrawable)res.getDrawable(R.drawable.greep_wisp);
      redWisp_ = (BitmapDrawable)res.getDrawable(R.drawable.red_wisp);
    } // PathOverlay

    @Override
    public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
      if (shadow)
        return;

      if(zoomLevel_ != mapView.getZoomLevel() && !mapView.isAnimating()) {
        ridePath_ = null;
        zoomLevel_ = mapView.getProjection().getZoomLevel();
      } // if ...

      if(ridePath_ == null)
        ridePath_ = journeyPath(mapView.getProjection());

      canvas.drawPath(ridePath_, rideBrush_);
      drawMarker(canvas, mapView.getProjection(), trip_.startLocation(), greenWisp_);
      drawMarker(canvas, mapView.getProjection(), trip_.endLocation(), redWisp_);

      if (initial_) {
        mapView_.zoomToBoundingBox(trip_.boundingBox());
        initial_ = false;
      } // if ...
    } // draw

    private Path journeyPath(final IProjection projection) {
      Path ridePath = newPath();

      Point screenPoint = new Point();

      boolean first = true;
      for(final GeoPoint gp : trip_.journey()) {
        screenPoint = projection.toPixels(gp, screenPoint);

        if(first) {
          ridePath.moveTo(screenPoint.x, screenPoint.y);
          first = false;
        } else
          ridePath.lineTo(screenPoint.x, screenPoint.y);
      } // for ...

      return ridePath;
    } // drawJourney

    private void drawMarker(final Canvas canvas,
                            final IProjection projection,
                            final GeoPoint location,
                            final BitmapDrawable marker) {
      Point screenPoint = new Point();
      projection.toPixels(location, screenPoint);

      canvas.getMatrix(canvasTransform_);
      canvasTransform_.getValues(transformValues_);

      final int halfWidth = marker.getIntrinsicWidth()/2;
      final int halfHeight = marker.getIntrinsicHeight()/2;
      bitmapTransform_.setTranslate(-halfWidth, -halfHeight);
      bitmapTransform_.postScale(1/transformValues_[Matrix.MSCALE_X], 1/transformValues_[Matrix.MSCALE_Y]);
      bitmapTransform_.postTranslate(screenPoint.x, screenPoint.y);
      canvas.drawBitmap(marker.getBitmap(), bitmapTransform_, bitmapPaint_);
    } // drawMarker

    private Paint createBrush(int colour) {
      final Paint brush = new Paint();

      brush.setColor(colour);
      brush.setStrokeWidth(2.0f);
      brush.setStyle(Paint.Style.STROKE);
      brush.setStrokeWidth(10.0f);

      return brush;
    } // createBrush

    private Path newPath() {
      final Path path = new Path();
      path.rewind();
      return path;
    } // newPath

  } // JourneyOverlay
} // ShowJourney

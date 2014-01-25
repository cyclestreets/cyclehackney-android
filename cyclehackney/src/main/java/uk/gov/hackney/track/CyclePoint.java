package uk.gov.hackney.track;

import org.osmdroid.util.GeoPoint;

public class CyclePoint extends GeoPoint {
	public float accuracy;
	public double altitude;
	public float speed;
	public double time;

  public CyclePoint(int lat, int lgt, double currentTime) {
    super(lat, lgt);
    this.time = currentTime;
  }

  public CyclePoint(int lat, int lgt, double currentTime, float accuracy) {
    super(lat, lgt);
    this.time = currentTime;
    this.accuracy = accuracy;
  }

	public CyclePoint(int lat, int lgt, double currentTime, float accuracy, double altitude, float speed) {
		super(lat, lgt);
		this.time = currentTime;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.speed = speed;
	}
} // class CyclePoint

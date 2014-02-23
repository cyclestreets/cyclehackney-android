package uk.gov.hackney.track;

public interface IRecordService {
	public int getState();
  public boolean hasRiderStopped();

	public TripData startRecording();
	public void cancelRecording();
	public long finishRecording();
	public void reset();

	public void setListener(RecordingActivity ra);
}

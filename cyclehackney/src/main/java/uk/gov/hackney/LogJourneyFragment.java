package uk.gov.hackney;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
public class LogJourneyFragment extends Fragment {
  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_log_journey, container, false);
  } // onCreateView
} // LogJourneyFragment

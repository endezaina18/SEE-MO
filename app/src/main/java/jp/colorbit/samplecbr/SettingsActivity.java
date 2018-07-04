package jp.colorbit.samplecbr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity
	implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	public static final String KEY_RESOLUTION = "resolution";
	public static final String KEY_PRESET = "preset";
	public static final String KEY_COLOR_TYPE = "colorType";
	public static final String KEY_DOWNSCALE = "downscale";

	public static final String EXTRA_CAPTURE_SIZE_LIST = "captureSizeList";
	
	private SettingsFragment fragment;
	private ListPreference resolutionPref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		fragment = new SettingsFragment();
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, fragment).commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		String[] captureSizeList =
				getIntent().getStringArrayExtra(EXTRA_CAPTURE_SIZE_LIST);
		
		resolutionPref = (ListPreference) fragment.findPreference(KEY_RESOLUTION);

		if (captureSizeList != null) {
			String[] values = new String[captureSizeList.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = "" + i;
			}
			resolutionPref.setEntryValues(values);
			resolutionPref.setEntries(captureSizeList);
		}
		
		resolutionPref.setSummary(resolutionPref.getEntry());

		setSummary(KEY_PRESET);
		setSummary(KEY_COLOR_TYPE);
	}
	
	private void setSummary(String key) {
		ListPreference pref = (ListPreference) fragment.findPreference(key);
		if (pref != null) {
			pref.setSummary(pref.getEntry());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (resolutionPref != null) {
			resolutionPref.setSummary(resolutionPref.getEntry());
		}
		setSummary(KEY_PRESET);
		setSummary(KEY_COLOR_TYPE);
	}

	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs);
		}
	}
}

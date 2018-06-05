package com.codeboy.qianghongbao;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

public class SettingActivity extends BaseSettingsActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected boolean isShowBack() {
		return false;
	}


	@Override
	public Fragment getSettingsFragment() {
		MainFragment mMainFragment = new MainFragment();
		return mMainFragment;
	}

	public static class MainFragment extends BaseSettingsFragment {

		private SwitchPreference notificationPref;
		private boolean notificationChangeByUser = true;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.setting);

			//检测外挂开关
			Preference detectPref = findPreference("KEY_ENABLE_DETECT");
			detectPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					SharedPreferences p = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = p.edit();
					if (p.getBoolean("detect", false)){
						editor.putBoolean("detect", false);
						editor.apply();
						return true;
					}
					else{
						editor.putBoolean("detect", true);
						editor.apply();
						return true;
					}
				}
			});



			Preference preference = findPreference("OPEN_ACCESSIBILITY");
			if(preference != null) {
				preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						try {
							Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
							startActivity(intent);
							Toast.makeText(getActivity(), R.string.tips, Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							e.printStackTrace();
						}
						return true;
					}
				});
			}

			ListPreference lspreference = (ListPreference)findPreference("timeListPreference");
			if(lspreference != null) {
				lspreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						SharedPreferences p = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = p.edit();
						editor.putInt("time", Integer.parseInt(((String)newValue)));
						editor.apply();
						return true;
					}
				});
			}
		}


	}
}

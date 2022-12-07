package com.github.bitstuffing.campdf;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    public static final String FILE_PREF_NAME = "prefix";
    public static final String QUALITY = "quality";
    public static final String THEME = "theme";

    public static final String THEME_DAY = "day";
    public static final String THEME_NIGHT = "night";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        actionBar.hide();

    }

    public static class SettingsFragment extends PreferenceFragmentCompat  implements SharedPreferences.OnSharedPreferenceChangeListener{

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Utils.setTheme(sharedPreferences);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            /*Preference preference = (Preference)findPreference("themeListPreference");
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //TODO adapt/migrate Utils.setTheme(sharedPreferences); to simple preference
                    return true;
                }
            });*/

        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Utils.setTheme(sharedPreferences);
        }
    }
}
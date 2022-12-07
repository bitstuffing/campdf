package com.github.bitstuffing.campdf;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.bitstuffing.campdf.fragment.SettingsFragment;

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

    @Override
    protected void onRestart() {
        this.recreate();
        super.onRestart();
    }


}
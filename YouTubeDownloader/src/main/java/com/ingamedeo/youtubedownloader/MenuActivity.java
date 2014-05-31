package com.ingamedeo.youtubedownloader;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class MenuActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Only API 11 > >
        int currentAPIVersion = android.os.Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {

            // RUN THE CODE SPECIFIC TO THE API LEVELS ABOVE HONEYCOMB (API 11+)
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true); //Modify Action Bar. Back Button
        }

        //Legacy Preferences
        addPreferencesFromResource(R.xml.settings);
    }
    public boolean onOptionsItemSelected(MenuItem item) { //Action Bar
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

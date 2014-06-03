package com.ingamedeo.youtubedownloader;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;


public class DonateActivity extends ActionBarActivity {

    //Context
    Context c = this;

    //UI Components Declaration
    TextView tdonate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        //Use Support ActionBar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //Set up UI Components
        tdonate = (TextView) findViewById(R.id.tdonate);
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

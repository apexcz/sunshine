package com.example.odogwu.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private  boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendBroadcast(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        if(findViewById(R.id.weather_detail_container) != null){
            //The detail container view will be present only in the large creen layouts.
            //If this view is present, then the activity should be in two-pane mode
            mTwoPane = true;
            Log.i(TAG,"Two pane mode activated");
            //In two-pane mode, show the detail view in this activty by adding or replacing the detail fragment fragment using a fragment transaction
            if(savedInstanceState == null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailActivityFragment()).commit();
            }
            else {
                mTwoPane = false;
                Log.i(TAG,"One pane mode activated");
            }
            ForecastFragment forecastFragment = ((ForecastFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_forecast));
            forecastFragment.setUseTodayLayout(!mTwoPane);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_map:
                openPreferredLocationInMap();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_default_location));

        Uri geolocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location).build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geolocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    @Override
    public void onItemSelected(String date) {
        if(mTwoPane){
            //In two-pane mode, show the detail view in this activity by adding or replacing the detail
            //fragment using a fragment transaction
            Bundle args = new Bundle();
            args.putString(DetailActivity.DATE_KEY,date);

            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container,fragment)
                    .commit();
        }
        else {
            Intent intent  = new Intent(this,DetailActivity.class)
                    .putExtra(DetailActivity.DATE_KEY,date);
            startActivity(intent);
        }
    }
}

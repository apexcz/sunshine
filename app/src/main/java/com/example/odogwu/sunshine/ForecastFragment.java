package com.example.odogwu.sunshine;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.odogwu.sunshine.data.WeatherContract;
import com.example.odogwu.sunshine.data.WeatherContract.LocationEntry;
import com.example.odogwu.sunshine.data.WeatherContract.WeatherEntry;

import java.util.Date;

/**
 * Created by Odogwu on 4/3/2016.
 */
public class ForecastFragment extends Fragment implements LoaderCallbacks<Cursor> {

    private ForecastAdapter mForecastAdapter;

    private String mLocation;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";

    private static final int FORECAST_LOADER = 0;

    //For the forecast view we are showing only a small subset of the stored date
    //Specify the columns we need
    private static final String[] FORECAST_COLUMNS = {
            //In this case the id needs to be fully qualified with a table name,since
            // the content provider joins the location & weather tables in the background
            // and both have an _id column

            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID
    };

    //These indices are tied to FORECAST_COLUMNS. if the forecast column changes, these must change alongside
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;

    public interface Callback{
//        DetailFragmentCallback for when an item has been selected
        public void onItemSelected(String date);
    }
    public ForecastFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Edu check","Truly entered the forecast fragment");
        //indicates need for callbacks to handle option menus
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_refresh:
                updateWeather();
                return  true;
            default:break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //The SimpleCursorAdapter/ArrayAdapter will take data from the source/db through the loader
        //and use it to populate the ListView it is attached to.
        mForecastAdapter = new ForecastAdapter(
                getActivity(),null,0
        );


        View rootView = inflater.inflate(R.layout.content_main, container, false);

        //Get a reference to the ListView, and attach this adapter to it
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mForecastAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback)getActivity()).onItemSelected(cursor.getString(COL_WEATHER_DATE));

//                    Intent intent = new Intent(getActivity(), DetailActivity.class)
//                            .putExtra(DetailActivity.DATE_KEY, cursor.getString(COL_WEATHER_DATE));
//                    startActivity(intent);
                }
                mPosition = position;
            }
        });

//        If there is instance state, fetch it for useful information.
//        The end goal here is that the user never knows that turning their device sideways does
//          crazy lifecycle related things. It should feel like some stuff stretched out, or magically
//        appeared to take advantage of room, but data or place in the app was never actually lost


        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)){
            //The listview probably hasn't even been populated yet.
            //Actually perform the swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWeather() {
        String location = Utility.getPreferredLocation(getActivity());
        new FetchWeatherTask(getActivity()).execute(location);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null &&
                !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //When tablets rotate, the currently selected list item needs to be saved.
        //When no item is selected, mPosition will be set to ListView.INVALID_POSITION
        //SO check for that before storing
        if (mPosition != ListView.INVALID_POSITION){
            outState.putInt(SELECTED_KEY,mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //This is called when a new Loader needs to be created. This fragment
        //only uses one loader, so we dont care about checking the id.

        //To only show current and future dates,get the string representation for today,
        //and filter the query to return weather only for dates after or including today.
        //only return data after today
        String startDate = WeatherContract.getDbDateString(new Date());

        //Sort order: Ascending by date
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);

        Log.i("Oty Loader","Weather Uri =  "+weatherForLocationUri.toString());
        //Now create and return a cursorloader that will take care of creating a cursor for the data being displayed

        return new CursorLoader(getActivity(), weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i("cursor size", String.valueOf(data.getCount()));
        mForecastAdapter.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION){
            //If we don't need to restart the loader, and there's a desired position to scroll to
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mForecastAdapter != null){
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}

package com.example.odogwu.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.odogwu.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Vector;

/**
 * Created by Odogwu on 5/28/2016.
 */

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String TAG = FetchWeatherTask.class.getSimpleName();

    private final Context mContext;
    private boolean DEBUG = true;

    public FetchWeatherTask(Context context) {
        mContext = context;
    }


    //Helper method to handle insertion of a new location
    private long addLocation(String locationSetting, String cityName, double lat, double lon) {
        Log.v(TAG, "inserting " + cityName + ", with coord: (" + lat + "," + lon + ")");

        //First, check if the location with this city name exists in the db
        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (cursor.moveToFirst()) {
            Log.v(TAG, "Found location item in the database");
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        } else {
            Log.v(TAG, "Didnt find item in db, so do insert now");
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri locationInsertUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locationValues);
            return ContentUris.parseId(locationInsertUri);
        }
    }

    private void getWeatherDataFromJson(String weatherdata, int numDays, String locationSetting) throws JSONException {
        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(weatherdata);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        Log.v(TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

        //Insert the location into the database
        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        //Get and insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            //These are the values that will be collected.

            long dateTime;
            double pressure;
            int humidity;
            double windSpeed, windDirection, high, low;
            int weatherId;

            // For now, using the format "Day, description, hi/low"

            String description;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".

            dateTime = dayForecast.getLong(OWM_DATETIME);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            JSONObject highLowObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = highLowObject.getDouble(OWM_MAX);
            low = highLowObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_ID, locationID);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
        }

        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            int rowsInserted = mContext.getContentResolver()
                    .bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            Log.v(TAG, "Inserted " + rowsInserted + " rows of weather data");

            //Use a DEBUG variable to get whether or not
//                if (DEBUG) {
//                    Cursor weatherCursor = mContext.getContentResolver().query(
//                            WeatherEntry.CONTENT_URI,
//                            null, null, null, null
//
//                    );
//
//                    if (weatherCursor.moveToFirst()) {
//                        ContentValues resultValues = new ContentValues();
//                        DatabaseUtils.cursorRowToContentValues(weatherCursor, resultValues);
//                        Log.v(TAG, "Query successful ********");
//                        for (String key : resultValues.keySet()) {
//                            Log.v(TAG, key + ": " + resultValues.getAsString(key));
//                        }
//
//                    } else {
//                        Log.v(TAG, "Query failed ******");
//                    }
//                }
        }
        //return resultStrs;
    }

    @Override
    protected Void doInBackground(String... params) {

        if(params.length==0){
            return null;
        }

        String locationQuery = params[0];

        //Http connection to open weather map api
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        //contains the fetched data as a string
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 7;
        final String apiKey = "2bdffdfca36622c7bb66509fc2a5f10e";

        try{
            //Construct the url to openweathermap api
            final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String API_PARAM = "APPID";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM,params[0])
                    .appendQueryParameter(FORMAT_PARAM,format)
                    .appendQueryParameter(UNITS_PARAM,units)
                    .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
                    .appendQueryParameter(API_PARAM,apiKey)
                    .build();

            //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&APPID=2bdffdfca36622c7bb66509fc2a5f10e");
            URL url = new URL(builtUri.toString());

            //create the request to the api and open connection
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //read the input stream into a string
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if(inputStream == null){
                //nothing to do
                return  null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null){
                //Since it is json, adding a newline isnt necessary (it wont affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line+"\n");
            }

            if(buffer.length()==0){
                //stream was empty,no point in parsing
                return null;
            }

            forecastJsonStr = buffer.toString();
            Log.i(TAG, "Result = " + forecastJsonStr);
        }
        catch (IOException ex){
            Log.e(TAG, "Error", ex);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        }
        finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(reader != null){
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(TAG,"Error closing stream", e);
                }
            }
        }

        try{
            getWeatherDataFromJson(forecastJsonStr, numDays, locationQuery);
        }
        catch (JSONException ex){
            Log.e(TAG, ex.getMessage(),ex);
            ex.printStackTrace();
        }
        return null;
    }

}

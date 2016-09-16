package com.example.odogwu.sunshine;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.odogwu.sunshine.data.WeatherContract;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Odogwu on 9/15/2016.
 */
public class WeatherWidget extends AppWidgetProvider {
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            // This works because the WeatherProvider returns location data joined with
            // weather data, even though they're stored in two different tables.
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    //These indices are tied to FORECAST_COLUMNS. if the forecast column changes, these must change alongside
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName watchWidget = new ComponentName(context, WeatherWidget.class);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_face);
        remoteViews.setTextViewText(R.id.high_temp, "Na");
        remoteViews.setTextViewText(R.id.low_temp, "Na");

        Intent intent = new Intent(context, WeatherWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.high_temp, pendingIntent);
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_face);
        ComponentName watchWidget = new ComponentName(context, WeatherWidget.class);

        String mLocation = Utility.getPreferredLocation(context);
        //Sort order: Ascending by date
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String todayDate = sdf.format(System.currentTimeMillis());
        boolean isMetric = Utility.isMetric(context);

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(mLocation, todayDate);
//        Log.i("Chexck", weatherForLocationUri.toString());

        //Now create and return a cursorLoader that will take care of creating a
        // cursor for the data being displayed

//         new CursorLoader(context,weatherForLocationUri,FORECAST_COLUMNS,null,null,sortOrder);
        Cursor cursor = context.getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
        while (cursor.moveToNext()) {
            remoteViews.setTextViewText(R.id.location,mLocation);
            remoteViews.setTextViewText(R.id.high_temp, Utility.formatTemperature(context,cursor.getDouble(COL_WEATHER_MAX_TEMP),isMetric));
            remoteViews.setTextViewText(R.id.low_temp, Utility.formatTemperature(context,cursor.getDouble(COL_WEATHER_MIN_TEMP),isMetric));
            remoteViews.setTextViewText(R.id.weather_condition,cursor.getString(COL_WEATHER_DESC));
            remoteViews.setImageViewResource(R.id.weather_image, Utility.getArtResourceForWeatherCondition(cursor.getInt(COL_WEATHER_CONDITION_ID)));
            Log.i("Check", cursor.getDouble(COL_WEATHER_CONDITION_ID) + "");
        }


        // Apply the changes
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }
}

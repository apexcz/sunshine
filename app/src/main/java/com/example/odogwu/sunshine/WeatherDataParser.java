package com.example.odogwu.sunshine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Odogwu on 4/4/2016.
 */
public class WeatherDataParser {

    public static double getMaxTemparatureForDay(String weatherJson,int dayIndex){

            JSONObject weather = null;
            try {
                weather = new JSONObject(weatherJson);
                JSONArray days = weather.getJSONArray("list");
                JSONObject dayInfo = days.getJSONObject(dayIndex);
                JSONObject tempInfo = dayInfo.getJSONObject("temp");
                return tempInfo.getDouble("max");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        return -1;
    }
}

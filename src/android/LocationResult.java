package org.apache.cordova.geolocation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by rinatmuhamedgaliev on 9/8/16.
 */
public class LocationResult {
    private double latitude;
    private double longitude;

    public LocationResult(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String toJson() {
        JSONObject json = new JSONObject();

        try {
            json.put("lat", getLatitude());
            json.put("lon", getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return(json.toString());
    }
}

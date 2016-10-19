/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at
         http://www.apache.org/licenses/LICENSE-2.0
       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */


package org.apache.cordova.geolocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Geolocation extends CordovaPlugin implements LocationListener {

    String TAG = "GeolocationPlugin";
    CallbackContext context;

    LocationManager locationManager = null;
    LocationListener locationListener = this;
    Handler locationTimeoutHandler = null;
    Runnable locationTimeoutTask = null;

    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "We are entering execute");
        context = callbackContext;

        // Default timeout 1 min.
        long locationTimeout = 60000;

        if (!args.isNull(0)) {
            locationTimeout = ((JSONObject) args.get(0)).getLong("timeout");
        }

        if (action.equals("getPermission")) {
            if (hasPermissions()) {
                PluginResult r = new PluginResult(PluginResult.Status.OK);
                context.sendPluginResult(r);
                return true;
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
            return true;
        } else if (action.equals("getCurrentPosition")) {
            if (hasPermissions()) {
                final Context applicationContext = cordova.getActivity().getApplicationContext();
                final Looper locationLooper = Looper.myLooper();
                locationTimeoutHandler = new Handler(locationLooper);
                locationTimeoutTask = new Runnable() {

                    public void run() {
                        onChangeLocationResult(null);
                    }

                };

                locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);

                boolean isGPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (isGPSProviderEnabled || isNetworkProviderEnabled) {
                    if (Build.VERSION.SDK_INT >= 23 &&
                            applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        PluginResult errorResult = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                        context.sendPluginResult(errorResult);
                        return false;
                    } else if (!hasPermissions()) {
                        PluginResult errorResult = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                        context.sendPluginResult(errorResult);
                        return false;
                    }

                    if(isGPSProviderEnabled){
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, locationLooper);
                    }

                    if(isNetworkProviderEnabled){
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, locationLooper);
                    }

                    locationTimeoutHandler.postDelayed(locationTimeoutTask, locationTimeout);
                } else {
                    PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 2);
                    context.sendPluginResult(r);
                    return false;
                }
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
            return true;
        }
        return false;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if (context != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    LOG.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    context.sendPluginResult(result);
                    return;
                }

            }
            result = new PluginResult(PluginResult.Status.OK);
            context.sendPluginResult(result);
        }
    }

    public boolean hasPermissions() {
        for (String p : permissions) {
            if (!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    /*
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     */
    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }

    public void onChangeLocationResult(Location location){
        if(locationManager != null){
            // FIXME
            locationManager.removeUpdates(locationListener);
            locationManager = null;
        }

        if(locationTimeoutHandler != null && locationTimeoutTask != null){
            locationTimeoutHandler.removeCallbacks(locationTimeoutTask);
            locationTimeoutHandler = null;
            locationTimeoutTask = null;
        }

        if(location != null){
            org.apache.cordova.geolocation.LocationResult locationResult = new org.apache.cordova.geolocation.LocationResult(location.getLatitude(), location.getLongitude());
            PluginResult r = new PluginResult(PluginResult.Status.OK, "" + locationResult.toJson());

            context.sendPluginResult(r);
        }else{
            PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 3);
            context.sendPluginResult(r);
        }
    }

    @Override
    public void onLocationChanged(Location location){
        LOG.d(TAG, "We are entering execute");

        onChangeLocationResult(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){
        LOG.d(TAG, "onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider){
        LOG.d(TAG, "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider){
        LOG.d(TAG, "onProviderDisabled");
    }

}

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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Geolocation extends CordovaPlugin {

    String TAG = "GeolocationPlugin";
    CallbackContext context;

    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    private Criteria criteria;
    private String provider;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "We are entering execute");
        context = callbackContext;


        criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        int timeOutMinute = 0;

        if (!args.isNull(0)) {

            double timeMilis = ((JSONObject) args.get(0)).getDouble("timeout");

            if (timeMilis > 0) {
                timeOutMinute = (int) ((timeMilis / 1000) / 60);
            }
        }

        timeOutMinute = timeOutMinute <= 0 ? 1 : timeOutMinute;

        if (action.equals("getPermission")) {
            if (hasPermisssion()) {
                PluginResult r = new PluginResult(PluginResult.Status.OK);
                context.sendPluginResult(r);
                return true;
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
            return true;
        } else if (action.equals("getCurrentPosition")) {
            if (hasPermisssion()) {
                if (isGPSEnabled() || isNetworkPositionEnabled()) {

                    Context geoContext = this.cordova.getActivity().getApplicationContext();

                    final LocationManager[] mLocationManager = {(LocationManager) geoContext.getSystemService(Context.LOCATION_SERVICE)};

                    List<String> providers = mLocationManager[0].getProviders(criteria, true);

                    final LocationListener mLocationListener = new LocationListener() {

                        @Override
                        public void onLocationChanged(Location location) {
                            LOG.d(TAG, "We are entering execute");
                            LocationResult locationResult = new LocationResult(location.getLatitude(), location.getLongitude());
                            PluginResult r = new PluginResult(PluginResult.Status.OK, "" + locationResult.toJson());

                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                            }
                            mLocationManager[0].removeUpdates(this);
                            mLocationManager[0] = null;

                            context.sendPluginResult(r);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            LOG.d(TAG, "onStatusChanged");
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            LOG.d(TAG, "onProviderEnabled");
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            LOG.d(TAG, "onProviderDisabled");
                        }
                    };

                    if (
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        PermissionHelper.requestPermissions(this, 0, permissions);
                    }

                    provider = mLocationManager[0].getBestProvider(criteria, true);

                    if (provider != null) {
                        if (provider.equals(LocationManager.GPS_PROVIDER)) {
                            mLocationManager[0].requestLocationUpdates(LocationManager.GPS_PROVIDER, 60,
                                    100, mLocationListener);
                        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                            mLocationManager[0].requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60,
                                    100, mLocationListener);
                        }  else if (provider.equals(LocationManager.PASSIVE_PROVIDER)) {
                            mLocationManager[0].requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 60,
                                    100, mLocationListener);
                        } else {
                            mLocationManager[0].requestLocationUpdates(LocationManager.GPS_PROVIDER, 60,
                                    100, mLocationListener);
                        }
                    } else {
                        PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                        context.sendPluginResult(r);
                    }

                } else {
                    PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 2);
                    context.sendPluginResult(r);
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

    public boolean hasPermisssion() {
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

    public boolean isGPSEnabled() {
        Context context = this.cordova.getActivity().getApplicationContext();
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isNetworkPositionEnabled() {
        Context context = this.cordova.getActivity().getApplicationContext();
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        } else {
            return true;
        }
    }

    private int checkSelfPermission(String accessFineLocation) {
        return hasPermisssion() ? 0: 1;
    }
}

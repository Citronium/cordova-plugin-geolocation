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

public class Geolocation extends CordovaPlugin {

    private final String TAG = "GeolocationPlugin";
    private final String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private final Context appContext = this.cordova.getActivity().getApplicationContext();

    private CallbackContext context;
    private Criteria criteria;
    private String provider;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "We are entering execute");
        this.context = callbackContext;
        this.criteria = new Criteria();
        final LocationManager[] mLocationManager = {(LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE)};

        long timeout = getTimeout(args);
        if (checkHighAccuracy(args)) {
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
        }

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

                    if (Build.VERSION.SDK_INT >= 23 &&
                            appContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            appContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        PluginResult errorResult = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                        context.sendPluginResult(errorResult);
                        return false;
                    } else {
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            PluginResult errorResult = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                            context.sendPluginResult(errorResult);
                            return false;
                        }
                    }

                    final LocationListener mLocationListener = createLocationListener(mLocationManager[0]);
                    provider = mLocationManager[0].getBestProvider(criteria, true);

                    Looper myLooper = Looper.myLooper();
                    if (provider != null) {
                        if (provider.equals(LocationManager.GPS_PROVIDER)) {
                            mLocationManager[0].requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, myLooper);
                        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                            mLocationManager[0].requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, myLooper);
                        } else if (provider.equals(LocationManager.PASSIVE_PROVIDER)) {
                            mLocationManager[0].requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, mLocationListener, myLooper);
                        } else {
                            mLocationManager[0].requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, myLooper);
                        }
                        final Handler myHandler = new Handler(myLooper);
                        myHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                mLocationManager[0].removeUpdates(mLocationListener);
                                PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 3);
                                context.sendPluginResult(r);
                            }
                        }, timeout);

                    } else {
                        PluginResult r = new PluginResult(PluginResult.Status.CLASS_NOT_FOUND_EXCEPTION, 1);
                        context.sendPluginResult(r);
                        return false;
                    }
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
        final LocationManager manager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isNetworkPositionEnabled() {
        final LocationManager manager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        } else {
            return true;
        }
    }

    private int checkSelfPermission(String accessFineLocation) {
        return hasPermisssion() ? 0 : 1;
    }

    /**
     * @param args
     * @return
     */
    private boolean checkHighAccuracy(JSONArray args) {
        boolean enableHighAccuracy = false;

        try {
            if (!args.isNull(0) && !((JSONObject) args.get(0)).isNull("enableHighAccuracy")) {
                enableHighAccuracy = ((JSONObject) args.get(0)).getBoolean("enableHighAccuracy");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return enableHighAccuracy;
    }

    /**
     * @param args
     * @return
     */
    private long getTimeout(JSONArray args) {
        long timeout = 0;
        try {
            if (!args.isNull(0) && !((JSONObject) args.get(0)).isNull("timeout")) {
                timeout = ((JSONObject) args.get(0)).getLong("timeout");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return timeout;
    }

    /**
     * @param mLocationManager
     * @return
     */
    private LocationListener createLocationListener(final LocationManager mLocationManager) {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LOG.d(TAG, "Start location detect");

                PluginResult r = new PluginResult(PluginResult.Status.OK, convertLocationToJson(location));

                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                }
                mLocationManager.removeUpdates(this);
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
    }

    private String convertLocationToJson(Location location) {
        org.apache.cordova.geolocation.LocationResult locationResult = new org.apache.cordova.geolocation.LocationResult(location.getLatitude(), location.getLongitude());
        return locationResult.toJson();
    }
}

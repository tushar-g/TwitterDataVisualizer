package com.android.tusharg.twitterdatavisualizer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import twitter4j.FilterQuery;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StallWarning;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_LOCATION_CHECK_SETTINGS = 0x1;
    private static final String LOG_TAG = "MainActivity";
    private static final long TIMER_INTERVAL = 20000; // Every 20 seconds

    GoogleMap mGoogleMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLocation;

    ProgressDialog progress;

    boolean locationAvailable;
    boolean mapIsReady;
    private boolean markersAddedOnMap;
    private volatile boolean isBusy;
    long sinceId = -1l;

    LinkedHashMap<String, StatusWithMarker> data = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupProgressDialog();
        getMap();
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart called");
        mGoogleApiClient.connect();
        super.onStart();
    }


    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause called");
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop called");
        mGoogleApiClient.disconnect();
        handler.removeCallbacks(runnable);
        if (mGoogleMap != null)
            mGoogleMap.clear();
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        super.onStop();
    }

    private void setupProgressDialog() {
        progress = new ProgressDialog(this);
        progress.setMessage("Searching for your Location...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();
    }

    private void getMap() {
        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.map_container);
        map.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
                mapIsReady = true;
                Log.d(LOG_TAG, "Going to call ShowMap from getMap");
                showMap();
            }
        });
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder lsr_builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                lsr_builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                }
            }
        });
    }

    /**
     * Dismiss the progress dialog if user chooses not to change the settings.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", Integer.toString(resultCode));
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Toast.makeText(MainActivity.this, "Location enabled!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        progress.dismiss();
                        Toast.makeText(MainActivity.this, "Location Not enabled!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default:
                        break;
                }
                break;
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void showMap() {
        if (mapIsReady & locationAvailable) {
            progress.dismiss();
            mGoogleMap.setMyLocationEnabled(true);
            LatLng objLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(objLatLng, 20));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(13), 2000, null);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleMap.addMarker(new MarkerOptions()
                    .position(objLatLng)
                    .title("This is Me"));
            fetchTweets();

        }
    }

    private final Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!isNetworkAvailable()) {
                Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            } else if (!isBusy) {
                new GetTweetsTask().execute(sinceId);
            }
        }
    };

    private Timer timer;
    private TimerTask tt = new TimerTask() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Vaue of isBusy: " + isBusy);
            handler.post(runnable);
        }
    };

    private void fetchTweets() {
        timer = new Timer();
        timer.schedule(tt, 0, TIMER_INTERVAL);
    }

    /*
    ===================================================

    Methods to be implemented for GoogleApiClient.ConnectionCallbacks interface

    ===================================================
    */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
    }

    private void getLocation() {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d(LOG_TAG, "Location found : " + mLocation.toString());
            locationAvailable = true;
            Log.d(LOG_TAG, "Going to call ShowMap from getLocation");
            showMap();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "Connection was Suspended, Trying to connect again...");
        mGoogleApiClient.connect();
    }

    /*
    ===================================================

    Methods to be implemented for GoogleApiClient.OnConnectionFailedListener interface

    ===================================================
    */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "Could not connect to GoogleApiClient");
        progress.dismiss();
    }

    /*
    ===================================================

    Methods to be implemented for LocationListener interface

    ===================================================
    */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLocation = location;
            Log.d(LOG_TAG, "Location found : " + location.toString());
            locationAvailable = true;
            Log.d(LOG_TAG, "Going to call ShowMap from onLocationChanged");
            if (!markersAddedOnMap) {
                showMap();
            }
        } else {
            progress.dismiss();
            Log.d(LOG_TAG, "returned location was null");
        }
    }

    private class GetTweetsTask extends AsyncTask<Long, Void, ArrayList<twitter4j.Status>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isBusy = true;
        }

        @Override
        protected ArrayList<twitter4j.Status> doInBackground(Long... params) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setApplicationOnlyAuthEnabled(true);
            cb.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY)
                    .setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET)
                    .setOAuthAccessToken(Constants.TWITTER_ACCES_TOKEN)
                    .setOAuthAccessTokenSecret(Constants.TWITTER_ACCES_TOKEN_SECRET);

            Twitter twitter = null;
            try {
                twitter = new TwitterFactory(cb.build()).getInstance();
                twitter.getOAuth2Token();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            ArrayList<twitter4j.Status> tweets = null;
            try {
                GeoLocation geo = new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude());
                Query query = new Query();
                query.setGeoCode(geo, 1, Query.Unit.mi);
                query.setCount(100);
                query.setSinceId(params[0]);
                QueryResult result = twitter.search(query);
                tweets = (ArrayList<twitter4j.Status>) result.getTweets();
            } catch (TwitterException te) {
                te.printStackTrace();
            }
            return tweets;
        }

        @Override
        protected void onPostExecute(ArrayList<twitter4j.Status> statuses) {
            super.onPostExecute(statuses);
            if (statuses != null) {
                Log.d(LOG_TAG, "No of tweets : " + statuses.size());
                int size = statuses.size();
                for (int i = size - 1; i > -1; i--) {
                    twitter4j.Status s = statuses.get(i);
                    if (s.getGeoLocation() != null) {
                        if (data.containsKey(s.getGeoLocation().toString())) {
                            data.remove(s.getGeoLocation().toString()).mMarker.remove();
                        }
                        Marker m = mGoogleMap.addMarker(getNewMarker(s, s.getGeoLocation()));
                        data.put(s.getGeoLocation().toString(), new StatusWithMarker(s, m));

                        if (data.size() > 100) {
                            Log.d(LOG_TAG, "Data size is more than 100, deleting last 10");
                            Object[] o = data.keySet().toArray();
                            for (int j = o.length - 1; j >= o.length - 10; j--) {
                                data.remove((String) o[j]).mMarker.remove();
                            }
                        }
                    }
                }
                Log.d(LOG_TAG, "Data size is : " + data.size());

                markersAddedOnMap = true;
                if (statuses.size() != 0) {
                    sinceId = statuses.get(0).getId();
                }
                Log.d(LOG_TAG, "SinceId is : " + sinceId);
            }
            isBusy = false;
        }
    }

    private void callStreamAPI() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);
        cb.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(Constants.TWITTER_ACCES_TOKEN)
                .setOAuthAccessTokenSecret(Constants.TWITTER_ACCES_TOKEN_SECRET);

//        AccessToken at = new AccessToken(Constants.TWITTER_ACCES_TOKEN, Constants.TWITTER_ACCES_TOKEN_SECRET);

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(twitter4j.Status status) {
                Log.d(LOG_TAG, "Tweet received : " + status.toString());
                updateMap(status);
            }

            @Override
            public void onException(Exception ex) {
                Log.d(LOG_TAG, "onException" + ex.toString());
                ex.printStackTrace();
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {
                Log.d(LOG_TAG, "onDeletionNotice");

            }

            @Override
            public void onScrubGeo(long arg0, long arg1) {
                Log.d(LOG_TAG, "onScrubGeo");


            }

            @Override
            public void onStallWarning(StallWarning arg0) {
                Log.d(LOG_TAG, "onStallWarning");

            }

            @Override
            public void onTrackLimitationNotice(int arg0) {
                Log.d(LOG_TAG, "onTrackLimitationNotice");

            }

        };
        twitterStream.addListener(listener);
        FilterQuery filterQuery = new FilterQuery();
        double lat = mLocation.getLatitude();
        double lon = mLocation.getLongitude();
        filterQuery.locations(new double[][]{{lat - 1, lon - 1}, {lat + 1, lon + 1}});
        twitterStream.filter(filterQuery);
    }

    private void updateMap(twitter4j.Status status) {
        GeoLocation gl = status.getGeoLocation();
        //Check if geo is not null
        if (gl != null) {
            //Check if the get inside bounding box is less than 1 mile from current location
            if (DistanceCalculator.get(mLocation.getLatitude(), mLocation.getLongitude(), gl.getLatitude(), gl.getLongitude()) <= 1) {
                Log.d(LOG_TAG, "Tweet : " + status.toString());
                addMarkerToMap(status, gl);
            }
        } else {
            if (status.getRetweetedStatus() != null) {
                gl = status.getRetweetedStatus().getGeoLocation();
                if (gl != null) {
                    if (DistanceCalculator.get(mLocation.getLatitude(), mLocation.getLongitude(), gl.getLatitude(), gl.getLongitude()) <= 1) {
                        Log.d(LOG_TAG, "Re-Tweet : " + status.toString());
                        addMarkerToMap(status, gl);
                    }
                }
            }
        }
    }

    private void addMarkerToMap(twitter4j.Status status, GeoLocation gl) {
        LatLng ll = new LatLng(gl.getLatitude(), gl.getLongitude());
        mGoogleMap.addMarker(new MarkerOptions()
                .position(ll)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title(status.getUser().getScreenName() + ": " + status.getText()));
    }

    private MarkerOptions getNewMarker(twitter4j.Status status, GeoLocation gl) {
        LatLng ll = new LatLng(gl.getLatitude(), gl.getLongitude());
        MarkerOptions m = new MarkerOptions()
                .position(ll)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title(status.getUser().getScreenName() + ": " + status.getText());
        return m;

    }

    public boolean isNetworkAvailable() {
        Context context = MainActivity.this;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}

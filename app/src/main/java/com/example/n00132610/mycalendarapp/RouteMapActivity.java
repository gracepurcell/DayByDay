package com.example.n00132610.mycalendarapp;


import android.Manifest;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class RouteMapActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Serializable,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DATE_EXTRA = "date";
    private static final int NOTES_LOADER = 0;
    private final String TAG = "MapsApp";
    private SimpleCursorAdapter cursorAdapter;

    GoogleMap mMap;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleApiClient mLocationClient;
    private Marker marker;
    private static final double
            CITY_LAT = 53.3478,
            CITY_LNG = -6.2597;

    Circle shape;
    public String lat;
    public String lng;
    private Date dt;
    private String dateString;
    ArrayList<LatLng> markerPoints;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);


        /** Adding it so it passes in the date that you are currently clicked on as an extra. */
        dt = (Date) getIntent().getSerializableExtra(DATE_EXTRA);
        dateString = new SimpleDateFormat("yyyy-MM-dd").format(dt);

        String[] markerPoints = new String[]{DBOpenHelper.NOTE_LOCATION};
        int[] to = {R.id.map};

        cursorAdapter = new SimpleCursorAdapter(this,
                R.layout.activity_route_map, null, markerPoints, to, 0);

        getLoaderManager().initLoader(NOTES_LOADER, null, this);

        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // check if enabled and if not send user to the GSP settings

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        if (servicesOK()) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (initMap()) {

                gotoLocation(CITY_LAT, CITY_LNG, 12);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);

                mLocationClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();

                mLocationClient.connect();


            } else {
                Toast.makeText(this, "Map not connected!", Toast.LENGTH_SHORT).show();
            }

        } else {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        }
    }

    private void gotoLocation(double lat, double lng, float zoom) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.moveCamera(update);
    }

    public boolean initMap() {
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment)
                    getSupportFragmentManager().findFragmentById(R.id.map);
            mMap = mapFragment.getMap();
        }

        if (mMap != null) {
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.info_window, null);
                    TextView tvLocality = (TextView) v.findViewById(R.id.tvLocality);
                    TextView tvLat = (TextView) v.findViewById(R.id.tvLat);
                    TextView tvLng = (TextView) v.findViewById(R.id.tvLng);
                    TextView tvSnippet = (TextView) v.findViewById(R.id.tvSnippet);

                    LatLng latLng = marker.getPosition();
                    tvLocality.setText(marker.getTitle());
                    tvLat.setText("Latitude: " + latLng.latitude);
                    tvLng.setText("Longitude: " + latLng.longitude);
                    tvSnippet.setText(marker.getSnippet());

                    return v;
                }
            });

            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    Geocoder gc = new Geocoder(RouteMapActivity.this);
                    List<Address> list = null;

                    try {
                        list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String msg = marker.getTitle() + " (" +
                            marker.getPosition().latitude + ", " +
                            marker.getPosition().longitude + ")";
                    Toast.makeText(RouteMapActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Geocoder gc = new Geocoder(RouteMapActivity.this);
                List<Address> list = null;
                LatLng ll = marker.getPosition();
                try {
                    list = gc.getFromLocation(ll.latitude, ll.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                Address add = list.get(0);
                marker.setTitle(add.getLocality());
                marker.setSnippet(add.getCountryName());
                marker.showInfoWindow();
            }
        });

        return (mMap != null);
    }

    public boolean servicesOK() {

        // this is my error message
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to mapping service", Toast.LENGTH_SHORT).show();
        }

        return false;
    }


    public void addMarker(Address add, double lat, double lng) {

        if (marker != null) {
            removeEverything();
        }


        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions options = new MarkerOptions()
                .title(add.getLocality())
                .position(latLng)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

        String country = add.getCountryName();
        if (country.length() > 0) {
            options.snippet(country);
        }

        marker = mMap.addMarker(options);

        CircleOptions circleOptions = new CircleOptions()
                .strokeColor(Color.BLUE)
                .strokeWidth(3)
                .fillColor(0x330000FF)
                .center(latLng)
                .radius(50);
        shape = mMap.addCircle(circleOptions);
    }

    private void removeEverything() {

        marker.remove();
        marker = null;

        shape.remove();
        shape = null;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }


    /** Adding a cursor loader so it can pass a SQL statement to the database and get the LAtLng as a return*/
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        markerPoints = new ArrayList<LatLng>();

        switch (id) {
            case NOTES_LOADER: {
                return new CursorLoader(
                        this,                                           // Parent activity context
                        NotesProvider.CONTENT_URI,                      // Table to query
                        null,                                           // Projection to return
                        DBOpenHelper.NOTE_DATE + " = '" + dateString + "'",    // No selection clause
                        null,                                           // No selection arguments
                        DBOpenHelper.NOTE_TIME +  " ASC"                                            // Default sort order; or DBOpenHelper.NOTE_CREATED + " DESC"
                );
            }

            default: {
                return null;
            }
        }
    }

    /** When the data gets passed back set up an array list and store the Lat Lng data into a string.*/
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        ArrayList<LatLng> points = new ArrayList<LatLng>();
        PolylineOptions lineOptions;

        try {
            int columnIndex = data.getColumnIndex(DBOpenHelper.NOTE_LOCATION);
            while (data.moveToNext()) {
                String latLongStr = data.getString(columnIndex);
                /** Getting the data as a whole and braking it down so it can be used.*/
                StringTokenizer tokens = new StringTokenizer(latLongStr, ",");
                String latString = tokens.nextToken();
                String longString = tokens.nextToken();
                double lat = Double.parseDouble(latString);
                double lng = Double.parseDouble(longString);
                /** Adding the data to the points array everytime there is a point to be added.*/
                points.add(new LatLng(lat, lng));
            }

            /** For every point there is add a new marker on it so the user can view the points that they have inputted*/
            for (int j = 0; j < points.size(); j++) {

                MarkerOptions options = new MarkerOptions();
                options.position(points.get(j));
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);
            }

            /** Passing the data so the JSON being sent to google will know the origin and the Destination + waypoints */
            if (points.size() >= 2) {
                LatLng origin = points.get(0);
                LatLng dest = points.get(1);

                String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

                /** Destination of route */
                String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

                String sensor = "sensor=false";

                String waypoints = "";
                for (int i = 2; i < points.size(); i++) {
                    LatLng point = (LatLng) points.get(i);
                    if (i == 2)
                        waypoints = "waypoints=";
                    waypoints += point.latitude + "," + point.longitude + "|";
                }

                /** Building the parameters to the web service */
                String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + waypoints;

                /** Output format */
                String output = "json";

                /** Building the url to the web service */
                String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

                DownloadTask downloadTask = new DownloadTask();

                /** Start downloading json data from Google Directions API */
                downloadTask.execute(url);
            }
        }
        catch (Exception e) {
            Log.d("CURSOR", e.getMessage());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        /** Downloading data in non-ui thread */
        @Override
        protected String doInBackground(String... url) {

            /** For storing data from web service */

            String data = "";

            try {
                /** Fetching the data from web service */
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        /** Executes in UI thread, after the execution of doInBackground() */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            /** Invokes the thread for parsing the JSON data */
            parserTask.execute(result);
        }

        private String downloadUrl(String strUrl) throws IOException {
            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);

                /** Creating an http connection to communicate with url */
                urlConnection = (HttpURLConnection) url.openConnection();

                /** Connecting to url */
                urlConnection.connect();

                /** Reading data from url */
                iStream = urlConnection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                data = sb.toString();

                br.close();

            } catch (Exception e) {
                Log.d("ROUTE_MAP_ACTIVITY", e.toString());
            } finally {
                iStream.close();
                urlConnection.disconnect();
            }
            return data;
        }
    }

    /**
     * Parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        /** Parsing the data in non-ui thread */
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                /** Starts parsing data */
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        /** Executes in UI thread, after the parsing process */
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            /** Traversing through all the routes */
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                /** Fetching i-th route */
                List<HashMap<String, String>> path = result.get(i);

                /** Fetching all the points in i-th route */
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                /** Adding all the points in the route to LineOptions */
                lineOptions.addAll(points);
                lineOptions.width(15);
                lineOptions.color(Color.parseColor("#FF009688"));
            }

            /** Drawing polyline in the Google Map for the i-th route */
            mMap.addPolyline(lineOptions);
        }
    }


}

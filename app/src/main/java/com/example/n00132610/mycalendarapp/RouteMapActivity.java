package com.example.n00132610.mycalendarapp;


import android.app.Dialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
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

import org.json.JSONArray;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RouteMapActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Serializable,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DATE_EXTRA = "date";
    private static final int NOTES_LOADER = 0;
    private static final int NOTES_LOCATION = 1;
    private final String TAG = "MapsApp";

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
    String location = lat + "," + lng;
    private Date dt;
    private String dateString;
    ArrayList<LatLng> markerPoints;
    private Loader<Cursor> fromCursorToArrayListString;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        dt = (Date) getIntent().getSerializableExtra(DATE_EXTRA);
        dateString = new SimpleDateFormat("yyyy-MM-dd").format(dt);

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

                    Address add = list.get(0);
                    RouteMapActivity.this.addMarker(add, latLng.latitude, latLng.longitude);
                    lat = String.valueOf(marker.getPosition().latitude);
                    lng = String.valueOf(marker.getPosition().longitude);
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

    ArrayList<LatLng> fromCursorToArrayListString(Cursor c){
        ArrayList markerPoints = new ArrayList<>();
        c.moveToFirst();
        for(int i = 0; i < c.getCount(); i++){
            String row = c.getString(c.getColumnIndex(DBOpenHelper.NOTE_LOCATION));
            markerPoints.toArray(new String[]{row});
            c.moveToNext();
            Log.i("result ", "Result : " + markerPoints.toString());
        }
        return markerPoints;
    }

//    public ArrayList<LatLng> getAllStringValues() {
//        ArrayList<String> yourStringValues = new ArrayList<String>();
//        Cursor result = DBOpenHelper.query(true, notes.db
//                new String[] { YOUR_COLUMN_NAME }, null, null, null, null,
//                null, null);
//
//        if (result.moveToFirst()) {
//            do {
//                yourStringValues.add(result.getString(result
//                        .getColumnIndex(YOUR_COLUMN_NAME)));
//            } while (result.moveToNext());
//        } else {
//            return null;
//        }
//        return YOUR_COLUMN_NAME;
//    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        markerPoints = new ArrayList<LatLng>();

        switch (id) {
            case NOTES_LOADER: {
                return new CursorLoader(
                        this,                                           // Parent activity context
                        NotesProvider.CONTENT_URI,                      // Table to query
                        null,                                           // Projection to return
                        DBOpenHelper.NOTE_DATE + " = " + dateString,    // No selection clause
                        null,                                           // No selection arguments
                        null                                            // Default sort order
                );
            }

            case NOTES_LOCATION: {
                return new CursorLoader(
                        this,
                        NotesProvider.CONTENT_URI,
                        null,
                        DBOpenHelper.NOTE_LOCATION,
                        null,
                        null
                );
            }

            default: {
                // An invalid id was passed in
                return fromCursorToArrayListString;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}

package com.example.n00132610.mycalendarapp;


import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class MapActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Serializable {

    public static final String LATITUDE_EXTRA = "latitude";
    public static final String LONGITUDE_EXTRA = "longitude";
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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** Setting the view of the intent to the map */
        setContentView(R.layout.activity_map);

        /** Adding the location manager so you can tell where you are */
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        /** check if enabled and if not send user to the GSP settings*/

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        /** Getting reference to Button */
        Button btnDraw = (Button) findViewById(R.id.btn_draw);

        /** If the services can connect and the location can be enabled start the map */
        if (servicesOK()) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (initMap()) {

                /** Default location for the map to start at*/
                gotoLocation(CITY_LAT, CITY_LNG, 12);

                /** Get the location you are currently positioned at */
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

        /** Drawing the button that you can see. Click the button to start a new intent and pass the values for it */
        btnDraw.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Checks, whether location is captured
                Intent intent = MapActivity.this.getIntent();
                intent.putExtra(LATITUDE_EXTRA, lat);
                intent.putExtra(LONGITUDE_EXTRA, lng);
                MapActivity.this.setResult(RESULT_OK, intent);
                MapActivity.this.finish();
            }
        });
    }

    /** Go to a certain location for the user map */
    private void gotoLocation(double lat, double lng, float zoom) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.moveCamera(update);
    }

    /** This is where the magic happens. Most of the code for the map parts belongs here */
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

                /** Get the contents of the marker placed by the long click on the map.*/
                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.info_window, null);
                    TextView tvLocality = (TextView) v.findViewById(R.id.tvLocality);
                    TextView tvLat = (TextView) v.findViewById(R.id.tvLat);
                    TextView tvLng = (TextView) v.findViewById(R.id.tvLng);
                    TextView tvSnippet = (TextView) v.findViewById(R.id.tvSnippet);

                    /** Setting the marker position to the LatLng that the user presses. */
                    LatLng latLng = marker.getPosition();
                    tvLocality.setText(marker.getTitle());
                    tvLat.setText("Latitude: " + latLng.latitude);
                    tvLng.setText("Longitude: " + latLng.longitude);
                    tvSnippet.setText(marker.getSnippet());

                    return v;
                }
            });

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                /** Map listener to say when the user clicks on the map make a new marker appear  */
                @Override
                public void onMapClick(LatLng latLng) {
                    Geocoder gc = new Geocoder(MapActivity.this);
                    List<Address> list = null;

                    try {
                        list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    Address add = list.get(0);
                    MapActivity.this.addMarker(add, latLng.latitude, latLng.longitude);
                    lat = String.valueOf(marker.getPosition().latitude);
                    lng = String.valueOf(marker.getPosition().longitude);
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override

                /** On marker click  display a message with the location of the marker*/
                public boolean onMarkerClick(Marker marker) {
                    String msg = marker.getTitle() + " (" +
                            marker.getPosition().latitude + ", " +
                            marker.getPosition().longitude + ")";
                    Toast.makeText(MapActivity.this, msg, Toast.LENGTH_SHORT).show();
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
            /** You cam drag the marker to a new location if you misclick*/
            public void onMarkerDragEnd(Marker marker) {
                Geocoder gc = new Geocoder(MapActivity.this);
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

    /** Showing the service is ok. Was called up the top*/
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


    /** Adding a marker element for the map*/
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

    /** Removing everything is the marker needs to be removed.*/
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

}

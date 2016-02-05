package com.example.n00132610.mycalendarapp;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
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
import java.util.List;

public class MapActivity extends FragmentActivity {

    GoogleMap mMap;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleApiClient mLocationClient;
    private Marker marker;

    Circle shape;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mMap= ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (initMap()) {

                //gotoLocation(currentLocation, 15);

                mLocationClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API)
                        //.addConnectionCallbacks(this)
                        //.addOnConnectionFailedListener(this)
                        .build();

                mLocationClient.connect();

                //mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Map not connected!", Toast.LENGTH_SHORT).show();
            }
    }

    private void gotoLocation(double lat, double lng, float zoom) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.moveCamera(update);
    }

    private boolean initMap () {
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
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
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

    private void addMarker(Address add, double lat, double lng) {

        if (marker != null) {
            removeEverything();
        }

        ///////////////////////////
        //Default marker options//
        /////////////////////////
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions options = new MarkerOptions()
                .title(add.getLocality())
                .position(latLng)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker));

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
                .radius(100);
        shape = mMap.addCircle(circleOptions);
    }

    private void removeEverything() {

        marker.remove();
        marker = null;

        shape.remove();
        shape = null;
    }


//    public void showCurrentLocation(MenuItem item) {
//        Location currentLocation = LocationServices.FusedLocationApi
//                .getLastLocation(mLocationClient);
//        if (currentLocation == null) {
//            Toast.makeText(this, "Couldn't connect!", Toast.LENGTH_SHORT).show();
//        } else {
//            LatLng latLng = new LatLng(
//                    currentLocation.getLatitude(),
//                    currentLocation.getLongitude()
//            );
//            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 15);
//            mMap.animateCamera(update);
//        }
//
//    }
}

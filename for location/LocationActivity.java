package com.ps.agrostand.location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.ps.agrostand.R;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    Marker marker;

    private static final int PERMISSION_REQUEST_CODE = 7001;
    private static final int PLAY_SERVICE_REQUEST = 7002;

    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    private LinearLayout btn_searchTeacher, editLocationLL;
    private TextView tv_location;

    private String mlatitude = "", mlongitude = "";
    //    private String data = "";
    String addressss = "";
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
//        getSupportActionBar().hide();
        btn_searchTeacher = findViewById(R.id.btn_searchTeacher);
        editLocationLL = findViewById(R.id.editLocationLL);
        tv_location = findViewById(R.id.tvLocation);
        btn_searchTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("ADDRESS", tv_location.getText().toString());
                intent.putExtra("LATITUDE", mlatitude);
                intent.putExtra("LONGITUDE", mlongitude);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        editLocationLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocationDialog();
            }
        });
        String apiKey = getString(R.string.places_api_key);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
            PlacesClient placesClient = Places.createClient(this);
        } else {
//            Toast.makeText(MapActivity.this, "-----initialize-----", Toast.LENGTH_LONG).show();
        }

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // PlaceFields
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // autocompleteFragment.setOnPlaceSelectedListener(MapsActivity.this);

        try {

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    displayLocation(place.getLatLng().latitude, place.getLatLng().longitude, place.getName());
//                    Toast.makeText(getApplicationContext(), "getName: " + place.getName() + " getLatLng: " + place.getLatLng(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(getApplicationContext(), "" + status.toString(), Toast.LENGTH_LONG).show();

                }

            });
        } catch (Exception e) {
            Toast.makeText(LocationActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setUpLocation();

    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            final double latitude = mLocation.getLatitude();
            final double longitude = mLocation.getLongitude();
            mlatitude = String.valueOf(latitude);
            mlongitude = String.valueOf(longitude);
            List<Address> addresses1;
            Geocoder geocoder = new Geocoder(LocationActivity.this, Locale.getDefault());
            try {
                addresses1 = geocoder.getFromLocation(latitude, longitude, 1);
                addressss = addresses1.get(0).getAddressLine(0);
                tv_location.setText(addressss);
                // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                double lat1 = 0.0, lat2 = 0.0;
                try {
                    lat1 = Double.parseDouble(getIntent().getStringExtra("L1"));
                    lat2 = Double.parseDouble(getIntent().getStringExtra("L2"));
                } catch (Exception e) {

                }
                if (lat1 > 1) {
                    marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat1, lat2)).snippet("Provider Location")
                            .icon(BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                }
            } catch (Exception e) {

            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 18.0f));
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////////
    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        displayLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
/////
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                LatLng latLng = mMap.getCameraPosition().target;
                System.out.println(latLng + "");
                //Toast.makeText(TeacherLocationActivity.this, "Idel Camera", Toast.LENGTH_SHORT).show();
                if (currentLocation == null) {
                    currentLocation = new Location("");
                }
                currentLocation.setLatitude(latLng.latitude);
                currentLocation.setLongitude(latLng.longitude);
                if (currentLocation != null) {
                    final double latitude = currentLocation.getLatitude();
                    final double longitude = currentLocation.getLongitude();
                    mlatitude = String.valueOf(latitude);
                    mlongitude = String.valueOf(longitude);
                    List<Address> addresses1;
                    Geocoder geocoder = new Geocoder(LocationActivity.this, Locale.getDefault());
                    try {
                        addresses1 = geocoder.getFromLocation(latitude, longitude, 1);
                        addressss = addresses1.get(0).getAddressLine(0);
                        tv_location.setText(addressss);
                        // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    if (marker != null) {
//                        marker.remove();
//                    }
//
//                    marker = mMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(latitude, longitude))
//                            .icon(BitmapDescriptorFactory
//                                    .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                }
            }
        });
    }


    ////////////////////////////

    private void displayLocation(final double latitude, final double longitude, String name) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            //if (mLocation != null) {
            //show marker

//                mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(name));

            mlatitude = String.valueOf(latitude);
            mlongitude = String.valueOf(longitude);

            List<Address> addresses1;
            Geocoder geocoder = new Geocoder(LocationActivity.this, Locale.getDefault());

            try {
                addresses1 = geocoder.getFromLocation(latitude, longitude, 1);
                addressss = addresses1.get(0).getAddressLine(0);

                tv_location.setText(addressss);
                // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (marker != null) {
                marker.remove();
            }

//            marker = mMap.addMarker(new MarkerOptions()
//                    .position(new LatLng(latitude, longitude))
//                    .icon(BitmapDescriptorFactory
//                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            //Animate camera to your position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));

            // }

        } catch (Exception e) {

//            Toast.makeText(getApplicationContext(), "" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void updateLocationDialog() {
        try {
            LayoutInflater factory = LayoutInflater.from(this);
            final View customView = factory.inflate(R.layout.edit_location_view, null);
            final android.app.AlertDialog optionDialog = new android.app.AlertDialog.Builder(this).create();
            optionDialog.getWindow().setGravity(Gravity.CENTER);
            optionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            optionDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            optionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            optionDialog.setView(customView);
            LinearLayout cancelLL = customView.findViewById(R.id.cancelLL);
            LinearLayout saveLL = customView.findViewById(R.id.saveLL);
            final EditText addressET = customView.findViewById(R.id.addressET);
            addressET.setText(tv_location.getText().toString().trim());
            cancelLL.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    optionDialog.dismiss();
                }
            });
            saveLL.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    optionDialog.dismiss();
                    if (addressET.getText().toString().trim().length() > 0) {
                        tv_location.setText(addressET.getText().toString().trim());
                    } else {
                        Toast.makeText(LocationActivity.this, "Enter Location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            optionDialog.show();
            Rect displayRectangle = new Rect();
            Window window = this.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
            optionDialog.getWindow().setLayout((int) (displayRectangle.width()), optionDialog.getWindow().getAttributes().height);
        } catch (Exception e) {

        }
    }
}


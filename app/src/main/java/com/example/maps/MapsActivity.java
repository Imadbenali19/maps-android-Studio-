package com.example.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.maps.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private final String tag="yy";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    RequestQueue requestQueue;
    RequestQueue requestQueue1;
    String insertUrl = "https://10.0.2.2/localisation/createPosition.php";
    String showUrl = "https://10.0.2.2/localisation/test.php";

    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }


            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleSSLHandshake();
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setContentView(R.layout.activity_maps);
        requestQueue1 = Volley.newRequestQueue(getApplicationContext());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //setUpMap();

        //Localisation
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissons.Request_FINE_LOCATION(this, 20);
            return;
        }

        requestQueue = Volley.newRequestQueue(getApplicationContext());



        LocationManager locationManager1 = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 50, new
                LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        altitude = location.getAltitude();
                        accuracy = location.getAccuracy();
                        @SuppressLint("StringFormatMatches") String msg = String.format(
                                getResources().getString(R.string.new_location), latitude, longitude, altitude, accuracy);
                        addPosition(latitude, longitude);
                        setUpMap();

                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        String newStatus = "";
                        switch (status) {
                            case LocationProvider.OUT_OF_SERVICE:
                                newStatus = "OUT_OF_SERVICE";
                                break;
                            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                newStatus = "TEMPORARILY_UNAVAILABLE";
                                break;
                            case LocationProvider.AVAILABLE:
                                newStatus = "AVAILABLE";
                                break;
                        }
                        String msg = String.format(getResources().getString(R.string.provider_new_status),
                                provider, newStatus);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        String msg = String.format(getResources().getString(R.string.provider_enabled),
                                provider);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        String msg = String.format(getResources().getString(R.string.provider_disabled),
                                provider);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });




    }

    private void setUpMap() {
        float zoomLevel = 4.0f;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, showUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONArray jsonArray = response.getJSONArray("positions");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject position = jsonArray.getJSONObject(i);

                                mMap.addMarker(new MarkerOptions().position(new
                                        LatLng(position.getDouble("latitude"),
                                        position.getDouble("longitude"))).title("Marker"));


                                LatLng position1 = new LatLng(position.getDouble("latitude"),
                                        position.getDouble("longitude"));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position1, zoomLevel));
                            }



                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue1.add(request);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {


        mMap = googleMap;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        Toast.makeText(getApplicationContext(), "ok", Toast.LENGTH_SHORT).show();
        setUpMap();
        boolean permissionGranted = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (permissionGranted) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30, 50, new
                    LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            /*double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Toast.makeText(getApplicationContext(), latitude+" "+longitude, Toast.LENGTH_SHORT).show();
                            mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,
                                    longitude)).title("Marker"));
                            */

                            //setUpMap();


                        }
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }
                        @Override
                        public void onProviderEnabled(String provider) {
                        }
                        @Override
                        public void onProviderDisabled(String provider) {
                            buildAlertMessageNoGps();
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
        //
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    }

    void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(Request.Method.POST,
                insertUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("MainActivity", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("MainActivity", error.toString());
            }
        }) {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                TelephonyManager telephonyManager =
                        (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                HashMap<String, String> params = new HashMap<String, String>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                params.put("latitude", lat + "");
                params.put("longitude", lon + "");
                params.put("date", sdf.format(new Date()) + "");
                //params.put("imei", telephonyManager.getImei());
                params.put("imei", "not supported");
                return params;
            }
        };
        requestQueue.add(request);
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new
                                Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    }
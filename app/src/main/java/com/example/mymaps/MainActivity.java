package com.example.mymaps;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    Button btnLocation;

    GoogleMap map;
    Marker marker;
    List<LatLng> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLocation = findViewById(R.id.btn_activity_main_location);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.frag);
        mapFragment.getMapAsync(this);
        list = new ArrayList<>();


        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText etLocation = findViewById(R.id.et_activity_main_location);


                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
                try {
                    List<Address> list = geocoder.getFromLocationName(etLocation.getText().toString(), 1);
                    Address address = list.get(0);

                    LatLng loc = new LatLng(address.getLatitude(), address.getLongitude());
                    Log.d("hims", "onClick: " + loc);
                    map.addMarker(new MarkerOptions().position(loc).title("Marker in loc").draggable(true));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,15f));

                    if(marker!=null)
                    marker.remove();


                    // https:"//maps.googleapis.com/maps/api/directions"+output+"=?"+paramaeters+"&key"+getString(R.string.apiKey);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (list.size() > 1) {
                    list.clear();
                    map.clear();
                }

                list.add(latLng);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if (list.size() == 1)
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                else
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                map.addMarker(markerOptions);
                String url;
                if (list.size() == 2) {
                    url = getStringURl(list.get(0), list.get(1));
                    TaskRequestDirection taskRequestDirection = new TaskRequestDirection();
                    taskRequestDirection.execute(url);
                   // Log.d("h", "onPostExecute: "+taskRequestDirection.execute(url));
                }
            }
        });



    }

    private String getStringURl(LatLng orgin, LatLng dest) {

        String strOrgin = "origin=" + orgin.latitude + "," + orgin.longitude;
        String strdest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "false";
        //String mode = "mode-driving";
        String param = strOrgin + "&" + strdest + "&" + sensor ;
        String output = "json";
        String url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=5b3ce3597851110001cf6248b617ea55c39e4fe2b53c4fdc02881a67&start=8.681495,49.41461&end=8.687872,49.420318";

        return url;
    }

    private String requestDirection(String reqUrl) {

        String reposeneSting = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            reposeneSting = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return reposeneSting;
    }


    public class TaskRequestDirection extends AsyncTask<String, Void, String> {

        String responseString = "";


        @Override
        protected String doInBackground(String... strings) {
            responseString = requestDirection(strings[0]);
            return responseString;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(s);
            Toast.makeText(MainActivity.this, "hello", Toast.LENGTH_SHORT).show();

        }


        private class ParserTask extends AsyncTask<String, Integer, String> {
String duration;
            // Parsing the data in non-ui thread
            @Override
            protected String doInBackground(String... jsonData) {

                JSONObject jObject;


                ArrayList<LatLng>points=new ArrayList<>();
                try {
                    jObject = new JSONObject(jsonData[0]);
                    Log.d("sd", "doInBackground: "+jsonData[0]);
                    JSONArray features=jObject.getJSONArray("features");
                    Log.d("hims", "doInBackground: "+features.length());
                    JSONObject zero=features.getJSONObject(0);
                    JSONArray bBox=zero.getJSONArray("bbox");

                    for(int i=0;i<bBox.length();i=i+2)
                    {

                        Double loc=bBox.getDouble(i);
                        Double loc1=bBox.getDouble(i+1);
                     LatLng latLng  =   new LatLng(loc,loc1);
                   points.add(latLng);

                    }
                    JSONObject properties=zero.getJSONObject("properties");
                    JSONArray segments=properties.getJSONArray("segments");
                    JSONObject zero1=segments.getJSONObject(0);
                     duration=zero1.getString("duration");


                } catch (Exception e) {
                    e.printStackTrace();
                }

                return  duration ;
            }

            @Override
            protected void onPostExecute(String result) {


                Toast.makeText(MainActivity.this, "duration"+duration, Toast.LENGTH_SHORT).show();
                PolylineOptions polylineOptions=new PolylineOptions().add(list.get(0)).add(list.get(1)).width(10f).color(Color.BLUE);
                //PolylineOptions polylineOptions=new PolylineOptions().addAll(result).width(10f).color(Color.BLUE);
                map.addPolyline(polylineOptions);












            }


        }


    }
}





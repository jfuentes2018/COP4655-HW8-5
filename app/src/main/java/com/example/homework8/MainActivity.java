package com.example.homework8;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final int REQ_CODE = 100;
    //requests
    private final String ByZipCode = "ByZipCode";
    private final String ByCity = "ByCity";
    private final String ByCoords = "ByCoords";
    //lat and long
    private String latSTR;
    private String lonSTR;
    private double latDBL;
    private double lonDBL;
    //google maps
    private GoogleMap mMap;
    //text to speech
    TextToSpeech t1;
    TextView ed1;
    ImageButton b1;


    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    //Speech to text
    TextView textView;
    // GPSTracker class
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //speech to text
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.input);
        ImageView speak = findViewById(R.id.speak);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ed1=findViewById(R.id.cityView);
        b1=findViewById(R.id.ttsButton);

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say city");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry your device not supported",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //location
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImageView location = findViewById(R.id.imageView2);

        // show location button click event
        location.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // create class object
                gps = new GPSTracker(MainActivity.this);

                // check if GPS enabled
                if(gps.canGetLocation()){

                    latDBL = gps.getLatitude();
                    lonDBL = gps.getLongitude();

                    coordsToString();

                }else{
                    // can't get location
                    // GPS or Network is not enabled
                    // Ask user to enable GPS/network in settings
                    gps.showSettingsAlert();
                }

            }

        });
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = ed1.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textView.setText((String)result.get(0));
                }
                break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng TutorialsPoint = new LatLng(latDBL, lonDBL);
        mMap.setMinZoomPreference(10);
        mMap.addMarker(new
                MarkerOptions().position(TutorialsPoint).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(TutorialsPoint));
    }

    public void sendMessage(View view) {

        EditText inputText = findViewById(R.id.input);
        String input = inputText.getText().toString();

        Log.d("inputTest", input);

        if (TextUtils.isDigitsOnly(input)) {
            requestType(input, ByZipCode);
        } else {
            requestType(input, ByCity);
        }
    }

    public void coordsToString() {

        latSTR = latDBL + "";
        lonSTR = lonDBL + "";

        requestType("", ByCoords);
    }


    //get the url depending on the request type
    public void requestType(String input, String type) {
        final String KEY = "&appid=8aa29092a2c9bae6c2990e2b69b41c65";
        final String cityURL = "https://api.openweathermap.org/data/2.5/weather?q=";
        final String zipURL = "https://api.openweathermap.org/data/2.5/weather?zip=";
        final String coordsLat = "https://api.openweathermap.org/data/2.5/weather?lat=";
        final String coordsLon = "&lon=";
        final String units = "&units=imperial";
        String url;

        switch (type) {
            case ByZipCode:
                url = zipURL + input + KEY + units;
                makeRequest(url);
                Log.d("urlType", "zip");
                break;
            case ByCity:
                url = cityURL + input + KEY + units;
                makeRequest(url);
                Log.d("urlType", "city");
                break;
            case ByCoords:
                url = coordsLat + latSTR + coordsLon + lonSTR + KEY + units;
                Log.d("urlType", "coords");
                makeRequest(url);
                break;
        }
    }

    //call the API using the url
    public void makeRequest(String url) {
        Log.d("test", "request");
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        DisplayWeatherResults(response);
                        Log.d("test", "request passed");
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.d("test", "request failed");
                    }
                });
        queue.add(jsonObjectRequest);
    }

    protected void DisplayWeatherResults(JSONObject response) {

        try {
            TextView cityView = findViewById(R.id.cityView);
            TextView tempView = findViewById(R.id.tempView);
            TextView minTempView = findViewById(R.id.minTempView);
            TextView maxTempView = findViewById(R.id.maxTempView);
            TextView windSpeedView = findViewById(R.id.windSpeedView);

            JSONObject coord = response.getJSONObject("coord");
            lonSTR = coord.get("lon").toString();
            latSTR = coord.get("lat").toString();
            JSONObject main = response.getJSONObject("main");
            String temp = main.get("temp").toString();
            String temp_min = main.get("temp_min").toString();
            String temp_max = main.get("temp_max").toString();
            JSONObject wind = response.getJSONObject("wind");
            String wind_speed = wind.get("speed").toString();
            JSONObject sys = response.getJSONObject("sys");
            String country = sys.get("country").toString();
            String cityName = response.getString("name");

            cityView.setText(cityName + ", " + country);
            tempView.setText(temp + " F");
            minTempView.setText(temp_min + " F");
            maxTempView.setText(temp_max + " F");
            windSpeedView.setText(wind_speed + " mph");

            latDBL = Double.parseDouble(latSTR);
            lonDBL = Double.parseDouble(lonSTR);
            onMapReady(mMap);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
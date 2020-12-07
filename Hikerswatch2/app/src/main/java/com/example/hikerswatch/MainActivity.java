package com.example.hikerswatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    LocationListener locationListener;
    TextView latlngTextView;
    TextView addressTextView;
    TextView weatherTextView;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    public void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @SuppressLint("SetTextI18n")
    public void updateLocationInfo(Location location) {
        Log.i("LocationInfo", location.toString());

        latlngTextView.setText("Latitude: " + location.getLatitude() + "\n" + "Longitude: " + location.getLongitude() + "\n" + "Accuracy: " + location.getAccuracy());
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {
            String address = "Could not find address";
            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (listAddresses != null && listAddresses.size() > 0 ) {
                Log.i("PlaceInfo", listAddresses.get(0).toString());
                address = "Address: \n";
                if(listAddresses.get(0).getAddressLine(0) != null && !listAddresses.get(0).getAddressLine(0).isEmpty()){
                    address += listAddresses.get(0).getAddressLine(0);
                }
            }
                addressTextView.setText(address);

            DownloadTask task = new DownloadTask();
            Log.i("locality: ", listAddresses.get(0).getLocality());
            task.execute("https://api.openweathermap.org/data/2.5/weather?q=" + listAddresses.get(0).getLocality() + "&appid=937ae51dbd201e18f7f0d9ab4eddaf43");

            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Could not find weather :(",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latlngTextView = findViewById(R.id.latlngTextView);
        addressTextView = (TextView) findViewById(R.id.addressTextView);
        weatherTextView = (TextView) findViewById(R.id.weatherTextView);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocationInfo(location);
            }
        };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    updateLocationInfo(location);
                }
            }

    }

    public class DownloadTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {

                JSONObject jsonObject = new JSONObject(s);
                String weatherInfo = jsonObject.getString("weather");
                JSONObject temp = jsonObject.getJSONObject("main");
                Log.i("Weather content", weatherInfo);

                JSONArray arr = new JSONArray(weatherInfo);
                String message = "";
                message = "Weather: \nTemp: " + String.format("%.2f", temp.getDouble("temp") - 273.15) + "°C\n";
                message += "feels like:  " + String.format("%.2f", temp.getDouble("feels_like") - 273.15) + "°C\n";
                message += "Max temp:  " + String.format("%.2f", temp.getDouble("temp_max") - 273.15) + "°C\n";
                message += "Min temp:  " + String.format("%.2f", temp.getDouble("temp_min") - 273.15) + "°C\n";
                message += "Pressure: " + temp.getDouble("pressure") + " Pa\n";
                message += "Humidity: " +  String.format("%.2f", temp.getDouble("humidity") - 273.15) + "°C\n";

                for (int i=0; i < arr.length(); i++) {
                    JSONObject jsonPart = arr.getJSONObject(i);
                    String main = jsonPart.getString("main");
                    String description = jsonPart.getString("description");
                    if (!main.equals("") && !description.equals("")) {
                        message +=  "Weather: " + main;
                    }
                }

                if (!message.equals("")) {
                    weatherTextView.setText(message);
                } else {
                    Toast.makeText(getApplicationContext(),"Could not find weather :(",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),"Could not find weather :(",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}

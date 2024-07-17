package com.kumar.weatherapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kumar.weatherapp.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private final String url = "http://api.openweathermap.org/data/2.5/weather";
    private final String api = "e1d56c31890a14e4cabc3090a3396b5d";
    DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        binding.searchButton.setOnClickListener(v -> {
            binding.loading.setVisibility(View.VISIBLE);
            binding.loading.playAnimation();

            binding.searchImage.setVisibility(View.GONE);
            binding.searchButton.setEnabled(false);

            getWeatherDetails();
        });

        // Requesting location permission if not granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceeding to get current location
            getCurrentLocation();
        }

        binding.preciseButton.setOnClickListener(v -> {
            binding.preciseLoading.setVisibility(View.VISIBLE);
            binding.preciseLoading.playAnimation();

            binding.preciseImage.setVisibility(View.GONE);
            binding.preciseButton.setEnabled(false);

            getCurrentLocation();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, getting current location
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            String tempUrl = url + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + api;
                            fetchWeatherData(tempUrl);
                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void getWeatherDetails() {
        String tempUrl = "";
        String city = binding.searchField.getText().toString().trim();

        if (city.isEmpty()) {
            // Using current location if no city is entered
            getCurrentLocation();

            Toast.makeText(this, "Enter city!", Toast.LENGTH_SHORT).show();
        } else {
            tempUrl = url + "?q=" + city + "&appid=" + api;
            fetchWeatherData(tempUrl);
        }
    }

    private void fetchWeatherData(String tempUrl) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, tempUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String description = jsonObjectWeather.getString("description");
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    double temp = jsonObjectMain.getDouble("temp") - 273.15;
                    double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
                    float pressure = jsonObjectMain.getInt("pressure");
                    int humidity = jsonObjectMain.getInt("humidity");
                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                    String wind = jsonObjectWind.getString("speed");
                    JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
                    String clouds = jsonObjectClouds.getString("all");
                    JSONObject jsonObjectSys = jsonResponse.getJSONObject("sys");
                    String countryName = jsonObjectSys.getString("country");
                    String cityName = jsonResponse.getString("name");

                    settingData(description, temp, feelsLike, pressure, humidity, wind, clouds, countryName, cityName);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Invalid Name", Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    private void settingData(String description, double temp, double feelsLike, float pressure, int humidity, String wind, String clouds, String countryName, String cityName) {
        binding.description.setText(description);
        binding.temp.setText(decimalFormat.format(temp) + " °C");
        binding.feelsLike.setText("Feels like " + decimalFormat.format(feelsLike) + " °C");
        binding.windPressure.setText(String.valueOf(pressure) + " hPa");
        binding.humidity.setText(String.valueOf(humidity) + "%");
        binding.windSpeed.setText(wind + "m/s");
        binding.cloudiness.setText(clouds + "%");
        binding.countryName.setText(cityName + " (" + countryName + ")");

        // Making searField empty after successful search
        binding.searchField.setText("");

        // Reset button animation
        resetButtonSearch();
        resetButtonPrecise();

        // Change image based on weather description
        if (description.toLowerCase().contains("scattered clouds")) {
            binding.imageView.setImageResource(R.drawable.scattered);
        } else if (description.toLowerCase().contains("clear sky")){
            binding.imageView.setImageResource(R.drawable.clear);
        } else if (description.toLowerCase().contains("haze")){
            binding.imageView.setImageResource(R.drawable.haze);
        } else if (description.toLowerCase().contains("smoke")){
            binding.imageView.setImageResource(R.drawable.smoke);
        } else if (description.toLowerCase().contains("overcast clouds")){
            binding.imageView.setImageResource(R.drawable.overcast);
        } else if (description.toLowerCase().contains("broken clouds")){
            binding.imageView.setImageResource(R.drawable.broken);
        } else if (description.toLowerCase().contains("light rain")){
            binding.imageView.setImageResource(R.drawable.light_rain);
        } else if (description.toLowerCase().contains("mist")){
            binding.imageView.setImageResource(R.drawable.mist);
        } else if (description.toLowerCase().contains("few clouds")){
            binding.imageView.setImageResource(R.drawable.sample);
        } else if (description.toLowerCase().contains("moderate rain")){
            binding.imageView.setImageResource(R.drawable.moderate_rain);
        } else if (description.toLowerCase().contains("rain")){
            binding.imageView.setImageResource(R.drawable.moderate_rain);
        } else {
            binding.imageView.setImageResource(R.drawable.sample);
        }
    }

    private void resetButtonSearch() {
        binding.loading.pauseAnimation();
        binding.loading.setVisibility(View.GONE);

        binding.searchImage.setVisibility(View.VISIBLE);
        binding.searchButton.setEnabled(true);
    }

    private void resetButtonPrecise() {
        binding.preciseLoading.pauseAnimation();
        binding.preciseLoading.setVisibility(View.GONE);

        binding.preciseImage.setVisibility(View.VISIBLE);
        binding.preciseButton.setEnabled(true);
    }
}

package com.codelama.weather_app;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener{
    private Handler handler = new Handler();
    TextView city,temp,weather,humidity,wind,realFeel,date;
    ImageView weatherImage;
    public FusedLocationProviderClient client;
    static int indexforecast=5;
    static String latitude;
    static String longitude;
    private String api_id = "818c99e38ef923e289462d13503c0aa9";
    private boolean useLocation = true;
    private String cityName = new String();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("WeatherAppPrefs", MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        city = findViewById(R.id.id_city);
        temp = findViewById(R.id.id_degree);
        weather = findViewById(R.id.id_weather);
        humidity = findViewById(R.id.id_humidity);
        wind = findViewById(R.id.id_wind);
        realFeel = findViewById(R.id.id_realfeel);
        weatherImage = findViewById(R.id.id_weatherImage);
        date=findViewById(R.id.id_date);

        client = LocationServices.getFusedLocationProviderClient(this);

        latitude = sharedPreferences.getString("latitude", "0");
        longitude = sharedPreferences.getString("longitude", "0");
        cityName = sharedPreferences.getString("cityName", "London");
        useLocation = sharedPreferences.getBoolean("useLocation", true);

        updateLocation();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLocation();
                handler.postDelayed(this, 1000);
            }
        }, 60*60*1000);

    }

    private void updateWidget(String temperature, String weatherIconCode) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, Widget.class);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget);

        remoteViews.setTextViewText(R.id.widget_temperature, temperature);
        remoteViews.setImageViewResource(R.id.widget_weather_icon, getWeatherIconResource(weatherIconCode));

        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }


    public void updateLocation() {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("latitude", latitude);
        editor.putString("longitude", longitude);
        editor.putString("cityName", cityName);
        editor.putBoolean("useLocation", useLocation);
        editor.apply();

        if(useLocation) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, ACCESS_FINE_LOCATION)){

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{ACCESS_FINE_LOCATION}, 1);
                }else{
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{ACCESS_FINE_LOCATION}, 1);
                }
            }
            client.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location!=null){
                        double lat=Math.round(location.getLatitude() * 100.0)/100.0;
                        latitude = String.valueOf(lat);

                        double lon=Math.round(location.getLongitude() * 100.0)/100.0;
                        longitude = String.valueOf(lon);

                        getWeather(latitude,longitude);
                    } else {
                        Toast.makeText(MainActivity.this, "Unable to access your location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            getWeather(cityName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1: {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                        client.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location!=null){

                                    double lat=Math.round(location.getLatitude() * 100.0)/100.0;
                                    latitude= String.valueOf(lat);

                                    double lon=Math.round(location.getLongitude() * 100.0)/100.0;
                                    longitude= String.valueOf(lon);

                                    getWeather(latitude,longitude);
                                }
                            }
                        });
                    }
                }else{
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void updateUI(String data) {
        try {
            JSONObject json=new JSONObject(data);
            TextView[] forecast = new TextView[5];
            TextView[] forecastTemp=new TextView[5];
            ImageView[] forecastIcons=new ImageView[5];
            IdAssign(forecast,forecastTemp,forecastIcons);

            indexforecast=5;
            for (int i=0;i<forecast.length;i++){
                forecastCal(forecast[i],forecastTemp[i],forecastIcons[i],indexforecast,json);
            }

            JSONArray list=json.getJSONArray("list");
            JSONObject objects = list.getJSONObject(0);
            JSONArray array=objects.getJSONArray("weather");
            JSONObject object=array.getJSONObject(0);

            String description=object.getString("description");
            String icons=object.getString("icon");

            Date currentDate=new Date();
            String dateString=currentDate.toString();
            String[] dateSplit=dateString.split(" ");
            String date=dateSplit[0]+", "+dateSplit[1] +" "+dateSplit[2];

            JSONObject Main=objects.getJSONObject("main");
            double temparature=Main.getDouble("temp");
            String Temp=Math.round(temparature)+"°C";
            double Humidity=Main.getDouble("humidity");
            String hum=Math.round(Humidity)+"%";
            double FeelsLike=Main.getDouble("feels_like");
            String feelsValue=Math.round(FeelsLike)+"°";

            JSONObject Wind=objects.getJSONObject("wind");
            String windValue=Wind.getString("speed")+" "+"km/h";

            JSONObject CityObject=json.getJSONObject("city");
            String City=CityObject.getString("name");

            updateWidget(Temp, icons);

            setDataText(city,City);
            setDataText(temp,Temp);
            setDataImage(weatherImage,icons);
            setDataText(weather,date);
            setDataText(humidity,hum);
            setDataText(realFeel,feelsValue);
            setDataText(wind,windValue);

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void getWeather(String... params) {
        String url;
        if (params.length == 1) {
            url = "https://api.openweathermap.org/data/2.5/forecast?q=" + params[0] + "&appid=" + api_id + "&units=metric";
        } else if (params.length == 2) {
            url = "https://api.openweathermap.org/data/2.5/forecast?lat=" + params[0] + "&lon=" + params[1] + "&appid=" + api_id + "&units=metric";
        } else {
            throw new IllegalArgumentException("Invalid number of parameters");
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String data = response.body().string();
                updateUI(data);
            }
        });
    }
    private void setDataText(final TextView text, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }
    private void setDataImage(final ImageView ImageView, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView.setImageDrawable(getResources().getDrawable(getWeatherIconResource(value)));
            }
        });
    }

    private int getWeatherIconResource(String iconCode) {
        switch (iconCode) {
            case "01d":
                return R.drawable.w01d;
            case "01n":
                return R.drawable.w01d;
            case "02d":
                return R.drawable.w02d;
            case "02n":
                return R.drawable.w02d;
            case "03d":
                return R.drawable.w03d;
            case "03n":
                return R.drawable.w03d;
            case "04d":
                return R.drawable.w04d;
            case "04n":
                return R.drawable.w04d;
            case "09d":
                return R.drawable.w09d;
            case "09n":
                return R.drawable.w09d;
            case "10d":
                return R.drawable.w10d;
            case "10n":
                return R.drawable.w10d;
            case "11d":
                return R.drawable.w11d;
            case "11n":
                return R.drawable.w11d;
            case "13d":
                return R.drawable.w13d;
            case "13n":
                return R.drawable.w13d;
            default:
                return R.drawable.w01d;
        }
    }

    private void IdAssign(TextView[] forecast,TextView[] forecastTemp,ImageView[] forecastIcons){
        forecast[0]=findViewById(R.id.id_forecastDay1);
        forecast[1]=findViewById(R.id.id_forecastDay2);
        forecast[2]=findViewById(R.id.id_forecastDay3);
        forecast[3]=findViewById(R.id.id_forecastDay4);
        forecast[4]=findViewById(R.id.id_forecastDay5);
        forecastTemp[0]=findViewById(R.id.id_forecastTemp1);
        forecastTemp[1]=findViewById(R.id.id_forecastTemp2);
        forecastTemp[2]=findViewById(R.id.id_forecastTemp3);
        forecastTemp[3]=findViewById(R.id.id_forecastTemp4);
        forecastTemp[4]=findViewById(R.id.id_forecastTemp5);
        forecastIcons[0]=findViewById(R.id.id_forecastIcon1);
        forecastIcons[1]=findViewById(R.id.id_forecastIcon2);
        forecastIcons[2]=findViewById(R.id.id_forecastIcon3);
        forecastIcons[3]=findViewById(R.id.id_forecastIcon4);
        forecastIcons[4]=findViewById(R.id.id_forecastIcon5);
    }

    private void forecastCal(TextView forecast,TextView forecastTemp,ImageView forecastIcons,int index,JSONObject json) throws JSONException {
        JSONArray list=json.getJSONArray("list");
        for (int i=index; i<list.length(); i++) {
            JSONObject object = list.getJSONObject(i);

            String dt=object.getString("dt_txt"); // dt_text.format=2020-06-26 12:00:00
            String[] a=dt.split(" ");
            if ((i==list.length()-1) && !a[1].equals("12:00:00")){
                String[] dateSplit=a[0].split("-");
                Calendar calendar=new GregorianCalendar(Integer.parseInt(dateSplit[0]),Integer.parseInt(dateSplit[1])-1,Integer.parseInt(dateSplit[2]));
                Date forecastDate=calendar.getTime();
                String dateString=forecastDate.toString();
                String[] forecastDateSplit=dateString.split(" ");
                String date=forecastDateSplit[0]+", "+forecastDateSplit[1] +" "+forecastDateSplit[2];
                setDataText(forecast, date);

                JSONObject Main=object.getJSONObject("main");
                double temparature=Main.getDouble("temp");
                String Temp=Math.round(temparature)+"°";
                setDataText(forecastTemp,Temp);

                JSONArray array=object.getJSONArray("weather");
                JSONObject object1=array.getJSONObject(0);
                String icons=object1.getString("icon");
                setDataImage(forecastIcons,icons);

                return;
            }
            else if (a[1].equals("12:00:00")){

                String[] dateSplit=a[0].split("-");
                Calendar calendar=new GregorianCalendar(Integer.parseInt(dateSplit[0]),Integer.parseInt(dateSplit[1])-1,Integer.parseInt(dateSplit[2]));
                Date forecastDate=calendar.getTime();
                String dateString=forecastDate.toString();
                String[] forecastDateSplit=dateString.split(" ");
                String date=forecastDateSplit[0]+", "+forecastDateSplit[1] +" "+forecastDateSplit[2];
                setDataText(forecast, date);

                JSONObject Main=object.getJSONObject("main");
                double temparature=Main.getDouble("temp");
                String Temp=Math.round(temparature)+"°";
                setDataText(forecastTemp,Temp);

                JSONArray array=object.getJSONArray("weather");
                JSONObject object1=array.getJSONObject(0);
                String icons=object1.getString("icon");
                setDataImage(forecastIcons,icons);

                indexforecast=i+1;
                return;
            }
        }
    }

    public void showPopup(View v){
        PopupMenu popup=new PopupMenu(this,v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.popup);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int temp_if = item.getItemId();
        if(temp_if == R.id.id_currentLocation) {
            useLocation = true;
            updateLocation();
            return true;
        } else if (temp_if == R.id.id_otherCity) {
            useLocation = false;
            Intent intent=new Intent(MainActivity.this, CitySearchActivity.class);
            startActivityForResult(intent,1);
            return false;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String citySearched = data.getStringExtra("result");
                useLocation = false;
                cityName = citySearched;
                updateLocation();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                updateLocation();
            }
        }
    }
}
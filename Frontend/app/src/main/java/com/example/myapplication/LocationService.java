package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import javax.crypto.SecretKey;

public class LocationService extends Service {

    private Handler handler;
    private Runnable runnable;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private static double latitude;
    private static double longitude;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    public static double getLatitude() {
        return latitude;
    }
    public static double getLongitude() {
        return longitude;
    }

    @Override
    public void onCreate() {
        Log.d("LocationService", "onCreate()");
        super.onCreate();
        handler = new Handler();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d("LocationService", "Latitude: " + latitude + ", Longitude: " + longitude);

                        // Start network request in a separate thread
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Retrieve or create the SecretKey
                                    SecretKey secretKey = KeyManager.getOrCreateKey();

                                    // Create data to encrypt
                                    String plainData = latitude + ", " + longitude;

                                    // Encrypt data
                                    String encryptedData = CryptoManager.encrypt(plainData, secretKey);
                                    Log.d("LocationService", "Encrypted data: " + encryptedData);

                                    // Use JSONObject to create the JSON payload
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("data", encryptedData);

                                    // Convert JSONObject to String
                                    String data = jsonObject.toString();
                                    Log.d("LocationService", "JSON Data: " + data);

                                    // Send encrypted data in the JSON format
                                    SessionClient.postMessage(getApplicationContext(), "https://lumi-ai.at/api/geo", data);

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }).start();
                    }
                }
            }
        };

        runnable = new Runnable() {
            @Override
            public void run() {
                requestLocationUpdate();
                // Wiederhole die Standortabfrage nach 10 Minuten (10 * 60 * 1000 ms)
                handler.postDelayed(this, 30*1000); //10 *60 * 1000
            }
        };

        // Starte die Standortabfrage sofort
        handler.post(runnable);

        // Foreground Service starten
        startForegroundService();
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdate() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000*10*60);  // 1 Sekunde Intervall, anpassen
        locationRequest.setFastestInterval(1000*10*60);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);  // Entferne Standort-Updates, wenn der Service beendet wird
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Erstellen der Benachrichtigung
        Intent notificationIntent = new Intent(this, OverlayActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Der Dienst zur Standortabfrage läuft im Hintergrund")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        // Starten als Foreground Service
        startForeground(1, notification);
    }
}

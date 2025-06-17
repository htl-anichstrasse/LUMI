package com.example.myapplication;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;

public class DataManager {
    private static Context context;
    private static JSONObject responseBody;

    private static final String TAG = "DataManager";
    private static TextToSpeechHelper textToSpeechHelper;

    DataManager(Context context, JSONObject responseBody) {
        this.context = context.getApplicationContext();
        this.responseBody = responseBody;
        textToSpeechHelper = TextToSpeechHelper.getInstance(context);
    }



    public static void manageData(Activity activity) throws JSONException, IOException {

//        InputStream inputStream = context.getResources().openRawResource(R.raw.locationapi);
//
//        // InputStream in einen Byte-Array umwandeln
//        byte[] buffer = new byte[inputStream.available()];
//        inputStream.read(buffer);
//        inputStream.close();
//
//        // Byte-Array in einen String umwandeln
//        String json = new String(buffer, StandardCharsets.UTF_8);
//
//        // JSON-String in einen JSONObject umwandeln
//        JSONObject responseBody = new JSONObject(json);

        if (activity != null) {
            activity.runOnUiThread(() -> {
                try {


                    Log.d(TAG, "Starting data management.");
                    if(responseBody!=null){ // responseBody.has("text")


                        //textToSpeechHelper.playStreamingAudio("Hallo Manuel, das ist ein Test der Text-to-Speech Funktion. Mein Name ist Lumi. Wie kann ich dir helfen?", "tts-1", "nova");
                    }
                    if (responseBody != null && responseBody.has("application")) {
                        String applicationType = responseBody.getString("application");
                        Log.d(TAG, "Application type: " + applicationType);

                        View overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null);
                        LinearLayout newLayout = overlayView.findViewById(R.id.additional_layout);

                        switch (applicationType) {
                            case "weather":
                                View weatherView = LayoutInflater.from(context).inflate(R.layout.weather_layout, null, false);
                                parseWeatherData(responseBody, weatherView);
                                parseWeatherData2(responseBody, weatherView);
                                newLayout.addView(weatherView);
                                break;
                            case "alarm":
                                View alarmView = LayoutInflater.from(context).inflate(R.layout.alarm_layout, null, false);
                                parseAlarmData(responseBody, alarmView, overlayView);
                                newLayout.addView(alarmView);
                                break;
                            case "call":
                                View callView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_call, null, false);
                                parseCall(responseBody, callView);
                                newLayout.addView(callView);
                                break;
                            case "chat":
                                View chatView = LayoutInflater.from(context).inflate(R.layout.chat_layout, null, false);
                                parseChatData(responseBody, chatView, overlayView);
                                newLayout.addView(chatView);
                                break;
                            case "app":
                                parseApp(responseBody);
                                break;

                            case "transport":
                                View fahrplanView = LayoutInflater.from(context).inflate(R.layout.public_transport, null, false);
                                parseFahrplanData(responseBody, fahrplanView);
                                newLayout.addView(fahrplanView);
                                break;

                            // im Rahmen des HCIN-Projekts hinzugefügt
                            case "location history":
                                View locationView = LayoutInflater.from(context).inflate(R.layout.location_layout, null, false);
                                parseLocationData(responseBody, locationView);
                                newLayout.addView(locationView);
                                break;

                            case "personal":
                                parsePersonalDecryption(responseBody, activity);
                                break;
                            default:
                                Log.w(TAG, "Unknown application type: " + applicationType);
                                break;
                        }

                        OverlayActivity.updateOverlayView(overlayView);
                        if(applicationType.equals("alarm") && responseBody.has("error") && "No time specified".equals(responseBody.getString("error"))){
                            OverlayActivity.setRecognizedText("Stelle einen Wecker auf ");
                        }
                        else {
                            OverlayActivity.setRecognizedText("");
                        }

                    } else {
                        Log.e(TAG, "Missing 'application' field in JSON");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Parsing error: " + e.getMessage(), e);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
                }
            });
        } else {
            Log.e(TAG, "Übergebener Activity-Kontext ist null.");
        }

    }


    private static void parseWeatherData(JSONObject jsonObject, View currentView) {
        try {
            Log.d(TAG, "Parsing weather data.");
            LinearLayout hourlyLayout = currentView.findViewById(R.id.hourly_forecast_layout);

            JSONArray list = jsonObject.getJSONObject("forecast").getJSONArray("list");
            Log.d(TAG, "Number of hourly items: " + list.length());

            for (int i = 0; i < 8; i++) {
                JSONObject item = list.getJSONObject(i);

                double temp = item.getJSONObject("main").getDouble("temp") - 273.15; // Kelvin to Celsius
                String icon = item.getJSONArray("weather").getJSONObject(0).getString("icon");
                double pop = item.getDouble("pop");
                String dtTxt = item.getString("dt_txt");
                String time = dtTxt.split(" ")[1].substring(0, 5); // "HH:mm"

                Log.d(TAG, String.format("Hour %d: Temp=%.1f°C, Icon=%s, Pop=%.0f%%, Time=%s", i, temp, icon, pop * 100, time));

                View hourlyView = LayoutInflater.from(context).inflate(R.layout.hourly_item, hourlyLayout, false);

                TextView hourlyTempView = hourlyView.findViewById(R.id.hourly_temp);
                ImageView hourlyIconView = hourlyView.findViewById(R.id.hourly_icon);
                TextView hourlyDescriptionView = hourlyView.findViewById(R.id.hourly_precip);
                TextView hourlyTimeView = hourlyView.findViewById(R.id.hourly_time);

                hourlyTempView.setText(String.format("%.1f°", temp));
                hourlyDescriptionView.setText(String.format("%.0f%%", pop * 100));

                String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
                Log.d(TAG, "Icon URL: " + iconUrl);

                new Handler(Looper.getMainLooper()).post(() -> {
                    Glide.with(context)
                            .load(iconUrl)
                            .into(hourlyIconView);
                });

                hourlyTimeView.setText(time);

                hourlyLayout.addView(hourlyView);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing error in parseWeatherData: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in parseWeatherData: " + e.getMessage(), e);
        }
    }

    private static void parseWeatherData2(JSONObject jsonObject, View currentView) {
        try {
            Log.d(TAG, "Parsing weather data 2.");
            TextView tempView = currentView.findViewById(R.id.temperature);
            TextView descView = currentView.findViewById(R.id.weather_description);
            TextView additionalInfoView = currentView.findViewById(R.id.additional_info);
            TextView locationDateView = currentView.findViewById(R.id.location_date);

            double temp = jsonObject.getJSONObject("current_weather").getJSONObject("main").getDouble("temp") - 273.15;
            String temp2 = String.format("%.0f", temp); // Kelvin to Celsius
            String tem3 = temp2.replace(",", "."); // Ersetze Komma durch Punkt
            textToSpeechHelper.speak("Die aktuelle Temperatur beträgt " + tem3 + " Grad Celsius.");
            String description = jsonObject.getJSONObject("current_weather").getJSONArray("weather").getJSONObject(0).getString("description");
            double tempMin = jsonObject.getJSONObject("current_weather").getJSONObject("main").getDouble("temp_min") - 273.15;
            double tempMax = jsonObject.getJSONObject("current_weather").getJSONObject("main").getDouble("temp_max") - 273.15;
            double feelsLike = jsonObject.getJSONObject("current_weather").getJSONObject("main").getDouble("feels_like") - 273.15;
            long timestamp = jsonObject.getJSONObject("current_weather").getLong("dt");

            Date date = new Date(timestamp * 1000L);
            String dayOfWeek = new SimpleDateFormat("EEEE", Locale.GERMAN).format(date);
            String location = jsonObject.getJSONObject("current_weather").getString("name");

            Log.d(TAG, String.format("Temp=%.1f°C, Description=%s, TempMin=%.1f°C, TempMax=%.1f°C, FeelsLike=%.1f°C, DayOfWeek=%s, Location=%s",
                    temp, description, tempMin, tempMax, feelsLike, dayOfWeek, location));

            tempView.setText(String.format("%.1f°C", temp));
            descView.setText(description);
            additionalInfoView.setText(String.format("Höchstwert: %.1f° Tiefstwert: %.1f° Gefühlt: %.1f°", tempMax, tempMin, feelsLike));
            locationDateView.setText(String.format("%s · %s", dayOfWeek, location));
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing error in parseWeatherData2: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in parseWeatherData2: " + e.getMessage(), e);
        }
    }


    private static void parseAlarmData(JSONObject jsonObject, View alarmView, View overlayView) throws JSONException {

        if (jsonObject.has("time") && !jsonObject.getString("time").isEmpty() && !jsonObject.has("error")) {
            OffsetDateTime alarmTime = OffsetDateTime.parse(jsonObject.getString("time"));

            Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, alarmTime.getHour());
            alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.getMinute());
            alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Alarm gesetzt durch Lumi");

            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(alarmIntent);
        } else if (jsonObject.has("error") && "No time specified".equals(jsonObject.getString("error"))) {
            TextView alarm_text = alarmView.findViewById(R.id.view_alarm_text);
            alarm_text.setText("Wann soll der Wecker gestellt werden?");
            textToSpeechHelper.speak("Wann soll der Wecker gestellt werden?");
            overlayView.findViewById(R.id.message_text).setVisibility(View.GONE);

            Log.d(TAG, "recognizedText nach 'notime': " + OverlayActivity.getRecognizedText());
        } else {
            Log.e(TAG, "Keine Alarmzeit angegeben.");
        }
    }


    private static void parseCall(JSONObject jsonObject, View callView) throws JSONException {
        // Telefonnummer im Uri-Format erstellen
        String contactName = jsonObject.getString("name");
        String phoneNumber = findContactPhoneNumber(contactName, callView);
        Log.d(TAG, "Contact name: " + contactName + ", Phone number: " + phoneNumber);
        if (phoneNumber != null) {
            // Telefonnummer im Uri-Format erstellen
            Uri callUri = Uri.parse("tel:" + phoneNumber);
            Intent callIntent = new Intent(Intent.ACTION_CALL, callUri);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Anruf starten, wenn die Berechtigung erteilt wurde
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(callIntent);
            } else {
                if (context instanceof Activity) {
                    ((Activity) context).requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1);
                }
            }
        } else {
            // Keine Übereinstimmung gefunden
            System.out.println("Kein Kontakt mit dem Namen " + contactName + " gefunden.");
        }
    }

    private static String findContactPhoneNumber(String name, View callView) {
        String bestPhoneNumber = null;
        int bestDistance = Integer.MAX_VALUE;  // Startwert: maximale Distanz
        String finalContactName = "";
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                // Berechne den Levenshtein-Abstand zwischen dem übergebenen Namen und dem Kontakt
                int distance = levenshteinDistance(name.toLowerCase(), contactName.toLowerCase());

                // Wenn der aktuelle Kontakt besser passt, speichere diesen
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestPhoneNumber = contactNumber;
                    finalContactName = contactName;
                }
            }
            cursor.close();
        }
        if (bestDistance > 2 && bestPhoneNumber != null){
            callView.setVisibility(View.VISIBLE);
            TextView text = callView.findViewById(R.id.messageTextView);
            text.setText("Möchtest du " + finalContactName + " anrufen?");
            textToSpeechHelper.speak("Möchtest du " + finalContactName + " anrufen?");
            Button yesButton = callView.findViewById(R.id.yesButton);
            Button noButton = callView.findViewById(R.id.noButton);
            String finalBestPhoneNumber = bestPhoneNumber;
            yesButton.setOnClickListener(v -> {
               initiateCall(finalBestPhoneNumber);
            });
            noButton.setOnClickListener(v -> {
                callView.setVisibility(View.GONE);
            });
        }
        else {
            return bestPhoneNumber;
        }
        return null;
    }

    private static void initiateCall(String phoneNumber) {
        Uri callUri = Uri.parse("tel:" + phoneNumber);
        Intent callIntent = new Intent(Intent.ACTION_CALL, callUri);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Anruf starten, wenn die Berechtigung erteilt wurde
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(callIntent);
        } else {
            if (context instanceof Activity) {
                ((Activity) context).requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1);
            }
        }
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private static void parseChatData(JSONObject responseBody, View chatView, View overlayView) {
        try {
            overlayView.findViewById(R.id.message_text).setVisibility(View.GONE);
            LinearLayout messageContainer = chatView.findViewById(R.id.message_container);
            ScrollView scrollView = chatView.findViewById(R.id.scroll_view);
            String responseContent = responseBody.getString("content");
            textToSpeechHelper.speak(responseContent);

            File cacheFile = new File(context.getCacheDir(), "chatData.json");
            JSONArray messagesArray;

            try {
                // Wenn die Datei existiert, lese den vorhandenen JSON-Inhalt
                if (cacheFile.exists()) {
                    FileReader reader = new FileReader(cacheFile);
                    StringBuilder content = new StringBuilder();
                    int c;
                    while ((c = reader.read()) != -1) {
                        content.append((char) c);
                    }
                    reader.close();

                    JSONObject chatData = new JSONObject(content.toString());
                    messagesArray = chatData.getJSONObject("chat").getJSONArray("messages");
                } else {
                    // Andernfalls ein neues JSON-Array und ein neues Chat-Objekt erstellen
                    messagesArray = new JSONArray();
                    JSONObject firstMessage = new JSONObject();
                    firstMessage.put("role", "user");
                    firstMessage.put("content", OverlayActivity.getRecognizedText());
                    messagesArray.put(firstMessage);
                }

                // Neues JSON-Objekt mit Rolle und Inhalt erstellen
                JSONObject newMessage = new JSONObject();
                newMessage.put("role", "assistant");
                newMessage.put("content", responseContent);
                messagesArray.put(newMessage);

                // JSON-Struktur erstellen und mit den Nachrichten aktualisieren
                JSONObject chatData = new JSONObject();
                JSONObject chat = new JSONObject();
                chat.put("messages", messagesArray);
                chatData.put("chat", chat);

                // Aktualisierte Daten zurück in die Datei schreiben
                FileWriter writer = new FileWriter(cacheFile);
                writer.write(chatData.toString());
                writer.close();

                Log.d("DataManager", "Chat data updated in cache: " + chatData.toString());

                for(int i = 0; i < messagesArray.length(); i++){
                    JSONObject message = messagesArray.getJSONObject(i);
                    String role = message.getString("role");
                    String content = message.getString("content");

                    View messageView;
                    if (role.equals("user")) {
                        messageView = LayoutInflater.from(context).inflate(R.layout.user_message_layout, messageContainer, false);
                        TextView userTextView = messageView.findViewById(R.id.user_input);
                        userTextView.setText(content);
                    } else {
                        messageView = LayoutInflater.from(context).inflate(R.layout.assistant_message_layout, messageContainer, false);
                        TextView assistantTextView = messageView.findViewById(R.id.response_text);
                        assistantTextView.setText(content);
                    }
                    messageContainer.addView(messageView);
                }
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing chat data: " + e.getMessage(), e);
        }
    }

    private static  void parseApp(JSONObject responseBody) throws JSONException {
        List<String> allAppNames = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            String appName = app.loadLabel(packageManager).toString();
            String packageName = app.packageName;
            allAppNames.add(appName);
            Log.d(TAG, "App Name: " + appName + ", Package Name: " + packageName);
        }


        Intent launchIntent = packageManager.getLaunchIntentForPackage(responseBody.getString("name"));
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        } else {
            Log.e(TAG, "App not found: " + responseBody.getString("name"));
        }
    }


    private static void parseFahrplanData(JSONObject responseBody, View fahrplanView) {
        // Container, der das ExpandableListView enthalten soll
        textToSpeechHelper.speak("Hier sind deine besten Zugverbindungen.");
        ExpandableListView expandableListView = fahrplanView.findViewById(R.id.transport_expandable_list);
        TextView originText = fahrplanView.findViewById(R.id.origin_name);
        TextView destinationText = fahrplanView.findViewById(R.id.destination_name);
        if (expandableListView == null) {
            return;
        }

        try {
            if (!responseBody.has("trips")) {
                return;
            }

            JSONArray trips = responseBody.getJSONArray("trips");
            originText.setText(trips.getJSONObject(0).getString("origin"));
            destinationText.setText(trips.getJSONObject(0).getString("destination"));
            if (trips.length() == 0) {
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(fahrplanView.getContext());
            List<Map<String, String>> groupList = new ArrayList<>();
            List<List<Map<String, String>>> childList = new ArrayList<>();

            for (int i = 0; i < trips.length(); i++) {
                JSONObject trip = trips.getJSONObject(i);
                if (!trip.has("origin") || !trip.has("destination")) {
                    continue;
                }

                String departureTime = trip.optString("departure_time", "--:--");
                String arrivalTime = trip.optString("arrival_time", "--:--");
                String tripTitle = trip.getString("origin") + " → " + trip.getString("destination") +
                        " (" + departureTime.substring(0, 5) + " - " + arrivalTime.substring(0, 5) + ")";

                Map<String, String> groupMap = new HashMap<>();
                groupMap.put("tripTitle", tripTitle);
                groupList.add(groupMap);

                JSONArray products = trip.optJSONArray("products");
                if (products == null) {
                    continue;
                }

                List<Map<String, String>> children = new ArrayList<>();

                for (int j = 0; j < products.length(); j++) {
                    JSONObject product = products.getJSONObject(j);
                    Map<String, String> childMap = new HashMap<>();

                    childMap.put("departure", product.optString("departure_time", "--:--").substring(0, 5));
                    childMap.put("arrival", product.optString("arrival_time", "--:--").substring(0, 5));

                    String travelTime = product.optString("travel_time", "0");
                    int travelMinutes = Integer.parseInt(travelTime.replaceAll("[^0-9]", ""));
                    if (travelMinutes >= 60) {
                        int hours = travelMinutes / 60;
                        int minutes = travelMinutes % 60;
                        childMap.put("travel_time", hours + "h " + (minutes > 0 ? minutes + " min" : ""));
                    } else {
                        childMap.put("travel_time", travelMinutes + " min");
                    }

                    childMap.put("track", product.optString("origin_track", "N/A") + " → " + product.optString("destination_track", "N/A"));
                    childMap.put("line", product.optString("line", "N/A"));

                    String delay = product.optString("delay", "0") + " min";
                    childMap.put("delay", delay);
                    childMap.put("delayColor", delay.equals("0 min") ? "#000000" : "#FF0000"); // Rot wenn Verzögerung

                    children.add(childMap);
                }
                childList.add(children);
            }

            if (groupList.isEmpty() || childList.isEmpty()) {
                return;
            }

            SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                    fahrplanView.getContext(),
                    groupList,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[]{"tripTitle"},
                    new int[]{android.R.id.text1},
                    childList,
                    R.layout.trip_child,
                    new String[]{"departure", "arrival", "travel_time", "track", "line", "delay"},
                    new int[]{R.id.departure_time, R.id.arrival_time, R.id.travel_time, R.id.platform, R.id.line, R.id.delay}
            ) {
                @Override
                public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                    View view = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
                    TextView delayTextView = view.findViewById(R.id.delay);
                    Map<String, String> item = (Map<String, String>) getChild(groupPosition, childPosition);
                    delayTextView.setTextColor(Color.parseColor(item.get("delayColor")));
                    return view;
                }
            };

            expandableListView.setAdapter(adapter);
        } catch (JSONException e) {
        }
    }



    // im Rahmen des HCIN-Projekts hinzugefügt
    private static void parseLocationData(JSONObject responseBody, View locationView) throws Exception {
        MapView mapView = locationView.findViewById(R.id.mapView);
        if (mapView == null) {
            Log.e(TAG, "MapView nicht gefunden.");
            return;
        }

        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(googleMap -> {
            googleMap.clear();
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

            JSONArray data;
            try {
                data = responseBody.getJSONArray("data");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            SecretKey secretKey = null;
            try {
                secretKey = KeyManager.getOrCreateKey();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LinearLayout locationContainer = locationView.findViewById(R.id.locationContainer);
            locationContainer.removeAllViews(); // Liste vorher leeren

            for (int i = 0; i < data.length(); i++) {
                JSONObject location = null;
                try {
                    location = data.getJSONObject(i);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String encrypted = null;
                try {
                    encrypted = location.getString("data");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String decrypted = null;
                try {
                    decrypted = CryptoManager.decrypt(encrypted, secretKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                String[] parts = decrypted.split(", ");

                double latitude = Double.parseDouble(parts[0]);
                double longitude = Double.parseDouble(parts[1]);

                LatLng latLng = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions().position(latLng));
                boundsBuilder.include(latLng);

                // Standort-Karte dynamisch hinzufügen
                View locationCard = LayoutInflater.from(locationView.getContext())
                        .inflate(R.layout.item_location, locationContainer, false);

                TextView coordsText = locationCard.findViewById(R.id.coordsText);
                coordsText.setText("Lat: " + latitude + " | Lng: " + longitude);

                locationContainer.addView(locationCard);
            }

            if (data.length() > 0) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            }
        });
    }

    private static void parsePersonalDecryption(JSONObject responseBody, Activity activity) {
        try {
            String encryptedText = responseBody.getString("vector");
            Log.d(TAG, "Vector verschl.: " + encryptedText);

            String[] outputs = encryptedText.split(": ", 2);
            if (outputs.length != 2) {
                Log.e(TAG, "❌ Ungültiges Format für den verschlüsselten Vector (erwarte genau ein ': ')");
                return;
            }

            String firstEncrypted = outputs[0].trim();
            String secondEncrypted = outputs[1].trim();

            SecretKey secretKey = KeyManager.getOrCreateKey();
            Log.d(TAG, "🔐 SecretKey geladen: " + secretKey.toString());

            String firstClear;
            String secondClear;
            try {
                firstClear = CryptoManager.decrypt(firstEncrypted, secretKey);
            } catch (AEADBadTagException e) {
                Log.e(TAG, "❌ Fehler bei der Entschlüsselung von 'firstEncrypted': Auth-Tag ungültig (Daten manipuliert oder IV falsch?)", e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "❌ Unerwarteter Fehler bei 'firstEncrypted'", e);
                return;
            }

            try {
                secondClear = CryptoManager.decrypt(secondEncrypted, secretKey);
            } catch (AEADBadTagException e) {
                Log.e(TAG, "❌ Fehler bei der Entschlüsselung von 'secondEncrypted': Auth-Tag ungültig", e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "❌ Unerwarteter Fehler bei 'secondEncrypted'", e);
                return;
            }

            String finalOutput = firstClear + ": " + secondClear;
            Log.d(TAG, "✅ Entschlüsselt: " + finalOutput);

            String question = responseBody.getString("question");
            Log.d(TAG, "❓ Frage: " + question);

            String getParameter = "?question=" + URLEncoder.encode(question, "UTF-8").replace("+", "%20") +
                    "&context=" + URLEncoder.encode(finalOutput, "UTF-8").replace("+", "%20");

            Log.d(TAG, "🌐 URL-Parameter: " + getParameter);

            // Netzwerkanfrage im Hintergrund-Thread ausführen
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    SessionClient.getMessage(context, activity, "https://lumi-ai.at/api/generate_response", getParameter);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Fehler bei der Netzwerkanfrage", e);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Allgemeiner Fehler beim Parsen oder Entschlüsseln", e);
        }
    }






}


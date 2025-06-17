package com.example.myapplication;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.se.omapi.Session;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class OverlayActivity extends AppCompatActivity {

    private static WindowManager windowManager;
    private static View overlayView;
    private View transparentTouchView;
    private static ImageButton microphoneLumi;
    private static LottieAnimationView microphoneButton;
    private static TextView messageText;
    private static final int REQUEST_CODE = 101;
    private Activity activity = this;

    private static String recognizedText="";
    private static TextToSpeechHelper ttsHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionClient.setContext(getApplicationContext());
        ttsHelper = TextToSpeechHelper.getInstance(this);

        // Broadcast-Receiver registrieren
        IntentFilter filter = new IntentFilter("com.example.myapplication.RECORDING_STOPPED");
        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (Exception e) {
                    Log.e("OverlayActivity", "Could not launch overlay permission intent", e);
                }
            } else {
                setupOverlay();
            }
        } else {
            setupOverlay();
        }

        startForegroundService(new Intent(this, LocationService.class));

        // Kalender
//        ContentValues values = new ContentValues();
//        values.put(CalendarContract.Events.ALL_DAY, 1);
//        values.put(CalendarContract.Events.DTSTART, 1730419200000L); // 1. November 2024, 00:00 Uhr
//        values.put(CalendarContract.Events.DTEND, 1730505600000L);   // 2. November 2024, 00:00 Uhr
//        values.put(CalendarContract.Events.TITLE, "Testtermin");
//        values.put(CalendarContract.Events.DESCRIPTION, "Das ist ein Testtermin");
//        values.put(CalendarContract.Events.CALENDAR_ID, 1);
//        values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
//        values.put(CalendarContract.Events.EVENT_LOCATION, "Berlin");
//
//        getApplicationContext().getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);



    }
    private static WindowManager.LayoutParams params;
    private void setupOverlay() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            if (windowManager == null) {
                Log.e("OverlayActivity", "WindowManager is null.");
                return;
            }

            // Entferne bestehendes Overlay
            if (overlayView != null) {
                try {
                    windowManager.removeView(overlayView);
                } catch (Exception e) {
                    Log.e("OverlayActivity", "Error removing existing overlay", e);
                }
            }

            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.BOTTOM;

            WindowManager.LayoutParams touchParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT);
            touchParams.gravity = Gravity.TOP | Gravity.START;

            transparentTouchView = new View(this);

            try {
                windowManager.addView(transparentTouchView, touchParams);
                windowManager.addView(overlayView, params);
            } catch (Exception e) {
                Log.e("OverlayActivity", "Failed to add overlay views", e);
                return;
            }

            transparentTouchView.setOnTouchListener((v, event) -> {
                if (event == null) return false;
                int action = event.getActionMasked();
                int overlayHeight = overlayView != null ? overlayView.getHeight() : 0;
                int touchY = (int) event.getRawY();

                if (action == MotionEvent.ACTION_DOWN) {
                    if (overlayHeight > 0 && touchY < windowManager.getDefaultDisplay().getHeight() - overlayHeight) {
                        finish();
                        return true;
                    }
                }
                return false;
            });

            messageText = overlayView.findViewById(R.id.message_text);
            microphoneLumi = overlayView.findViewById(R.id.microphone_button);
            microphoneButton = overlayView.findViewById(R.id.microphone_animation);
            microphoneButton.setFrame(12);

            microphoneLumi.setOnClickListener(v -> {
                startRecognitionService();
                microphoneLumi.setVisibility(View.GONE);
                microphoneButton.setVisibility(View.VISIBLE);
            });

        } catch (Exception e) {
            Log.e("OverlayActivity", "Exception in setupOverlay", e);
        }
    }


    public static void updateOverlayView(View updatedOverlayView) {
        if (windowManager != null) {
            windowManager.removeView(overlayView);
            windowManager.addView(updatedOverlayView, params);
            messageText = updatedOverlayView.findViewById(R.id.message_text);
            messageText.setText(recognizedText);
            microphoneLumi = updatedOverlayView.findViewById(R.id.microphone_button);
            microphoneButton = updatedOverlayView.findViewById(R.id.microphone_animation);
            microphoneButton.setFrame(12);
            microphoneLumi.setOnClickListener(v -> {
                startRecognitionService();
                microphoneLumi.setVisibility(View.GONE);
                microphoneButton.setVisibility(View.VISIBLE);
            });
            overlayView = updatedOverlayView;
        }
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.example.myapplication.RECORDING_STOPPED")) {
                recognizedText += intent.getStringExtra("recognizedText");
                microphoneButton.cancelAnimation();
                overlayView.findViewById(R.id.loading_bar).setVisibility(View.VISIBLE);
                microphoneButton.setVisibility(View.GONE);
                messageText.setText(recognizedText);

                JSONArray messagesArray = null;
                try {
                    File cacheFile = new File(context.getCacheDir(), "chatData.json");

                    // Prüfen, ob die Datei existiert und den Inhalt lesen
                    if (cacheFile.exists()) {
                        FileReader reader = new FileReader(cacheFile);
                        StringBuilder content = new StringBuilder();
                        int c;
                        while ((c = reader.read()) != -1) {
                            content.append((char) c);
                        }
                        reader.close();

                        // JSON aus dem Dateiinhalt parsen
                        JSONObject chatData = new JSONObject(content.toString());
                        messagesArray = chatData.getJSONObject("chat").getJSONArray("messages");
                    }

                    // Log-Ausgabe des bestehenden Chat-Arrays
                    if (messagesArray != null) {
                        Log.d("OverlayActivity", "Existing ChatData: " + messagesArray.toString());

                        // Neuen JSON-Eintrag erstellen und hinzufügen
                        JSONObject newMessage = new JSONObject();
                        newMessage.put("role", "user");
                        newMessage.put("content", getRecognizedText());
                        messagesArray.put(newMessage);

                        // Aktualisierte Daten in die Datei schreiben
                        JSONObject updatedChatData = new JSONObject();
                        JSONObject chat = new JSONObject();
                        chat.put("messages", messagesArray);
                        updatedChatData.put("chat", chat);

                        FileWriter writer = new FileWriter(cacheFile);
                        writer.write(updatedChatData.toString());
                        writer.close();

                        // Neue Chat-Daten für den API-Aufruf vorbereiten und loggen
                        String newChatData = messagesArray.toString();
                        Log.d("OverlayActivity", "ChatData: " + newChatData);

                        // API-Anfrage im Hintergrund ausführen
                        new Thread(() -> {
                            try {
                                SessionClient.getMessage(getApplicationContext(), activity, "https://lumi-ai.at/api/query?message=", newChatData);
                            } catch (IOException e) {
                                Log.e("OverlayActivity", "Error sending chat data", e);
                            }
                        }).start();
                    } else {
                        new Thread(() -> {
                            try {
                                JSONArray newArray = new JSONArray();
                                JSONObject newMessage = new JSONObject();
                                newMessage.put("content", getRecognizedText());
                                newArray.put(newMessage);
                                SessionClient.getMessage(getApplicationContext(), activity,"https://lumi-ai.at/api/query?message=", newArray.toString());
                                Log.i("OverlayActivity", "Received recognized text: " + getRecognizedText());
                            } catch (IOException | JSONException e) {
                                Log.d("SessionClient", e.toString());
                            }
                        }).start();}
                    } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    };

    public static String getRecognizedText() {
        return recognizedText;
    }
    public static void setRecognizedText(String recognizedText) {
        OverlayActivity.recognizedText = recognizedText;
    }


    private static void startRecognitionService() {
        microphoneButton.playAnimation();
        Intent intent = new Intent(overlayView.getContext(), AudioRecordService.class);
        overlayView.getContext().startService(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);

            if (overlayView != null && overlayView.getWindowToken() != null) {
                windowManager.removeView(overlayView);
            }
            if (transparentTouchView != null && transparentTouchView.getWindowToken() != null) {
                windowManager.removeView(transparentTouchView);
            }
            File cacheFile = new File(getCacheDir(), "chatData.json");
            if (cacheFile.exists()) {
                boolean deleted = cacheFile.delete();
                if (deleted) {
                    Log.d("OverlayActivity", "chatData.json was successfully deleted.");
                } else {
                    Log.d("OverlayActivity", "chatData.json could not be deleted.");
                }
            }
            // Stop the AudioRecordService
            Intent stopServiceIntent = new Intent(this, AudioRecordService.class);
            stopService(stopServiceIntent);
        } catch (Exception e) {
            Log.e("OverlayActivity", "Error during onDestroy", e);
        }
    }


}

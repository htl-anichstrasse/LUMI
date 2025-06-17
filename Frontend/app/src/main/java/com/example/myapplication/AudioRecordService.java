package com.example.myapplication;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioRecordService extends Service {
    private static final String TAG = "AudioRecordService";
    private SpeechRecognizer speechRecognizer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Spracheingabe überbrücken
//        Intent intent = new Intent("com.example.myapplication.RECORDING_STOPPED");
//        intent.putExtra("recognizedText", "nur um Spracheingabe zu überbrücken");
//        sendBroadcast(intent);
//        stopSelf(); // Service schließen



        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d(TAG, "Sound level changed: " + rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d(TAG, "Buffer received");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
            }

            @Override
            public void onError(int error) {
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "Client error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "No speech input";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "No match found";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "RecognitionService busy";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "Insufficient permissions";
                        break;
                    default:
                        message = "Unknown error";
                        break;
                }
                Log.e(TAG, "Error: " + message);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.i(TAG, "Recognition result: " + recognizedText); // Log the recognized text
                    // Broadcast für erkannten Text
                    Intent intent = new Intent("com.example.myapplication.RECORDING_STOPPED");
                    intent.putExtra("recognizedText", recognizedText);
                    sendBroadcast(intent);
                    stopSelf(); // Service schließen
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.d(TAG, "Partial results received");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.d(TAG, "Event occurred: " + eventType);
            }
        });

        speak();
    }

    public void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            Log.d(TAG, "SpeechRecognizer destroyed");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}

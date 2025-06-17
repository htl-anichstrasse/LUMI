package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioRecordServiceAlt extends Service {
    private MediaRecorder mediaRecorder;
    private File tempFile;
    private Handler handler = new Handler();
    private static final long NO_VOICE_TIMEOUT = 1500; // Millisekunden für Timeout (2 Sekunden)
    private long lastVoiceDetectedTime; // Zeitstempel der letzten erkannten Stimme

    private Runnable stopRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaRecorder != null) {
                long currentTime = System.currentTimeMillis();
                int amplitude = mediaRecorder.getMaxAmplitude();
                Log.d("AudioRecordService", "Current amplitude: " + amplitude);

                if (amplitude <= 1000) {
                    // Keine Stimme erkannt
                    if (currentTime - lastVoiceDetectedTime >= NO_VOICE_TIMEOUT) {
                        Log.d("AudioRecordService", "No voice detected for 2 seconds; stopping recording.");
                        stopRecording();
                    } else {
                        // Timeout wieder setzen
                        handler.postDelayed(this, NO_VOICE_TIMEOUT);
                    }
                } else {
                    // Stimme erkannt, also den Zeitstempel aktualisieren
                    lastVoiceDetectedTime = currentTime;
                    Log.d("AudioRecordService", "Voice detected; resetting timer.");
                    handler.postDelayed(this, NO_VOICE_TIMEOUT);
                }
            } else {
                Log.d("AudioRecordService", "MediaRecorder is null; stopping handler callbacks.");
                handler.removeCallbacks(this);
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyAppAudio.mp4");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setAudioChannels(1); // Mono ist ausreichend
            mediaRecorder.setAudioEncodingBitRate(128000); // 128 kbps

            mediaRecorder.setOutputFile(tempFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            lastVoiceDetectedTime = System.currentTimeMillis();
            // Start the Runnable immediately
            handler.post(stopRecordingRunnable);
        } catch (IOException e) {
            Log.e("AudioRecordService", "Error starting recording", e);
            stopSelf(); // Stop the service if an error occurs
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (tempFile != null && tempFile.exists()) {
          //  uploadAudioFileWithOkHttp(tempFile, "192.168.62.100/request");
          //  tempFile.delete();
        }
        handler.removeCallbacks(stopRecordingRunnable);
        // Broadcast senden, dass Aufnahme gestoppt
        Intent intent = new Intent("com.example.myapplication.RECORDING_STOPPED");
        sendBroadcast(intent);
        stopSelf(); // Service schließen
    }

    @Override
    public void onDestroy() {
        stopRecording();
        handler.removeCallbacks(stopRecordingRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void uploadAudioFileWithOkHttp(File audioFile, String serverUrl) {
        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("audio/mp4"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audioFile", audioFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("Upload", "Error uploading file: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i("Upload", "File Upload Completed. Response: " + response.body().string());
                } else {
                    Log.e("Upload", "File Upload Failed. Response: " + response.body().string());
                }
            }
        });
    }
}

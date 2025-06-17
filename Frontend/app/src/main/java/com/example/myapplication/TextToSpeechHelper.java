package com.example.myapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TextToSpeechHelper {
    private static final String TAG = "TextToSpeechHelper";
    private static final String API_URL = "https://lumi-ai.at/api/tts"; // URL deines lokalen Servers

    private static TextToSpeechHelper instance;
    private final Context context;
    private final OkHttpClient client;

    private TextToSpeechHelper(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient();
    }

    public static synchronized TextToSpeechHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TextToSpeechHelper(context);
        }
        return instance;
    }

    public void speak(String text) {
        Log.d(TAG, "Sending TTS request for text: " + text);
        String urlWithParams = API_URL + "?text=" + text.replace(" ", "%20");

        Request request = new Request.Builder()
                .url(urlWithParams)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "TTS request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "TTS response not successful: " + response.code());
                    return;
                }

                if (response.body() != null) {
                    playMp3Stream(response.body().byteStream());
                } else {
                    Log.e(TAG, "TTS response body is null");
                }
            }
        });
    }

    private void playMp3Stream(InputStream audioStream) {
        try {
            // Temporäre Datei speichern
            File tempMp3 = File.createTempFile("tts_audio", ".mp3", context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempMp3)) {
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // MP3 abspielen
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempMp3.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                tempMp3.delete();
                Log.d(TAG, "TTS audio playback complete. Temp file deleted.");
            });
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "Error playing MP3 stream", e);
        }
    }
}

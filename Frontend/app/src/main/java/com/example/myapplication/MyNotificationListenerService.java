package com.example.myapplication;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.crypto.SecretKey;

public class MyNotificationListenerService extends NotificationListenerService {

    Long lastpostTime;
    String lastContent;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String content = extras.getString(Notification.EXTRA_TEXT);
        String type = sbn.getNotification().category;
        Long postTime = sbn.getPostTime();
        String packageName = sbn.getPackageName();

        // Filtern von Nachrichten, um nur relevante Nachrichten zu senden
        if (isRelevantNotification(packageName, title, content)) {

            if (lastContent == null || !content.equals(lastContent) || postTime - lastpostTime > 500) {
                Log.d("NotificationListener", "Package: " + packageName);
                Log.d("NotificationListener", "Title: " + title);
                Log.d("NotificationListener", "Content: " + content);
                Log.d("NotificationListener", "Type: " + type);

                JSONObject json = new JSONObject();
                try {
                    json.put("package", packageName);
                    json.put("title", title);
                    json.put("content", content);
                    json.put("type", "message");
                    json.put("postTime", postTime / 1000);
                } catch (JSONException e) {
                    Log.e("NotificationListener", "JSON Exception: " + e.toString());
                }

                try {
                    SecretKey secretKey = KeyManager.getOrCreateKey();
                    String encryptedTitle = CryptoManager.encrypt(title, secretKey);
                    String encryptedContent = CryptoManager.encrypt(content, secretKey);
                    json.put("encrypted_title", encryptedTitle.trim());
                    json.put("encrypted_content", encryptedContent.trim());
                } catch (Exception e) {
                    Log.e("NotificationListener", "Encryption error: " + e.toString());
                }

                String jsonString = json.toString();

                lastpostTime = postTime;
                lastContent = content;

                // Senden der Nachricht an den Server
                new Thread(() -> {
                    try {
                        SessionClient.postMessage(getApplicationContext(), "https://lumi-ai.at/api/message", jsonString);
                        Log.d("NotificationListener", "Message sent to server");
                        Log.d("NotificationListener", jsonString);

                    } catch (IOException e) {
                        Log.e("SessionClient", "Error sending message: " + e.toString());
                    }
                }).start();
            }
        }
    }

    private boolean isRelevantNotification(String packageName, String title, String content) {
        if (packageName.equals("com.whatsapp") || packageName.equals("com.email")) {
            if (content != null && !content.isEmpty() && !content.matches(".*\\d+ neue Nachrichten.*") && !content.matches(".*\\d+ Nachrichten aus \\d+ Chats.*")) {
                return true;
            }
        }
        return false;
    }

}



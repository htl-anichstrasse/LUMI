package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionClient {
    private static final String LOGIN_URL = "https://lumi-ai.at/auth/login";
    private static final String REFRESH_URL = "https://lumi-ai.at/auth/refresh";
    private static final String TAG = "SessionClient";
    private static String bodyResponse;
    private static DataManager dataManager;
    private static final MediaType JSON = MediaType.get("application/json");
    private static Context context;
    public static void setContext(Context context){
        SessionClient.context = context;
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request originalRequest = chain.request();
                Response response = chain.proceed(originalRequest);

                try {
                    if (response.code() == 401) { // Token ungültig oder abgelaufen
                        Log.d(TAG, "Access Token abgelaufen. Versuche Erneuerung...");

                        // Versuche, das Token zu erneuern
                        if (context == null) {
                            Log.e(TAG, "AppContext nicht verfügbar. Token-Erneuerung abgebrochen.");
                            return response;
                        }

                        boolean tokenRefreshed = refreshAccessToken(context);
                        Log.d(TAG, "Token-Erneuerung: " + tokenRefreshed);
                        if (tokenRefreshed) {
                            // Neuen Access Token aus SharedPreferences abrufen
                            SharedPreferences sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                            String newAccessToken = sharedPreferences.getString("access_token", null);

                            if (newAccessToken != null) {
                                // Erstelle die Anfrage mit dem neuen Token neu
                                Request modifiedRequest = originalRequest.newBuilder()
                                        .header("Authorization", "Bearer " + newAccessToken)
                                        .build();

                                // Close the original response before proceeding
                                response.close();

                                return chain.proceed(modifiedRequest); // Anfrage erneut senden
                            }
                        }
                    }
                } finally {
                    // Ensure the response is always closed
                    if (!response.isSuccessful()) {
                        response.close();
                    }
                }

                return response; // Ursprüngliche Antwort zurückgeben
            })
            .build();

    public static void login(Context context, String email, String password) throws IOException {
        if (context == null) throw new IllegalArgumentException("Context darf nicht null sein.");

        String json = "{\n" +
                "  \"email\": \"" + email + "\",\n" +
                "  \"password\": \"" + password + "\"\n" +
                "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        Log.d(TAG, "Sende Login-Anfrage an " + LOGIN_URL);

        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Response Code: " + response.code());

            if (!response.isSuccessful()) {
                Log.e(TAG, "Login fehlgeschlagen: " + response.message());
                throw new IOException("Unerwarteter Code " + response);
            }

            if (response.body() != null) {
                String responseBody = response.body().string();
                Log.d(TAG, "Antwort: " + responseBody);
                parseAndStoreTokens(context, responseBody);
            } else {
                Log.w(TAG, "Antwort ist leer.");
            }
        }
    }

    public static boolean refreshAccessToken(Context context) {
        if (context == null) throw new IllegalArgumentException("Context darf nicht null sein.");

        SharedPreferences sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String refreshToken = sharedPreferences.getString("refresh_token", null);
        Log.d(TAG, "Refresh Token beim Refresh: " + refreshToken);

        if (refreshToken == null) {
            Log.e(TAG, "Refresh Token fehlt. Benutzer muss sich erneut anmelden.");
            return false;
        }

        String json = "{\n" +
                "  \"refresh_token\": \"" + refreshToken + "\"\n" +
                "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(REFRESH_URL)
                .post(body)
                .header("Authorization", "Bearer " + refreshToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.d(TAG, "Token-Erneuerung erfolgreich: " + responseBody);
                parseAndStoreTokens(context, responseBody);
                return true;
            } else {
                Log.e(TAG, "Token-Erneuerung fehlgeschlagen: " + response.message());
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Fehler bei der Token-Erneuerung.", e);
            return false;
        }
    }

    private static void parseAndStoreTokens(Context context, String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);

            String accessToken = jsonResponse.optString("access_token", null);
            String refreshToken = jsonResponse.optString("refresh_token", null);
            Log.d(TAG, "Access Token: " + accessToken);
            Log.d(TAG, "Refresh Token: " + refreshToken);

            if (accessToken != null) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token", accessToken);
                editor.apply();
                if(refreshToken != null){
                    editor.putString("refresh_token", refreshToken);
                    editor.apply();
                }


                Log.d(TAG, "Tokens gespeichert.");
            } else {
                Log.e(TAG, "Tokens fehlen in der Antwort.");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Fehler beim Parsen der JSON-Antwort.", e);
        }
    }


    public static void postMessage(Context context, String url, String content) throws IOException {
        String sessionCookie = CookieManager.getCookie(context);
        Log.d(TAG, "Abgerufener Session Cookie: " + sessionCookie);

        if (sessionCookie.isEmpty()) {
            Log.d(TAG, "Keine gültige Sitzung gefunden. Führe Login durch.");
            sessionCookie = CookieManager.getCookie(context);
            if (sessionCookie.isEmpty()) {
                Log.e(TAG, "Kein gültiges Cookie nach dem Login erhalten.");
                return;
            }
        }

        RequestBody body = RequestBody.create(content, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Cookie", sessionCookie)
                .build();

        Log.d(TAG, "Sending request to " + url);
        Log.d(TAG, "Request Header mit Cookie: " + sessionCookie);
        Log.d(TAG, "Request Body: " + content);

        try (Response response = client.newCall(request).execute()) {
            String responseCode = response.code()+ "";
            Log.d(TAG, "Response Code: " + responseCode);
            if(responseCode != "200"){
                Log.e("TestSession", "Test Code: " + responseCode);
            }

            Log.d(TAG, "Response Headers: " + response.headers());

            if (!response.isSuccessful()) {
                Log.e(TAG, "Request failed: " + response.message());
                throw new IOException("Unexpected code " + response);
            }
        } catch (IOException e) {
            Log.e(TAG, "Request failed", e);
            throw e;
        }
    }

    public static void getMessage(Context context, Activity activity, String url, String content) throws IOException {
        login(context, "john.doe@example.com", "Password123");
        String sessionCookie = CookieManager.getCookie(context);
        Log.d(TAG, "Abgerufener Session Cookie: " + sessionCookie);

        if (sessionCookie.isEmpty()) {
            Log.d(TAG, "Keine gültige Sitzung gefunden. Führe Login durch.");
            sessionCookie = CookieManager.getCookie(context);
            if (sessionCookie.isEmpty()) {
                Log.e(TAG, "Kein gültiges Cookie nach dem Login erhalten.");
                return;
            }
        }

        String finalUrl = url + content + "&longitude="+ LocationService.getLongitude() + "&latitude=" + LocationService.getLatitude();
        Log.d(TAG, "Final URL: " + finalUrl);
        Request request = new Request.Builder()
                .url(finalUrl)
                .get()
                .addHeader("Cookie", sessionCookie)
                .build();

        Log.d(TAG, "Sending GET request to " + finalUrl);
        Log.d(TAG, "Request Header mit Cookie: " + sessionCookie);

        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Response Code: " + response.code());
            // kommt weck, unten wieder auskommentieren
//            dataManager = new DataManager(context, null);
//            DataManager.manageData(activity);
            if (response.body() != null) {
                bodyResponse = response.body().string();
                Log.d(TAG, "Response Body: " + bodyResponse);

                try {
//                    JSONArray jsonArray = new JSONArray(bodyResponse);
//                    for (int i = 0; i < jsonArray.length(); i++) {
//                        JSONObject jsonObject = jsonArray.getJSONObject(i);
//                        String [] encrypted_text = jsonObject.getString("text").split(": ");
//                        String encrypted_content = encrypted_text[1].replace("\n", "").replace("\r", "");
//                        String encrypted_title = encrypted_text[0].replace("\n", "").replace("\r", "");
//
//                        try{
//                            String decrypted_title = CryptoManager.decrypt(encrypted_title, KeyManager.getOrCreateKey(context));
//                            String decrypted_content = CryptoManager.decrypt(encrypted_content, KeyManager.getOrCreateKey(context));
//                            Log.d(TAG, "Decrypted Text: " + decrypted_title + ": " + decrypted_content);
//                        }catch (Exception e){
//                            Log.e(TAG, "Error: " + e);
//                        }
//                        Log.d(TAG, "Object " + i + ": " + jsonObject);
//                    }

                    JSONObject jsonObject = new JSONObject(bodyResponse);

                    // nächste 2 Zeilen wieder kommentieren, falls hardgecodetes JSON-Objekt
                    dataManager = new DataManager(context, jsonObject);
                    DataManager.manageData(activity);
                } catch (JSONException e) {
                    Log.e(TAG, "Response Body ist kein gültiges JSON-Array", e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                Log.w(TAG, "Response Body is null");
            }

            Log.d(TAG, "Response Headers: " + response.headers());
            if (!response.isSuccessful()) {
                Log.e(TAG, "Request failed: " + response.message());
                throw new IOException("Unexpected code " + response);
            }
        } catch (IOException e) {
            Log.e(TAG, "GET request failed", e);
            //throw e;
        }
    }
}

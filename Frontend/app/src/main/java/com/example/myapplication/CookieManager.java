package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CookieManager {
    private static final String TAG = "SessionClient"; // Tag für die Logs

    private static final String PREFS_NAME = "SessionPrefs";
    private static final String COOKIE_KEY = "session_cookie";

    public static void saveCookie(Context context, String cookie) {
        Log.d(TAG, "Speichere Cookie: " + cookie);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(COOKIE_KEY, cookie);
        editor.apply();  // Verwende apply(), um asynchron zu speichern
        Log.d(TAG, "Cookie nach Speicherung: " + prefs.getString(COOKIE_KEY, "Nichts gespeichert"));
    }

    public static String getCookie(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cookie = prefs.getString(COOKIE_KEY, "");
        Log.d(TAG, "Abgerufener Cookie: " + cookie);
        return cookie;  // Falls kein Cookie vorhanden ist, wird ein leerer String zurückgegeben
    }

}

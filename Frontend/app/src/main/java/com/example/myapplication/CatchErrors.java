package com.example.myapplication;

import android.app.Application;
import android.util.Log;

public class CatchErrors extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Setze einen globalen Fehlerbehandler für alle nicht abgefangenen Ausnahmen
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("GlobalException", "Nicht abgefangene Ausnahme", throwable);

            // Beende die App kontrolliert
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }
}

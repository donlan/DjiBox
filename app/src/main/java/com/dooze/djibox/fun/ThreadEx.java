package com.dooze.djibox.fun;


import android.os.Handler;
import android.os.Looper;

public class ThreadEx {

    private final static Handler handler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(Runnable var0) {
        ThreadEx.handler.post(var0);
    }

}



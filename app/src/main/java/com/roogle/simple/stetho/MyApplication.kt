package com.roogle.simple.stetho

import android.app.Application
import com.lazy.library.logging.Logcat

class MyApplication:Application(){

    override fun onCreate() {
        super.onCreate()
        Logcat.initialize(this)
    }
}
package com.mobil80.albumapp.core

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.mobil80.albumapp.BuildConfig
import timber.log.Timber

class MyApp: Application() {

    companion object {
        private lateinit var instance: MyApp

        fun getContext(): Context {
            return instance
        }

//        var connectivityManager: ConnectivityManager? = null
//        var networkCallback: ConnectivityManager.NetworkCallback ? = null
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree()) // Logs only in debug mode
        }
    }
}
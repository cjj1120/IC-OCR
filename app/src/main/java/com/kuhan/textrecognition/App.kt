package com.kuhan.textrecognition

import android.app.Application
import android.content.Context
import android.util.Log

class App : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: App? = null

        val context: Context
            get() {
                return instance!!.applicationContext
            }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("App", "onCreate")
    }
}
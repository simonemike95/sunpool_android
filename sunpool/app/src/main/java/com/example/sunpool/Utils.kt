package com.example.sunpool

import android.content.Context
import android.content.SharedPreferences

class Utils {
//    lateinit var context: Context = context.applicationContext
//        set(value) { field = value!!.applicationContext }

    fun loadSharedPrefs(context: Context): SharedPreferences? {
        return context.getSharedPreferences("SUNPOOL_PREFS", Context.MODE_PRIVATE)
    }

    fun loadSavedKey(context: Context): String? {
        val prefs = context.getSharedPreferences("SUNPOOL_PREFS", Context.MODE_PRIVATE)
        return prefs?.getString("PUBLIC_KEY", null)
    }
}


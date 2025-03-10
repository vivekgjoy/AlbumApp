package com.mobil80.albumapp.presentation.helper

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast

object Functions {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    fun showToast(context: Context){
        Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show()
    }

}
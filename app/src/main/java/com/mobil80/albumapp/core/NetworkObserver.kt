package com.mobil80.albumapp.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetAvailable = MutableStateFlow(false)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isInternetAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isInternetAvailable.value = false
        }
    }

    init {
        // Register the network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Check initial network state
        _isInternetAvailable.value = isNetworkAvailable()
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
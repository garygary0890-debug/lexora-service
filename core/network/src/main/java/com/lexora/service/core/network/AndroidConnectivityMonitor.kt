package com.lexora.service.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class AndroidConnectivityMonitor(context: Context) {
    private val connectivity = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isOnline(): Boolean = connectivity.activeNetwork
        ?.let(connectivity::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

    fun register(onChanged: (Boolean) -> Unit): AutoCloseable {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChanged(isOnline())
            override fun onLost(network: Network) = onChanged(isOnline())
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                onChanged(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onChanged(isOnline())
        return AutoCloseable { runCatching { connectivity.unregisterNetworkCallback(callback) } }
    }
}

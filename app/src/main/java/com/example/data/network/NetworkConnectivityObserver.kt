package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
    ONLINE_HIGH_SPEED, // 4G / 5G / High-Speed Wi-Fi
    ONLINE_LOW_DATA,   // 2G / 3G / Slow Connection (Lite Mode)
    OFFLINE_LOCAL      // Zero Internet / Local Ground Hotspot
}

class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkCurrentStatus(network))
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.OFFLINE_LOCAL)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (hasInternet && isValidated) {
                    val downSpeed = networkCapabilities.linkDownstreamBandwidthKbps
                    // If speed is very low (< 300 kbps), mark as Low Data / Slow Net
                    val status = if (downSpeed in 1..300) {
                        NetworkStatus.ONLINE_LOW_DATA
                    } else {
                        NetworkStatus.ONLINE_HIGH_SPEED
                    }
                    trySend(status)
                } else {
                    trySend(NetworkStatus.OFFLINE_LOCAL)
                }
            }
        }

        // Send initial state
        trySend(getCurrentStatus())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    fun getCurrentStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkStatus.OFFLINE_LOCAL
        return checkCurrentStatus(activeNetwork)
    }

    private fun checkCurrentStatus(network: Network): NetworkStatus {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkStatus.OFFLINE_LOCAL
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return NetworkStatus.OFFLINE_LOCAL

        val downSpeed = capabilities.linkDownstreamBandwidthKbps
        return if (downSpeed in 1..300) {
            NetworkStatus.ONLINE_LOW_DATA
        } else {
            NetworkStatus.ONLINE_HIGH_SPEED
        }
    }
}

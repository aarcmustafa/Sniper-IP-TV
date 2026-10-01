package com.stitten.stitteniptv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkMonitor {

    data class NetworkState(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val isEthernet: Boolean,
        val downKbps: Int
    )

    fun observe(context: Context): Flow<NetworkState> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: run {
            trySend(NetworkState(false, false, false, 0))
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readState(cm))
            }

            override fun onLost(network: Network) {
                trySend(NetworkState(false, false, false, 0))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) {
                trySend(readState(cm))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)
        trySend(readState(cm))

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun readState(cm: ConnectivityManager): NetworkState {
        val network = cm.activeNetwork
            ?: return NetworkState(false, false, false, 0)
        val caps = cm.getNetworkCapabilities(network)
            ?: return NetworkState(false, false, false, 0)

        return NetworkState(
            isConnected = caps.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ),
            isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            downKbps = caps.linkDownstreamBandwidthKbps
        )
    }
}

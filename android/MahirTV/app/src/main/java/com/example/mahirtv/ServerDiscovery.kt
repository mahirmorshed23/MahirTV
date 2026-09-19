package com.example.mahirtv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class ServerDiscovery(
    context: Context,
    private val onServerFound: (String) -> Unit
) {

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var serverFound = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(serviceType: String) {
            Log.d("MahirTV", "Discovery started")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {

            Log.d(
                "MahirTV",
                "Service found: ${serviceInfo.serviceName}"
            )

            // Ignore services after we have already found MahirTV
            if (serverFound) {
                return
            }

            if (serviceInfo.serviceType == "_mahir-tv._tcp.") {

                Log.d(
                    "MahirTV",
                    "Resolving MahirTV server..."
                )

                nsdManager.resolveService(
                    serviceInfo,
                    resolveListener
                )
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {

            Log.d(
                "MahirTV",
                "Service lost: ${serviceInfo.serviceName}"
            )

            if (serviceInfo.serviceType == "_mahir-tv._tcp.") {
                serverFound = false
            }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d("MahirTV", "Discovery stopped")
        }

        override fun onStartDiscoveryFailed(
            serviceType: String,
            errorCode: Int
        ) {
            Log.e(
                "MahirTV",
                "Discovery failed: $errorCode"
            )

            serverFound = false

            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(
            serviceType: String,
            errorCode: Int
        ) {
            Log.e(
                "MahirTV",
                "Stop discovery failed: $errorCode"
            )
        }
    }

    private val resolveListener =
        object : NsdManager.ResolveListener {

            override fun onResolveFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {
                Log.e(
                    "MahirTV",
                    "Resolve failed: $errorCode"
                )
            }

            override fun onServiceResolved(
                serviceInfo: NsdServiceInfo
            ) {

                // Another resolve may have completed before this one
                if (serverFound) {
                    return
                }

                val host = serviceInfo.host
                val port = serviceInfo.port

                val serverUrl =
                    "http://${host.hostAddress}:$port"

                serverFound = true

                Log.d(
                    "MahirTV",
                    "Server resolved: $serverUrl"
                )

                onServerFound(serverUrl)
            }
        }

    fun start() {

        serverFound = false

        nsdManager.discoverServices(
            "_mahir-tv._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stop() {

        try {
            nsdManager.stopServiceDiscovery(
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(
                "MahirTV",
                "Error stopping discovery",
                e
            )
        }
    }
}
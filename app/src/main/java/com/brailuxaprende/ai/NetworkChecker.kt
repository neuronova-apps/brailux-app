package com.brailuxaprende.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun interface NetworkChecker {
    fun isConnected(): Boolean
}

class AndroidNetworkChecker(private val context: Context) : NetworkChecker {
    override fun isConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return hasInternet && isValidated
    }
}

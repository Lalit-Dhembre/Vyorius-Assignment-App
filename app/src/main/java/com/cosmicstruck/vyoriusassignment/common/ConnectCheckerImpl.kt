package com.cosmicstruck.vyoriusassignment.common

import android.util.Log
import com.pedro.common.ConnectChecker
import javax.inject.Inject

class ConnectCheckerImpl @Inject constructor() : ConnectChecker {
    override fun onConnectionStarted(url: String) {
        Log.d("RTSP", "Connection started: ${url}")
    }

    override fun onConnectionSuccess() {
        Log.d("RTSP", "Connection success")
    }

    override fun onConnectionFailed(reason: String) {
        Log.e("RTSP", "Connection failed: $reason")
    }

    override fun onDisconnect() {
        Log.d("RTSP", "Disconnected")
    }

    override fun onAuthError() {
        Log.e("RTSP", "Auth error")
    }

    override fun onAuthSuccess() {
        Log.d("RTSP", "Auth success")
    }


}

package com.cosmicstruck.vyoriusassignment.streamingScreen

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import android.view.SurfaceView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.common.ConnectChecker
import com.pedro.library.rtsp.RtspCamera1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamingScreenViewModel @Inject constructor(
    val application: Application,
    private val connectionChecker: ConnectChecker,
): ViewModel(){
    var isStreaming =  mutableStateOf(false)
    var rtspUrl = mutableStateOf("")
    lateinit var rtspCamera1: RtspCamera1



    fun setupRTSPServer(
        surfaceView: SurfaceView,
    ){
        viewModelScope.launch {
            val context = application.applicationContext
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            rtspUrl.value = "rtsp://$ipAddress:1935/stream"
            connectionChecker.onConnectionStarted(rtspUrl.value)
            rtspCamera1 = RtspCamera1(surfaceView,connectionChecker)
            val success = rtspCamera1.prepareVideo(1280, 720, 30, 2000 * 1024, 0)
            if (success) {
                rtspCamera1.prepareAudio()
                rtspCamera1.startPreview() // required for visual output in some versions
            } else {
                Log.e("RTSP", "Failed to prepare video.")
            }
        }

    }

    fun startStream(){
        if (!rtspCamera1.isStreaming) {
            if (rtspCamera1.isRecording ||
                (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo())) {
                rtspCamera1.startStream(rtspUrl.value)
            }
        }else{
            Log.d("CHECKING STREAM","STREAMED SUCCESS")
        }
    }
    private fun stopStream() {
        if (rtspCamera1.isStreaming) {
            rtspCamera1.stopStream()
        }
    }
}
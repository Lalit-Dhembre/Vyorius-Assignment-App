package com.cosmicstruck.vyoriusassignment.streamingScreen

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.library.view.AutoFitTextureView
import com.pedro.rtspserver.RtspServerCamera1
import com.pedro.rtspserver.server.ClientListener
import com.pedro.rtspserver.server.ServerClient
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StreamingActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // The TextureView is created here and owned by the ViewModel
    val textureView = AutoFitTextureView(context)
    private var rtspServerCamera1: RtspServerCamera1? = null

    // Private mutable state flows
    private val _isStreaming = MutableStateFlow(false)
    private val _isRecording = MutableStateFlow(false)
    private val _rtspUrl = MutableStateFlow("")
    private val _cameraError = MutableStateFlow<String?>(null)
    private val _isInitialized = MutableStateFlow(false)

    // Public immutable state flows for the UI to observe
    val isStreaming = _isStreaming.asStateFlow()
    val isRecording = _isRecording.asStateFlow()
    val rtspUrl = _rtspUrl.asStateFlow()
    val cameraError = _cameraError.asStateFlow()
    val isInitialized = _isInitialized.asStateFlow()

    // Camera state management
    private var isCameraReady = false
    private val cameraLock = Mutex()
    private var isAudioPrepared = false
    private var initializationAttempts = 0
    private val maxInitAttempts = 3

    init {
        initializeCamera()
    }

    private fun initializeCamera() {
        viewModelScope.launch {
            try {
                initializationAttempts++

                if (initializationAttempts > maxInitAttempts) {
                    _cameraError.value = "Failed to initialize camera after $maxInitAttempts attempts. Virtual camera service may be restricted on this device."
                    _isInitialized.value = false
                    return@launch
                }

                // Add delay to allow system services to be ready
                delay(1000)

                try {
                    rtspServerCamera1 = RtspServerCamera1(textureView, createConnectChecker(), 1935)
                    rtspServerCamera1?.streamClient?.setClientListener(createClientListener())

                    _isInitialized.value = true
                    _cameraError.value = null

                    // Set up camera error handling
                    setupCameraErrorHandling()

                    showToast("Camera initialized successfully")

                } catch (e: Exception) {
                    handleInitializationError(e)
                }

            } catch (e: Exception) {
                handleInitializationError(e)
            }
        }
    }

    private fun handleInitializationError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("virtual camera service") == true ||
                    e.message?.contains("disallowed by service restrictions") == true -> {
                "Virtual camera service is restricted on this device. Try using a different camera app or check device permissions."
            }
            e.message?.contains("Camera") == true -> {
                "Camera initialization failed. Please ensure camera permissions are granted and camera is not being used by another app."
            }
            else -> "Camera service error: ${e.message}"
        }

        _cameraError.value = errorMessage
        _isInitialized.value = false

        // Retry initialization after delay
        if (initializationAttempts < maxInitAttempts) {
            viewModelScope.launch {
                delay(2000)
                initializeCamera()
            }
        }
    }

    private fun setupCameraErrorHandling() {
        viewModelScope.launch {
            delay(500)
            isCameraReady = true
        }
    }

    // --- Public Control Functions (Called from the UI) ---

    fun startPreview() {
        if (!_isInitialized.value || rtspServerCamera1 == null) {
            showToast("Camera not initialized. Please wait or try reset.")
            return
        }

        viewModelScope.launch {
            try {
                cameraLock.withLock {
                    if (!rtspServerCamera1!!.isOnPreview && isCameraReady) {
                        try {
                            rtspServerCamera1!!.startPreview()
                            _cameraError.value = null
                        } catch (e: Exception) {
                            handlePreviewError(e)
                        }
                    }
                }
            } catch (e: Exception) {
                _cameraError.value = "Camera preview error: ${e.message}"
                showToast("Error starting preview: ${e.message}")
            }
        }
    }

    private suspend fun handlePreviewError(e: Exception) {
        when {
            e.message?.contains("NullPointerException") == true ||
                    e.message?.contains("Parameters") == true -> {
                // Handle Xiaomi camera parameters issue
                var retryCount = 0
                while (retryCount < 3) {
                    delay(1000) // Wait outside the lock
                    try {
                        cameraLock.withLock {
                            // Try to reinitialize camera parameters
                            if (!rtspServerCamera1!!.isOnPreview) {
                                rtspServerCamera1!!.startPreview()
                                _cameraError.value = null
                                return
                            }
                        }
                        break
                    } catch (retryException: Exception) {
                        retryCount++
                        if (retryCount >= 3) {
                            _cameraError.value = "Camera parameters error - device may have compatibility issues"
                            showToast("Camera preview failed after retries. Try switching camera or reset.")
                        }
                    }
                }
            }
            e.message?.contains("remote stream not started") == true -> {
                _cameraError.value = "Remote camera stream not available"
                showToast("Remote camera features not supported on this device")
            }
            else -> {
                _cameraError.value = "Preview start failed: ${e.message}"
                showToast("Camera preview failed: ${e.message}")
            }
        }
    }

    fun stopPreview() {
        if (!_isInitialized.value || rtspServerCamera1 == null) return

        viewModelScope.launch {
            try {
                cameraLock.withLock {
                    if (rtspServerCamera1!!.isOnPreview) {
                        rtspServerCamera1!!.stopPreview()
                    }
                }
            } catch (e: Exception) {
                showToast("Error stopping preview: ${e.message}")
            }
        }
    }

    private fun prepareAudioStreamSafely(): Boolean {
        if (!_isInitialized.value || rtspServerCamera1 == null) return false

        return try {
            if (isAudioPrepared) return true

            try {
                val result = rtspServerCamera1!!.prepareAudio()
                if (result) {
                    isAudioPrepared = true
                    return true
                }
            } catch (e: AbstractMethodError) {
                showToast("Audio not supported on this device - using video only")
            } catch (e: Exception) {
                // Handle other audio preparation errors
            }

            // Fallback to video-only mode
            showToast("Proceeding with video-only mode")
            isAudioPrepared = true
            return true

        } catch (e: Exception) {
            showToast("Audio preparation failed: ${e.message}")
            isAudioPrepared = false
            return false
        }
    }

    private fun prepareAudioWithReflection(): Boolean {
        if (!_isInitialized.value || rtspServerCamera1 == null) return false

        return try {
            val clazz = rtspServerCamera1!!.javaClass

            try {
                val method = clazz.getMethod("prepareAudio")
                val result = method.invoke(rtspServerCamera1) as Boolean
                if (result) {
                    isAudioPrepared = true
                    return true
                }
            } catch (e: NoSuchMethodException) {
                // Method doesn't exist, continue to next approach
            } catch (e: Exception) {
                // Other reflection errors
            }

            showToast("Using video-only mode")
            isAudioPrepared = true
            return true

        } catch (e: Exception) {
            showToast("Audio setup failed: ${e.message}")
            return false
        }
    }

    private fun prepareVideoStreamSafely(): Boolean {
        if (!_isInitialized.value || rtspServerCamera1 == null) return false

        return try {
            if (!isCameraReady) {
                showToast("Camera not ready yet, please wait...")
                return false
            }

            runBlocking {
                cameraLock.withLock {
                    var retryCount = 0
                    while (retryCount < 3) {
                        try {
                            return@runBlocking rtspServerCamera1!!.prepareVideo()
                        } catch (e: NullPointerException) {
                            retryCount++
                            if (retryCount >= 3) {
                                showToast("Camera parameters error - device compatibility issue")
                                return@runBlocking false
                            }
                        } catch (e: Exception) {
                            showToast("Video preparation error: ${e.message}")
                            return@runBlocking false
                        }
                    }
                    false
                }
            }
        } catch (e: Exception) {
            showToast("Video preparation failed: ${e.message}")
            false
        }
    }

    fun toggleStreaming() {
        if (!_isInitialized.value || rtspServerCamera1 == null) {
            showToast("Camera not initialized. Please wait or try reset.")
            return
        }

        viewModelScope.launch {
            try {
                if (_isStreaming.value) {
                    cameraLock.withLock {
                        rtspServerCamera1!!.stopStream()
                    }
                } else {
                    if (!isCameraReady) {
                        showToast("Camera not ready yet, please wait...")
                        return@launch
                    }

                    val audioReady = prepareAudioStreamSafely() || prepareAudioWithReflection()
                    val videoReady = prepareVideoStreamSafely()

                    if (videoReady) {
                        if (!audioReady) {
                            showToast("Starting video-only stream (audio unavailable)")
                        }

                        cameraLock.withLock {
                            rtspServerCamera1!!.startStream()
                            _rtspUrl.value = rtspServerCamera1!!.streamClient.getEndPointConnection()
                        }
                    } else {
                        showToast("Error preparing video stream")
                    }
                }
            } catch (e: AbstractMethodError) {
                showToast("Library compatibility issue - trying video-only mode")
                try {
                    if (prepareVideoStreamSafely()) {
                        cameraLock.withLock {
                            rtspServerCamera1!!.startStream()
                            _rtspUrl.value = rtspServerCamera1!!.streamClient.getEndPointConnection()
                        }
                    }
                } catch (e2: Exception) {
                    _isStreaming.value = false
                    _rtspUrl.value = ""
                    showToast("Failed to start video-only stream: ${e2.message}")
                }
            } catch (e: Exception) {
                showToast("Streaming error: ${e.message}")
                _isStreaming.value = false
                _rtspUrl.value = ""
            }
        }
    }

    fun toggleRecording() {
        if (!_isInitialized.value || rtspServerCamera1 == null) {
            showToast("Camera not initialized. Please wait or try reset.")
            return
        }

        viewModelScope.launch {
            try {
                if (_isRecording.value) {
                    cameraLock.withLock {
                        rtspServerCamera1!!.stopRecord()
                    }
                    _isRecording.value = false
                } else {
                    if (!isCameraReady) {
                        showToast("Camera not ready yet, please wait...")
                        return@launch
                    }

                    val audioReady = prepareAudioStreamSafely() || prepareAudioWithReflection()
                    val videoReady = prepareVideoStreamSafely()

                    if (videoReady) {
                        if (!audioReady) {
                            showToast("Starting video-only recording (audio unavailable)")
                        }

                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val recordPath = "${context.getExternalFilesDir(null)?.absolutePath}/${sdf.format(Date())}.mp4"

                        cameraLock.withLock {
                            rtspServerCamera1!!.startRecord(recordPath)
                        }
                        _isRecording.value = true
                    } else {
                        showToast("Error preparing recording")
                    }
                }
            } catch (e: AbstractMethodError) {
                showToast("Library compatibility issue - trying video-only recording")
                try {
                    if (prepareVideoStreamSafely()) {
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val recordPath = "${context.getExternalFilesDir(null)?.absolutePath}/${sdf.format(Date())}.mp4"

                        cameraLock.withLock {
                            rtspServerCamera1!!.startRecord(recordPath)
                        }
                        _isRecording.value = true
                    }
                } catch (e2: Exception) {
                    _isRecording.value = false
                    showToast("Failed to start video-only recording: ${e2.message}")
                }
            } catch (e: Exception) {
                showToast("Recording error: ${e.message}")
                _isRecording.value = false
            }
        }
    }

    fun switchCamera() {
        if (!_isInitialized.value || rtspServerCamera1 == null) {
            showToast("Camera not initialized. Please wait or try reset.")
            return
        }

        viewModelScope.launch {
            try {
                val wasStreaming = _isStreaming.value
                val wasRecording = _isRecording.value

                cameraLock.withLock {
                    if (wasStreaming) rtspServerCamera1!!.stopStream()
                    if (wasRecording) rtspServerCamera1!!.stopRecord()
                    if (rtspServerCamera1!!.isOnPreview) rtspServerCamera1!!.stopPreview()
                }

                isCameraReady = false
                isAudioPrepared = false

                delay(500)

                cameraLock.withLock {
                    rtspServerCamera1!!.switchCamera()
                }

                delay(1000)
                isCameraReady = true

                startPreview()

                if (wasStreaming) {
                    delay(500)
                    toggleStreaming()
                }
                if (wasRecording) {
                    delay(500)
                    toggleRecording()
                }

                _cameraError.value = null
                showToast("Camera switched successfully")

            } catch (e: CameraOpenException) {
                _cameraError.value = "Camera switch failed: ${e.message}"
                showToast(e.message ?: "Failed to switch camera")
            } catch (e: Exception) {
                _cameraError.value = "Camera switch error: ${e.message}"
                showToast("Camera switch error: ${e.message}")
            }
        }
    }

    fun resetCamera() {
        viewModelScope.launch {
            try {
                rtspServerCamera1?.let { camera ->
                    cameraLock.withLock {
                        if (_isStreaming.value) camera.stopStream()
                        if (_isRecording.value) camera.stopRecord()
                        if (camera.isOnPreview) camera.stopPreview()
                    }
                }

                _isStreaming.value = false
                _isRecording.value = false
                _rtspUrl.value = ""
                isCameraReady = false
                isAudioPrepared = false
                _isInitialized.value = false

                delay(1000)

                // Reinitialize camera completely
                rtspServerCamera1 = null
                initializationAttempts = 0
                initializeCamera()

            } catch (e: Exception) {
                _cameraError.value = "Camera reset failed: ${e.message}"
                showToast("Reset failed: ${e.message}")
            }
        }
    }

    // Force reinitialize - more aggressive reset
    fun forceReinitialize() {
        viewModelScope.launch {
            try {
                // Clean up completely
                rtspServerCamera1 = null
                _isStreaming.value = false
                _isRecording.value = false
                _rtspUrl.value = ""
                _isInitialized.value = false
                isCameraReady = false
                isAudioPrepared = false
                initializationAttempts = 0

                delay(2000) // Longer delay for complete cleanup

                showToast("Force reinitializing camera...")
                initializeCamera()

            } catch (e: Exception) {
                _cameraError.value = "Force reinitialization failed: ${e.message}"
                showToast("Force reinitialization failed: ${e.message}")
            }
        }
    }

    // --- Listeners for RtspServerCamera1 ---

    private fun createConnectChecker(): ConnectChecker {
        return object : ConnectChecker {
            override fun onConnectionStarted(url: String) {
                showToast("Connection starting...")
            }

            override fun onConnectionSuccess() {
                showToast("Connection Success")
                _isStreaming.value = true
                _cameraError.value = null
            }

            override fun onConnectionFailed(reason: String) {
                showToast("Connection Failed: $reason")
                viewModelScope.launch {
                    try {
                        rtspServerCamera1?.let { camera ->
                            cameraLock.withLock {
                                camera.stopStream()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                    _isStreaming.value = false
                    _rtspUrl.value = ""
                }
            }

            override fun onDisconnect() {
                showToast("Disconnected")
                _isStreaming.value = false
                _rtspUrl.value = ""
            }

            override fun onAuthError() {
                showToast("Auth Error")
                _isStreaming.value = false
            }

            override fun onAuthSuccess() {
                showToast("Auth Success")
            }

            override fun onNewBitrate(bitrate: Long) {
                // Handle bitrate changes if needed
            }
        }
    }

    private fun createClientListener(): ClientListener {
        return object : ClientListener {
            override fun onClientConnected(client: ServerClient) {
                showToast("Client connected: ${client.clientAddress}")
            }
            override fun onClientDisconnected(client: ServerClient) {
                showToast("Client disconnected: ${client.clientAddress}")
            }
        }
    }

    // --- Resource Cleanup ---

    override fun onCleared() {
        super.onCleared()
        try {
            runBlocking {
                rtspServerCamera1?.let { camera ->
                    cameraLock.withLock {
                        if (camera.isRecording) camera.stopRecord()
                        if (camera.isStreaming) camera.stopStream()
                        if (camera.isOnPreview) camera.stopPreview()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
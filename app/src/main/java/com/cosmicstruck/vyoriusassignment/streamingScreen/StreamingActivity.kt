package com.cosmicstruck.vyoriusassignment.streamingScreen

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmicstruck.vyoriusassignment.R
import com.cosmicstruck.vyoriusassignment.ui.theme.VyoriusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StreamingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VyoriusTheme {
                RtspCameraScreen()
            }
        }
    }
}

@Composable
fun RtspCameraScreen() {
    val viewModel: StreamingActivityViewModel = hiltViewModel()
    CameraStreamingContent(viewModel = viewModel)
}

@Composable
fun CameraStreamingContent(viewModel: StreamingActivityViewModel) {
    // Collect state from the ViewModel with lifecycle awareness
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val rtspUrl by viewModel.rtspUrl.collectAsStateWithLifecycle()
    val cameraError by viewModel.cameraError.collectAsStateWithLifecycle()
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = {
                viewModel.textureView.apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (isInitialized) {
                                    viewModel.startPreview()
                                }
                            }, 500) // Increased delay for initialization
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {
                            // Handle size changes if needed
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            if (isInitialized) {
                                viewModel.stopPreview()
                            }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            // Handle texture updates if needed
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Initialization overlay
        if (!isInitialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Initializing Camera...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (cameraError != null) {
                        Text(
                            text = "Having trouble? This may take a moment...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }

        // Show RTSP URL when streaming
        if (isStreaming && rtspUrl.isNotEmpty() && isInitialized) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "RTSP URL:\n$rtspUrl",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Show camera error if any
        cameraError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Error:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = error,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Error-specific action buttons
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (error.contains("virtual camera service") ||
                            error.contains("restricted") ||
                            error.contains("initialization failed")) {
                            Button(
                                onClick = { viewModel.forceReinitialize() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Force Reset",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.resetCamera() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Reset Camera",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Control buttons at the bottom (only show when initialized)
        if (isInitialized) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Switch Camera Button
                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_cameraswitch_24),
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Streaming Button
                IconButton(
                    onClick = { viewModel.toggleStreaming() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isStreaming) Color.Red.copy(alpha = 0.8f) else Color.Green.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isStreaming) "Stop Stream" else "Start Stream",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Recording Button
                IconButton(
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Reset Camera Button
                IconButton(
                    onClick = { viewModel.resetCamera() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.Red.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Force Reset Button (for severe issues)
                IconButton(
                    onClick = { viewModel.forceReinitialize() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.Red.copy(alpha = 0.6f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Force Reset",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Status indicators (top left)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Initialization status
            if (!isInitialized) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = "INITIALIZING",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isStreaming && isInitialized) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Blinking red dot for streaming indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STREAMING",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isRecording && isInitialized) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RECORDING",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Device-specific help message for virtual camera service issues
        if (cameraError?.contains("virtual camera service") == true ||
            cameraError?.contains("disallowed by service restrictions") == true) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Blue.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Device Info:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "This device has restricted camera services. Try:\n" +
                                "• Grant all camera permissions\n" +
                                "• Close other camera apps\n" +
                                "• Restart the device\n" +
                                "• Use Force Reset button",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
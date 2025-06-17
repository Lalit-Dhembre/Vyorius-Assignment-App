package com.cosmicstruck.vyoriusassignment.streamingScreen

import android.view.SurfaceView
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun StreamingScreen(
    viewModel: StreamingScreenViewModel = hiltViewModel<StreamingScreenViewModel>(),
    modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceView = remember { mutableStateOf<SurfaceView?>(null) }

    LaunchedEffect(surfaceView.value) {
        surfaceView.value?.let { view ->
            viewModel.setupRTSPServer(view)
            delay(500) // Give time to prepare video
            viewModel.startStream()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    surfaceView.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = viewModel.rtspUrl.value,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }
}
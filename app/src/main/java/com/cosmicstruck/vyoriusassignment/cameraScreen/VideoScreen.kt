package com.cosmicstruck.vyoriusassignment.cameraScreen

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cosmicstruck.vyoriusassignment.R
import com.cosmicstruck.vyoriusassignment.ui.theme.VyoriusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@AndroidEntryPoint
class VideoScreenActivity : ComponentActivity() {
    private var isInPipMode by mutableStateOf(false)
    private lateinit var receiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            VyoriusTheme {
                val rtspUrlEncoded = intent.getStringExtra("URL")
                val rtspUrl = Uri.decode(rtspUrlEncoded)
                VideoScreen(rtspUrl = rtspUrl, isPip = isInPipMode, onRecordingStopped = { finish() })
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "ACTION_PAUSE" -> {

                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("ACTION_PAUSE")
        }
        registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun VideoScreen(
    isPip: Boolean,
    rtspUrl: String,
    onRecordingStopped: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val videoLayoutRef = remember { mutableStateOf<VLCVideoLayout?>(null) }

    val libVLC = remember {
        LibVLC(
            context,
            arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--rtsp-tcp")
        )
    }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    val mediaPlayer = remember { MediaPlayer(libVLC) }
    var isRecordingStarted by remember { mutableStateOf(false) }

    val outputPath = remember {
        "/sdcard/Download/stream_${System.currentTimeMillis()}.mp4"
    }

    var recordingStarted by remember { mutableStateOf(false) }
    val connectionStatus = remember { mutableStateOf("Connecting...") }
    val connectionColor = remember { mutableStateOf(Color.Yellow) }


    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.media?.release()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    LaunchedEffect(videoLayoutRef.value) {
        if (videoLayoutRef.value != null && !isRecordingStarted) {
            isRecordingStarted = true

            withContext(Dispatchers.IO) {
                val media = Media(libVLC, Uri.parse(rtspUrl)).apply {
//                    addOption(":sout=#file{dst=$outputPath}")
                    addOption(":sout=#duplicate{dst=file{dst=$outputPath}, dst=display}")
                    addOption("-vvv")
                    addOption(":sout-keep")
                    addOption(":no-sout-all")
                    addOption(":sout-avformat-mux=mp4")
                    setHWDecoderEnabled(true, false)
                    addOption(":network-caching=300")
                }

                mediaPlayer.media = media
                mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                mediaPlayer.play()
            }
        }
    }
    LaunchedEffect(isRecordingStarted) {
        while (true){
            delay(1000L)
            secondsElapsed++
        }
    }

    val formattedTime = String.format(
        "%02d:%02d",
        secondsElapsed / 60,
        secondsElapsed % 60
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    videoLayoutRef.value = this
                    mediaPlayer.attachViews(this, null, false, false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    connectionStatus.value = "🟡 Connecting..."
                    connectionColor.value = Color.Yellow
                }
                MediaPlayer.Event.Playing -> {
                    connectionStatus.value = "🟢 Connected"
                    connectionColor.value = Color.Green
                    isRecordingStarted = true
                }
                MediaPlayer.Event.EndReached, MediaPlayer.Event.Stopped -> {
                    connectionStatus.value = "⚫ Disconnected"
                    connectionColor.value = Color.Red
                }
                MediaPlayer.Event.EncounteredError -> {
                    connectionStatus.value = "🔴 Connection Failed"
                    connectionColor.value = Color.Red
                }
            }
        }


        if (!isPip) {
            IconButton(
                onClick = {
                    if (isRecordingStarted) {
                        mediaPlayer.stop()
                        Toast.makeText(context, "Recording saved to Downloads", Toast.LENGTH_SHORT).show()
                        onRecordingStopped()
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.record_circle_svgrepo_com),
                    contentDescription = "Stop Recording",
                    tint = Color.Red
                )
            }

            IconButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = PendingIntent.getBroadcast(
                            context,
                            0,
                            Intent("ACTION_PAUSE"),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        val icon = Icon.createWithResource(context, R.drawable.record_circle_svgrepo_com)

                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .setActions(listOf(RemoteAction(icon, "Stop", "Stop", intent)))
                                .build()
                        )
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.pip_svgrepo_com),
                    contentDescription = "PiP",
                    tint = Color.White
                )
            }

            Text(
                text = "🔴 "+formattedTime,
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter),
                color = Color.Red,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = connectionStatus.value,
                color = connectionColor.value,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = rtspUrl,
                modifier = Modifier.align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

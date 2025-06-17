package com.cosmicstruck.vyoriusassignment

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.cosmicstruck.vyoriusassignment.common.NavigationGraph
import com.cosmicstruck.vyoriusassignment.ui.theme.VyoriusTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
        android.Manifest.permission.ACCESS_WIFI_STATE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (!hasPermissions()) {
                ActivityCompat.requestPermissions(this, requiredPermissions, 0)
            }
            VyoriusTheme {
                val navHostController = rememberNavController()
                NavigationGraph(navHostController)
            }
        }
    }
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}


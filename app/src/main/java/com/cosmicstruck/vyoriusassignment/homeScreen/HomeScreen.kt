package com.cosmicstruck.vyoriusassignment.homeScreen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicstruck.vyoriusassignment.R
import com.cosmicstruck.vyoriusassignment.cameraScreen.VideoScreenActivity
import com.cosmicstruck.vyoriusassignment.homeScreen.components.InfiniteLottieAnimation
import com.cosmicstruck.vyoriusassignment.streamingScreen.StreamingActivity

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel<HomeScreenViewModel>(),
    modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surfaceDim
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    20.dp,
                    alignment = Alignment.CenterVertically
                )
            ) {
                InfiniteLottieAnimation(
                    animationRes = R.raw.stream_animation,
                    modifier = Modifier
                        .size(150.dp)
                )
                Text(
                    text = "Cam Stream",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = homeScreenViewModel.urlTextState.value,
                    placeholder = {
                        Text(
                            text = "Enter Url e.g rtsp://192.168.1.1:8080/ch0",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.5f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    textStyle = MaterialTheme.typography.labelMedium,
                    onValueChange = { it ->
                        homeScreenViewModel.urlTextState.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (homeScreenViewModel.urlTextState.value.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "URL Cannot be Empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (!homeScreenViewModel.urlTextState.value.startsWith("rtsp://")) {
                                Toast.makeText(
                                    context,
                                    "URL NOT SUITABLE",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val encodedUrl = Uri.encode(homeScreenViewModel.urlTextState.value)
    //                            navigateToHomeScreen(encodedUrl)
                                val intent = Intent(context, VideoScreenActivity::class.java).apply {
                                    putExtra("URL", encodedUrl)
                                }
                                context.startActivity(intent)
                            }

                        },
                        shape = RectangleShape,
                        modifier = Modifier
                            .width(100.dp),
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.inversePrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Start",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }


                    Button(
                        onClick = {
                            val intent = Intent(context, StreamingActivity::class.java)
                            context.startActivity(intent)
                        },
                        shape = RectangleShape,
                        modifier = Modifier,
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.inversePrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Start Streaming",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}


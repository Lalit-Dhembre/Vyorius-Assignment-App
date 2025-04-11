package com.cosmicstruck.vyoriusassignment.cameraScreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VideoScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
): ViewModel(){

    var recordButtonState = mutableStateOf(false) // true = recording
        private set

    fun toggleRecording() {
        recordButtonState.value = !recordButtonState.value
    }

    fun resetRecording() {
        recordButtonState.value = false
    }
}
package com.cosmicstruck.vyoriusassignment.cameraScreen

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
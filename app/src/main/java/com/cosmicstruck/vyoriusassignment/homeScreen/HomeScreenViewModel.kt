package com.cosmicstruck.vyoriusassignment.homeScreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor() :
ViewModel(){

    val urlTextState = mutableStateOf("")
}
package com.cosmicstruck.vyoriusassignment.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cosmicstruck.vyoriusassignment.cameraScreen.VideoScreen
import com.cosmicstruck.vyoriusassignment.homeScreen.HomeScreen

@Composable
fun NavigationGraph(
    navHostController: NavHostController,
    modifier: Modifier = Modifier) {

    NavHost(
        navController = navHostController,
        startDestination = Screens.HOMESCREEN.route
    ){
        composable(route = Screens.HOMESCREEN.route){
            HomeScreen(
                navigateToHomeScreen = {it->
                    navHostController.saveState()?.putString("URL",it)
                    navHostController.navigate(
                        Screens.CAMERASCREEN.route + "/${it}"
                    )
                }
            )
        }
        composable(Screens.CAMERASCREEN.route + "/{url}"){
//            VideoScreen()
        }
    }
}

enum class Screens(val route: String){
    HOMESCREEN(route = "home_screen"),
    CAMERASCREEN(route = "camera_screen")
}
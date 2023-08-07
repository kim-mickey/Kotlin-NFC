package com.freakyaxel.emvreader

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.freakyaxel.emvreader.ui.theme.AmountEntryScreen

@Composable
fun NavGraphComponent(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "amount") {
        composable("pinInput"){
            PinInputScreen(navController)
        }
        composable("success"){
            SuccessScreen()
        }
    }
}
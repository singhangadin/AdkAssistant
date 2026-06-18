package `in`.singhangad.adkassistant.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.singhangad.adkassistant.presentation.about.AboutScreen
import `in`.singhangad.adkassistant.presentation.chat.ChatScreen

@Composable
fun AdkNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destination.Chat.route) {
        composable(Destination.Chat.route) {
            ChatScreen(onAbout = { navController.navigate(Destination.About.route) })
        }
        composable(Destination.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

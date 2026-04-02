package ch.ebu.peachdemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.ebu.peachdemo.ui.screens.AudioPlayerScreen
import ch.ebu.peachdemo.ui.screens.RecommendationsScreen
import ch.ebu.peachdemo.ui.screens.VideoPlayerScreen

object Routes {
    const val RECOMMENDATIONS = "recommendations"
    const val VIDEO_PLAYER = "video_player"
    const val AUDIO_PLAYER = "audio_player"
}

@Composable
fun PeachDemoNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.RECOMMENDATIONS) {
        composable(Routes.RECOMMENDATIONS) {
            RecommendationsScreen(navController = navController)
        }
        composable(Routes.VIDEO_PLAYER) {
            VideoPlayerScreen()
        }
        composable(Routes.AUDIO_PLAYER) {
            AudioPlayerScreen()
        }
    }
}

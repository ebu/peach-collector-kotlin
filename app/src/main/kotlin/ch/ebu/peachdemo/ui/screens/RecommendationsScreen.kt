package ch.ebu.peachdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ch.ebu.peachcollector.Event
import ch.ebu.peachcollector.EventContext
import ch.ebu.peachcollector.EventContextComponent
import ch.ebu.peachdemo.R
import ch.ebu.peachdemo.ui.navigation.Routes

data class RecommendationItem(
    val id: String,
    val imageRes: Int,
    val title: String
)

@Composable
fun RecommendationsScreen(navController: NavController) {
    val context = LocalContext.current

    val items = listOf(
        RecommendationItem("reco00", R.drawable.reco00, "Video Player"),
        RecommendationItem("reco01", R.drawable.reco01, "Audio Player"),
        RecommendationItem("reco02", R.drawable.reco02, "Item 3"),
        RecommendationItem("reco03", R.drawable.reco03, "Item 4"),
    )

    val itemIds = listOf("reco00", "reco01", "reco02", "reco03")
    val eventContext = EventContext.recommendationContext().apply {
        component = EventContextComponent(
            type = "carousel",
            name = "MainCarousel",
            version = "1.0"
        )
        this.items = itemIds
    }

    // Send recommendation displayed event when the screen appears
    LaunchedEffect(Unit) {
        Event.sendRecommendationDisplayed(
            recommendationID = "recoList01",
            items = itemIds,
            context = eventContext
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recommendations",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(items) { index, item ->
                RecommendationCard(
                    item = item,
                    onClick = {
                        // Send recommendation hit
                        val hitContext = EventContext.recommendationContext().apply {
                            component = EventContextComponent(
                                type = "carousel",
                                name = "MainCarousel",
                                version = "1.0"
                            )
                        }
                        Event.sendRecommendationHit(
                            recommendationID = "recoList01",
                            itemID = item.id,
                            hitIndex = index,
                            context = hitContext
                        )

                        // Navigate based on index
                        when (index) {
                            0 -> navController.navigate(Routes.VIDEO_PLAYER)
                            1 -> navController.navigate(Routes.AUDIO_PLAYER)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    item: RecommendationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

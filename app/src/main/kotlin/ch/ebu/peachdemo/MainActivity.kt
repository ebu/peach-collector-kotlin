package ch.ebu.peachdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import ch.ebu.peachcollector.EventType
import ch.ebu.peachcollector.PeachCollector
import ch.ebu.peachcollector.Publisher
import ch.ebu.peachdemo.ui.components.LogPanel
import ch.ebu.peachdemo.ui.components.PublisherInfoCard
import ch.ebu.peachdemo.ui.navigation.PeachDemoNavGraph
import ch.ebu.peachdemo.ui.theme.PeachDemoTheme
import ch.ebu.peachdemo.viewmodel.DemoViewModel
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set a device ID
        PeachCollector.setDeviceID(UUID.randomUUID().toString())

        // Add a default publisher (collects all events)
        val defaultPublisher = Publisher("zzebu00000000017").apply {
            interval = 10
            maxEventsPerBatch = 5
        }
        PeachCollector.addPublisher(defaultPublisher, "Default Publisher")

        // Add a media-only publisher (filters for media events)
        val mediaPublisher = object : Publisher("zzebu00000000017") {
            override fun shouldProcessEvent(event: ch.ebu.peachcollector.Event): Boolean {
                return event.type.startsWith("media_")
            }
        }.apply {
            interval = 10
            maxEventsPerBatch = 5
        }
        PeachCollector.addPublisher(mediaPublisher, "Media Publisher")

        setContent {
            PeachDemoTheme {
                PeachDemoScreen()
            }
        }
    }
}

@Composable
fun PeachDemoScreen(viewModel: DemoViewModel = viewModel()) {
    val navController = rememberNavController()
    val logMessages by viewModel.logMessages.collectAsState()
    val publishers = PeachCollector.shared?.publishers ?: emptyMap()

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PeachDemoNavGraph(navController = navController)
        }

        // Publisher info cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for ((name, publisher) in publishers) {
                PublisherInfoCard(
                    name = name,
                    url = publisher.serviceURL,
                    interval = publisher.interval,
                    maxEventsPerBatch = publisher.maxEventsPerBatch
                )
            }
        }

        // Log panel
        LogPanel(
            messages = logMessages,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

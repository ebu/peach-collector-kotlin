package ch.ebu.peachdemo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PublisherInfoCard(
    name: String,
    url: String?,
    interval: Int,
    maxEventsPerBatch: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "URL: ${url ?: "not set"}",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Interval: ${interval}s | Batch: $maxEventsPerBatch",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

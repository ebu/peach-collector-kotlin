package ch.ebu.peachdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogPanel(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(messages) { message ->
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF00FF00),
                lineHeight = 14.sp
            )
        }
    }
}

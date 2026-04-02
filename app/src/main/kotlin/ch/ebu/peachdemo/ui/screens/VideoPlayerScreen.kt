package ch.ebu.peachdemo.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ch.ebu.peachcollector.EventContext
import ch.ebu.peachcollector.EventContextComponent
import ch.ebu.peachcollector.EventProperties
import ch.ebu.peachcollector.PeachPlayerTracker
import ch.ebu.peachdemo.R

@Composable
fun VideoPlayerScreen() {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.peach}")
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }

    // Setup PeachPlayerTracker
    DisposableEffect(exoPlayer) {
        PeachPlayerTracker.setPlayer(exoPlayer)

        val properties = EventProperties().apply {
            videoMode = "normal"
        }
        val eventContext = EventContext.mediaContext("test0001").apply {
            component = EventContextComponent(
                type = "media_player",
                name = "MainPlayer",
                version = "2.3.0"
            )
        }

        PeachPlayerTracker.trackMedia(
            mediaID = "test0001",
            properties = properties,
            context = eventContext
        )

        onDispose {
            PeachPlayerTracker.clearCurrentItem()
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Video Player",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
    }
}

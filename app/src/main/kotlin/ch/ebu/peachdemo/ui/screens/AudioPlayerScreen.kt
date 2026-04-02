package ch.ebu.peachdemo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import ch.ebu.peachcollector.EventContext
import ch.ebu.peachcollector.EventContextComponent
import ch.ebu.peachcollector.EventProperties
import ch.ebu.peachcollector.PeachPlayerTracker
import ch.ebu.peachdemo.R

@Composable
fun AudioPlayerScreen() {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val audioUri = "https://upload.wikimedia.org/wikipedia/commons/6/6c/Grieg_Lyric_Pieces_Kobold.ogg"
            setMediaItem(MediaItem.fromUri(audioUri))
            prepare()
        }
    }

    // Setup PeachPlayerTracker
    DisposableEffect(exoPlayer) {
        PeachPlayerTracker.setPlayer(exoPlayer)

        val properties = EventProperties().apply {
            audioMode = "normal"
        }
        val eventContext = EventContext.mediaContext("audio001").apply {
            component = EventContextComponent(
                type = "media_player",
                name = "MainPlayer",
                version = "2.3.0"
            )
        }

        PeachPlayerTracker.trackMedia(
            mediaID = "audio001",
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Audio Player",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Grieg - Lyric Pieces: Kobold",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        IconButton(
            onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
                isPlaying = !isPlaying
            },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.icon_pause else R.drawable.icon_play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

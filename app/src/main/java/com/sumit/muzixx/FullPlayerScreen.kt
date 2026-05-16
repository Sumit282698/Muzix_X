package com.sumit.muzixx

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.concurrent.TimeUnit

@Composable
fun FullPlayerScreen(viewModel: MusicViewModel, context: Context) {
    val song = viewModel.selectedSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        AsyncImage(
            model = song?.artUri,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .shadow(8.dp)
                .background(Color.DarkGray),
            error = painterResource(id = R.drawable.default_art),
            placeholder = painterResource(id = R.drawable.default_art),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(song?.title ?: "No Track", style = MaterialTheme.typography.headlineSmall)
        Text(song?.artist ?: "Unknown Artist", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Slider(
            value = viewModel.currentPosition.toFloat(),
            onValueChange = { newValue ->
                viewModel.seekTo(newValue.toLong())
            },
            valueRange = 0f..(viewModel.totalDuration.toFloat().coerceAtLeast(1f)),
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                thumbColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(viewModel.currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = formatTime(viewModel.totalDuration), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                Icon(
                    imageVector = if (viewModel.currentRepeatMode == MusicViewModel.RepeatMode.ONE)
                        Icons.Default.RepeatOne else Icons.Default.Repeat,
                    tint = if (viewModel.currentRepeatMode != MusicViewModel.RepeatMode.NONE)
                        MaterialTheme.colorScheme.primary else Color.Gray,
                    contentDescription = "Repeat"
                )
            }

            IconButton(onClick = { viewModel.playPrevious(context) }) {
                Icon(Icons.Default.SkipPrevious, "Prev", modifier = Modifier.size(40.dp))
            }

            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    modifier = Modifier.size(32.dp),
                    contentDescription = "Play/Pause"
                )
            }

            IconButton(onClick = { viewModel.playNext(context) }) {
                Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(40.dp))
            }

            IconButton(onClick = { /* Queue feature */ }) {
                Icon(Icons.AutoMirrored.Filled.List, "Queue", tint = Color.Gray)
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
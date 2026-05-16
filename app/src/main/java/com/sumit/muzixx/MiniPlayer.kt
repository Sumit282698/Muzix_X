package com.sumit.muzixx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit
) {
    if (song != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                .clickable { onMiniPlayerClick() }
                // 1. FIXED: Changed 'symmetric = 8.dp' to 'all = 8.dp' or horizontal/vertical parameters
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Display Block
            AsyncImage(
                model = song.artUri,
                contentDescription = "MiniPlayer Album Art",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                error = painterResource(id = R.drawable.default_art),
                placeholder = painterResource(id = R.drawable.default_art),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Details Block
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                // 2. FIXED: Removed 'color = Color.Gray' fallback styling if you want it to match light/dark systems smoothly via MaterialTheme
                Text(text = song.artist, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Play/Pause Action Button
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause"
                )
            }
        }
    }
}
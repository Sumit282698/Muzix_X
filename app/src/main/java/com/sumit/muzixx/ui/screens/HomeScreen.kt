package com.sumit.muzixx.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val allSongs = viewModel.songs

    val trendingOnline = viewModel.trendingOnlineSongs
    val hitSongs = viewModel.trendingHitSongs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Home", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            item {
                Column {
                    Text(
                        text = "Trending Songs 🔥",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    if (viewModel.isTrendingLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        SongCarouselSection(
                            title = "",
                            songs = trendingOnline,
                            onSongClick = { index ->
                                viewModel.playSong(context, trendingOnline, index)
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Hindi Hits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    if (viewModel.isTrendingLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        SongCarouselSection(
                            title = "",
                            songs = hitSongs,
                            onSongClick = { index ->
                                viewModel.playSong(context, hitSongs, index)
                            }
                        )
                    }
                }
            }

            item {
                SongCarouselSection(
                    title = "Discover Local Songs",
                    songs = allSongs,
                    onSongClick = { index ->
                        viewModel.playSong(context, allSongs, index)
                    }
                )
            }
        }
    }
}

@Composable
fun SongCarouselSection(
    title: String,
    songs: List<Song>,
    onSongClick: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (songs.isEmpty()) {
            Text(
                text = "No tracks available in this section",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    HomeRowSongCard(
                        song = song,
                        onClick = { onSongClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeRowSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = "Album Cover Art",
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = painterResource(id = R.drawable.default_music),
            placeholder = painterResource(id = R.drawable.default_music),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun highwaySongsFilter(list: List<Song>): List<Song> {

    return list.filter { it.title.contains("Hindi", ignoreCase = true) || it.artist.contains("Arijit", ignoreCase = true) }
        .ifEmpty { list.shuffled().take(5) }
}
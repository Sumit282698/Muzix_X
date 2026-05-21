package com.sumit.muzixx.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var showPlaylistSelector by remember { mutableStateOf(false) }

    val currentPlaylist = viewModel.selectedPlaylist

    Column(modifier = modifier.fillMaxSize()) {
        if (currentPlaylist != null) {
            TopAppBar(
                title = {
                    Text(
                        text = currentPlaylist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectedPlaylist = null }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                    }
                },
                actions = {
                    if (currentPlaylist.songs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playSong(context, currentPlaylist.songs, 0) }) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play All")
                        }
                    }
                }
            )

            if (currentPlaylist.songs.isEmpty()) {
                Text(
                    text = "This playlist is empty.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    itemsIndexed(currentPlaylist.songs) { index, song ->
                        LibrarySongItem(
                            song = song,
                            isSelected = viewModel.selectedSong == song,
                            isLocalSongsPlaylist = currentPlaylist.id == "local_songs",
                            onAddClick = {
                                activeSongForMenu = song
                                showPlaylistSelector = true
                            },
                            onClick = {
                                viewModel.playSong(context, currentPlaylist.songs, index)
                            }
                        )
                    }
                }
            }

        } else {
            // ALL PLAYLISTS LIST VIEW
            TopAppBar(
                title = { Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
                    }
                }
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(viewModel.playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectedPlaylist = playlist }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Playlist Folder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = playlist.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "${playlist.songs.size} songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }


    if (showPlaylistSelector && activeSongForMenu != null) {
        val customPlaylistsOnly = viewModel.playlists.filter { it.id != "local_songs" }

        AlertDialog(
            onDismissRequest = { showPlaylistSelector = false },
            title = { Text("Add to Playlist") },
            text = {
                if (customPlaylistsOnly.isEmpty()) {
                    Text("Please create a custom playlist first using the '+' button.")
                } else {
                    LazyColumn {
                        items(customPlaylistsOnly) { targetPlaylist ->
                            Text(
                                text = targetPlaylist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(targetPlaylist.id, activeSongForMenu!!)
                                        showPlaylistSelector = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistSelector = false }) { Text("Close") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                newPlaylistName = ""
            },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createCustomPlaylist(newPlaylistName)
                        showDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    newPlaylistName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LibrarySongItem(
    song: Song,
    isSelected: Boolean,
    isLocalSongsPlaylist: Boolean,
    onAddClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = "Song Album Art",
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            error = painterResource(id = R.drawable.default_music),
            placeholder = painterResource(id = R.drawable.default_music),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = song.artist,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLocalSongsPlaylist) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
}
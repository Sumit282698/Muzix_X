package com.sumit.muzixx

// imports
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.ui.theme.MuzixXTheme

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MuzixXTheme {
                val context = LocalContext.current
                var showFullPlayer by remember { mutableStateOf(false) }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // System callback when user interacts with permission prompt
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    musicViewModel.initMediaController(context)

                    val trackList = fetchLocalSongs(context)
                    musicViewModel.loadSongs(trackList)
                }

                Scaffold(
                    bottomBar = {
                        Column {
                            MiniPlayer(
                                song = musicViewModel.selectedSong,
                                isPlaying = musicViewModel.isPlaying,
                                onPlayPause = { musicViewModel.togglePlayPause() },
                                onMiniPlayerClick = { showFullPlayer = true }
                            )
                            MuzixBottomBar()
                        }
                    }
                ) { innerPadding ->
                    SongList(
                        viewModel = musicViewModel,
                        modifier = Modifier.padding(innerPadding),
                        context = context
                    )
                }

                if (showFullPlayer) {
                    ModalBottomSheet(
                        onDismissRequest = { showFullPlayer = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        FullPlayerScreen(
                            viewModel = musicViewModel,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongList(viewModel: MusicViewModel, modifier: Modifier = Modifier, context: android.content.Context) {
    val songs = viewModel.songs

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Text(
                "LOCAL TRACKS (${songs.size})",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(songs) { song ->
            SongItem(
                song = song,
                onClick = { viewModel.playSong(context, song) }
            )
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            headlineContent = { Text(song.title, color = Color.White) },
            supportingContent = { Text(song.artist, color = Color.Gray) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun MuzixBottomBar() {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = Color(0xFF330000)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Search, "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.AutoMirrored.Filled.List, "Library") },
            label = { Text("Library") }
        )
    }
}
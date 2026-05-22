package com.sumit.muzixx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.sumit.muzixx.data.fetchLocalSongs
import com.sumit.muzixx.ui.theme.MuzixXTheme
import com.sumit.muzixx.ui.screens.FullPlayerScreen
import com.sumit.muzixx.ui.components.MiniPlayer
import com.sumit.muzixx.ui.components.MuzixBottomBar
import com.sumit.muzixx.viewmodel.MusicViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sumit.muzixx.ui.screens.HomeScreen
import com.sumit.muzixx.ui.screens.LibraryScreen
import com.sumit.muzixx.ui.screens.SearchScreen

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.sumit.muzixx.data.network.YouTubeAudioExtractor.init()

        setContent {
            MuzixXTheme {
                val context = LocalContext.current

                var currentScreen by remember { mutableStateOf("Home") }
                var showFullPlayer by remember { mutableStateOf(false) }

                val loadLocalTracks = {
                    val trackList = fetchLocalSongs(context)
                    musicViewModel.loadSongs(trackList)
                    musicViewModel.initializeLocalSongsPlaylist()
                }

                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        println("MUZIX_MAIN: Storage permission granted by user. Loading local tracks.")
                        loadLocalTracks()
                    } else {
                        println("MUZIX_MAIN_ERROR: Storage permission denied. Local tracks unavailable.")
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    musicViewModel.initMediaController(context)
                    musicViewModel.initStorage(context)

                    musicViewModel.loadOnlineTrendingContent()
                    musicViewModel.loadOnlineHindiHitsContent()

                    val requiredStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, requiredStoragePermission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        println("MUZIX_MAIN: Permission already present. Syncing tracks.")
                        loadLocalTracks()
                    } else {
                        println("MUZIX_MAIN: Requesting required storage permissions...")
                        storagePermissionLauncher.launch(requiredStoragePermission)
                    }
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

                            MuzixBottomBar(
                                currentScreen = currentScreen,
                                onTabSelected = { selectedTab -> currentScreen = selectedTab }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "Home" -> {
                            HomeScreen(
                                viewModel = musicViewModel,
                                context = context,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "Search" -> {
                            SearchScreen(
                                viewModel = musicViewModel,
                                context = context,
                                onAddToPlaylistClick = { song ->
                                    musicViewModel.playlists.firstOrNull { it.id != "local_songs" }?.let { targetPlaylist ->
                                        musicViewModel.addSongToPlaylist(targetPlaylist.id, song)
                                    }
                                    musicViewModel.selectedPlaylist = null
                                    currentScreen = "Library"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "Library" -> {
                            LibraryScreen(
                                viewModel = musicViewModel,
                                context = context,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                if (showFullPlayer) {
                    ModalBottomSheet(
                        onDismissRequest = { showFullPlayer = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        FullPlayerScreen(
                            viewModel = musicViewModel
                        )
                    }
                }
            }
        }
    }
}
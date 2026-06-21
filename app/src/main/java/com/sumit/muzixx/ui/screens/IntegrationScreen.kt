package com.sumit.muzixx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integrations", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Import your external playlists directly into MuzixX seamlessly.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Spotify Integration Option
            item {
                IntegrationCard(
                    title = "Import Spotify Playlist",
                    description = "Sync your favorite tracks from public Spotify links.",
                    brandColor = Color(0xFF1DB954),
                    onClick = {
                        Toast.makeText(context,"Under Development", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // YouTube Music Integration Option
            item {
                IntegrationCard(
                    title = "Import YouTube Music Playlist",
                    description = "Bring over your specialized streaming queues.",
                    brandColor = Color(0xFFFF0000),
                    onClick = {
                        Toast.makeText(context,"Under Development", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // YouTube Integration Option
            item {
                IntegrationCard(
                    title = "Import YouTube Playlist",
                    description = "Convert video collections directly to standard audio formats.",
                    brandColor = Color(0xFFE62117),
                    onClick = {
                        Toast.makeText(context,"Under Development", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun IntegrationCard(
    title: String,
    description: String,
    brandColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 40.dp)
                    .background(brandColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.CallReceived,
                contentDescription = "Import Arrow",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
package com.example.purrytify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography
import java.io.File

@Composable
fun PlaylistItemComponent(
    item: PlaylistItem,
    position: Int? = null,
    isPlaying: Boolean = false,
    onClick: (PlaylistItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(vertical = 8.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position number (if provided)
        if (position != null) {
            Text(
                text = position.toString(),
                style = Typography.bodyLarge,
                color = if (isPlaying) PurrytifyGreen else PurrytifyLightGray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
        }
        
        // Album artwork
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            when (item) {
                is PlaylistItem.LocalSong -> {
                    // Local song artwork
                    if (item.artworkPath.isNotEmpty() && File(item.artworkPath).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file://${item.artworkPath}")
                                .crossfade(true)
                                .build(),
                            contentDescription = "${item.title} by ${item.artist}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = painterResource(id = R.drawable.ic_launcher_foreground)
                        )
                    } else {
                        // Default placeholder
                        AsyncImage(
                            model = R.drawable.default_artwork,
                            contentDescription = "Default artwork",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is PlaylistItem.OnlineSong -> {
                    // Online song artwork
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${item.title} by ${item.artist}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.ic_launcher_foreground)
                    )
                }
            }
        }
        
        // Song info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = item.title,
                style = Typography.bodyLarge,
                color = if (isPlaying) PurrytifyGreen else PurrytifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.artist,
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}
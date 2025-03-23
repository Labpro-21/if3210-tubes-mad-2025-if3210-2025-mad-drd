package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLighterBlack
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onLikeClick: () -> Unit,
    onBarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PurrytifyLighterBlack)
            .clickable { onBarClick() }
    ) {
        // Player content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album artwork
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${song.title} by ${song.artist}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            }
            
            // Song info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = song.title,
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artist,
                    style = Typography.bodySmall,
                    color = PurrytifyWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Like button
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.CheckCircle else Icons.Default.Add,
                    contentDescription = if (song.isLiked) "Remove from Liked" else "Add to Liked",
                    tint = if (song.isLiked) PurrytifyGreen else PurrytifyWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Play/Pause button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = PurrytifyWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Progress indicator
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = PurrytifyGreen,
            trackColor = PurrytifyLighterBlack,
            strokeCap = StrokeCap.Round
        )
    }
}
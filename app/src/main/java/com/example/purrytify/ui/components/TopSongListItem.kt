package com.example.purrytify.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.domain.model.OnlineSong
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyLighterBlack
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography

/**
 * Component for displaying a top song (online song) in a list
 * Styled to match the library page XML styling
 */
@Composable
fun TopSongListItem(
    song: OnlineSong,
    position: Int,
    isPlaying: Boolean = false,
    onClick: (OnlineSong) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(song) }
            .padding(vertical = 8.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position number
        Text(
            text = position.toString(),
            style = Typography.bodyLarge,
            color = if (isPlaying) PurrytifyGreen else PurrytifyLightGray,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )
        
        // Album artwork
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PurrytifyLighterBlack),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${song.title} by ${song.artist}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
//                error = Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(PurrytifyLighterBlack)
//                )
            )
        }
        
        // Song info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = song.title,
                style = Typography.bodyLarge,
                color = if (isPlaying) PurrytifyGreen else PurrytifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = song.artist,
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration (keeping this for online songs as it's provided by the API)
        Text(
            text = song.duration,
            style = Typography.bodySmall,
            color = PurrytifyLightGray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
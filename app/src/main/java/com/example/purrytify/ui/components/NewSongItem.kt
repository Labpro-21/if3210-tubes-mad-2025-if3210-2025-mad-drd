package com.example.purrytify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography

/**
 * Component for displaying a song in the "New songs" horizontal grid
 */
@Composable
fun NewSongItem(
    song: Song,
    isPlaying: Boolean = false,
    onClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick(song) }
    ) {
        // Album artwork
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = "${song.title} by ${song.artist}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(4.dp)),
            error = painterResource(id = R.drawable.ic_launcher_foreground)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Song title
        Text(
            text = song.title,
            style = Typography.bodyMedium,
            color = if (isPlaying) PurrytifyGreen else PurrytifyWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Artist name
        Text(
            text = song.artist,
            style = Typography.bodySmall,
            color = PurrytifyLightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
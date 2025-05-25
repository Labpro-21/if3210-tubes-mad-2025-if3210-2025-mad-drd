package com.example.purrytify.domain.model

/**
 * Represents an item in a mixed playlist (either an online song or a local song)
 */
sealed class PlaylistItem {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    abstract val duration: String
    
    /**
     * Represents a local song in the playlist
     */
    data class LocalSong(
        override val id: String,
        override val title: String,
        override val artist: String,
        val artworkPath: String,
        val filePath: String,
        val durationMs: Long,
        override val duration: String,
        val isLiked: Boolean
    ) : PlaylistItem()
    
    /**
     * Represents an online song in the playlist
     */
    data class OnlineSong(
        override val id: String,
        override val title: String,
        override val artist: String,
        val artworkUrl: String,
        val songUrl: String,
        override val duration: String,
        val originalId: String
    ) : PlaylistItem()
    
    companion object {
        /**
         * Create a playlist item from a local song
         */
        fun fromLocalSong(song: Song): LocalSong {
            val minutes = song.duration / 1000 / 60
            val seconds = (song.duration / 1000) % 60
            val durationStr = String.format("%d:%02d", minutes, seconds)
            
            return LocalSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                artworkPath = song.artworkPath,
                filePath = song.filePath,
                durationMs = song.duration,
                duration = durationStr,
                isLiked = song.isLiked
            )
        }
        
        /**
         * Create a playlist item from an online song
         */
        fun fromOnlineSong(song: com.example.purrytify.domain.model.OnlineSong): OnlineSong {
            return OnlineSong(
                id = "online_${song.id}",  // Prefix to differentiate from local songs
                title = song.title,
                artist = song.artist,
                artworkUrl = song.artworkUrl,
                songUrl = song.songUrl,
                duration = song.duration,
                originalId = song.id.toString()
            )
        }
    }
}
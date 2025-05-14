package com.example.purrytify.ui.screens.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.purrytify.R
import com.example.purrytify.domain.model.Song
import java.io.File

/**
 * Adapter for displaying songs in a RecyclerView
 */
class SongsAdapter(
    private var songs: List<Song>,
    private var currentPlayingSong: Song? = null,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, song.id == currentPlayingSong?.id, onSongClick)
    }

    override fun getItemCount(): Int = songs.size

    /**
     * Update the list of songs
     */
    fun updateSongs(newSongs: List<Song>) {
        val diffCallback = SongDiffCallback(songs, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        songs = newSongs
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * Update the currently playing song
     */
    fun updatePlayingSong(song: Song?) {
        val oldPlayingSong = currentPlayingSong
        currentPlayingSong = song
        
        // Notify change for old playing song
        oldPlayingSong?.let { oldSong ->
            val oldIndex = songs.indexOfFirst { it.id == oldSong.id }
            if (oldIndex >= 0) {
                notifyItemChanged(oldIndex)
            }
        }
        
        // Notify change for new playing song
        song?.let { newSong ->
            val newIndex = songs.indexOfFirst { it.id == newSong.id }
            if (newIndex >= 0) {
                notifyItemChanged(newIndex)
            }
        }
    }

    /**
     * ViewHolder for song items
     */
    class SongViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textSongTitle)
        private val artistTextView: TextView = itemView.findViewById(R.id.textArtist)
        private val artworkImageView: ImageView = itemView.findViewById(R.id.imageSongArtwork)
        private val itemContainer: View = itemView.findViewById(R.id.itemContainer)
        
        fun bind(song: Song, isPlaying: Boolean, onSongClick: (Song) -> Unit) {
            titleTextView.text = song.title
            artistTextView.text = song.artist
            
            // Load artwork if available
            if (song.artworkPath.isNotEmpty()) {
                val artworkFile = File(song.artworkPath)
                if (artworkFile.exists()) {
                    artworkImageView.setImageURI(android.net.Uri.fromFile(artworkFile))
                } else {
                    artworkImageView.setImageResource(R.drawable.default_artwork)
                }
            } else {
                artworkImageView.setImageResource(R.drawable.default_artwork)
            }
            
            // Highlight if currently playing
            if (isPlaying) {
                titleTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.purrytify_green))
                itemContainer.setBackgroundResource(R.drawable.bg_song_item_playing)
            } else {
                titleTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                itemContainer.setBackgroundResource(R.drawable.bg_song_item)
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onSongClick(song)
            }
        }
    }
    
    /**
     * DiffUtil.Callback implementation for songs
     */
    private class SongDiffCallback(
        private val oldSongs: List<Song>,
        private val newSongs: List<Song>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldSongs.size
        
        override fun getNewListSize(): Int = newSongs.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldSongs[oldItemPosition].id == newSongs[newItemPosition].id
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldSong = oldSongs[oldItemPosition]
            val newSong = newSongs[newItemPosition]
            
            return oldSong.title == newSong.title &&
                    oldSong.artist == newSong.artist &&
                    oldSong.artworkPath == newSong.artworkPath &&
                    oldSong.isLiked == newSong.isLiked
        }
    }
}
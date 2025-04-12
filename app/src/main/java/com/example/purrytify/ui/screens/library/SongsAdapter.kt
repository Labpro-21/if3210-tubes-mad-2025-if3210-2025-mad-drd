package com.example.purrytify.ui.screens.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.purrytify.R
import com.example.purrytify.domain.model.Song
import coil.load

class SongsAdapter(
    private var songs: List<Song>,
    private var currentPlayingSong: Song?,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    private var inflater: LayoutInflater? = null
    private var currentlyPlayingIndex: Int = RecyclerView.NO_POSITION

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
        val artworkImageView: ImageView = itemView.findViewById(R.id.songArtworkImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }
        val view = inflater!!.inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        val isPlaying = currentPlayingSong?.id == song.id

        holder.titleTextView.text = song.title
        holder.titleTextView.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isPlaying) R.color.purrytify_green else R.color.purrytify_white
            )
        )

        holder.artistTextView.text = song.artist

        holder.artworkImageView.load(song.artworkUri) {
            crossfade(true)
            error(R.drawable.ic_launcher_foreground)
        }

        holder.itemView.setOnClickListener {
            val previousPlayingIndex = currentlyPlayingIndex
            val clickedPosition = holder.adapterPosition
            val clickedSong = songs.getOrNull(clickedPosition)

            if (clickedSong != null) {
                onSongClick(clickedSong)
                updatePlayingIndex(clickedPosition)

                // Notify changes for the newly playing and previously playing items
                if (previousPlayingIndex != RecyclerView.NO_POSITION && previousPlayingIndex != clickedPosition) {
                    notifyItemChanged(previousPlayingIndex)
                }
                notifyItemChanged(clickedPosition)
            }
        }
    }

    override fun getItemCount(): Int = songs.size

    // Method to update data when songs list changes
    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged() // Still use this when the entire list changes
    }

    // Method to update the currently playing song and notify item changes
    fun updatePlayingSong(newCurrentPlayingSong: Song?) {
        val previousPlayingIndex = currentlyPlayingIndex
        currentPlayingSong = newCurrentPlayingSong
        currentlyPlayingIndex = songs.indexOfFirst { it.id == newCurrentPlayingSong?.id }

        // Notify changes for the newly playing and previously playing items
        if (previousPlayingIndex != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPlayingIndex)
        }
        if (currentlyPlayingIndex != RecyclerView.NO_POSITION) {
            notifyItemChanged(currentlyPlayingIndex)
        }
    }

    private fun updatePlayingIndex(newIndex: Int) {
        if (currentlyPlayingIndex != newIndex) {
            val oldIndex = currentlyPlayingIndex
            currentlyPlayingIndex = newIndex
            if (oldIndex != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldIndex)
            }
            notifyItemChanged(newIndex)
        }
    }
}
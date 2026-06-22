package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_table")
data class SyncedLyricsEntity(
    @PrimaryKey val songId: String, // title + artist signature or file path
    val lyricsText: String // LRC format or simple JSON
)

@Entity(tableName = "eq_preset_table")
data class EqPresetEntity(
    @PrimaryKey val presetName: String,
    val isCustom: Boolean,
    val band60: Float,  // In dB or percentage (-15 to +15)
    val band230: Float,
    val band910: Float,
    val band4k: Float,
    val band14k: Float
)

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_table")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val songIds: String, // Comma separated list of song IDs
    val emoji: String = "🎵"
)

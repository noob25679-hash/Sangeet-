package com.example.data

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val emoji: String,
    val colorStart: Long, // Hex representation e.g. 0xFF1A3A2A
    val colorEnd: Long,   // Hex representation e.g. 0xFF2A6A4A
    val isOnline: Boolean
)

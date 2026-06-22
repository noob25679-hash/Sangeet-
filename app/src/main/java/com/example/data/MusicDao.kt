package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Lyrics queries
    @Query("SELECT * FROM lyrics_table WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: String): SyncedLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(lyrics: SyncedLyricsEntity)

    // Equalizer Preset queries
    @Query("SELECT * FROM eq_preset_table")
    fun getAllPresetsFlow(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_preset_table WHERE presetName = :name LIMIT 1")
    suspend fun getPresetByName(name: String): EqPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(preset: EqPresetEntity)

    // Liked Songs queries
    @Query("SELECT * FROM liked_songs ORDER BY timestamp DESC")
    fun getLikedSongsFlow(): Flow<List<LikedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLikedSong(song: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun removeLikedSong(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    suspend fun isSongLiked(songId: String): Boolean

    // Playlists queries
    @Query("SELECT * FROM playlist_table")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
}

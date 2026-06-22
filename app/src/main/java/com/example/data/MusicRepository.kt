package com.example.data

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {

    val likedSongs: Flow<List<LikedSongEntity>> = musicDao.getLikedSongsFlow()
    val allPresets: Flow<List<EqPresetEntity>> = musicDao.getAllPresetsFlow()
    val playlists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylistsFlow()

    suspend fun getLyrics(songId: String): String? {
        return musicDao.getLyricsForSong(songId)?.lyricsText
    }

    suspend fun saveLyrics(songId: String, text: String) {
        musicDao.saveLyrics(SyncedLyricsEntity(songId, text))
    }

    suspend fun savePreset(preset: EqPresetEntity) {
        musicDao.savePreset(preset)
    }

    suspend fun getPresetByName(name: String): EqPresetEntity? {
        return musicDao.getPresetByName(name)
    }

    suspend fun toggleLikedSong(songId: String, title: String, artist: String, album: String, duration: Long, filePath: String) {
        if (musicDao.isSongLiked(songId)) {
            musicDao.removeLikedSong(songId)
        } else {
            musicDao.addLikedSong(LikedSongEntity(
                songId = songId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = filePath
            ))
        }
    }

    suspend fun isSongLiked(songId: String): Boolean {
        return musicDao.isSongLiked(songId)
    }

    suspend fun savePlaylist(playlist: PlaylistEntity) {
        musicDao.savePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        musicDao.deletePlaylist(playlist)
    }
}

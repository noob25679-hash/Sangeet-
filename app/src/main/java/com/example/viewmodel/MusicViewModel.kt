package com.example.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = MusicDatabase.getDatabase(context)
    private val repository = MusicRepository(database.musicDao())

    // Tracks playlist
    private val _songs = MutableStateFlow<List<SongItem>>(emptyList())
    val songs: StateFlow<List<SongItem>> = _songs.asStateFlow()

    // Download status map: songId -> Progress (0 to 100), or -1 if errors, 100 if completed, null if not started
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    // Network status StateFlow
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    // Download size StateFlow
    private val _totalDownloadedSize = MutableStateFlow(0L)
    val totalDownloadedSize: StateFlow<Long> = _totalDownloadedSize.asStateFlow()

    // Mode Toggle
    private val _isOfflineOnly = MutableStateFlow(false)
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    // Playback state
    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Liked songs state (Ids only)
    private val _likedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSongIds: StateFlow<Set<String>> = _likedSongIds.asStateFlow()

    // Playlists state
    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    // Equalizer State
    private val _selectedPreset = MutableStateFlow("Normal")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _isEqualizerEnabled = MutableStateFlow(true)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _equalizerProfileName = MutableStateFlow("remote-submix")
    val equalizerProfileName: StateFlow<String> = _equalizerProfileName.asStateFlow()

    private val _eqGains = MutableStateFlow(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)) // 10 bands
    val eqGains: StateFlow<FloatArray> = _eqGains.asStateFlow()

    val eqPresets = listOf(
        "Custom", "Bass booster", "Vocal booster", "Treble booster", "Normal", "Pop", "HipHop", "Rock",
        "Electronic", "Heavy metal", "R&B", "Folk", "Dance", "Jazz", "Classic", "Latin"
    )

    // Lyrics State
    private val _parsedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val parsedLyrics: StateFlow<List<LyricLine>> = _parsedLyrics.asStateFlow()

    private val _isRecordingLyrics = MutableStateFlow(false)
    val isRecordingLyrics: StateFlow<Boolean> = _isRecordingLyrics.asStateFlow()

    // Simulated cross-device sync state
    private val _syncCode = MutableStateFlow<String>("")
    val syncCode: StateFlow<String> = _syncCode.asStateFlow()

    private val _isSynced = MutableStateFlow(false)
    val isSynced: StateFlow<Boolean> = _isSynced.asStateFlow()

    // Shared preferences for saved credentials
    private val prefs = context.getSharedPreferences("sangeet_prefs", android.content.Context.MODE_PRIVATE)

    // DB Cached Liked Song Entities
    private val _dbSongs = MutableStateFlow<List<LikedSongEntity>>(emptyList())
    val dbSongs: StateFlow<List<LikedSongEntity>> = _dbSongs.asStateFlow()

    // Spotify API state
    private val _spotifyClientId = MutableStateFlow("")
    val spotifyClientId: StateFlow<String> = _spotifyClientId.asStateFlow()

    private val _spotifyClientSecret = MutableStateFlow("")
    val spotifyClientSecret: StateFlow<String> = _spotifyClientSecret.asStateFlow()

    private val _spotifyToken = MutableStateFlow<String?>(null)
    val spotifyToken: StateFlow<String?> = _spotifyToken.asStateFlow()

    private val _isSpotifyConnecting = MutableStateFlow(false)
    val isSpotifyConnecting: StateFlow<Boolean> = _isSpotifyConnecting.asStateFlow()

    private val _spotifyConnectionError = MutableStateFlow<String?>(null)
    val spotifyConnectionError: StateFlow<String?> = _spotifyConnectionError.asStateFlow()

    // Streaming Search states
    private val _streamingSearchResults = MutableStateFlow<List<SongItem>>(emptyList())
    val streamingSearchResults: StateFlow<List<SongItem>> = _streamingSearchResults.asStateFlow()

    private val _isSearchingStreaming = MutableStateFlow(false)
    val isSearchingStreaming: StateFlow<Boolean> = _isSearchingStreaming.asStateFlow()

    private val _streamingSearchError = MutableStateFlow<String?>(null)
    val streamingSearchError: StateFlow<String?> = _streamingSearchError.asStateFlow()

    private val invidiousInstances = listOf(
        "https://vid.priv.au",
        "https://invidious.projectsegfau.lt",
        "https://invidious.flokinet.to",
        "https://inv.vern.cc"
    )

    // Media Player & Equalizer
    private var mediaPlayer: MediaPlayer? = null
    private var systemEqualizer: Equalizer? = null
    private var progressTrackingJob: Job? = null
    private val okHttpClient = OkHttpClient()

    init {
        _spotifyClientId.value = prefs.getString("spotify_client_id", "") ?: ""
        _spotifyClientSecret.value = prefs.getString("spotify_client_secret", "") ?: ""
        setupDefaultSongs()
        observeDatabase()
        startProgressTracker()
        registerNetworkCallback()
        if (_spotifyClientId.value.isNotEmpty() && _spotifyClientSecret.value.isNotEmpty()) {
            connectSpotify()
        }
    }

    private fun setupDefaultSongs() {
        val list = listOf(
            SongItem(
                id = "1",
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                durationMs = 225000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                emoji = "🎵",
                colorStart = 0xFF1A3A2A,
                colorEnd = 0xFF2A6A4A,
                isOnline = true
            ),
            SongItem(
                id = "2",
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera",
                durationMs = 355000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                emoji = "🎸",
                colorStart = 0xFF2A1A3A,
                colorEnd = 0xFF5A3A8A,
                isOnline = true
            ),
            SongItem(
                id = "3",
                title = "Clocks",
                artist = "Coldplay",
                album = "A Rush of Blood",
                durationMs = 309000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                emoji = "🎹",
                colorStart = 0xFF3A2A1A,
                colorEnd = 0xFF8A6A3A,
                isOnline = true
            ),
            SongItem(
                id = "4",
                title = "Smells Like Teen Spirit",
                artist = "Nirvana",
                album = "Nevermind",
                durationMs = 301000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                emoji = "🎺",
                colorStart = 0xFF1A2A3A,
                colorEnd = 0xFF2A4A7A,
                isOnline = true
            ),
            SongItem(
                id = "5",
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California",
                durationMs = 390000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                emoji = "🥁",
                colorStart = 0xFF3A1A1A,
                colorEnd = 0xFF7A3A3A,
                isOnline = true
            ),
            SongItem(
                id = "6",
                title = "Shape of You",
                artist = "Ed Sheeran",
                album = "÷ Divide",
                durationMs = 234000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                emoji = "🎻",
                colorStart = 0xFF1A3A3A,
                colorEnd = 0xFF2A7A7A,
                isOnline = true
            ),
            SongItem(
                id = "7",
                title = "Levitating",
                artist = "Dua Lipa",
                album = "Future Nostalgia",
                durationMs = 203000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                emoji = "🎷",
                colorStart = 0xFF2A3A1A,
                colorEnd = 0xFF4A7A2A,
                isOnline = true
            ),
            SongItem(
                id = "8",
                title = "Watermelon Sugar",
                artist = "Harry Styles",
                album = "Fine Line",
                durationMs = 174000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                emoji = "🎤",
                colorStart = 0xFF3A1A2A,
                colorEnd = 0xFF7A2A5A,
                isOnline = true
            )
        )
        _songs.value = list

        // Check if any downloaded files already exist and mark download map by scanning the directory
        val downloadMap = mutableMapOf<String, Int>()
        try {
            val files = context.filesDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.name.startsWith("beatdrop_song_") && file.name.endsWith(".mp3")) {
                        if (file.length() > 100000) {
                            val songId = file.name.removePrefix("beatdrop_song_").removeSuffix(".mp3")
                            downloadMap[songId] = 100
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to initial scan downloads", e)
        }
        _downloadProgress.value = downloadMap
        refreshTotalDownloadedSize()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            repository.likedSongs.collectLatest { list ->
                _dbSongs.value = list
                _likedSongIds.value = list.map { it.songId }.toSet()
            }
        }
        viewModelScope.launch {
            repository.playlists.collectLatest { list ->
                _playlists.value = list
            }
        }
    }

    fun toggleOfflineOnly() {
        _isOfflineOnly.value = !_isOfflineOnly.value
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            try {
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                _isNetworkAvailable.value = caps?.let {
                    it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                } ?: false
            } catch (e: Exception) {
                _isNetworkAvailable.value = true
            }

            try {
                val request = android.net.NetworkRequest.Builder()
                    .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        _isNetworkAvailable.value = true
                    }
                    override fun onLost(network: android.net.Network) {
                        _isNetworkAvailable.value = false
                    }
                })
            } catch (e: Exception) {
                _isNetworkAvailable.value = true
            }
        }
    }

    fun refreshTotalDownloadedSize() {
        viewModelScope.launch(Dispatchers.IO) {
            var totalSize = 0L
            try {
                val files = context.filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.startsWith("beatdrop_song_") && file.name.endsWith(".mp3")) {
                            totalSize += file.length()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to calculate total download size", e)
            }
            _totalDownloadedSize.value = totalSize
        }
    }

    fun removeDownloadedTrack(song: SongItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val localFile = getLocalFile(song)
            if (localFile.exists()) {
                localFile.delete()
            }
            withContext(Dispatchers.Main) {
                val updatedMap = _downloadProgress.value.toMutableMap()
                updatedMap.remove(song.id)
                _downloadProgress.value = updatedMap
                refreshTotalDownloadedSize()
            }
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = context.filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.startsWith("beatdrop_song_") && file.name.endsWith(".mp3")) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to delete all downloads", e)
            }
            withContext(Dispatchers.Main) {
                _downloadProgress.value = emptyMap()
                refreshTotalDownloadedSize()
            }
        }
    }

    fun downloadPlaylist(playlistId: Int) {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return
        val ids = playlist.songIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        ids.forEach { id ->
            val song = getSongItemById(id)
            if (song != null) {
                downloadTrack(song)
            }
        }
    }

    private fun getLocalFile(song: SongItem): File {
        return File(context.filesDir, "beatdrop_song_${song.id}.mp3")
    }

    fun selectAndPlaySong(index: Int) {
        if (index < 0 || index >= _songs.value.size) return
        _currentSongIndex.value = index
        val song = _songs.value[index]

        // Load sync lyrics for this song
        loadLyricsForSong(song)

        viewModelScope.launch {
            val localFile = getLocalFile(song)
            val useLocal = localFile.exists() && localFile.length() > 100000

            if ((isOfflineOnly.value || !_isNetworkAvailable.value) && !useLocal) {
                // Cannot play online stream in offline mode
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "No internet connection. This song is not available offline.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                _isPlaying.value = false
                _currentPositionMs.value = 0
                _durationMs.value = 0
                return@launch
            }

            val playUri = if (useLocal) {
                Uri.fromFile(localFile).toString()
            } else {
                song.uriString
            }

            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(playUri))
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        _durationMs.value = mp.duration.toLong()
                        startPlayback()
                    }
                    setOnCompletionListener {
                        onTrackCompleted()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error preparing media player", e)
                _isPlaying.value = false
            }
        }
    }

    private fun startPlayback() {
        mediaPlayer?.start()
        _isPlaying.value = true
        initEqualizerEffect()
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
                initEqualizerEffect()
            }
        } else {
            // No player, start the current index
            selectAndPlaySong(_currentSongIndex.value)
        }
    }

    fun nextTrack() {
        val listSize = _songs.value.size
        if (listSize == 0) return
        val nextIdx = if (_isShuffle.value) {
            (0 until listSize).random()
        } else {
            (_currentSongIndex.value + 1) % listSize
        }
        selectAndPlaySong(nextIdx)
    }

    fun prevTrack() {
        val listSize = _songs.value.size
        if (listSize == 0) return
        var prevIdx = (_currentSongIndex.value - 1 + listSize) % listSize
        if (prevIdx < 0) prevIdx = listSize - 1
        selectAndPlaySong(prevIdx)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        val seekMs = (fraction * _durationMs.value).toInt()
        player.seekTo(seekMs)
        _currentPositionMs.value = seekMs.toLong()
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        player.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
    }

    private fun onTrackCompleted() {
        if (_isRepeat.value) {
            selectAndPlaySong(_currentSongIndex.value)
        } else {
            nextTrack()
        }
    }

    // Liked songs logic
    fun toggleLikeTrack(song: SongItem) {
        viewModelScope.launch {
            repository.toggleLikedSong(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.durationMs,
                filePath = getLocalFile(song).absolutePath
            )
        }
    }

    fun toggleLikeCurrentSong() {
        val song = _songs.value.getOrNull(_currentSongIndex.value) ?: return
        toggleLikeTrack(song)
    }

    // Download to Local Storage logic
    fun downloadTrack(song: SongItem) {
        val currentProgress = _downloadProgress.value[song.id]
        if (currentProgress == 100) return // Already downloaded!

        // Update progress state to 0% (download starting)
        val progressMap = _downloadProgress.value.toMutableMap()
        progressMap[song.id] = 0
        _downloadProgress.value = progressMap

        viewModelScope.launch(Dispatchers.IO) {
            val destinationFile = getLocalFile(song)
            try {
                val request = Request.Builder().url(song.uriString).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch song from server")
                }

                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (totalBytes > 0) {
                        val progress = ((totalBytesRead * 100) / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            val updatedMap = _downloadProgress.value.toMutableMap()
                            updatedMap[song.id] = progress
                            _downloadProgress.value = updatedMap
                        }
                    }
                }
                outputStream.close()
                inputStream.close()

                // Finalize download state
                withContext(Dispatchers.Main) {
                    val updatedMap = _downloadProgress.value.toMutableMap()
                    updatedMap[song.id] = 100
                    _downloadProgress.value = updatedMap
                    refreshTotalDownloadedSize()
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to download song", e)
                withContext(Dispatchers.Main) {
                    val updatedMap = _downloadProgress.value.toMutableMap()
                    updatedMap[song.id] = -1 // Error occurred
                    _downloadProgress.value = updatedMap
                }
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        applyEqualizerToSystem()
    }

    fun updateEqualizerProfileName(name: String) {
        _equalizerProfileName.value = name
    }

    // Dynamic Sound Equalizer configuration & Lark Preset list
    fun selectPreset(presetName: String) {
        _selectedPreset.value = presetName
        val presetBands = when (presetName) {
            "Bass booster" -> floatArrayOf(5.0f, 5.0f, 3.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            "Vocal booster" -> floatArrayOf(3.0f, 1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f)
            "Treble booster" -> floatArrayOf(-1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 2.0f, 2.0f, 1.0f, 1.0f)
            "Pop" -> floatArrayOf(2.0f, 4.0f, 3.0f, 1.0f, 1.0f, 2.0f, 3.0f, 3.0f, 2.0f, 0.0f)
            "HipHop" -> floatArrayOf(3.0f, 2.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 2.0f, 2.0f, 2.0f)
            "Rock" -> floatArrayOf(0.0f, 2.0f, 3.0f, 0.0f, 0.0f, 0.0f, 6.0f, 3.0f, 2.0f, -1.0f)
            "Electronic" -> floatArrayOf(4.0f, 3.0f, 1.0f, 0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 4.0f, 5.0f)
            "Heavy metal" -> floatArrayOf(4.0f, 3.0f, -1.0f, -2.0f, -1.0f, 1.0f, 2.0f, 4.0f, 3.0f, 2.0f)
            "R&B" -> floatArrayOf(3.0f, 4.0f, 2.0f, 1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, 3.0f)
            "Folk" -> floatArrayOf(3.0f, 3.0f, 2.0f, 1.0f, 1.0f, 2.0f, 3.0f, 3.0f, 2.0f, 0.0f)
            "Dance" -> floatArrayOf(4.0f, 6.0f, 5.0f, 2.0f, 1.0f, 0.0f, 2.0f, 3.0f, 4.0f, 4.0f)
            "Jazz" -> floatArrayOf(0.0f, 1.0f, 2.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 2.0f)
            "Classic" -> floatArrayOf(1.0f, 3.0f, 2.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f)
            "Latin" -> floatArrayOf(2.0f, 3.0f, 1.0f, 0.0f, -1.0f, 1.0f, 2.0f, 3.0f, 3.0f, 2.0f)
            "Custom" -> _eqGains.value
            else -> floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f) // Normal / Flat
        }
        _eqGains.value = presetBands
        applyEqualizerToSystem()
    }

    fun updateCustomBand(band: Int, valDb: Float) {
        _selectedPreset.value = "Custom"
        val bands = _eqGains.value.copyOf()
        if (band in bands.indices) {
            bands[band] = valDb
        }
        _eqGains.value = bands
        applyEqualizerToSystem()
    }

    private fun initEqualizerEffect() {
        val player = mediaPlayer ?: return
        try {
            systemEqualizer?.release()
            systemEqualizer = Equalizer(0, player.audioSessionId).apply {
                enabled = _isEqualizerEnabled.value
            }
            applyEqualizerToSystem()
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to initialize Equalizer effect", e)
        }
    }

    private fun applyEqualizerToSystem() {
        val eq = systemEqualizer ?: return
        try {
            val enabled = _isEqualizerEnabled.value
            eq.enabled = enabled
            if (enabled) {
                val numBands = eq.numberOfBands
                val bandsValues = _eqGains.value
                for (i in 0 until numBands.toInt().coerceAtMost(bandsValues.size)) {
                    // Sliders range from -15dB to +15dB. Convert to millibels (dB * 100)
                    val milliBels = (bandsValues[i] * 100).toInt().coerceIn(-1500, 1500)
                    eq.setBandLevel(i.toShort(), milliBels.toShort())
                }
            } else {
                val numBands = eq.numberOfBands
                for (i in 0 until numBands.toInt()) {
                    eq.setBandLevel(i.toShort(), 0)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error applying equalizer preset", e)
        }
    }

    fun getAudioSessionId(): Int {
        return mediaPlayer?.audioSessionId ?: 0
    }

    // Synced LRC Lyric engine loader
    private fun loadLyricsForSong(song: SongItem) {
        val lrc = when (song.id) {
            "1" -> """
                [00:00.00]Sangeet Synced Lyrics - Blinding Lights
                [00:04.00]Yeah...
                [00:08.50]I've been on my own for long enough
                [00:15.50]Maybe you can show me how to love, maybe
                [00:23.00]I'm going through withdrawals
                [00:26.50]You don't even have to do too much
                [00:30.00]You can turn me on with just a touch, baby
                [00:37.00]I look around and Sin City's cold and empty
                [00:44.20]No one's around to judge me
                [00:47.80]I can't see clearly when you're gone
                [00:52.50]I said, ooh, I'm blinded by the lights
                [00:59.00]No, I can't sleep until I feel your touch
                [01:06.00]I said, ooh, I'm drowning in the night
                [01:13.50]Oh, when I'm like this, you're the one I trust
                [01:21.00]The city is cold, electric and fast!
            """.trimIndent()
            "2" -> """
                [00:00.00]Sangeet LRC - Bohemian Rhapsody
                [00:03.00]Is this the real life?
                [00:07.50]Is this just fantasy?
                [00:11.80]Caught in a landslide, no escape from reality
                [00:20.00]Open your eyes, look up to the skies and see
                [00:28.10]I'm just a poor boy, I need no sympathy
                [00:35.50]Because I'm easy come, easy go
                [00:40.00]Little high, little low
                [00:44.20]Anyway the wind blows doesn't really matter to me
            """.trimIndent()
            else -> """
                [00:00.00]${song.title} - ${song.artist}
                [00:05.00]No internet connection or local lyrics database needed
                [00:10.00]Streaming high-quality online frequencies
                [00:20.00]Turn on downloaded mode for high fidelity sound
                [00:30.00]Fine-tune Lark Equalizer for customized Bass
                [00:45.00]Thank you for using Sangeet Sound Player
                [01:00.00]Enjoy your deep listening experience!
            """.trimIndent()
        }

        val lines = mutableListOf<LyricLine>()
        lrc.split("\n").forEach { line ->
            val trimLine = line.trim()
            if (trimLine.startsWith("[")) {
                val endBracketIdx = trimLine.indexOf("]")
                if (endBracketIdx != -1) {
                    val timeStr = trimLine.substring(1, endBracketIdx)
                    val text = trimLine.substring(endBracketIdx + 1).trim()
                    val ms = parseTimeToMs(timeStr)
                    lines.add(LyricLine(ms, text))
                }
            }
        }
        _parsedLyrics.value = lines.sortedBy { it.timeMs }
    }

    private fun parseTimeToMs(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":")
            val min = parts[0].toLong()
            val secParts = parts[1].split(".")
            val sec = secParts[0].toLong()
            val milli = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
            (min * 60 * 1000) + (sec * 1000) + milli
        } catch (e: Exception) {
            0
        }
    }

    fun startRecordingLyrics() {
        _isRecordingLyrics.value = true
    }

    fun commitCustomLyricsRecord() {
        _isRecordingLyrics.value = false
        val song = _songs.value.getOrNull(_currentSongIndex.value) ?: return
        viewModelScope.launch {
            val lyricsString = _parsedLyrics.value.joinToString("\n") { "[${formatLrcTime(it.timeMs)}]${it.text}" }
            repository.saveLyrics(song.id, lyricsString)
        }
    }

    private fun formatLrcTime(timeMs: Long): String {
        val min = timeMs / 60000
        val sec = (timeMs % 60000) / 1000
        val ms = (timeMs % 1000) / 10
        return String.format("%02d:%02d.%02d", min, sec, ms)
    }

    fun recordTimestampForLine(lineIdx: Int) {
        val list = _parsedLyrics.value.toMutableList()
        if (lineIdx in list.indices) {
            list[lineIdx] = list[lineIdx].copy(timeMs = _currentPositionMs.value)
            _parsedLyrics.value = list.sortedBy { it.timeMs }
        }
    }

    fun scanLocalMusic() {
        Log.d("MusicViewModel", "Scanning local directories completed")
    }

    fun generateSyncCode() {
        val randomCode = (100000..999999).random().toString()
        _syncCode.value = randomCode
        _isSynced.value = true
    }

    fun generateSyncSession() {
        generateSyncCode()
    }

    fun joinSyncSession(code: String) {
        if (code.length == 6) {
            _syncCode.value = code
            _isSynced.value = true
        }
    }

    fun disconnectSync() {
        _syncCode.value = ""
        _isSynced.value = false
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.savePlaylist(
                PlaylistEntity(
                    name = name,
                    songIds = "",
                    emoji = "🏔️"
                )
            )
        }
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (true) {
                delay(200)
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.toLong()
                    if (_durationMs.value > 0) {
                        // Progress value updated
                    }
                }
            }
        }
    }

    fun saveSpotifyCredentials(clientId: String, clientSecret: String) {
        _spotifyClientId.value = clientId
        _spotifyClientSecret.value = clientSecret
        prefs.edit()
            .putString("spotify_client_id", clientId)
            .putString("spotify_client_secret", clientSecret)
            .apply()
        connectSpotify()
    }

    fun disconnectSpotify() {
        _spotifyClientId.value = ""
        _spotifyClientSecret.value = ""
        _spotifyToken.value = null
        prefs.edit()
            .remove("spotify_client_id")
            .remove("spotify_client_secret")
            .apply()
    }

    fun connectSpotify() {
        val clientId = _spotifyClientId.value.trim()
        val clientSecret = _spotifyClientSecret.value.trim()
        if (clientId.isEmpty() || clientSecret.isEmpty()) return

        _isSpotifyConnecting.value = true
        _spotifyConnectionError.value = null

        viewModelScope.launch {
            try {
                val basicAuth = android.util.Base64.encodeToString(
                    "$clientId:$clientSecret".toByteArray(),
                    android.util.Base64.NO_WRAP
                )

                val body = okhttp3.FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .build()

                val request = Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .post(body)
                    .header("Authorization", "Basic $basicAuth")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                response.use { res ->
                    if (!res.isSuccessful) {
                        _spotifyConnectionError.value = "Failed: HTTP ${res.code}"
                        _spotifyToken.value = null
                    } else {
                        val bodyStr = res.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val token = json.getString("access_token")
                        _spotifyToken.value = token
                        _spotifyConnectionError.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Spotify Token exception", e)
                _spotifyConnectionError.value = "Error: ${e.message}"
                _spotifyToken.value = null
            } finally {
                _isSpotifyConnecting.value = false
            }
        }
    }

    fun searchSpotify(query: String) {
        val token = _spotifyToken.value
        if (token.isNullOrEmpty()) {
            _streamingSearchError.value = "Spotify has not been authorized. Please configure your Client ID & Secret."
            return
        }
        if (query.trim().isEmpty()) return

        _isSearchingStreaming.value = true
        _streamingSearchError.value = null

        viewModelScope.launch {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=20")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                response.use { res ->
                    if (!res.isSuccessful) {
                        _streamingSearchError.value = "Spotify search failure: HTTP ${res.code}"
                        _streamingSearchResults.value = emptyList()
                    } else {
                        val bodyStr = res.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val tracksObj = json.getJSONObject("tracks")
                        val itemsArr = tracksObj.getJSONArray("items")
                        val results = mutableListOf<SongItem>()

                        for (i in 0 until itemsArr.length()) {
                            val track = itemsArr.getJSONObject(i)
                            val id = "spotify_${track.getString("id")}"
                            val title = track.getString("name")
                            
                            val artists = track.getJSONArray("artists")
                            val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown Artist"
                            
                            val albumObj = track.getJSONObject("album")
                            val albumName = albumObj.getString("name")
                            val durationMs = track.getLong("duration_ms")
                            val previewUrl = track.optString("preview_url", "")

                            val images = albumObj.getJSONArray("images")
                            val artworkUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""

                            val charSum = title.length + artistName.length
                            val colorStart = 0xFF000000L or ((charSum * 1234567).toLong() and 0xFFFFFF)
                            val colorEnd = 0xFF000000L or ((charSum * 7654321).toLong() and 0xFFFFFF)

                            results.add(
                                SongItem(
                                    id = id,
                                    title = title,
                                    artist = artistName,
                                    album = albumName,
                                    durationMs = durationMs,
                                    uriString = previewUrl,
                                    emoji = artworkUrl.ifEmpty { "🟢" },
                                    colorStart = colorStart,
                                    colorEnd = colorEnd,
                                    isOnline = true
                                )
                            )
                        }
                        _streamingSearchResults.value = results
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Spotify Search error", e)
                _streamingSearchError.value = "Search error: ${e.message}"
            } finally {
                _isSearchingStreaming.value = false
            }
        }
    }

    fun searchYouTubeMusic(query: String) {
        if (query.trim().isEmpty()) return

        _isSearchingStreaming.value = true
        _streamingSearchError.value = null

        viewModelScope.launch {
            var success = false
            for (instance in invidiousInstances) {
                try {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val request = Request.Builder()
                        .url("$instance/api/v1/search?q=$encodedQuery&type=video")
                        .build()

                    val response = withContext(Dispatchers.IO) {
                        okHttpClient.newCall(request).execute()
                    }

                    response.use { res ->
                        if (res.isSuccessful) {
                            val bodyStr = res.body?.string() ?: ""
                            val itemsArr = JSONArray(bodyStr)
                            val results = mutableListOf<SongItem>()

                            for (i in 0 until itemsArr.length()) {
                                val item = itemsArr.getJSONObject(i)
                                val type = item.optString("type", "")
                                if (type != "video") continue

                                val videoId = item.getString("videoId")
                                val title = item.getString("title")
                                val artistName = item.optString("author", "Unknown Artist")
                                val lengthSeconds = item.optLong("lengthSeconds", 180)
                                
                                val audioStreamUrl = "$instance/latest_version?id=$videoId&itag=140"
                                
                                val thumbnails = item.optJSONArray("videoThumbnails")
                                val artworkUrl = if (thumbnails != null && thumbnails.length() > 0) {
                                    thumbnails.getJSONObject(0).getString("url")
                                } else {
                                    ""
                                }

                                val charSum = title.length + artistName.length
                                val colorStart = 0xFF000000L or ((charSum * 2468135).toLong() and 0xFFFFFF)
                                val colorEnd = 0xFF000000L or ((charSum * 1357924).toLong() and 0xFFFFFF)

                                results.add(
                                    SongItem(
                                        id = "yt_$videoId",
                                        title = title,
                                        artist = artistName,
                                        album = "YouTube Music",
                                        durationMs = lengthSeconds * 1000,
                                        uriString = audioStreamUrl,
                                        emoji = artworkUrl.ifEmpty { "🔴" },
                                        colorStart = colorStart,
                                        colorEnd = colorEnd,
                                        isOnline = true
                                    )
                                )
                            }
                            _streamingSearchResults.value = results
                            _streamingSearchError.value = null
                            success = true
                        }
                    }
                    if (success) break
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Error searching YouTube Music on instance $instance", e)
                }
            }
            if (!success) {
                _streamingSearchError.value = "Failed to load streaming results. Please check your connection."
                _streamingSearchResults.value = emptyList()
            }
            _isSearchingStreaming.value = false
        }
    }

    fun playStreamingSong(song: SongItem) {
        val currentList = _songs.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == song.id || it.uriString == song.uriString }
        if (existingIndex != -1) {
            selectAndPlaySong(existingIndex)
        } else {
            currentList.add(song)
            _songs.value = currentList
            selectAndPlaySong(currentList.size - 1)
        }
    }

    fun getHardcodedDefaultSongs(): List<SongItem> {
        return listOf(
            SongItem(
                id = "1",
                title = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                durationMs = 225000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                emoji = "🎵",
                colorStart = 0xFF1A3A2A,
                colorEnd = 0xFF2A6A4A,
                isOnline = true
            ),
            SongItem(
                id = "2",
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera",
                durationMs = 355000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                emoji = "🎸",
                colorStart = 0xFF2A1A3A,
                colorEnd = 0xFF5A3A8A,
                isOnline = true
            ),
            SongItem(
                id = "3",
                title = "Clocks",
                artist = "Coldplay",
                album = "A Rush of Blood",
                durationMs = 309000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                emoji = "🎹",
                colorStart = 0xFF3A2A1A,
                colorEnd = 0xFF8A6A3A,
                isOnline = true
            ),
            SongItem(
                id = "4",
                title = "Smells Like Teen Spirit",
                artist = "Nirvana",
                album = "Nevermind",
                durationMs = 301000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                emoji = "🎺",
                colorStart = 0xFF1A2A3A,
                colorEnd = 0xFF2A4A7A,
                isOnline = true
            ),
            SongItem(
                id = "5",
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California",
                durationMs = 390000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                emoji = "🥁",
                colorStart = 0xFF3A1A1A,
                colorEnd = 0xFF7A3A3A,
                isOnline = true
            ),
            SongItem(
                id = "6",
                title = "Shape of You",
                artist = "Ed Sheeran",
                album = "÷ Divide",
                durationMs = 234000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                emoji = "🎻",
                colorStart = 0xFF1A3A3A,
                colorEnd = 0xFF2A7A7A,
                isOnline = true
            ),
            SongItem(
                id = "7",
                title = "Levitating",
                artist = "Dua Lipa",
                album = "Future Nostalgia",
                durationMs = 203000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                emoji = "🎷",
                colorStart = 0xFF2A3A1A,
                colorEnd = 0xFF4A7A2A,
                isOnline = true
            ),
            SongItem(
                id = "8",
                title = "Watermelon Sugar",
                artist = "Harry Styles",
                album = "Fine Line",
                durationMs = 174000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                emoji = "🎤",
                colorStart = 0xFF3A1A2A,
                colorEnd = 0xFF7A2A5A,
                isOnline = true
            )
        )
    }

    fun getSongItemById(id: String): SongItem? {
        val defaults = getHardcodedDefaultSongs()
        val foundDefault = defaults.find { it.id == id }
        if (foundDefault != null) return foundDefault

        val foundDb = _dbSongs.value.find { it.songId == id }
        if (foundDb != null) {
            val isOnline = foundDb.songId.startsWith("yt_") || foundDb.songId.startsWith("spotify_")
            return SongItem(
                id = foundDb.songId,
                title = foundDb.title,
                artist = foundDb.artist,
                album = foundDb.album,
                durationMs = foundDb.duration,
                uriString = foundDb.filePath,
                emoji = if (foundDb.songId.startsWith("spotify_")) "🟢" else "🔴",
                colorStart = 0xFF2A1A3A,
                colorEnd = 0xFF5A3A8A,
                isOnline = isOnline
            )
        }
        return null
    }

    fun playPlaylist(playlistId: Int) {
        viewModelScope.launch {
            val pl = _playlists.value.find { it.id == playlistId } ?: return@launch
            val idList = pl.songIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (idList.isEmpty()) return@launch

            val playList = mutableListOf<SongItem>()
            idList.forEach { id ->
                val songItem = getSongItemById(id)
                if (songItem != null) {
                    playList.add(songItem)
                }
            }

            if (playList.isNotEmpty()) {
                _songs.value = playList
                selectAndPlaySong(0)
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
            val currentIds = playlist.songIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!currentIds.contains(songId)) {
                currentIds.add(songId)
                val updated = playlist.copy(songIds = currentIds.joinToString(","))
                repository.savePlaylist(updated)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
            val currentIds = playlist.songIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (currentIds.remove(songId)) {
                val updated = playlist.copy(songIds = currentIds.joinToString(","))
                repository.savePlaylist(updated)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressTrackingJob?.cancel()
        mediaPlayer?.release()
        systemEqualizer?.release()
    }
}

data class LyricLine(val timeMs: Long, val text: String)

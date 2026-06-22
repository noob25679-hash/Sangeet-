package com.example.ui

import android.media.audiofx.Visualizer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.LyricLine
import com.example.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

// Master theme colors based on html definition
val DarkBg = Color(0xFF0E0E14)
val DarkSurface = Color(0xFF13131C)
val DarkCard = Color(0xFF1E1E28)
val DarkCard2 = Color(0xFF262632)
val NeonGreen = Color(0xFF6BF0A0)
val MutedText = Color(0xFF9896A8)
val DimColor = Color(0xFF3D3B52)

// Light theme colors
val LightBg = Color(0xFFF0EEF8)
val LightSurface = Color(0xFFE8E5F5)
val LightCard = Color(0xFFE4E2F0)
val LightCard2 = Color(0xFFDCDAF0)
val LightText = Color(0xFF1A1826)
val LightMutedText = Color(0xFF7A788A)

enum class ScreenType {
    HOME,
    LIBRARY,
    NOW_PLAYING,
    FOLDERS,
    PLAYLISTS,
    EQUALIZER,
    SYNC_CENTER,
    STREAMING
}

@Composable
fun BeatDropApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDarkMode by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf(ScreenType.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var playlistInput by remember { mutableStateOf("") }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }

    // ViewModel observational states using collectAsState with explicit type arguments
    val songs by viewModel.songs.collectAsState(initial = emptyList<com.example.data.SongItem>())
    val currentIdx by viewModel.currentSongIndex.collectAsState(initial = 0)
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val currentPositionMs by viewModel.currentPositionMs.collectAsState(initial = 0L)
    val durationMs by viewModel.durationMs.collectAsState(initial = 0L)
    val isShuffle by viewModel.isShuffle.collectAsState(initial = false)
    val isRepeat by viewModel.isRepeat.collectAsState(initial = false)
    val likedIds by viewModel.likedSongIds.collectAsState(initial = emptySet<String>())
    val parsedLyrics by viewModel.parsedLyrics.collectAsState(initial = emptyList<com.example.viewmodel.LyricLine>())
    val isRecordingLyrics by viewModel.isRecordingLyrics.collectAsState(initial = false)
    val selectedPreset by viewModel.selectedPreset.collectAsState(initial = "Normal")
    val eqGains by viewModel.eqGains.collectAsState(initial = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val isEqualizerEnabled by viewModel.isEqualizerEnabled.collectAsState(initial = true)
    val equalizerProfileName by viewModel.equalizerProfileName.collectAsState(initial = "remote-submix")
    val eqPresets = viewModel.eqPresets
    val playlists by viewModel.playlists.collectAsState(initial = emptyList<com.example.data.PlaylistEntity>())
    val syncCode by viewModel.syncCode.collectAsState(initial = "")
    val isSynced by viewModel.isSynced.collectAsState(initial = false)

    val spotifyClientId by viewModel.spotifyClientId.collectAsState(initial = "")
    val spotifyClientSecret by viewModel.spotifyClientSecret.collectAsState(initial = "")
    val spotifyToken by viewModel.spotifyToken.collectAsState(initial = null)
    val isSpotifyConnecting by viewModel.isSpotifyConnecting.collectAsState(initial = false)
    val spotifyConnectionError by viewModel.spotifyConnectionError.collectAsState(initial = null)
    val streamingSearchResults by viewModel.streamingSearchResults.collectAsState(initial = emptyList())
    val isSearchingStreaming by viewModel.isSearchingStreaming.collectAsState(initial = false)
    val streamingSearchError by viewModel.streamingSearchError.collectAsState(initial = null)
    
    var selectedPlaylistDetail by remember { mutableStateOf<PlaylistEntity?>(null) }

    val currentSong = songs.getOrNull(currentIdx)

    // Layout Styling parameters based on dynamic theme color
    val activeBg = if (isDarkMode) DarkBg else LightBg
    val activeSurface = if (isDarkMode) DarkSurface else LightSurface
    val activeCard = if (isDarkMode) DarkCard else LightCard
    val activeCard2 = if (isDarkMode) DarkCard2 else LightCard2
    val activeTextColor = if (isDarkMode) Color.White else LightText
    val activeMutedColor = if (isDarkMode) MutedText else LightMutedText

    // Dynamic gradient background animation matching --bg1 and --bg2 from active song colors!
    val bgGradStart = currentSong?.colorStart?.let { Color(it) } ?: Color(0xFF1A2A3A)
    val bgGradEnd = currentSong?.colorEnd?.let { Color(it) } ?: Color(0xFF0D1520)

    val backgroundBrush = if (isDarkMode) {
        Brush.radialGradient(
            colors = listOf(
                bgGradStart.copy(alpha = 0.55f),
                bgGradEnd.copy(alpha = 0.85f),
                Color(0xFF09090F)
            ),
            center = Offset(250f, 150f),
            radius = 1200f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                bgGradStart.copy(alpha = 0.25f),
                Color(0xFFEDEAF8),
                Color(0xFFF5F4FA)
            ),
            center = Offset(250f, 150f),
            radius = 1200f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App top level scaffold & core panel
        Column(modifier = Modifier.fillMaxSize()) {

            // Custom Toolbar with Status time + Logo + Theme toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & Logo Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF20DB), Color(0xFF8A20FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Sangeet",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Header actions row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action Buttons
                    IconButton(
                        onClick = { currentScreen = ScreenType.SYNC_CENTER },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Sync",
                            tint = if (isSynced) NeonGreen else activeMutedColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { currentScreen = ScreenType.EQUALIZER },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Equalizer",
                            tint = if (selectedPreset != "Normal") NeonGreen else activeMutedColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Theme button
                    IconButton(
                        onClick = { isDarkMode = !isDarkMode },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeCard)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = activeTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Avatar Letter
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFFF8E53)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Main Container holding Active Screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        // Framer-Motion style fluid enter and exit curves
                        slideInHorizontally(animationSpec = tween(400)) { width -> width / 3 } + fadeIn(animationSpec = tween(400)) togetherWith
                                slideOutHorizontally(animationSpec = tween(400)) { width -> -width / 3 } + fadeOut(animationSpec = tween(400))
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        ScreenType.HOME -> HomeScreen(
                            viewModel = viewModel,
                            songs = songs,
                            currentSong = currentSong,
                            likedIds = likedIds,
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2,
                            onNavigate = { currentScreen = it }
                        )
                        ScreenType.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            songs = songs,
                            currentSong = currentSong,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2
                        )
                        ScreenType.NOW_PLAYING -> NowPlayingScreen(
                            viewModel = viewModel,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isShuffle = isShuffle,
                            isRepeat = isRepeat,
                            likedIds = likedIds,
                            parsedLyrics = parsedLyrics,
                            isRecordingLyrics = isRecordingLyrics,
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2,
                            isDarkMode = isDarkMode,
                            onNavigateToEq = { currentScreen = ScreenType.EQUALIZER },
                            onNavigateToSync = { currentScreen = ScreenType.SYNC_CENTER }
                        )
                        ScreenType.FOLDERS -> FoldersScreen(
                            viewModel = viewModel,
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard
                        )
                        ScreenType.STREAMING -> StreamingScreen(
                            viewModel = viewModel,
                            spotifyClientId = spotifyClientId,
                            spotifyClientSecret = spotifyClientSecret,
                            spotifyToken = spotifyToken,
                            isSpotifyConnecting = isSpotifyConnecting,
                            spotifyConnectionError = spotifyConnectionError,
                            streamingSearchResults = streamingSearchResults,
                            isSearchingStreaming = isSearchingStreaming,
                            streamingSearchError = streamingSearchError,
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2,
                            playlists = playlists,
                            likedIds = likedIds
                        )
                        ScreenType.PLAYLISTS -> {
                            val detailPl = selectedPlaylistDetail
                            if (detailPl != null) {
                                val livePl = playlists.find { it.id == detailPl.id } ?: detailPl
                                PlaylistDetailScreen(
                                    playlist = livePl,
                                    viewModel = viewModel,
                                    activeTextColor = activeTextColor,
                                    activeMutedColor = activeMutedColor,
                                    activeCard = activeCard,
                                    onBack = { selectedPlaylistDetail = null }
                                )
                            } else {
                                PlaylistsScreen(
                                    playlists = playlists,
                                    activeTextColor = activeTextColor,
                                    activeMutedColor = activeMutedColor,
                                    activeCard = activeCard,
                                    onNewPlaylistClick = { showNewPlaylistDialog = true },
                                    onPlaylistClick = { selectedPlaylistDetail = it }
                                )
                            }
                        }
                        ScreenType.EQUALIZER -> EqualizerScreen(
                            selectedPreset = selectedPreset,
                            gains = eqGains,
                            presets = eqPresets,
                            onSelectPreset = { viewModel.selectPreset(it) },
                            onBandChange = { band, valDb -> viewModel.updateCustomBand(band, valDb) },
                            isEqualizerEnabled = isEqualizerEnabled,
                            onToggleEqualizer = { viewModel.toggleEqualizer(it) },
                            equalizerProfileName = equalizerProfileName,
                            onUpdateProfileName = { viewModel.updateEqualizerProfileName(it) },
                            audioSessionId = viewModel.getAudioSessionId(),
                            isPlaying = isPlaying,
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2,
                            onDismiss = { currentScreen = ScreenType.NOW_PLAYING }
                        )
                        ScreenType.SYNC_CENTER -> SyncCenterScreen(
                            syncCode = syncCode ?: "",
                            isSynced = isSynced,
                            onGenerateSync = { viewModel.generateSyncSession() },
                            onJoinSync = { viewModel.joinSyncSession(it) },
                            onDisconnect = { viewModel.disconnectSync() },
                            activeTextColor = activeTextColor,
                            activeMutedColor = activeMutedColor,
                            activeCard = activeCard,
                            activeCard2 = activeCard2,
                            onDismiss = { currentScreen = ScreenType.NOW_PLAYING }
                        )
                    }
                }
            }

            // Mini Player Bar (Visible on all screens except NOW_PLAYING, EQUALIZER, and SYNC_CENTER)
            if (currentScreen != ScreenType.NOW_PLAYING && currentScreen != ScreenType.EQUALIZER && currentScreen != ScreenType.SYNC_CENTER && currentSong != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(activeCard2)
                        .clickable { currentScreen = ScreenType.NOW_PLAYING }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(currentSong.colorStart), Color(currentSong.colorEnd)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentSong.emoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            color = activeTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong.artist,
                            color = activeMutedColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.prevTrack() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Prev",
                                tint = activeMutedColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Mini play block with spring interaction animations
                        val miniPlayInteractionSource = remember { MutableInteractionSource() }
                        val isMiniPlayPressed by miniPlayInteractionSource.collectIsPressedAsState()
                        
                        val miniPlayScale by animateFloatAsState(
                            targetValue = if (isMiniPlayPressed) 0.88f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "MiniPlayScale"
                        )

                        val miniIconScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.0f else 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "MiniPlayIconScale"
                        )

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            interactionSource = miniPlayInteractionSource,
                            modifier = Modifier
                                .size(36.dp)
                                .scale(miniPlayScale)
                                .clip(CircleShape)
                                .background(activeTextColor)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "PlayPause",
                                tint = if (isDarkMode) Color.Black else Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(miniIconScale)
                            )
                        }
                        IconButton(onClick = { viewModel.nextTrack() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = activeMutedColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeSurface.copy(alpha = 0.96f))
                    .border(1.dp, activeMutedColor.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == ScreenType.HOME,
                    onClick = { currentScreen = ScreenType.HOME },
                    activeColor = NeonGreen,
                    inactiveColor = activeMutedColor,
                    modifier = Modifier.testTag("nav_home")
                )
                NavItem(
                    label = "Library",
                    icon = Icons.Default.List,
                    isSelected = currentScreen == ScreenType.LIBRARY,
                    onClick = { currentScreen = ScreenType.LIBRARY },
                    activeColor = NeonGreen,
                    inactiveColor = activeMutedColor,
                    modifier = Modifier.testTag("nav_library")
                )
                // Highlighted core circle button for NOW_PLAYING
                Box(
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                        .clickable { currentScreen = ScreenType.NOW_PLAYING }
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Playing",
                        tint = DarkBg,
                        modifier = Modifier.size(26.dp)
                    )
                }
                NavItem(
                    label = "Streaming",
                    icon = Icons.Default.Cloud,
                    isSelected = currentScreen == ScreenType.STREAMING,
                    onClick = { currentScreen = ScreenType.STREAMING },
                    activeColor = NeonGreen,
                    inactiveColor = activeMutedColor,
                    modifier = Modifier.testTag("nav_streaming")
                )
                NavItem(
                    label = "Playlists",
                    icon = Icons.Default.Favorite,
                    isSelected = currentScreen == ScreenType.PLAYLISTS,
                    onClick = { currentScreen = ScreenType.PLAYLISTS },
                    activeColor = NeonGreen,
                    inactiveColor = activeMutedColor,
                    modifier = Modifier.testTag("nav_playlists")
                )
            }
        }

        // New Playlist creation dialog
        if (showNewPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showNewPlaylistDialog = false },
                title = { Text("Create Playlist", color = activeTextColor) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = playlistInput,
                            onValueChange = { playlistInput = it },
                            label = { Text("Playlist Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = activeTextColor,
                                unfocusedTextColor = activeTextColor
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (playlistInput.isNotBlank()) {
                                viewModel.createPlaylist(playlistInput)
                                playlistInput = ""
                                showNewPlaylistDialog = false
                                Toast.makeText(context, "Playlist created successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewPlaylistDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

// ══════════════ HOME SCREEN Composable ══════════════
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    songs: List<SongItem>,
    currentSong: SongItem?,
    likedIds: Set<String>,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color,
    onNavigate: (ScreenType) -> Unit
) {
    val isOfflineOnly by viewModel.isOfflineOnly.collectAsState()
    
    val spotifyClientId by viewModel.spotifyClientId.collectAsState(initial = "")
    val spotifyClientSecret by viewModel.spotifyClientSecret.collectAsState(initial = "")
    val spotifyToken by viewModel.spotifyToken.collectAsState(initial = null)
    val isSpotifyConnecting by viewModel.isSpotifyConnecting.collectAsState(initial = false)
    val spotifyConnectionError by viewModel.spotifyConnectionError.collectAsState(initial = null)
    val streamingSearchResults by viewModel.streamingSearchResults.collectAsState(initial = emptyList())
    val isSearchingStreaming by viewModel.isSearchingStreaming.collectAsState(initial = false)
    val streamingSearchError by viewModel.streamingSearchError.collectAsState(initial = null)
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        // Sleek top-right offline/online toggle on landing page (HomeScreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOfflineOnly) "Offline Library" else "Online Streams",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeTextColor
            )
            
            // Capsule toggle selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(activeCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isOfflineOnly) NeonGreen else Color.Transparent)
                        .clickable { if (!isOfflineOnly) { viewModel.toggleOfflineOnly() } }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Offline Mode",
                            tint = if (isOfflineOnly) Color.Black else activeMutedColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOfflineOnly) Color.Black else activeMutedColor
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (!isOfflineOnly) NeonGreen else Color.Transparent)
                        .clickable { if (isOfflineOnly) { viewModel.toggleOfflineOnly() } }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Online Mode",
                            tint = if (!isOfflineOnly) Color.Black else activeMutedColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isOfflineOnly) Color.Black else activeMutedColor
                        )
                    }
                }
            }
        }

        if (isOfflineOnly) {
            var selectedCategory by remember { mutableStateOf("All") }
            val categories = listOf("All", "Rock", "Pop", "Hip Hop", "Chill", "Workout", "Trek")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // Mood / Genre pills row
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        if (selectedCategory == category) NeonGreen.copy(alpha = 0.15f)
                                        else activeCard
                                    )
                                    .border(
                                        1.dp,
                                        if (selectedCategory == category) NeonGreen.copy(alpha = 0.40f)
                                        else Color.Transparent,
                                        RoundedCornerShape(50.dp)
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    color = if (selectedCategory == category) NeonGreen else activeMutedColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Promo Banner
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1A2A5A), Color(0xFF0D1840))))
                            .border(1.dp, Color(0xFF5082FF).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { onNavigate(ScreenType.LIBRARY) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your library is ready",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "248 songs scanned · Tap to explore",
                                color = MutedText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Details",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Section Title: Recently Played Grid (3 Columns Aspect Ratio = 1)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Played",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeTextColor
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View More",
                            tint = activeMutedColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                item {
                    // Speed dial grid: 3 items in a row, up to 6 items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val songIdx = row * 3 + col
                                    val song = songs.getOrNull(songIdx)
                                    if (song != null) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd))))
                                                .clickable { viewModel.selectAndPlaySong(songIdx) }
                                        ) {
                                            // Emoji art in center
                                            Text(
                                                text = song.emoji,
                                                fontSize = 32.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                            // Card title overlay with gradient background for readability
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = song.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Picks Section (horizontal scroll list)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Picks",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeTextColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(activeCard)
                                .border(1.dp, activeMutedColor.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                .clickable { viewModel.selectAndPlaySong(0) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Play all", fontSize = 11.sp, color = activeTextColor)
                        }
                    }
                }

                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Showing items 6, 7 and 0 as Quick Picks matching HTML
                        val qpIndices = listOf(6, 7, 0)
                        items(qpIndices) { idx ->
                            val song = songs.getOrNull(idx)
                            if (song != null) {
                                Row(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(activeCard)
                                        .clickable { viewModel.selectAndPlaySong(idx) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = song.emoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 11.sp,
                                            color = activeMutedColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    // Small Play Button Green Circular
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "▶", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Title: Liked Songs list
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Liked Songs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeTextColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(activeCard)
                                .border(1.dp, activeMutedColor.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                .clickable {
                                    val likedIndices = songs.indices.filter { likedIds.contains(songs[it].id) }
                                    if (likedIndices.isNotEmpty()) {
                                        viewModel.selectAndPlaySong(likedIndices.first())
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Play all", fontSize = 11.sp, color = activeTextColor)
                        }
                    }
                }

                // Sub List displaying Liked items only or defaults representing Liked songs (0, 4, 2)
                val likedIndicesDisplay = songs.indices.filter { likedIds.contains(songs[it].id) }.ifEmpty { listOf(0, 4, 2) }
                items(likedIndicesDisplay) { idx ->
                    val song = songs.getOrNull(idx)
                    if (song != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectAndPlaySong(idx) }
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = song.emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 11.sp,
                                    color = activeMutedColor
                                )
                            }
                            Text(
                                text = "3:45",
                                fontSize = 11.sp,
                                color = DimColor
                            )
                        }
                    }
                }
            }
        } else {
            // Streaming Screen Content inline on landing page when Online is selected!
            StreamingScreen(
                viewModel = viewModel,
                spotifyClientId = spotifyClientId,
                spotifyClientSecret = spotifyClientSecret,
                spotifyToken = spotifyToken,
                isSpotifyConnecting = isSpotifyConnecting,
                spotifyConnectionError = spotifyConnectionError,
                streamingSearchResults = streamingSearchResults,
                isSearchingStreaming = isSearchingStreaming,
                streamingSearchError = streamingSearchError,
                activeTextColor = activeTextColor,
                activeMutedColor = activeMutedColor,
                activeCard = activeCard,
                activeCard2 = activeCard2,
                playlists = playlists,
                likedIds = likedIds,
                showTitle = false
            )
        }
    }
}

// ══════════════ LIBRARY SCREEN Composable ══════════════
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    songs: List<SongItem>,
    currentSong: SongItem?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color
) {
    var libraryTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeTextColor
            )
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = activeMutedColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Sliding tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("All Songs", "Downloads", "Albums", "Artists")
            tabs.forEachIndexed { i, title ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (libraryTab == i) activeTextColor.copy(alpha = 0.18f) else activeTextColor.copy(alpha = 0.06f))
                        .clickable { libraryTab = i }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (libraryTab == i) activeTextColor else activeMutedColor
                    )
                }
            }
        }

        // Search Bar matching Html looks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(activeTextColor.copy(alpha = 0.06f))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = DimColor,
                    modifier = Modifier.size(14.dp)
                )
                BasicTextFieldWithoutDecoration(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search your library...",
                    textColor = activeTextColor,
                    placeholderColor = DimColor,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }
        }

        // Standard Filter lists
        val downloadProgress by viewModel.downloadProgress.collectAsState()
        val songsToRender = if (libraryTab == 1) {
            songs.filter { downloadProgress[it.id] == 100 }.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (libraryTab == 1) {
                item {
                    val totalSizeFlow by viewModel.totalDownloadedSize.collectAsState()
                    val downloadedSongsCount = songs.count { downloadProgress[it.id] == 100 }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(activeCard2)
                            .border(1.dp, activeTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Storage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeMutedColor
                            )
                            Text(
                                text = String.format("%.1f MB used", totalSizeFlow / (1024f * 1024f)),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeTextColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "$downloadedSongsCount offline tracks ready",
                                fontSize = 11.sp,
                                color = activeMutedColor
                            )
                        }

                        if (downloadedSongsCount > 0) {
                            TextButton(
                                onClick = { viewModel.deleteAllDownloads() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete All",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (songsToRender.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (libraryTab == 1) "No downloaded songs" else "No songs matching query",
                            fontWeight = FontWeight.Bold,
                            color = activeTextColor,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (libraryTab == 1) "Download songs to play offline!" else "Try searching something else",
                            color = activeMutedColor,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                itemsIndexed(songsToRender) { _, song ->
                    val isPlayingThis = currentSong?.id == song.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPlayingThis) activeTextColor.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable {
                                val masterIndex = songs.indexOfFirst { it.id == song.id }
                                if (masterIndex != -1) {
                                    viewModel.selectAndPlaySong(masterIndex)
                                } else {
                                    viewModel.playStreamingSong(song)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = song.emoji, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlayingThis) NeonGreen else activeTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 11.sp,
                                color = activeMutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SongDownloadIndicator(
                                song = song,
                                viewModel = viewModel,
                                activeTextColor = activeTextColor,
                                activeMutedColor = activeMutedColor
                            )

                            if (isPlayingThis) {
                                // Immersive Equalizer Bouncing Simulation Bars
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .height(16.dp)
                                        .padding(end = 8.dp)
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
                                    val barHeights = listOf(
                                        infiniteTransition.animateValue(
                                            initialValue = 6.dp, targetValue = 15.dp, typeConverter = DpToValue,
                                            animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
                                        ),
                                        infiniteTransition.animateValue(
                                            initialValue = 12.dp, targetValue = 4.dp, typeConverter = DpToValue,
                                            animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
                                        ),
                                        infiniteTransition.animateValue(
                                            initialValue = 16.dp, targetValue = 8.dp, typeConverter = DpToValue,
                                            animationSpec = infiniteRepeatable(animation = tween(450, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
                                        )
                                    )
                                    barHeights.forEach { heightVal ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(heightVal.value)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(NeonGreen)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "3:45",
                                    fontSize = 11.sp,
                                    color = DimColor,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongDownloadIndicator(
    song: SongItem,
    viewModel: MusicViewModel,
    activeTextColor: Color,
    activeMutedColor: Color,
    modifier: Modifier = Modifier
) {
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val progress = downloadProgress[song.id]

    Box(
        modifier = modifier
            .size(36.dp)
            .clickable {
                if (progress == null || progress == -1) {
                    viewModel.downloadTrack(song)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            progress == 100 -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Available Offline",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
            }
            progress != null && progress in 0..99 -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        color = Color(0xFFE65A73),
                        trackColor = activeMutedColor.copy(alpha = 0.2f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "$progress",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )
                }
            }
            progress == -1 -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Download Failed",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Tap to Download",
                    tint = activeMutedColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

val DpToValue = TwoWayConverter<androidx.compose.ui.unit.Dp, AnimationVector1D>(
    convertToVector = { AnimationVector1D(it.value) },
    convertFromVector = { it.value.dp }
)

@Composable
fun BasicTextFieldWithoutDecoration(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    placeholderColor: Color,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = placeholderColor, style = textStyle)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = textColor),
            cursorBrush = SolidColor(textColor),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ══════════════ NOW PLAYING SCREEN Composable ══════════════
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    currentSong: SongItem?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    likedIds: Set<String>,
    parsedLyrics: List<LyricLine>,
    isRecordingLyrics: Boolean,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color,
    isDarkMode: Boolean,
    onNavigateToEq: () -> Unit,
    onNavigateToSync: () -> Unit
) {
    if (currentSong == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select song to play", color = activeTextColor)
        }
        return
    }

    // Secondary subTabs matching online-offline controls
    var panelTab by remember { mutableStateOf(0) } // 0 = Player Controls, 1 = Synced Lyrics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Option Menu Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateToSync,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(activeCard2)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Sync Info",
                    tint = activeTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Now Playing",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = activeMutedColor
            )
            IconButton(
                onClick = onNavigateToEq,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(activeCard2)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Eq Mixer",
                    tint = activeTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // SubTabs for switching lyrics & player cards
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (panelTab == 0) activeTextColor.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { panelTab = 0 }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Vibe Controls", fontSize = 11.sp, color = if (panelTab == 0) activeTextColor else activeMutedColor)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (panelTab == 1) activeTextColor.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { panelTab = 1 }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Scrolling Lyrics", fontSize = 11.sp, color = if (panelTab == 1) activeTextColor else activeMutedColor)
            }
        }

        if (panelTab == 0) {
            // Album artwork with smooth rotation animation when active
            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = if (isPlaying) 360f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(1f)
                        .rotate(rotationAngle)
                        .shadow(24.dp, shape = RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(currentSong.colorStart), Color(currentSong.colorEnd)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentSong.emoji,
                        fontSize = 88.sp
                    )
                }
            }

            // Real-time frequency spectral audio visualizer
            RealtimeAudioVisualizer(
                audioSessionId = viewModel.getAudioSessionId(),
                isPlaying = isPlaying,
                activeTextColor = activeTextColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 6.dp),
                barCount = 36,
                accentColor = Color(0xFFE65A73)
            )

            // Track details row with like toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentSong.artist} · ${currentSong.album}",
                        fontSize = 13.sp,
                        color = activeMutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val isLiked = likedIds.contains(currentSong.id)
                IconButton(
                    onClick = { viewModel.toggleLikeCurrentSong() },
                    modifier = Modifier.testTag("like_song_button")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF5C3A) else activeMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Interaction source and states for Seekbar Slider dynamic animation
            val sliderInteractionSource = remember { MutableInteractionSource() }
            val isSliderDragged by sliderInteractionSource.collectIsDraggedAsState()
            val isSliderPressed by sliderInteractionSource.collectIsPressedAsState()
            val isSliderActive = isSliderDragged || isSliderPressed

            // Gentle spring animation for the seek bar scale when being adjusted
            val sliderScale by animateFloatAsState(
                targetValue = if (isSliderActive) 1.05f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "SeekBarScale"
            )

            // Smooth spring floating glider value for timeline progression
            val targetPosition = if (durationMs > 0) currentPositionMs.toFloat() else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = targetPosition,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "SeekBarProgress"
            )
            // Use animated progression when simple playing, raw precise position when active dragging
            val displayedProgress = if (isSliderActive) targetPosition else animatedProgress

            // Floating Player Slider Card Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .scale(sliderScale),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = activeCard)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Progress scrub bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            fontSize = 11.sp,
                            color = activeMutedColor
                        )
                        Slider(
                            value = displayedProgress,
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 100f),
                            interactionSource = sliderInteractionSource,
                            colors = SliderDefaults.colors(
                                thumbColor = activeTextColor,
                                activeTrackColor = activeTextColor,
                                inactiveTrackColor = activeTextColor.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .testTag("playback_progress_slider")
                        )
                        Text(
                            text = "-" + formatTime(maxOf(0L, durationMs - currentPositionMs)),
                            fontSize = 11.sp,
                            color = activeMutedColor
                        )
                    }

                    // Multi Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) NeonGreen else activeMutedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.prevTrack() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Prev",
                                tint = activeTextColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Center white play block with spring interaction animations
                        val playButtonInteractionSource = remember { MutableInteractionSource() }
                        val isPlayButtonPressed by playButtonInteractionSource.collectIsPressedAsState()
                        
                        // Button shrink and bounce-back on touch/release
                        val playButtonScale by animateFloatAsState(
                            targetValue = if (isPlayButtonPressed) 0.88f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "PlayButtonScale"
                        )

                        // Icon pop-bounce scale on state change
                        val iconScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.0f else 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "PlayIconScale"
                        )

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            interactionSource = playButtonInteractionSource,
                            modifier = Modifier
                                .size(52.dp)
                                .scale(playButtonScale)
                                .clip(CircleShape)
                                .background(activeTextColor)
                                .shadow(8.dp, CircleShape)
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isDarkMode) Color.Black else Color.White,
                                modifier = Modifier
                                    .size(26.dp)
                                    .scale(iconScale)
                            )
                        }

                        IconButton(onClick = { viewModel.nextTrack() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = activeTextColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeat) NeonGreen else activeMutedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Sound Track Volume Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "MUTE",
                            tint = DimColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(activeTextColor.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.70f)
                                    .fillMaxHeight()
                                    .background(activeTextColor.copy(alpha = 0.40f))
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "MAX",
                            tint = DimColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Dynamic scrolled lyrics pane that user can tap to scrub!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synchronized Scrolling",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )

                    // LRC Recorder Trigger Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isRecordingLyrics) Color.Red.copy(alpha = 0.2f) else activeCard)
                            .border(1.dp, if (isRecordingLyrics) Color.Red else activeTextColor.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                            .clickable {
                                if (isRecordingLyrics) {
                                    viewModel.commitCustomLyricsRecord()
                                } else {
                                    viewModel.startRecordingLyrics()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRecordingLyrics) "🔴 Save LRC" else "✏️ Sync Custom LRC",
                            fontSize = 10.sp,
                            color = if (isRecordingLyrics) Color.Red else activeTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Find currently active lyric line
                val activeLineIdx = parsedLyrics.indexOfLast { currentPositionMs >= it.timeMs }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 40.dp)
                ) {
                    itemsIndexed(parsedLyrics) { lineIdx, line ->
                        val isActive = lineIdx == activeLineIdx
                        val scopeMultiplier = if (isActive) 1.25f else 0.9f
                        val spotlightColor = if (isActive) NeonGreen else activeTextColor.copy(alpha = 0.4f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (isRecordingLyrics) {
                                        viewModel.recordTimestampForLine(lineIdx)
                                    } else {
                                        viewModel.seekTo(line.timeMs)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = line.text,
                                color = spotlightColor,
                                fontSize = (14 * scopeMultiplier).sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                            if (isRecordingLyrics && line.timeMs > 0L) {
                                Text(
                                    text = "Synced: " + formatTime(line.timeMs),
                                    fontSize = 9.sp,
                                    color = NeonGreen.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

// ══════════════ FOLDERS SCREEN Composable ══════════════
@Composable
fun FoldersScreen(
    viewModel: MusicViewModel,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color
) {
    val context = LocalContext.current
    val folders = listOf(
        Pair("Music", "248 songs"),
        Pair("Downloads", "64 songs"),
        Pair("Podcasts", "12 files"),
        Pair("Trek Vibes", "38 songs"),
        Pair("WhatsApp Audio", "7 files")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Folders",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeTextColor
            )
            IconButton(onClick = {
                viewModel.scanLocalMusic()
                Toast.makeText(context, "Scanning completed!", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Scan Storage",
                    tint = activeMutedColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Tip: Tap Search icon in corner to scan external local folders!",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            items(folders) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.scanLocalMusic()
                            Toast.makeText(context, "Importing audio metadata...", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = folder.first,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTextColor
                            )
                            Text(
                                text = folder.second,
                                fontSize = 11.sp,
                                color = activeMutedColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = activeMutedColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ══════════════ PLAYLISTS SCREEN Composable ══════════════
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    onNewPlaylistClick: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeTextColor
            )
            IconButton(onClick = onNewPlaylistClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New",
                    tint = activeMutedColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Manual customized "New Playlist" dotted cell
            item {
                Box(
                    modifier = Modifier
                        .height(168.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(NeonGreen, activeMutedColor)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onNewPlaylistClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "＋", fontSize = 26.sp, color = activeMutedColor)
                        Text(text = "New Playlist", fontSize = 11.sp, color = activeMutedColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Database saved playlists representation
            items(playlists) { playlist ->
                val trackCount = if (playlist.songIds.isEmpty()) 0 else playlist.songIds.split(",").size
                Card(
                    modifier = Modifier
                        .height(168.dp)
                        .clickable { onPlaylistClick(playlist) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeCard)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(Color(0xFF3A1A1A), Color(0xFF7A2A1A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = playlist.emoji, fontSize = 38.sp)
                        }
                        Column(modifier = Modifier.padding(9.dp)) {
                            Text(
                                text = playlist.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (trackCount == 1) "1 track" else "$trackCount tracks",
                                fontSize = 11.sp,
                                color = activeMutedColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════ LARK PRESET EQUALIZER Composable ══════════════
@Composable
fun EqualizerScreen(
    selectedPreset: String,
    gains: FloatArray,
    presets: List<String>,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    isEqualizerEnabled: Boolean,
    onToggleEqualizer: (Boolean) -> Unit,
    equalizerProfileName: String,
    onUpdateProfileName: (String) -> Unit,
    audioSessionId: Int,
    isPlaying: Boolean,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equalizer Toolbar header with Profile Renamer & Enable/Disable Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Collapse",
                    tint = activeTextColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Equalizer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = activeTextColor
                )
                
                var showRenameDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clickable { showRenameDialog = true }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Name",
                        tint = activeMutedColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = equalizerProfileName,
                        fontSize = 13.sp,
                        color = activeMutedColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showRenameDialog) {
                    var tempName by remember { mutableStateOf(equalizerProfileName) }
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename Equalizer Profile") },
                        text = {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. remote-submix") }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (tempName.isNotBlank()) {
                                        onUpdateProfileName(tempName)
                                    }
                                    showRenameDialog = false
                                }
                            ) {
                                Text("Rename")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Master switch toggling bypass state
            Switch(
                checked = isEqualizerEnabled,
                onCheckedChange = { onToggleEqualizer(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFE65A73),
                    uncheckedThumbColor = activeMutedColor,
                    uncheckedTrackColor = activeCard
                )
            )
        }

        // Live feedback Audio Visualizer
        RealtimeAudioVisualizer(
            audioSessionId = audioSessionId,
            isPlaying = isPlaying,
            activeTextColor = activeTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            barCount = 28,
            accentColor = Color(0xFFE65A73)
        )

        // 10-Band Vertical Slider layout (Mixer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(activeCard2.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val faderBands = listOf("31", "63", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
            gains.forEachIndexed { bandIdx, gain ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // dB indicator at top
                    Text(
                        text = String.format("%+.0f", gain),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEqualizerEnabled) Color(0xFFE65A73) else activeMutedColor
                    )

                    // Vertical slider track and thumb
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(20.dp)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isEqualizerEnabled) Color(0xFFE65A73).copy(alpha = 0.2f)
                                    else activeTextColor.copy(alpha = 0.05f)
                                )
                        )

                        // Rotated Slider element
                        Slider(
                            value = gain,
                            onValueChange = { if (isEqualizerEnabled) onBandChange(bandIdx, it) },
                            valueRange = -15f..15f,
                            enabled = isEqualizerEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = if (isEqualizerEnabled) Color(0xFFE65A73) else activeMutedColor,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxHeight()
                                .rotate(270f)
                        )
                    }

                    // Frequency label at bottom
                    Text(
                        text = faderBands.getOrElse(bandIdx) { "" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeMutedColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid-based Presets arrangement
        val chunkedPresets = presets.chunked(2)
        chunkedPresets.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPresets.forEach { preset ->
                    val isSel = selectedPreset == preset
                    PresetCard(
                        preset = preset,
                        isSel = isSel,
                        isEqualizerEnabled = isEqualizerEnabled,
                        activeCard = activeCard,
                        onClick = { if (isEqualizerEnabled) onSelectPreset(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPresets.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: String,
    isSel: Boolean,
    isEqualizerEnabled: Boolean,
    activeCard: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (preset) {
        "Custom" -> Icons.Default.Tune
        "Bass booster" -> Icons.Default.GraphicEq
        "Vocal booster" -> Icons.Default.Mic
        "Treble booster" -> Icons.Default.Hearing
        "Normal" -> Icons.Default.SentimentSatisfied
        "Pop" -> Icons.Default.MusicNote
        "HipHop" -> Icons.Default.SpeakerGroup
        "Rock" -> Icons.Default.Star
        "Electronic" -> Icons.Default.FlashOn
        "Heavy metal" -> Icons.Default.Album
        "R&B" -> Icons.Default.Favorite
        "Folk" -> Icons.Default.MusicNote
        "Dance" -> Icons.Default.Celebration
        "Jazz" -> Icons.Default.MusicNote
        "Classic" -> Icons.Default.MusicNote
        "Latin" -> Icons.Default.MusicNote
        else -> Icons.Default.MusicNote
    }

    val cardBg = if (isSel) Color(0xFFFACCD3) else activeCard
    val contentColor = if (isSel) Color(0xFF4C0519) else Color.White
    val mutedColor = if (isSel) Color(0xFF881337) else Color(0xFF9896A8)

    Box(
        modifier = modifier
            .alpha(if (isEqualizerEnabled) 1.0f else 0.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable(enabled = isEqualizerEnabled) { onClick() }
            .padding(12.dp)
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = preset,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
                Text(
                    text = if (preset == "Custom") "Personalized" else "Profile",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = mutedColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RealtimeAudioVisualizer(
    audioSessionId: Int,
    isPlaying: Boolean,
    activeTextColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    accentColor: Color = Color(0xFFE65A73)
) {
    val barHeights = remember { mutableStateListOf<Float>().apply {
        repeat(barCount) { add(0.1f) }
    } }

    var isUsingNative by remember { mutableStateOf(false) }

    DisposableEffect(audioSessionId) {
        var hostVisualizer: Visualizer? = null
        if (audioSessionId > 0) {
            try {
                val viz = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtMost(256)
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            sRate: Int
                        ) {}

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            sRate: Int
                        ) {
                            if (fft == null || fft.isEmpty()) return
                            val numBins = fft.size / 2
                            val groupSize = maxOf(1, numBins / barCount)
                            
                            for (i in 0 until barCount) {
                                var sumMag = 0f
                                val startBin = (i * groupSize).coerceIn(0, numBins - 1)
                                val endBin = ((i + 1) * groupSize).coerceIn(0, numBins)
                                var count = 0
                                
                                for (k in startBin until endBin) {
                                    val r = fft[2 * k].toFloat()
                                    val im = fft[2 * k + 1].toFloat()
                                    val mag = kotlin.math.sqrt(r * r + im * im)
                                    sumMag += mag
                                    count++
                                }
                                
                                val rawHeight = if (count > 0) sumMag / count else 0f
                                val normalizedHeight = (rawHeight / 15f).coerceIn(0f, 1f)
                                
                                val currentVal = barHeights.getOrElse(i) { 0.1f }
                                val targetVal = if (isPlaying) maxOf(0.08f, normalizedHeight) else 0.08f
                                if (i in barHeights.indices) {
                                    barHeights[i] = currentVal * 0.4f + targetVal * 0.6f
                                }
                            }
                            isUsingNative = true
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)
                    
                    enabled = true
                }
                hostVisualizer = viz
            } catch (e: Exception) {
                android.util.Log.e("AudioVisualizer", "Native Visualizer bind failed: ${e.message}")
                isUsingNative = false
            }
        } else {
            isUsingNative = false
        }

        onDispose {
            try {
                hostVisualizer?.enabled = false
                hostVisualizer?.release()
            } catch (e: Exception) {
                android.util.Log.e("AudioVisualizer", "Error freeing Visualizer: ${e.message}")
            }
        }
    }

    LaunchedEffect(isPlaying) {
        var phase = 0f
        while (true) {
            withFrameNanos { _ ->
                phase += 0.05f
                if (phase > 2 * kotlin.math.PI.toFloat()) {
                    phase -= 2 * kotlin.math.PI.toFloat()
                }

                if (!isUsingNative) {
                    for (i in 0 until barCount) {
                        val currentVal = barHeights.getOrElse(i) { 0.1f }
                        val targetHeight = if (isPlaying) {
                            val bassFreq = kotlin.math.sin(phase * 1.8f + i * 0.2f) * 0.35f
                            val midFreq = kotlin.math.cos(phase * 3.5f - i * 0.35f) * 0.2f
                            val trebleFreq = kotlin.math.sin(phase * 8.0f + i * 0.7f) * 0.1f
                            
                            val envelope = if (i < barCount * 0.25f) {
                                0.7f - (i * 0.04f)
                            } else if (i < barCount * 0.75f) {
                                0.4f + (kotlin.math.sin(i * 0.15f) * 0.08f)
                            } else {
                                0.25f - ((i - barCount * 0.75f) * 0.03f)
                            }

                            val combined = 0.08f + (bassFreq + midFreq + trebleFreq + 0.7f) * envelope
                            val randomizedNudge = (kotlin.math.sin(phase * 15f + i) * 0.02f)
                            (combined + randomizedNudge).coerceIn(0.08f, 1f)
                        } else {
                            0.04f
                        }

                        val smoothingFactor = if (isPlaying) 0.15f else 0.08f
                        if (i in barHeights.indices) {
                            barHeights[i] = currentVal + (targetHeight - currentVal) * smoothingFactor
                        }
                    }
                } else if (!isPlaying) {
                    for (i in 0 until barCount) {
                        val currentVal = barHeights.getOrElse(i) { 0.1f }
                        if (i in barHeights.indices) {
                            barHeights[i] = currentVal + (0.04f - currentVal) * 0.12f
                        }
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCountFloat = barCount.toFloat()
        
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCountFloat

        for (i in 0 until barCount) {
            val barHeightCoeff = barHeights.getOrNull(i) ?: 0.05f
            val calculatedBarHeight = height * barHeightCoeff
            
            val top = (height - calculatedBarHeight) / 2f
            val bottom = top + calculatedBarHeight
            val left = i * (barWidth + spacing)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.3f),
                        accentColor,
                        accentColor.copy(alpha = 0.9f),
                        accentColor.copy(alpha = 0.3f)
                    ),
                    startY = top,
                    endY = bottom
                ),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, calculatedBarHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun Arrangement.SpaceSpacey(): Arrangement.Horizontal {
    return Arrangement.SpaceAround
}

// ══════════════ CROSS DEVICE SYNC (Echo pair) Composable ══════════════
@Composable
fun SyncCenterScreen(
    syncCode: String,
    isSynced: Boolean,
    onGenerateSync: () -> Unit,
    onJoinSync: (String) -> Unit,
    onDisconnect: () -> Unit,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = activeTextColor
                )
            }
            Text(
                text = "Echo Sync Network",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = activeTextColor
            )
            Spacer(modifier = Modifier.width(36.dp))
        }

        Text(
            text = "Pair your active music session over the air to echo song details, custom lyrics, and Lark equalizer presets in flawless harmony on secondary phones/tabs!",
            color = activeMutedColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        if (isSynced) {
            // Connected device panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = activeCard2)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Synced",
                            tint = NeonGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "Session Broadcaster Active",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )

                    Text(
                        text = "Device Signature Code: $syncCode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonGreen
                    )

                    Text(
                        text = "Currently Syncing:\n- Stream Queue\n- Visual Spectral\n- Scrolling Timestamps",
                        fontSize = 11.sp,
                        color = activeMutedColor,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            onDisconnect()
                            Toast.makeText(context, "Session disconnected", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("Stop Syncing", color = Color.White)
                    }
                }
            }
        } else {
            // Create sync container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = activeCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generate Broadcaster Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )
                    Button(
                        onClick = {
                            onGenerateSync()
                            Toast.makeText(context, "Sync Session Created!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Broadcast Session", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // JOIN session container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = activeCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Echo Secondary Session",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { if (it.length <= 6) inputCode = it },
                        placeholder = { Text("Code") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inputCode.length == 6) {
                                    onJoinSync(inputCode)
                                    Toast.makeText(context, "Connected to paired session!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = activeTextColor,
                            unfocusedTextColor = activeTextColor,
                            focusedBorderColor = NeonGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .testTag("sync_session_code_input")
                    )

                    Button(
                        onClick = {
                            if (inputCode.length == 6) {
                                onJoinSync(inputCode)
                                Toast.makeText(context, "Connected to paired session!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Join session", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ══════════════ PLAYLIST DETAIL SCREEN Composable ══════════════
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    viewModel: MusicViewModel,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    onBack: () -> Unit
) {
    val songsList = remember(playlist.songIds, viewModel.dbSongs.collectAsState().value) {
        val ids = playlist.songIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        ids.mapNotNull { viewModel.getSongItemById(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = activeTextColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = playlist.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )
                    Text(
                        text = "${songsList.size} tracks",
                        fontSize = 13.sp,
                        color = activeMutedColor
                    )
                }
            }

            if (songsList.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.playPlaylist(playlist.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.downloadPlaylist(playlist.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = activeCard),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, activeMutedColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download All",
                                tint = activeTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Download All", color = activeTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (songsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏔️", fontSize = 48.sp)
                    Text(
                        "No songs in this playlist yet.",
                        fontSize = 14.sp,
                        color = activeMutedColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Search YT Music or Spotify in the 'Streaming' tab to add songs!",
                        fontSize = 11.sp,
                        color = activeMutedColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(songsList) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeCard.copy(alpha = 0.5f))
                            .clickable { viewModel.playStreamingSong(song) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd)))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.emoji.startsWith("http")) {
                                    coil.compose.AsyncImage(
                                        model = song.emoji,
                                        contentDescription = "Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(text = song.emoji, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 12.sp,
                                    color = activeMutedColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SongDownloadIndicator(
                                song = song,
                                viewModel = viewModel,
                                activeTextColor = activeTextColor,
                                activeMutedColor = activeMutedColor
                            )
                            IconButton(
                                onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════ ONLINE STREAMING SCREEN Composable ══════════════
@Composable
fun StreamingScreen(
    viewModel: MusicViewModel,
    spotifyClientId: String,
    spotifyClientSecret: String,
    spotifyToken: String?,
    isSpotifyConnecting: Boolean,
    spotifyConnectionError: String?,
    streamingSearchResults: List<SongItem>,
    isSearchingStreaming: Boolean,
    streamingSearchError: String?,
    activeTextColor: Color,
    activeMutedColor: Color,
    activeCard: Color,
    activeCard2: Color,
    playlists: List<PlaylistEntity>,
    likedIds: Set<String>,
    showTitle: Boolean = true
) {
    var searchTab by remember { mutableStateOf("youtube") } // "youtube" or "spotify"
    var queryText by remember { mutableStateOf("") }
    
    var spClientIdField by remember(spotifyClientId) { mutableStateOf(spotifyClientId) }
    var spClientSecretField by remember(spotifyClientSecret) { mutableStateOf(spotifyClientSecret) }
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        if (showTitle) {
            Text(
                text = "Online Streams",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeTextColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(activeCard)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (searchTab == "youtube") NeonGreen else Color.Transparent)
                    .clickable { searchTab = "youtube"; queryText = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔴 YouTube Music",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (searchTab == "youtube") Color.Black else activeTextColor
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (searchTab == "spotify") NeonGreen else Color.Transparent)
                    .clickable { searchTab = "spotify"; queryText = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🟢 Spotify",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (searchTab == "spotify") Color.Black else activeTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (searchTab == "spotify" && spotifyToken.isNullOrEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = activeCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connect to Spotify Web API",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor
                    )
                    Text(
                        text = "Sangeet can fetch high fidelity tracks directly from Spotify. To connect, please enter your Spotify Developer API details below:",
                        fontSize = 12.sp,
                        color = activeMutedColor,
                        lineHeight = 16.sp
                    )
                    
                    OutlinedTextField(
                        value = spClientIdField,
                        onValueChange = { spClientIdField = it },
                        label = { Text("Client ID", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = activeTextColor,
                            unfocusedTextColor = activeTextColor,
                            focusedBorderColor = NeonGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spotify_client_id_input")
                    )

                    OutlinedTextField(
                        value = spClientSecretField,
                        onValueChange = { spClientSecretField = it },
                        label = { Text("Client Secret", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = activeTextColor,
                            unfocusedTextColor = activeTextColor,
                            focusedBorderColor = NeonGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spotify_client_secret_input")
                    )

                    if (spotifyConnectionError != null) {
                        Text(
                            text = spotifyConnectionError,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "developer.spotify.com",
                            fontSize = 11.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                if (spClientIdField.isNotEmpty() && spClientSecretField.isNotEmpty()) {
                                    viewModel.saveSpotifyCredentials(spClientIdField, spClientSecretField)
                                } else {
                                    Toast.makeText(context, "Fill in both fields to authorize", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            enabled = !isSpotifyConnecting,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSpotifyConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("Authorize", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("Search title, artist...", color = activeMutedColor, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = activeTextColor,
                        unfocusedTextColor = activeTextColor,
                        focusedBorderColor = NeonGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("streaming_search_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (queryText.isNotEmpty()) {
                            if (searchTab == "spotify") viewModel.searchSpotify(queryText) else viewModel.searchYouTubeMusic(queryText)
                        }
                    })
                )

                Button(
                    onClick = {
                        if (queryText.isNotEmpty()) {
                            if (searchTab == "spotify") viewModel.searchSpotify(queryText) else viewModel.searchYouTubeMusic(queryText)
                        } else {
                            Toast.makeText(context, "Enter search terms first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (searchTab == "spotify") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔒 Connected", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Disconnect Client",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.disconnectSpotify()
                            Toast.makeText(context, "Disconnected Spotify", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (isSearchingStreaming) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (streamingSearchError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = streamingSearchError,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (streamingSearchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = if (searchTab == "spotify") "🟢" else "🔴", fontSize = 42.sp)
                        Text(
                            text = "Search across millions of online streams",
                            fontSize = 13.sp,
                            color = activeMutedColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Instant previews and audio scaling supported",
                            fontSize = 11.sp,
                            color = activeMutedColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(streamingSearchResults) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeCard2)
                                .clickable { viewModel.playStreamingSong(song) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(listOf(Color(song.colorStart), Color(song.colorEnd)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.emoji.startsWith("http")) {
                                        coil.compose.AsyncImage(
                                            model = song.emoji,
                                            contentDescription = "Cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(text = song.emoji, fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 11.sp,
                                        color = activeMutedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SongDownloadIndicator(
                                    song = song,
                                    viewModel = viewModel,
                                    activeTextColor = activeTextColor,
                                    activeMutedColor = activeMutedColor
                                )

                                val isLiked = likedIds.contains(song.id)
                                IconButton(
                                    onClick = { viewModel.toggleLikeTrack(song) }
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) NeonGreen else activeMutedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                var showDropdown by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = "Add to playlist",
                                            tint = activeMutedColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showDropdown,
                                        onDismissRequest = { showDropdown = false },
                                        modifier = Modifier.background(activeCard)
                                    ) {
                                        if (playlists.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No custom playlists", color = activeMutedColor) },
                                                onClick = { showDropdown = false }
                                            )
                                        } else {
                                            playlists.forEach { playlist ->
                                                val isAlreadyIn = playlist.songIds.split(",").contains(song.id)
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = (if (isAlreadyIn) "✓ " else "＋ ") + playlist.name,
                                                            color = if (isAlreadyIn) NeonGreen else activeTextColor
                                                        )
                                                    },
                                                    onClick = {
                                                        showDropdown = false
                                                        viewModel.addSongToPlaylist(playlist.id, song.id)
                                                        Toast.makeText(context, "${song.title} added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

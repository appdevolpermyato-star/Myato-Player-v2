package com.example.ui.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Playing(val currentSong: Song, val isPlaying: Boolean) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

enum class SortOption {
    TITLE,
    ARTIST,
    DURATION,
    DATE
}

class MusicViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    // 0: Off, 1: Repeat All, 2: Repeat One
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow(SortOption.TITLE)

    val isScanning = MutableStateFlow(false)
    val scanMessage = MutableStateFlow<String?>(null)

    // Equalizer state from AudioEffectManager singleton
    val equalizerState: StateFlow<EqualizerState> = AudioEffectManager.instance.state

    init {
        // Observe and seed demo songs if empty so player is immediately functional
        viewModelScope.launch {
            repository.allSongs.first().let { initialSongs ->
                if (initialSongs.isEmpty()) {
                    val count = repository.scanLocalSongs()
                    if (count == 0) {
                        repository.seedSampleSongsIfEmpty()
                    }
                }
            }
        }
    }

    fun initController(context: Context) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            setupController()
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        val player = controller ?: return
        _isShuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onPlaybackStateChanged(state: Int) {
                // Playback state updates
            }
        })
        updatePlaybackState()

        // Timer for smooth seekbar updates
        viewModelScope.launch {
            while (true) {
                controller?.let { p ->
                    if (p.isPlaying) {
                        _currentPosition.value = p.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun updatePlaybackState() {
        val player = controller ?: return
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            val songId = currentMediaItem.mediaId
            val song = _currentQueue.value.find { it.id == songId }
                ?: allSongs.value.find { it.id == songId }
            if (song != null) {
                _playbackState.value = PlaybackState.Playing(song, player.isPlaying)
            }
        } else {
            _playbackState.value = PlaybackState.Idle
        }
    }

    fun playSong(song: Song) {
        val queue = if (_currentQueue.value.any { it.id == song.id }) {
            _currentQueue.value
        } else {
            listOf(song) + allSongs.value.filter { it.id != song.id }
        }
        playQueue(queue, queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _currentQueue.value = songs
        val player = controller ?: return

        val mediaItems = songs.map { s ->
            val metadata = MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                .setAlbumTitle(s.album)
                .setArtworkUri(if (!s.coverUri.isNullOrEmpty()) Uri.parse(s.coverUri) else null)
                .setDisplayTitle(s.title)
                .build()

            MediaItem.Builder()
                .setMediaId(s.id)
                .setUri(s.getUri())
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipToNext() {
        controller?.seekToNext()
    }

    fun skipToPrevious() {
        val player = controller ?: return
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else {
            player.seekToPrevious()
        }
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position
        controller?.seekTo(position)
    }

    fun toggleShuffle() {
        val player = controller ?: return
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        _isShuffleEnabled.value = newShuffle
    }

    fun toggleRepeatMode() {
        val player = controller ?: return
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun scanLocalFiles() {
        viewModelScope.launch {
            isScanning.value = true
            val count = repository.scanLocalSongs()
            isScanning.value = false
            scanMessage.value = if (count > 0) "$count músicas encontradas" else "Nenhuma música nova encontrada"
            delay(3000)
            scanMessage.value = null
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        viewModelScope.launch {
            isScanning.value = true
            val imported = repository.importAudioUris(uris)
            isScanning.value = false
            if (imported.isNotEmpty()) {
                scanMessage.value = "${imported.size} músicas importadas com sucesso"
            }
            delay(3000)
            scanMessage.value = null
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }

    fun getSongsInPlaylist(playlistId: String): Flow<List<Song>> {
        return repository.getSongsInPlaylist(playlistId)
    }

    // Equalizer controls forwarding
    fun setEqualizerEnabled(enabled: Boolean) = AudioEffectManager.instance.setEnabled(enabled)
    fun setEqualizerPreset(preset: String) = AudioEffectManager.instance.setPreset(preset)
    fun setBassBoost(strength: Int) = AudioEffectManager.instance.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = AudioEffectManager.instance.setVirtualizer(strength)
    fun setBandLevel(band: Short, level: Short) = AudioEffectManager.instance.setBandLevel(band, level)

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

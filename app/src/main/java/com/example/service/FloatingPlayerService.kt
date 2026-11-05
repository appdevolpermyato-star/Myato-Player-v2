package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.Coil
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.Locale

class FloatingPlayerService : Service() {

    companion object {
        const val TAG = "FloatingPlayerService"
        const val ACTION_START = "com.example.service.action.START_FLOATING"
        const val ACTION_STOP = "com.example.service.action.STOP_FLOATING"
        const val ACTION_EXPAND = "com.example.service.action.EXPAND_PANEL"
        const val ACTION_COLLAPSE = "com.example.service.action.COLLAPSE_PANEL"
        const val ACTION_UPDATE_CUSTOMIZATION = "com.example.service.action.UPDATE_CUSTOMIZATION"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingPlayerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingPlayerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateCustomization(context: Context) {
            if (isRunning) {
                val intent = Intent(context, FloatingPlayerService::class.java).apply {
                    action = ACTION_UPDATE_CUSTOMIZATION
                }
                context.startService(intent)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isPanelOpen: Boolean = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController? get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        initMediaController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_EXPAND -> {
                showPanel()
            }
            ACTION_COLLAPSE -> {
                hidePanel()
            }
            ACTION_UPDATE_CUSTOMIZATION -> {
                applyCustomization()
            }
            else -> {
                if (FloatingPlayerPrefs.canDrawOverlays(this)) {
                    createBubbleView()
                } else {
                    Log.w(TAG, "Cannot draw overlays. Permission not granted.")
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun initMediaController() {
        try {
            val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
            controllerFuture?.addListener({
                setupPlayerListener()
                updateUiWithCurrentTrack()
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to MediaSession in FloatingPlayerService", e)
        }
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mainHandler.post { updateUiWithCurrentTrack() }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mainHandler.post {
                    updatePlayPauseState()
                    if (isPlaying) startProgressUpdater() else stopProgressUpdater()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mainHandler.post { updateShuffleState() }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                mainHandler.post { updateRepeatState() }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                mainHandler.post { updateUiWithCurrentTrack() }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubbleView() {
        if (bubbleView != null) return

        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val sizeDp = FloatingPlayerPrefs.getBubbleSize(this)
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (savedX, savedY) = FloatingPlayerPrefs.getSavedPosition(this)

        bubbleParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        // Setup Drag & Snap gestures
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchStartTime = 0L

        bubbleView?.setOnTouchListener { _, event ->
            val params = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(bubbleView, params)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val totalDx = Math.abs(event.rawX - initialTouchX)
                    val totalDy = Math.abs(event.rawY - initialTouchY)
                    val duration = System.currentTimeMillis() - touchStartTime

                    // If tap with minimal movement: CLICK -> Open Panel
                    if (totalDx < 16 && totalDy < 16 && duration < 300) {
                        showPanel()
                    } else {
                        // Snap to nearest lateral edge (Left or Right)
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val middle = screenWidth / 2

                        params.x = if (params.x + sizePx / 2 < middle) {
                            16
                        } else {
                            screenWidth - sizePx - 16
                        }

                        // Clamp Y inside screen
                        params.y = params.y.coerceIn(80, displayMetrics.heightPixels - sizePx - 120)

                        FloatingPlayerPrefs.savePosition(this@FloatingPlayerService, params.x, params.y)
                        try {
                            windowManager?.updateViewLayout(bubbleView, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add bubble to WindowManager", e)
        }
    }

    private fun showPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        // Hide bubble while panel is expanded
        bubbleView?.visibility = View.GONE

        if (panelView == null) {
            createPanelView()
        } else {
            panelView?.visibility = View.VISIBLE
            applyCustomization()
            updateUiWithCurrentTrack()
        }
    }

    private fun hidePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false

        panelView?.visibility = View.GONE
        bubbleView?.visibility = View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPanelView() {
        val inflater = LayoutInflater.from(this)
        panelView = inflater.inflate(R.layout.layout_floating_panel, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val width = (340 * displayMetrics.density).toInt().coerceAtMost(displayMetrics.widthPixels - 32)

        panelParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            alpha = 1.0f
        }

        panelView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hidePanel()
                true
            } else {
                false
            }
        }

        setupPanelControls()
        applyCustomization()

        try {
            windowManager?.addView(panelView, panelParams)
            updateUiWithCurrentTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add panel to WindowManager", e)
        }
    }

    private fun applyCustomization() {
        val view = panelView ?: return

        // 1. Proportional scale (70% - 120%)
        val sizePercent = FloatingPlayerPrefs.getPanelSizePercent(this)
        val scale = (sizePercent / 100f).coerceIn(0.7f, 1.2f)
        view.scaleX = scale
        view.scaleY = scale

        // 2. Transparência exclusiva do fundo do painel (30% - 100%)
        // Textos, botões, ícones e capa permanecem 100% nítidos e visíveis
        val opacityPercent = FloatingPlayerPrefs.getPanelOpacityPercent(this)
        val alphaInt = (255 * (opacityPercent / 100f)).toInt().coerceIn(76, 255)
        val bgColor = android.graphics.Color.argb(alphaInt, 18, 17, 22)
        val strokeColor = android.graphics.Color.parseColor("#A855F7")
        val density = resources.displayMetrics.density
        val strokeWidth = (1.5f * density).toInt()

        val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 24f * density
            setStroke(strokeWidth, strokeColor)
        }
        view.background = backgroundDrawable

        panelParams?.let { params ->
            params.alpha = 1.0f
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    private fun setupPanelControls() {
        val view = panelView ?: return

        // Close button (X)
        view.findViewById<ImageButton>(R.id.btn_panel_close)?.setOnClickListener {
            hidePanel()
        }

        // Play / Pause Button
        view.findViewById<ImageButton>(R.id.btn_panel_play_pause)?.setOnClickListener {
            val p = controller ?: return@setOnClickListener
            if (p.isPlaying) p.pause() else p.play()
        }

        // Previous
        view.findViewById<ImageButton>(R.id.btn_panel_prev)?.setOnClickListener {
            val p = controller ?: return@setOnClickListener
            if (p.currentPosition > 3000) p.seekTo(0) else p.seekToPrevious()
        }

        // Next
        view.findViewById<ImageButton>(R.id.btn_panel_next)?.setOnClickListener {
            controller?.seekToNext()
        }

        // Shuffle
        view.findViewById<ImageButton>(R.id.btn_panel_shuffle)?.setOnClickListener {
            val p = controller ?: return@setOnClickListener
            p.shuffleModeEnabled = !p.shuffleModeEnabled
            updateShuffleState()
        }

        // Repeat
        view.findViewById<ImageButton>(R.id.btn_panel_repeat)?.setOnClickListener {
            val p = controller ?: return@setOnClickListener
            val nextMode = when (p.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            p.repeatMode = nextMode
            updateRepeatState()
        }

        // SeekBar
        val seekBar = view.findViewById<SeekBar>(R.id.sb_panel_progress)
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    controller?.seekTo(progress.toLong())
                    view.findViewById<TextView>(R.id.tv_panel_current_time)?.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // 4 Quick Access Shortcuts
        // 1. Biblioteca
        view.findViewById<View>(R.id.card_panel_library)?.setOnClickListener {
            launchMainActivity("library")
            hidePanel()
        }

        // 2. Favoritos
        view.findViewById<View>(R.id.card_panel_favorites)?.setOnClickListener {
            launchMainActivity("favorites")
            hidePanel()
        }

        // 3. Playlists
        view.findViewById<View>(R.id.card_panel_playlists)?.setOnClickListener {
            launchMainActivity("playlists")
            hidePanel()
        }

        // 4. Abrir App
        view.findViewById<View>(R.id.card_panel_open_app)?.setOnClickListener {
            launchMainActivity("home")
            hidePanel()
        }
    }

    private fun launchMainActivity(destination: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("target_screen", destination)
        }
        startActivity(intent)
    }

    private fun updateUiWithCurrentTrack() {
        val view = panelView ?: return
        val player = controller ?: return
        val item = player.currentMediaItem

        val titleTv = view.findViewById<TextView>(R.id.tv_panel_title)
        val artistTv = view.findViewById<TextView>(R.id.tv_panel_artist)
        val coverIv = view.findViewById<ImageView>(R.id.iv_panel_cover)
        val durationTv = view.findViewById<TextView>(R.id.tv_panel_duration)
        val seekBar = view.findViewById<SeekBar>(R.id.sb_panel_progress)

        if (item != null) {
            val title = item.mediaMetadata.title?.toString() ?: "Música"
            val artist = item.mediaMetadata.artist?.toString() ?: "Myato Player"
            val artworkUri = item.mediaMetadata.artworkUri

            titleTv?.text = title
            artistTv?.text = artist

            if (artworkUri != null) {
                val imageRequest = ImageRequest.Builder(this)
                    .data(artworkUri)
                    .target(coverIv)
                    .crossfade(true)
                    .error(R.drawable.ic_mp_street_logo)
                    .placeholder(R.drawable.ic_mp_street_logo)
                    .build()
                Coil.imageLoader(this).enqueue(imageRequest)
            } else {
                coverIv?.setImageResource(R.drawable.ic_mp_street_logo)
            }

            val duration = player.duration.coerceAtLeast(0L)
            durationTv?.text = formatTime(duration)
            seekBar?.max = duration.toInt()
        } else {
            titleTv?.text = "Nenhuma música tocando"
            artistTv?.text = "Toque em Abrir App para escolher"
            coverIv?.setImageResource(R.drawable.ic_mp_street_logo)
            durationTv?.text = "0:00"
            seekBar?.progress = 0
        }

        updatePlayPauseState()
        updateShuffleState()
        updateRepeatState()
    }

    private fun updatePlayPauseState() {
        val view = panelView ?: return
        val player = controller ?: return
        val playBtn = view.findViewById<ImageButton>(R.id.btn_panel_play_pause)

        if (player.isPlaying) {
            playBtn?.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            playBtn?.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun updateShuffleState() {
        val view = panelView ?: return
        val player = controller ?: return
        val shuffleBtn = view.findViewById<ImageButton>(R.id.btn_panel_shuffle)
        val color = if (player.shuffleModeEnabled) 0xFFA855F7.toInt() else 0xFF918A99.toInt()
        shuffleBtn?.setColorFilter(color)
    }

    private fun updateRepeatState() {
        val view = panelView ?: return
        val player = controller ?: return
        val repeatBtn = view.findViewById<ImageButton>(R.id.btn_panel_repeat)
        val color = if (player.repeatMode != Player.REPEAT_MODE_OFF) 0xFFA855F7.toInt() else 0xFF918A99.toInt()
        repeatBtn?.setColorFilter(color)
    }

    private fun startProgressUpdater() {
        stopProgressUpdater()
        progressRunnable = object : Runnable {
            override fun run() {
                val player = controller
                if (player != null && player.isPlaying && isPanelOpen) {
                    val pos = player.currentPosition
                    panelView?.findViewById<SeekBar>(R.id.sb_panel_progress)?.progress = pos.toInt()
                    panelView?.findViewById<TextView>(R.id.tv_panel_current_time)?.text = formatTime(pos)
                }
                mainHandler.postDelayed(this, 500)
            }
        }
        mainHandler.post(progressRunnable!!)
    }

    private fun stopProgressUpdater() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%d:%02d", min, sec)
    }

    override fun onDestroy() {
        isRunning = false
        stopProgressUpdater()

        try {
            if (bubbleView != null) {
                windowManager?.removeView(bubbleView)
                bubbleView = null
            }
            if (panelView != null) {
                windowManager?.removeView(panelView)
                panelView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay views on destroy", e)
        }

        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }

        super.onDestroy()
    }
}

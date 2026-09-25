package com.example.mahirtv

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlaybackActivity : FragmentActivity() {

    companion object {
        const val MOVIE = "movie"

        private const val PREFS_NAME = "playback_progress"
        private const val SAVE_INTERVAL = 5000L
        private const val COMPLETION_THRESHOLD = 0.95
    }

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private lateinit var movieKey: String

    private val handler = Handler(Looper.getMainLooper())

    private val saveProgressRunnable = object : Runnable {
        override fun run() {
            savePlaybackPosition()
            handler.postDelayed(this, SAVE_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playback)

        playerView = findViewById(R.id.player_view)

        val movie =
            intent.getSerializableExtra(MOVIE) as Movie

        val videoUrl = movie.videoUrl
        val subtitleUrl = movie.subtitleUrl

        /*
         * Use the filename as the key.
         *
         * This is better than using the complete URL because
         * the PC's IP address could change.
         */
        movieKey = videoUrl
            ?.substringAfterLast("/")
            ?: movie.title
                    ?: "unknown"

        // Create ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // Connect ExoPlayer to PlayerView
        playerView.player = player

        // Create MediaItem
        val mediaItemBuilder =
            MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))

        // Add SRT subtitle if available
        if (subtitleUrl != null) {

            val subtitle =
                MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(subtitleUrl)
                )
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("en")
                    .setLabel("English")
                    .setSelectionFlags(
                        C.SELECTION_FLAG_DEFAULT
                    )
                    .build()

            mediaItemBuilder.setSubtitleConfigurations(
                listOf(subtitle)
            )
        }

        player.setMediaItem(
            mediaItemBuilder.build()
        )

        // Restore previous position
        val savedPosition = getSavedPosition()

        if (savedPosition > 0) {
            player.seekTo(savedPosition)
        }

        // Prepare video
        player.prepare()

        // Start automatically
        player.playWhenReady = true

        // Start periodically saving progress
        handler.postDelayed(
            saveProgressRunnable,
            SAVE_INTERVAL
        )
    }

    private fun savePlaybackPosition() {

        if (!::player.isInitialized) {
            return
        }

        val position = player.currentPosition
        val duration = player.duration

        if (position <= 0 || duration <= 0) {
            return
        }

        /*
         * If the movie is almost finished, consider it watched.
         * Remove the saved position so it starts from the beginning
         * next time.
         */
        if (position.toDouble() / duration >= COMPLETION_THRESHOLD) {

            clearSavedPosition()

        } else {

            getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
                .edit()
                .putLong(movieKey, position)
                .apply()
        }
    }

    private fun getSavedPosition(): Long {

        return getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .getLong(movieKey, 0L)
    }

    private fun clearSavedPosition() {

        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(movieKey)
            .apply()
    }

    override fun onStop() {
        super.onStop()

        // Save one final time before leaving
        savePlaybackPosition()

        // Stop periodic saving
        handler.removeCallbacks(
            saveProgressRunnable
        )

        // Release player
        player.release()
    }
}
package com.example.mahirtv

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue:
            PlaybackTransportControlGlue<MediaPlayerAdapter>

    private lateinit var progressKey: String

    private val handler = Handler(Looper.getMainLooper())

    private val saveProgressRunnable = object : Runnable {
        override fun run() {

            savePlaybackPosition()

            // Save again after 5 seconds
            handler.postDelayed(this, 5000)
        }
    }

    private val preferences by lazy {
        requireContext().getSharedPreferences(
            "playback_progress",
            Context.MODE_PRIVATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val movie =
            activity?.intent?.getSerializableExtra(
                PlaybackActivity.MOVIE
            ) as Movie

        val title = movie.title
        val description = movie.description
        val videoUrl = movie.videoUrl

        /*
         * Use the filename as the ID for playback progress.
         *
         * This means changing the PC's IP address won't
         * destroy the saved progress.
         */
        val filename =
            Uri.parse(videoUrl).lastPathSegment ?: title

        progressKey = "position_$filename"

        val savedPosition =
            preferences.getLong(progressKey, 0L)

        val glueHost =
            VideoSupportFragmentGlueHost(
                this@PlaybackVideoFragment
            )

        val playerAdapter =
            MediaPlayerAdapter(requireActivity())

        playerAdapter.setRepeatAction(
            PlaybackControlsRow.RepeatAction.INDEX_NONE
        )

        mTransportControlGlue =
            PlaybackTransportControlGlue(
                requireActivity(),
                playerAdapter
            )

        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = title
        mTransportControlGlue.subtitle = description
        mTransportControlGlue.setSeekEnabled(true)

        /*
         * This is the important part.
         *
         * We listen to the GLUE callback, not the
         * MediaPlayerAdapter callback.
         */
        mTransportControlGlue.addPlayerCallback(
            object : PlaybackGlue.PlayerCallback() {

                override fun onPreparedStateChanged(
                    glue: PlaybackGlue
                ) {

                    if (savedPosition > 0) {

                        val duration =
                            mTransportControlGlue.duration

                        if (
                            duration > 0 &&
                            savedPosition < duration - 5000
                        ) {

                            mTransportControlGlue.seekTo(
                                savedPosition
                            )
                        }
                    }
                }

                override fun onPlayCompleted(
                    glue: PlaybackGlue
                ) {

                    // Movie finished.
                    // Remove saved progress.
                    preferences.edit()
                        .remove(progressKey)
                        .apply()
                }
            }
        )

        playerAdapter.setDataSource(
            Uri.parse(videoUrl)
        )

        mTransportControlGlue.playWhenPrepared()

        // Start saving every 5 seconds.
        handler.postDelayed(
            saveProgressRunnable,
            5000
        )
    }

    private fun savePlaybackPosition() {

        if (!mTransportControlGlue.isPrepared) {
            return
        }

        val position =
            mTransportControlGlue.currentPosition

        if (position > 0) {

            preferences.edit()
                .putLong(progressKey, position)
                .apply()
        }
    }

    override fun onPause() {

        // Save immediately when leaving the video.
        savePlaybackPosition()

        // Stop periodic saving.
        handler.removeCallbacks(
            saveProgressRunnable
        )

        mTransportControlGlue.pause()

        super.onPause()
    }

    override fun onDestroy() {

        // Final save.
        savePlaybackPosition()

        handler.removeCallbacks(
            saveProgressRunnable
        )

        super.onDestroy()
    }
}
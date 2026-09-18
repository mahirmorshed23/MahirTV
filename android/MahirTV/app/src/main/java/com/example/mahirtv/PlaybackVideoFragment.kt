package com.example.mahirtv

import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow

import android.view.WindowManager

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue:
            PlaybackTransportControlGlue<MediaPlayerAdapter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val (_, title, description, _, _, videoUrl) =
            activity?.intent?.getSerializableExtra(PlaybackActivity.MOVIE) as Movie

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)

        val playerAdapter = MediaPlayerAdapter(activity)

        playerAdapter.setRepeatAction(
            PlaybackControlsRow.RepeatAction.INDEX_NONE
        )

        mTransportControlGlue =
            PlaybackTransportControlGlue(getActivity(), playerAdapter)

        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = title
        mTransportControlGlue.subtitle = description
        mTransportControlGlue.setSeekEnabled(true)

        playerAdapter.setDataSource(Uri.parse(videoUrl))

        mTransportControlGlue.playWhenPrepared()
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }
}
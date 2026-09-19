package com.example.mahirtv

import java.util.Collections
import java.util.Timer
import java.util.TimerTask

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {


    private val client = OkHttpClient()
    private var serverUrl: String? = null
    private var serverDiscovery: ServerDiscovery? = null

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()

        setupEventListeners()

        startServerDiscovery()
    }

    override fun onDestroy() {
        serverDiscovery?.stop()

        super.onDestroy()

        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())

        mBackgroundTimer?.cancel()
    }
    private fun prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity!!.window)
        mDefaultBackground = ContextCompat.getDrawable(activity!!, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(activity!!, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(activity!!, R.color.search_opaque)
    }

    private fun loadRows() {

        val url = serverUrl ?: return

        Thread {

            try {
                val request = Request.Builder()
                    .url("$url/movies")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Server returned ${response.code}")
                }

                val json = response.body?.string()
                    ?: throw Exception("Empty response from server")

                val jsonArray = JSONArray(json)

                val movies = ArrayList<Movie>()

                for (i in 0 until jsonArray.length()) {

                    val jsonMovie = jsonArray.getJSONObject(i)

                    val title = jsonMovie.getString("title")
                    val filename = jsonMovie.getString("filename")
                    val poster = jsonMovie.optString("poster", "")

                    val movie = Movie(
                        id = i.toLong(),
                        title = title,
                        description = "",
                        backgroundImageUrl = null,
                        cardImageUrl = if (poster.isNotEmpty()) {
                            "$url/movies/$poster"
                        } else {
                            null
                        },
                        videoUrl = "$url/movies/$filename",
                        studio = ""
                    )

                    movies.add(movie)
                }

                requireActivity().runOnUiThread {

                    val rowsAdapter =
                        ArrayObjectAdapter(ListRowPresenter())

                    val cardPresenter = CardPresenter()

                    val listRowAdapter =
                        ArrayObjectAdapter(cardPresenter)

                    for (movie in movies) {
                        listRowAdapter.add(movie)
                    }

                    val header = HeaderItem(
                        0,
                        "MY MOVIES"
                    )

                    rowsAdapter.add(
                        ListRow(
                            header,
                            listRowAdapter
                        )
                    )

                    adapter = rowsAdapter
                }

            } catch (e: Exception) {

                Log.e(TAG, "Failed to load movies", e)

                requireActivity().runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Could not connect to PC: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(activity!!, "Implement your own in-app search", Toast.LENGTH_LONG)
                .show()
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {

            if (item is Movie) {
                Log.d(TAG, "Item: " + item.toString())
                val intent = Intent(activity!!, PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.MOVIE, item)

                startActivity(intent)

//                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                    activity!!,
//                    (itemViewHolder.view as ImageCardView).mainImageView!!,
//                    DetailsActivity.SHARED_ELEMENT_NAME
//                )
//                    .toBundle()
//                startActivity(intent, bundle)
            } else if (item is String) {
                if (item.contains(getString(R.string.error_fragment))) {
                    val intent = Intent(activity!!, BrowseErrorActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(activity!!, item, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is Movie) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
        }
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(activity!!)
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_ROWS = 6
        private val NUM_COLS = 15
    }




    private fun startServerDiscovery() {

        Toast.makeText(
            activity,
            "Looking for MahirTV server...",
            Toast.LENGTH_SHORT
        ).show()

        serverDiscovery = ServerDiscovery(requireContext()) { discoveredUrl ->

            requireActivity().runOnUiThread {

                // Ignore repeated discovery of the same server
                if (serverUrl == discoveredUrl) {
                    return@runOnUiThread
                }

                serverUrl = discoveredUrl

                Log.d(
                    TAG,
                    "MahirTV server discovered: $serverUrl"
                )

                Toast.makeText(
                    activity,
                    "MahirTV server found",
                    Toast.LENGTH_SHORT
                ).show()

                loadRows()
            }
        }

        serverDiscovery?.start()
    }
}
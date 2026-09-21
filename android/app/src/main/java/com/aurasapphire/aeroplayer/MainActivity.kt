package com.aurasapphire.aeroplayer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var adapter: TrackAdapter
    private val tracks = mutableListOf<File>()
    private val visibleTracks = mutableListOf<File>()
    private var currentIndex = -1
    private var shuffle = false

    private val pickMusic = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris?.forEach { importUri(it) }
        loadLibrary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player = ExoPlayer.Builder(this).build()
        adapter = TrackAdapter(visibleTracks) { file -> playFile(file) }

        findViewById<RecyclerView>(R.id.trackList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.importButton).setOnClickListener {
            pickMusic.launch(arrayOf("audio/*"))
        }
        findViewById<Button>(R.id.playButton).setOnClickListener {
            if (player.isPlaying) player.pause()
            else if (player.currentMediaItem != null) player.play()
            else if (visibleTracks.isNotEmpty()) playFile(visibleTracks.first())
            updatePlayButton()
        }
        findViewById<Button>(R.id.nextButton).setOnClickListener { nextTrack() }
        findViewById<Button>(R.id.prevButton).setOnClickListener { previousTrack() }
        findViewById<Button>(R.id.shuffleButton).setOnClickListener {
            shuffle = !shuffle
            findViewById<Button>(R.id.shuffleButton).alpha = if (shuffle) 1f else .55f
        }

        val seek = findViewById<SeekBar>(R.id.seekBar)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) player.seekTo(p.toLong())
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val search = findViewById<EditText>(R.id.searchBox)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                val q = s.toString().trim().lowercase(Locale.getDefault())
                visibleTracks.clear()
                visibleTracks.addAll(if (q.isEmpty()) tracks else tracks.filter { it.name.lowercase(Locale.getDefault()).contains(q) })
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updatePlayButton()
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) nextTrack()
            }
        })

        loadLibrary()
        window.decorView.post(updateProgress)
    }

    private fun musicDir(): File = File(filesDir, "music").apply { mkdirs() }

    private fun loadLibrary() {
        tracks.clear()
        tracks.addAll(
            musicDir().listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in setOf("mp3","m4a","aac","ogg","wav","flac") }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        )
        visibleTracks.clear()
        visibleTracks.addAll(tracks)
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.libraryCount).text = "${tracks.size} songs"
    }

    private fun importUri(uri: Uri) {
        val name = queryName(uri) ?: "song_${System.currentTimeMillis()}.audio"
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val out = File(musicDir(), safe)
        if (out.exists()) out.delete()
        contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun queryName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return null
    }

    private fun playFile(file: File) {
        currentIndex = visibleTracks.indexOf(file)
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.play()
        findViewById<TextView>(R.id.nowTitle).text = file.nameWithoutExtension
        findViewById<TextView>(R.id.nowArtist).text = "Local • Offline"
        updatePlayButton()
    }

    private fun nextTrack() {
        if (visibleTracks.isEmpty()) return
        val next = if (shuffle) (0 until visibleTracks.size).random() else (currentIndex + 1) % visibleTracks.size
        playFile(visibleTracks[next])
    }

    private fun previousTrack() {
        if (visibleTracks.isEmpty()) return
        val prev = if (currentIndex <= 0) visibleTracks.lastIndex else currentIndex - 1
        playFile(visibleTracks[prev])
    }

    private fun updatePlayButton() {
        findViewById<Button>(R.id.playButton).text = if (player.isPlaying) "Ⅱ" else "▶"
    }

    private val updateProgress = object : Runnable {
        override fun run() {
            val seek = findViewById<SeekBar>(R.id.seekBar)
            val current = player.currentPosition.coerceAtLeast(0)
            val duration = player.duration.takeIf { it > 0 } ?: 0
            seek.max = duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            seek.progress = current.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            findViewById<TextView>(R.id.currentTime).text = format(current)
            findViewById<TextView>(R.id.totalTime).text = format(duration)
            seek.postDelayed(this, 500)
        }
    }

    private fun format(ms: Long): String {
        val total = ms / 1000
        return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60)
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

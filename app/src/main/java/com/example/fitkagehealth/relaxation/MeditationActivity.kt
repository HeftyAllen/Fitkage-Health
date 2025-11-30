package com.example.fitkagehealth.relaxation

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.MainActivity
import com.example.fitkagehealth.R
import java.util.*
import java.util.concurrent.TimeUnit

class MeditationActivity : BaseActivity(), TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var isPlaying = false

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var voiceIndex = 0

    // mapping spinner positions to raw resource IDs
    private val musicTracks = listOf(
        Pair("Ocean Pad", R.raw.ambient1),
        Pair("Gentle Bells", R.raw.ambient2),
        Pair("Soft Flute", R.raw.ambient3)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meditation)

        // Init TTS
        tts = TextToSpeech(this, this)

        val spnMusic = findViewById<Spinner>(R.id.spnMusic)
        val spnFeeling = findViewById<Spinner>(R.id.spnFeeling)
        val spnCause = findViewById<Spinner>(R.id.spnCause)
        val spnRelax = findViewById<Spinner>(R.id.spnRelax)
        val spnChange = findViewById<Spinner>(R.id.spnChange)

        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val tvCurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
        val tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)
        val switchVoiceOver = findViewById<Switch>(R.id.switchVoiceOver)
        val btnPlayVoiceOver = findViewById<Button>(R.id.btnPlayVoiceOver)

        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Populate music spinner
        val musicNames = musicTracks.map { it.first }
        spnMusic.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, musicNames)

        // Dropdown data (preserve earlier lists)
        val feelings = arrayOf("Select...", "Happy", "Stressed", "Sad", "Anxious", "Calm")
        val causes = arrayOf("Select...", "Workload", "Family", "Sleep", "Health", "Other")
        val relaxOptions = arrayOf("Select...", "Music", "Meditation", "Walks", "Breathing exercises", "Reading")
        val changeOptions = arrayOf("Select...", "Sleep earlier", "Take breaks", "Exercise", "Talk to someone", "Eat healthier")

        spnFeeling.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, feelings)
        spnCause.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, causes)
        spnRelax.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, relaxOptions)
        spnChange.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, changeOptions)

        // When the user changes track while playing, switch immediately
        spnMusic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val trackRes = musicTracks[position].second
                if (isPlaying) {
                    // stop and start new track
                    stopAndReleasePlayer()
                    startAmbientAudio(trackRes, btnPlayPause, seekBar, tvCurrentTime, tvTotalTime)
                } else {
                    // preload duration info for UI (optional)
                    preloadDuration(trackRes, tvTotalTime)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlayPause.setOnClickListener {
            val selectedRes = musicTracks[spnMusic.selectedItemPosition].second
            if (!isPlaying) startAmbientAudio(selectedRes, btnPlayPause, seekBar, tvCurrentTime, tvTotalTime)
            else pauseAmbientAudio(btnPlayPause)
        }

        // Seekbar interactions
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var userSeeking = false
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val pos = (mediaPlayer!!.duration * progress) / 100
                    tvCurrentTime.text = formatMillis(pos.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userSeeking = false
                mediaPlayer?.let { mp ->
                    val newPos = (mp.duration * (seekBar?.progress ?: 0)) / 100
                    mp.seekTo(newPos)
                }
            }
        })

        // Voice-over: when enabled, Play Voice-Over button will trigger TTS phrases
        btnPlayVoiceOver.setOnClickListener {
            if (!ttsReady) {
                Toast.makeText(this, "Voice engine not ready", Toast.LENGTH_SHORT).show()
            } else {
                if (switchVoiceOver.isChecked) playVoiceOverSequence()
                else Toast.makeText(this, "Enable Guided voice-over to use this", Toast.LENGTH_SHORT).show()
            }
        }

        btnSubmit.setOnClickListener {
            val feeling = spnFeeling.selectedItem.toString()
            val cause = spnCause.selectedItem.toString()
            val relax = spnRelax.selectedItem.toString()
            val change = spnChange.selectedItem.toString()

            if (feeling == "Select..." || cause == "Select..." || relax == "Select..." || change == "Select...") {
                Toast.makeText(this, "Please complete all selections before continuing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (reflection, videoDescription, meditationUrl) = when (feeling.lowercase()) {
                "stressed" -> Triple(
                    "Take slow, long breaths. Notice tension release on the exhale.",
                    "A guided breathing and relaxation video.",
                    "https://www.youtube.com/watch?v=ZToicYcHIOU"
                )
                "sad" -> Triple(
                    "Acknowledge your sadness with kindness. Breathe gently.",
                    "A mindfulness meditation focusing on self-compassion.",
                    "https://www.youtube.com/watch?v=inpok4MKVLM"
                )
                "happy" -> Triple(
                    "Sit with the joy. Let it expand softly through your body.",
                    "A short gratitude practice to deepen joy.",
                    "https://www.youtube.com/watch?v=O-6f5wQXSu8"
                )
                "anxious" -> Triple(
                    "Place one hand on your belly and breathe into it slowly.",
                    "A calming practice to steady your nervous system.",
                    "https://www.youtube.com/watch?v=MIr3RsUWrdo"
                )
                "calm" -> Triple(
                    "Enjoy this calmness. Breathe and notice sensations.",
                    "A gentle session to sustain your centered state.",
                    "https://www.youtube.com/watch?v=2OEL4P1Rz04"
                )
                else -> Triple(
                    "Keep breathing gently. You’re doing something kind for yourself.",
                    "A general mindfulness meditation.",
                    "https://www.youtube.com/watch?v=inpok4MKVLM"
                )
            }

            findViewById<TextView>(R.id.tvResult).apply {
                text = reflection
                visibility = TextView.VISIBLE
            }

            AlertDialog.Builder(this)
                .setTitle("Open Meditation Video?")
                .setMessage("$videoDescription\n\nWould you like to open this YouTube video now?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meditationUrl))
                    startActivity(intent)
                }
                .setNegativeButton("No") { dialog, _ ->
                    Toast.makeText(this, "Take a moment to reflect before continuing.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun preloadDuration(resId: Int, tvTotalTime: TextView) {
        // quick prepare to get duration without long prepareAsync; create, get duration, release
        val mp = MediaPlayer.create(this, resId)
        mp?.let {
            val dur = it.duration.takeIf { d -> d > 0 } ?: 0
            tvTotalTime.text = formatMillis(dur.toLong())
            try { it.release() } catch (_: Exception) {}
        }
    }

    private fun startAmbientAudio(
        rawResId: Int,
        btnPlayPause: ImageButton,
        seekBar: SeekBar,
        tvCurrent: TextView,
        tvTotal: TextView
    ) {
        // If already prepared and same resource, resume
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    // use local raw resource
                    val afd = resources.openRawResourceFd(rawResId) ?: return
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    isLooping = true
                    prepare()
                    start()
                    this@MeditationActivity.isPlaying = true
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    tvTotal.text = formatMillis(duration.toLong())
                    scheduleSeekbarUpdates(this, seekBar, tvCurrent)
                } catch (e: Exception) {
                    Toast.makeText(this@MeditationActivity, "Unable to play audio", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            mediaPlayer?.let {
                it.start()
                it.isLooping = true
                isPlaying = true
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                scheduleSeekbarUpdates(it, seekBar, tvCurrent)
            }
        }
    }

    private fun pauseAmbientAudio(btnPlayPause: ImageButton) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                btnPlayPause.setImageResource(R.drawable.ic_play)
                updateRunnable?.let { r -> uiHandler.removeCallbacks(r) }
            }
        }
    }

    private fun stopAndReleasePlayer() {
        updateRunnable?.let { uiHandler.removeCallbacks(it) }
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        mediaPlayer = null
        isPlaying = false
    }

    private fun scheduleSeekbarUpdates(mp: MediaPlayer, seekBar: SeekBar, tvCurrent: TextView) {
        updateRunnable?.let { uiHandler.removeCallbacks(it) }

        updateRunnable = Runnable {
            try {
                if (mp.isPlaying) {
                    val pos = mp.currentPosition
                    val dur = mp.duration.takeIf { it > 0 } ?: 1
                    val percent = (pos * 100) / dur
                    seekBar.progress = percent
                    tvCurrent.text = formatMillis(pos.toLong())
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                uiHandler.postDelayed(updateRunnable!!, 600)
            }
        }
        uiHandler.post(updateRunnable!!)
    }

    private fun formatMillis(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%d:%02d", minutes, seconds)
    }

    // TextToSpeech initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.0f)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
            ttsReady = false
        }
    }

    // Hard-coded voice-over phrases and sequence
    private fun playVoiceOverSequence() {
        if (!ttsReady) return

        val phrases = listOf(
            "Welcome. Find a comfortable seated position and let your shoulders soften.",
            "Slowly close your eyes and breathe in deeply through your nose.",
            "Exhale gently. Release any tension you are holding in the body.",
            "Say to yourself: I am safe. I am breathing. I am present.",
            "When you are ready, bring your awareness back to the room and open your eyes."
        )

        var delayMs = 0L
        for ((index, p) in phrases.withIndex()) {
            uiHandler.postDelayed({
                tts.speak(p, TextToSpeech.QUEUE_FLUSH, null, "VOICE_$index")
            }, delayMs)
            // estimate: length of phrase * 80ms per word + padding (simple heuristic)
            val words = p.split("\\s+".toRegex()).size
            delayMs += (words * 650).toLong() // 650ms per phrase word ~ slower pace
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
            isPlaying = false
        }
        // stop TTS speaking if activity is paused
        if (ttsReady) tts.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndReleasePlayer()
        updateRunnable?.let { uiHandler.removeCallbacks(it) }
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}

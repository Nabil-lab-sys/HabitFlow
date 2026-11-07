package com.example.todo

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Locale

class PomodoroFragment : Fragment() {

    private lateinit var timerText: TextView
    private lateinit var phaseLabel: TextView
    private lateinit var sessionHint: TextView
    private lateinit var timerRing: TimerRingView
    private lateinit var playPauseButton: ImageButton
    private lateinit var workButton: Button
    private lateinit var breakButton: Button
    private lateinit var focusTodayText: TextView
    private lateinit var focusWeekText: TextView
    private lateinit var focusTotalText: TextView

    private lateinit var focusStorage: FocusStorage

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = WORK_TIME
    private var isWorkSession = true
    private var isRunning = false
    private var justFinished = false

    /** Realtime mark of when the current focus stretch started, if one is running. */
    private var focusStartRealtime: Long? = null

    private var fallbackRingtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Prompts at most once per fragment instance, so starting the timer repeatedly doesn't re-ask. */
    private var permissionPrompted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pomodoro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerText = view.findViewById(R.id.timerText)
        phaseLabel = view.findViewById(R.id.phaseLabel)
        sessionHint = view.findViewById(R.id.sessionHint)
        timerRing = view.findViewById(R.id.timerRing)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        workButton = view.findViewById(R.id.workButton)
        breakButton = view.findViewById(R.id.breakButton)
        focusTodayText = view.findViewById(R.id.focusTodayText)
        focusWeekText = view.findViewById(R.id.focusWeekText)
        focusTotalText = view.findViewById(R.id.focusTotalText)

        focusStorage = FocusStorage(requireContext())
        PomodoroNotifier.ensureChannel(requireContext())

        savedInstanceState?.let {
            timeLeftInMillis = it.getLong(KEY_TIME_LEFT, WORK_TIME)
            isWorkSession = it.getBoolean(KEY_IS_WORK, true)
            justFinished = it.getBoolean(KEY_JUST_FINISHED, false)
            isRunning = it.getBoolean(KEY_IS_RUNNING, false)
        }

        playPauseButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        view.findViewById<ImageButton>(R.id.resetButton).setOnClickListener {
            resetTimer()
        }

        view.findViewById<ImageButton>(R.id.skipButton).setOnClickListener {
            switchSession()
        }

        workButton.setOnClickListener {
            commitFocusTime()
            isWorkSession = true
            resetTimer()
        }

        breakButton.setOnClickListener {
            commitFocusTime()
            isWorkSession = false
            resetTimer()
        }

        view.findViewById<View>(R.id.focusStatsCard).setOnClickListener {
            showFocusRecord()
        }

        render()

        // Resume a session that was running when the view was destroyed (e.g. rotation).
        if (isRunning) startTimer()
    }

    private fun startTimer() {
        ensureNotificationPermission()

        if (timeLeftInMillis <= 0) {
            timeLeftInMillis = sessionLength()
        }
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 250) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimeDisplay()
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                updateTimeDisplay()
                commitFocusTime()
                val finishedWork = isWorkSession
                if (finishedWork) focusStorage.addCompletedSession()

                isRunning = false
                justFinished = true
                // Line the next session up so the dial is ready to go again.
                isWorkSession = !isWorkSession
                timeLeftInMillis = sessionLength()
                render()

                val notified = PomodoroNotifier.notifyFinished(requireContext(), finishedWork)
                if (!notified) playFallbackAlarm()
            }
        }.start()

        if (isWorkSession) startFocusTracking()
        isRunning = true
        justFinished = false
        render()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        commitFocusTime()
        isRunning = false
        render()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        commitFocusTime()
        isRunning = false
        justFinished = false
        timeLeftInMillis = sessionLength()
        render()
    }

    private fun switchSession() {
        countDownTimer?.cancel()
        countDownTimer = null
        commitFocusTime()
        isRunning = false
        justFinished = false
        isWorkSession = !isWorkSession
        timeLeftInMillis = sessionLength()
        render()
    }

    private fun startFocusTracking() {
        if (focusStartRealtime == null) {
            focusStartRealtime = SystemClock.elapsedRealtime()
        }
    }

    /** Saves the focused time accumulated since the last start/pause. */
    private fun commitFocusTime() {
        val started = focusStartRealtime ?: return
        focusStartRealtime = null
        if (!::focusStorage.isInitialized) return

        val elapsed = SystemClock.elapsedRealtime() - started
        focusStorage.addFocusedMillis(elapsed)
    }

    private fun render() {
        phaseLabel.text = getString(
            if (isWorkSession) R.string.pomodoro_focus else R.string.pomodoro_break
        )

        sessionHint.text = when {
            isRunning -> getString(R.string.pomodoro_running)
            justFinished -> getString(
                if (isWorkSession) R.string.pomodoro_break_done else R.string.pomodoro_focus_done
            )
            timeLeftInMillis == sessionLength() -> getString(R.string.pomodoro_ready)
            else -> getString(R.string.pomodoro_paused)
        }

        playPauseButton.setImageResource(
            if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
        )
        playPauseButton.contentDescription = getString(
            if (isRunning) R.string.pomodoro_pause else R.string.pomodoro_start
        )

        styleChip(workButton, selected = isWorkSession)
        styleChip(breakButton, selected = !isWorkSession)

        updateTimeDisplay()
        updateFocusStats()
    }

    private fun updateTimeDisplay() {
        val totalSeconds = (timeLeftInMillis / 1000).coerceAtLeast(0)
        timerText.text = String.format(
            Locale.US,
            "%02d:%02d",
            totalSeconds / 60,
            totalSeconds % 60
        )

        val elapsed = 1f - timeLeftInMillis.toFloat() / sessionLength().toFloat()
        timerRing.setProgress(elapsed)
    }

    private fun updateFocusStats() {
        focusTodayText.text = FocusStorage.formatDuration(focusStorage.todayMillis())
        focusWeekText.text = FocusStorage.formatDuration(focusStorage.weekMillis())
        focusTotalText.text = FocusStorage.formatDuration(focusStorage.totalMillis())
    }

    private fun showFocusRecord() {
        val days = focusStorage.recentDays()
        val prettyDate = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val rawDate = SimpleDateFormat(FocusStorage.DAY_PATTERN, Locale.US)

        val message = buildString {
            appendLine("Today: ${FocusStorage.formatDuration(focusStorage.todayMillis())}")
            appendLine("Last 7 days: ${FocusStorage.formatDuration(focusStorage.weekMillis())}")
            appendLine("All time: ${FocusStorage.formatDuration(focusStorage.totalMillis())}")
            append(
                "Completed focus sessions: ${focusStorage.completedSessions()}"
            )

            if (days.isNotEmpty()) {
                appendLine()
                appendLine()
                appendLine("Daily record")
                for (day in days) {
                    val parsed = runCatching { rawDate.parse(day.date) }.getOrNull()
                    val label = parsed?.let { prettyDate.format(it) } ?: day.date
                    appendLine("$label — ${FocusStorage.formatDuration(day.millis)}")
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.focus_card_title)
            .setMessage(message.trim())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (permissionPrompted) return

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionPrompted = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Sound fallback for when notifications are blocked or not allowed. */
    private fun playFallbackAlarm() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        fallbackRingtone?.stop()
        fallbackRingtone = RingtoneManager.getRingtone(requireContext(), uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            play()
        }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ fallbackRingtone?.stop() }, 6000L)
    }

    private fun styleChip(button: Button, selected: Boolean) {
        button.backgroundTintList = null
        button.setBackgroundResource(
            if (selected) R.drawable.bg_pill_accent else R.drawable.bg_pill_muted
        )
        val colorRes = if (selected) R.color.white else R.color.timer_text_muted
        button.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun sessionLength(): Long = if (isWorkSession) WORK_TIME else BREAK_TIME

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::timerText.isInitialized) {
            outState.putLong(KEY_TIME_LEFT, timeLeftInMillis)
            outState.putBoolean(KEY_IS_WORK, isWorkSession)
            outState.putBoolean(KEY_JUST_FINISHED, justFinished)
            outState.putBoolean(KEY_IS_RUNNING, isRunning)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        commitFocusTime()
        handler.removeCallbacksAndMessages(null)
        fallbackRingtone?.stop()
        fallbackRingtone = null
    }

    companion object {
        private const val WORK_TIME = 25 * 60 * 1000L
        private const val BREAK_TIME = 5 * 60 * 1000L
        private const val KEY_TIME_LEFT = "time_left"
        private const val KEY_IS_WORK = "is_work_session"
        private const val KEY_JUST_FINISHED = "just_finished"
        private const val KEY_IS_RUNNING = "is_running"
    }
}

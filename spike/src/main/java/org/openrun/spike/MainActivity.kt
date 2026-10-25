package org.openrun.spike

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** Throwaway test bench for T0 (docs/MVP.md): no design, just the switches that matter. */
class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var startBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = Prefs.sp(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(24)) }

        col.addView(TextView(this).apply { text = "openRun · spike T0"; textSize = 22f; setTypeface(null, Typeface.BOLD) })
        status = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setPadding(0, dp(8), 0, dp(8)) }
        col.addView(status)

        fun check(label: String, key: String, def: Boolean) = CheckBox(this).apply {
            text = label; isChecked = prefs.getBoolean(key, def)
            setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(key, v).apply() }
        }
        col.addView(check("Voix", Prefs.VOICE, true))
        col.addView(check("Bips", Prefs.BEEPS, true))
        col.addView(check("Vibrations", Prefs.VIB, true))

        col.addView(TextView(this).apply { text = "Avec la musique / vidéo"; setPadding(0, dp(8), 0, 0) })
        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("Superposer", "Baisser", "Pause").forEachIndexed { i, label ->
            group.addView(RadioButton(this).apply { id = 100 + i; text = label })
        }
        group.check(100 + prefs.getInt(Prefs.MODE, 0))
        group.setOnCheckedChangeListener { _, id -> prefs.edit().putInt(Prefs.MODE, id - 100).apply() }
        col.addView(group)

        col.addView(TextView(this).apply { text = "Volume de la voix"; setPadding(0, dp(8), 0, 0) })
        col.addView(SeekBar(this).apply {
            max = 100; progress = prefs.getInt(Prefs.VOL, 80)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { prefs.edit().putInt(Prefs.VOL, p).apply() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        startBtn = Button(this).apply { setOnClickListener { toggle() } }
        col.addView(startBtn)
        col.addView(Button(this).apply {
            text = "Dire « Course » (test voix seul)"
            setOnClickListener { startService(Intent(this@MainActivity, RunService::class.java).setAction(RunService.ACTION_SAY)) }
        })

        setContentView(ScrollView(this).apply { addView(col, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) })
    }

    override fun onResume() { super.onResume(); ui.post(refresh) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(refresh) }

    private val refresh = object : Runnable {
        override fun run() {
            val s = SpikeState
            startBtn.text = if (s.running) "ARRÊTER" else "DÉMARRER"
            val el = if (s.running) (SystemClock.elapsedRealtime() - s.startedAt) / 1000 else 0
            status.text = buildString {
                appendLine("état      : ${if (s.running) "EN COURS" else "arrêté"}  ${el / 60}:${"%02d".format(el % 60)}")
                appendLine("phase     : ${s.phase}  (${s.phaseLeftMs / 1000}s)  #${s.phaseCount}")
                appendLine("GPS       : ${s.fixes} points  ${"%.0f".format(s.distanceM)} m  préc ${"%.0f".format(s.lastAccuracy)} m")
                appendLine("trou GPS  : dernier ${s.lastGapMs} ms  max ${s.maxGapMs} ms")
                appendLine("voix prête: ${s.ttsReady}")
                appendLine("--- journal ---")
                append(s.recent())
            }
            ui.postDelayed(this, 500)
        }
    }

    private fun toggle() {
        if (SpikeState.running) {
            startService(Intent(this, RunService::class.java).setAction(RunService.ACTION_STOP)); return
        }
        val need = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) { requestPermissions(need.toTypedArray(), 1); return }
        startForegroundService(Intent(this, RunService::class.java).setAction(RunService.ACTION_START))
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        startForegroundService(Intent(this, RunService::class.java).setAction(RunService.ACTION_START))
    }
}

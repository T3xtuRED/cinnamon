package com.t3xturedstudios.cinnamon

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.t3xturedstudios.cinnamon.R
import com.t3xturedstudios.cinnamon.latin.LatinIME
import java.io.File

class StatsPanel(
    private val context: Context,
    private val ime: LatinIME
) {

    private val root: FrameLayout by lazy {
        ime.window.window!!.decorView.findViewById(R.id.input_root)
    }

    private var panel: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 1000L

    fun show() {
        if (panel == null) {
            panel = FrameLayout(context).apply {
                setBackgroundColor(Color.parseColor("#CC000000"))
                visibility = View.GONE
                elevation = 1000f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            }
        }

        if (panel?.parent == null) {
            root.addView(panel)
        }

        buildUI()
        panel?.visibility = View.VISIBLE
        startRefreshing()
    }

    private fun buildUI() {
        panel?.removeAllViews()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(Color.parseColor("#202020"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(context).apply {
            text = "Diagnostics Panel"
            setTextColor(Color.WHITE)
            textSize = 22f
            setPadding(0, 0, 0, 20)
        }
        container.addView(title)

        val scroll = ScrollView(context)
        val statsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(statsLayout)

        container.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        val closeBtn = TextView(context).apply {
            text = "Close"
            setTextColor(Color.RED)
            textSize = 18f
            setPadding(0, 20, 0, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 80   // 🔼 podnosi przycisk
            layoutParams = params
            setOnClickListener { hide() }
        }

        container.addView(closeBtn)

        panel?.addView(container)

        updateStats(statsLayout)
    }

    private fun updateStats(layout: LinearLayout) {
        layout.removeAllViews()

        fun add(label: String, value: String) {
            val tv = TextView(context).apply {
                text = "$label: $value"
                setTextColor(Color.LTGRAY)
                textSize = 15f
                setPadding(0, 10, 0, 10)
            }
            layout.addView(tv)
        }

        // Uptime
        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = uptimeMs / 3600000
        val minutes = (uptimeMs / 60000) % 60
        add("Uptime", "${hours}h ${minutes}m")

        // Battery
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        fun getBatteryTemp(): Float? {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            return if (temp > 0) temp / 10f else null
        }

        add("Battery Level", "$batteryPct%")
        val batteryTemp = getBatteryTemp()
        add("Battery Temp", batteryTemp?.let { "${it}°C" } ?: "N/A")


        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramUsed = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
        val ramTotal = memInfo.totalMem / (1024 * 1024)
        add("RAM", "$ramUsed MB / $ramTotal MB")

        // Storage
        val stat = StatFs(context.filesDir.absolutePath)
        val total = stat.totalBytes / (1024 * 1024)
        val free = stat.availableBytes / (1024 * 1024)
        add("Storage", "$free MB free / $total MB total")

        // CPU Temp (best-effort)
        val cpuTemp = readCpuTemp()
        add("CPU Temp", if (cpuTemp != null) "${cpuTemp}°C" else "N/A")

        // Sensors
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val sensorList = sensors.getSensorList(android.hardware.Sensor.TYPE_ALL)
        add("Sensors", "${sensorList.size} available")

        sensorList.take(10).forEach { sensor ->
            add("• ${sensor.name}", "type ${sensor.type}")
        }
    }

    private fun readCpuTemp(): Float? {
        val paths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/hwmon/hwmon0/temp1_input"
        )

        for (path in paths) {
            try {
                val f = File(path)
                if (f.exists()) {
                    val raw = f.readText().trim()
                    val value = raw.toFloat() / 1000f
                    if (value > 0) return value
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun startRefreshing() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                panel?.let {
                    val scroll = (it.getChildAt(0) as LinearLayout).getChildAt(1) as ScrollView
                    val statsLayout = scroll.getChildAt(0) as LinearLayout
                    updateStats(statsLayout)
                }
                handler.postDelayed(this, refreshInterval)
            }
        }, refreshInterval)
    }

    private fun hide() {
        panel?.visibility = View.GONE
        handler.removeCallbacksAndMessages(null)
    }
}

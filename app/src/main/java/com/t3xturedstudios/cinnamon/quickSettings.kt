package com.t3xturedstudios.cinnamon

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.t3xturedstudios.cinnamon.R
import com.t3xturedstudios.cinnamon.latin.LatinIME

class QuickSettings(
    private val context: Context,
    private val ime: LatinIME
) {

    private val root: FrameLayout by lazy {
        ime.window.window!!.decorView.findViewById(R.id.input_root)
    }

    private val panel: FrameLayout by lazy {
        FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            alpha = 0.97f
            visibility = View.GONE
            elevation = 999f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
    }

    fun show() {
        if (panel.parent == null) root.addView(panel)
        panel.removeAllViews()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // ============================
        // 🔋 BATERIA
        // ============================
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val batteryRow = makeInfoRow(R.drawable.ic_battery, "Battery: $batteryLevel%")
        container.addView(batteryRow)

        // ============================
        // 🔊 GŁOŚNOŚĆ
        // ============================
        container.addView(makeLabel("Volume"))

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        val volumeSlider = SeekBar(context).apply {
            max = maxVol
            progress = currentVol
            setPadding(0, 10, 0, 30)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    am.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        value,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(volumeSlider)

        // ============================
        // 🔆 JASNOŚĆ
        // ============================
        container.addView(makeLabel("Brightness"))

        val currentBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            125
        )

        val brightnessSlider = SeekBar(context).apply {
            max = 255
            progress = currentBrightness
            setPadding(0, 10, 0, 30)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        value
                    )
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(brightnessSlider)

        // ============================
        // 🟦 GRID IKON
        // ============================
        val grid = GridLayout(context).apply {
            rowCount = 2
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = true
            setPadding(0, 20, 0, 20)
        }

        grid.addView(makeIconButton(R.drawable.ic_wifi) { openWifiSettings() })
        grid.addView(makeIconButton(R.drawable.ic_data) { openInternetSettings() })
        grid.addView(makeIconButton(R.drawable.ic_bluetooth) { openBluetoothSettings() })
        grid.addView(makeIconButton(R.drawable.ic_admin) { openDeviceAdminSettings() })
        grid.addView(makeIconButton(R.drawable.ic_settings) { openFullSettings() })
        grid.addView(makeIconButton(R.drawable.ic_close) { panel.visibility = View.GONE })

        container.addView(grid)

        panel.addView(container)
        panel.visibility = View.VISIBLE
    }

    // ============================
    // UI HELPERS
    // ============================

    private fun makeIconButton(icon: Int, action: () -> Unit): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }

        val img = ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setOnClickListener { action() }
        }

        layout.addView(img)
        return layout
    }

    private fun makeInfoRow(icon: Int, text: String): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 20)
        }

        val img = ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(80, 80)
        }

        val tv = TextView(context).apply {
            this.text = text
            setTextColor(Color.LTGRAY)
            textSize = 16f
            setPadding(20, 0, 0, 0)
        }

        row.addView(img)
        row.addView(tv)
        return row
    }

    private fun makeLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
    }

    // ============================
    // INTENTY
    // ============================

    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openInternetSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openDeviceAdminSettings() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openFullSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

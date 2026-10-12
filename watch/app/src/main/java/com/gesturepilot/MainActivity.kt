package com.gesturepilot

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var status: TextView
    private lateinit var motion: TextView
    private lateinit var toggle: Button
    private lateinit var calibrate: Button
    private var tracking = false
    private var lastGestureAt = 0L
    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 0f
    private var calibrated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.rgb(12, 18, 28))
        }
        val title = TextView(this).apply {
            text = "GESTURE PILOT"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Připraveno"
            textSize = 14f
            setTextColor(Color.rgb(100, 220, 170))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        motion = TextView(this).apply {
            text = "Pohyb: čekám na spuštění"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 12)
        }
        toggle = Button(this).apply {
            text = "Spustit sledování"
            setOnClickListener { setTracking(!tracking) }
        }
        calibrate = Button(this).apply {
            text = "Kalibrovat výchozí polohu"
            setOnClickListener {
                calibrated = false
                status.text = "Drž hodinky chvíli v klidu…"
            }
        }
        root.addView(title)
        root.addView(status)
        root.addView(motion)
        root.addView(toggle)
        root.addView(calibrate)
        setContentView(root)

        if (accelerometer == null) {
            status.text = "Akcelerometr není dostupný"
            toggle.isEnabled = false
        }
    }

    private fun setTracking(enabled: Boolean) {
        tracking = enabled
        if (enabled) {
            accelerometer?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            gyroscope?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            toggle.text = "Zastavit sledování"
            status.text = "Sleduji pohyb ruky"
        } else {
            sensors.unregisterListener(this)
            toggle.text = "Spustit sledování"
            status.text = "Sledování pozastaveno"
            motion.text = "Pohyb: —"
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!tracking) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            if (!calibrated) {
                baselineX = x; baselineY = y; baselineZ = z
                calibrated = true
                status.text = "Výchozí poloha nastavena"
            }
            val dx = x - baselineX
            val dy = y - baselineY
            val dz = z - baselineZ
            val magnitude = sqrt(dx * dx + dy * dy + dz * dz)
            motion.text = "X %.1f  Y %.1f  Z %.1f".format(dx, dy, dz)
            val now = SystemClock.elapsedRealtime()
            if (magnitude > 5.5f && now - lastGestureAt > 900) {
                lastGestureAt = now
                status.text = "Pohyb rozpoznán ✓"
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val speed = sqrt(event.values[0] * event.values[0] +
                event.values[1] * event.values[1] + event.values[2] * event.values[2])
            if (speed > 2.8f) {
                status.text = "Rychlé otočení zápěstí"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onResume() {
        super.onResume()
        if (tracking) setTracking(true)
    }

    override fun onPause() {
        sensors.unregisterListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        sensors.unregisterListener(this)
        super.onDestroy()
    }
}

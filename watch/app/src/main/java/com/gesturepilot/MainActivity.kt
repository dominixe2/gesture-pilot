package com.gesturepilot

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private lateinit var status: TextView
    private lateinit var ipInput: EditText
    private lateinit var startButton: Button
    private var running = false
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private val port = 50123
    private val sender = Executors.newSingleThreadExecutor()
    private var baseX = 0f
    private var baseY = 0f
    private var calibrated = false
    private var lastSend = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(16, 10, 16, 10) }
        root.addView(TextView(this).apply { text = "GESTURE PILOT"; textSize = 19f; gravity = Gravity.CENTER })
        status = TextView(this).apply { text = "Zadej IP notebooku"; textSize = 13f; gravity = Gravity.CENTER; setPadding(0, 8, 0, 8) }
        ipInput = EditText(this).apply { hint = "IP notebooku, např. 192.168.1.20"; setSingleLine(true); textSize = 12f }
        startButton = Button(this).apply { text = "Spustit kurzor"; setOnClickListener { if (running) stopTracking() else startTracking() } }
        root.addView(status); root.addView(ipInput); root.addView(startButton)
        root.addView(Button(this).apply { text = "Kalibrovat"; setOnClickListener { calibrated = false; status.text = "Drž ruku chvíli v klidu" } })
        root.addView(Button(this).apply { text = "Klik"; setOnClickListener { send("{\"type\":\"click\"}") } })
        root.addView(TextView(this).apply { text = "Hodinky a notebook musí být na stejné Wi-Fi. Port 50123."; textSize = 11f; gravity = Gravity.CENTER })
        setContentView(root)
    }

    private fun startTracking() {
        try {
            address = InetAddress.getByName(ipInput.text.toString().trim())
            socket = DatagramSocket()
            val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (sensor == null) { status.text = "Akcelerometr není dostupný"; socket?.close(); socket = null; return }
            sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            running = true; calibrated = false; startButton.text = "Zastavit kurzor"; status.text = "Sleduji pohyb — kalibruji"
        } catch (_: Exception) { status.text = "Chybná IP nebo problém se sítí"; socket?.close(); socket = null }
    }

    private fun stopTracking() {
        running = false; sensors.unregisterListener(this); socket?.close(); socket = null
        startButton.text = "Spustit kurzor"; status.text = "Sledování zastaveno"
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]
        if (!calibrated) { baseX = x; baseY = y; calibrated = true; status.text = "Kalibrováno — pohybuj zápěstím"; return }
        val now = System.currentTimeMillis()
        if (now - lastSend < 35) return
        lastSend = now
        val dx = ((baseY - y) * 3f).coerceIn(-18f, 18f)
        val dy = ((x - baseX) * 3f).coerceIn(-18f, 18f)
        send("{\"type\":\"move\",\"x\":" + dx + ",\"y\":" + dy + "}")
    }

    private fun send(message: String) {
        val target = address ?: return
        sender.execute { try { val bytes = message.toByteArray(Charsets.UTF_8); socket?.send(DatagramPacket(bytes, bytes.size, target, port)) } catch (_: Exception) { runOnUiThread { status.text = "Odeslání selhalo — zkontroluj Wi-Fi" } } }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onPause() { super.onPause(); if (running) stopTracking() }
    override fun onDestroy() { stopTracking(); sender.shutdownNow(); super.onDestroy() }
}

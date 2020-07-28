package me.hackerchick.raisetoanswer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException


class MainActivity : AppCompatActivity() {
    private var PERMISSION_REQUEST_READ_PHONE_STATE = 1

    private val setListenerState: Queue<Boolean> = LinkedList<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(this, RaiseToAnswerSensorEventListener::class.java)
        this.startService(intent)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS), PERMISSION_REQUEST_READ_PHONE_STATE)

        var sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        var accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (proximitySensor == null || accelerometer == null) {
            Toast.makeText(applicationContext, getString(R.string.could_not_bind_sensor), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val testButton: Button = findViewById(R.id.test_button)
        testButton.setOnClickListener {
            Toast.makeText(applicationContext, getString(R.string.hold_to_ear_test), Toast.LENGTH_SHORT).show()
            RaiseToAnswerSensorEventListener.instance!!.waitUntilEarPickup { }
        }

        val activeSwitch: Switch = findViewById<Switch>(R.id.raise_to_answer_switch)
        val appEnabled = getSharedPreferences(getString(R.string.app_enabled_key), Context.MODE_PRIVATE)

        activeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with (appEnabled.edit()) {
                    putInt(getString(R.string.app_enabled_key), 1)
                    commit()
                }
            } else {
                with (appEnabled.edit()) {
                    putInt(getString(R.string.app_enabled_key), 0)
                    commit()
                }
            }
            setListenerState.add(isChecked)
            testButton.isEnabled = isChecked
        }

        activeSwitch.isChecked = appEnabled.getInt(getString(R.string.app_enabled_key), 1) == 1
        testButton.isEnabled = activeSwitch.isChecked

        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleWithFixedDelay({
            var listener: RaiseToAnswerSensorEventListener? = RaiseToAnswerSensorEventListener.instance

            if (listener == null)
                return@scheduleWithFixedDelay

            var value: Boolean? = null

            while (true) {
                try {
                    value = setListenerState.remove()
                } catch (_: NoSuchElementException) {
                    break
                }
            }

            when (value) {
                true -> {
                    listener.bind(this, sensorManager, proximitySensor, accelerometer)
                }
                false -> {
                    listener.disable()
                }
            }
        }, 0L, 1000, TimeUnit.MILLISECONDS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(applicationContext, getString(R.string.permissions_needed), Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
                return
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.privacy_policy -> {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://thelastproject.github.io/RaiseToAnswer/PRIVACY_POLICY"))
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

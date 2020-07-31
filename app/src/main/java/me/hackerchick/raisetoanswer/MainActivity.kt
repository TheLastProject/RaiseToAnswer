package me.hackerchick.raisetoanswer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException


class MainActivity : AppCompatActivity() {
    private var PERMISSION_REQUEST_READ_PHONE_STATE = 1

    private val setListenerRaiseToAnswerState: Queue<Boolean> = LinkedList<Boolean>()
    private val setListenerFlipOverToDeclineState: Queue<Boolean> = LinkedList<Boolean>()

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
            RaiseToAnswerSensorEventListener.instance!!.waitUntilDesiredState(
                pickupCallback = {
                    Looper.prepare()
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.test_result))
                        .setMessage(getString(R.string.detected_raise_to_answer))
                        .show();
                    Looper.loop()
                },
                declineCallback = {
                    Looper.prepare()
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.test_result))
                        .setMessage(getString(R.string.detected_flip_over))
                        .show()
                    Looper.loop()
                }
            )
        }

        val raiseOption: CheckedTextView = findViewById<CheckedTextView>(R.id.raise_to_answer_option)
        val flipOverOption: CheckedTextView = findViewById<CheckedTextView>(R.id.flip_over_to_decline_option)

        raiseOption.setOnClickListener { _->
            setRaiseOption(!raiseOption.isChecked)
        }

        flipOverOption.setOnClickListener { _->
            setFlipOverOption(!flipOverOption.isChecked)
        }

        val raiseEnabled = getSharedPreferences(getString(R.string.raise_enabled_key), Context.MODE_PRIVATE)
        val flipOverEnabled = getSharedPreferences(getString(R.string.flip_over_enabled_key), Context.MODE_PRIVATE)

        setRaiseOption(raiseEnabled.getInt(getString(R.string.raise_enabled_key), 1) == 1)
        setFlipOverOption(flipOverEnabled.getInt(getString(R.string.flip_over_enabled_key), 0) == 1)

        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleWithFixedDelay({
            var listener: RaiseToAnswerSensorEventListener? = RaiseToAnswerSensorEventListener.instance

            if (listener == null)
                return@scheduleWithFixedDelay

            var raiseStateEnabled: Boolean? = null
            var flipOverStateEnabled: Boolean? = null

            // Get RaiseEnabled state
            while (true) {
                try {
                    raiseStateEnabled = setListenerRaiseToAnswerState.remove()
                } catch (_: NoSuchElementException) {
                    break
                }
            }

            System.out.println("R :$raiseStateEnabled")

            // Get FlipOverEnabled state
            while (true) {
                try {
                    flipOverStateEnabled = setListenerFlipOverToDeclineState.remove()
                } catch (_: NoSuchElementException) {
                    break
                }
            }

            System.out.println("F :$flipOverStateEnabled")

            if (raiseStateEnabled != null) {
                listener.watchPickupState(raiseStateEnabled)
            }
            if (flipOverStateEnabled != null) {
                listener.watchDeclineState(flipOverStateEnabled)
            }

            if (listener.pickupState() || listener.declineState()) {
                listener.bind(this, sensorManager, proximitySensor, accelerometer)
                testButton.isEnabled = true
            } else {
                listener.disable()
                testButton.isEnabled = false
            }
        }, 0L, 200, TimeUnit.MILLISECONDS)
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

    private fun setRaiseOption(value: Boolean) {
        val raiseOption: CheckedTextView = findViewById<CheckedTextView>(R.id.raise_to_answer_option)
        raiseOption.isChecked = value

        val raiseEnabled = getSharedPreferences(getString(R.string.raise_enabled_key), Context.MODE_PRIVATE)
        with (raiseEnabled.edit()) {
            putInt(getString(R.string.raise_enabled_key), if (value) 1 else 0)
            commit()
        }
        setListenerRaiseToAnswerState.add(value)
    }

    private fun setFlipOverOption(value: Boolean) {
        val flipOverOption: CheckedTextView = findViewById<CheckedTextView>(R.id.flip_over_to_decline_option)
        flipOverOption.isChecked = value

        val flipOverEnabled = getSharedPreferences(getString(R.string.flip_over_enabled_key), Context.MODE_PRIVATE)
        with (flipOverEnabled.edit()) {
            putInt(getString(R.string.flip_over_enabled_key), if (value) 1 else 0)
            commit()
        }
        setListenerFlipOverToDeclineState.add(value)
    }
}

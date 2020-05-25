package me.hackerchick.raisetoanswer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private var PERMISSION_REQUEST_READ_PHONE_STATE = 1

    private var mSensorEventListener: RaiseToAnswerSensorEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS), PERMISSION_REQUEST_READ_PHONE_STATE)

        var sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        var accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mSensorEventListener = RaiseToAnswerSensorEventListener(sensorManager, proximitySensor, accelerometer)

        val testButton: Button = findViewById(R.id.test_button)
        testButton.setOnClickListener {
            Toast.makeText(applicationContext, getString(R.string.hold_to_ear_test), Toast.LENGTH_SHORT).show()
            mSensorEventListener!!.waitUntilEarPickup {}
        }

        val activeSwitch: Switch = findViewById(R.id.raise_to_answer_switch)
        val appEnabled = getSharedPreferences(getString(R.string.app_enabled_key), Context.MODE_PRIVATE)

        activeSwitch.isChecked = appEnabled.getInt(getString(R.string.app_enabled_key), 1) == 1

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
        }
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
}

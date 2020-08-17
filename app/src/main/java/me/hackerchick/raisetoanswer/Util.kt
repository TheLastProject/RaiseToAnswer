package me.hackerchick.raisetoanswer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager


class Util {
    companion object {
        var serviceIntent: Intent? = null

        private fun getRaiseFeatureSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.raise_enabled_key), Context.MODE_PRIVATE)
        }

        fun raiseFeatureEnabled(context: Context): Boolean {
            return getRaiseFeatureSharedPreference(context).getInt(context.getString(R.string.raise_enabled_key), 1) == 1
        }

        fun setRaiseFeatureEnabled(context: Context, enabled: Boolean) {
            with (getRaiseFeatureSharedPreference(context).edit()) {
                putInt(context.getString(me.hackerchick.raisetoanswer.R.string.raise_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        private fun getFlipOverFeatureSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.flip_over_enabled_key), Context.MODE_PRIVATE)
        }

        fun flipOverFeatureEnabled(context: Context): Boolean {
            return getFlipOverFeatureSharedPreference(context).getInt(context.getString(R.string.flip_over_enabled_key), 0) == 1
        }

        fun setFlipOverFeatureEnabled(context: Context, enabled: Boolean) {
            with (getFlipOverFeatureSharedPreference(context).edit()) {
                putInt(context.getString(me.hackerchick.raisetoanswer.R.string.flip_over_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        private fun getBeepBehaviourSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.beep_behaviour_enabled_key), Context.MODE_PRIVATE)
        }

        fun beepBehaviourEnabled(context: Context): Boolean {
            return getBeepBehaviourSharedPreference(context).getInt(context.getString(R.string.beep_behaviour_enabled_key), 1) == 1
        }

        fun setBeepBehaviourEnabled(context: Context, enabled: Boolean) {
            with (getBeepBehaviourSharedPreference(context).edit()) {
                putInt(context.getString(me.hackerchick.raisetoanswer.R.string.beep_behaviour_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        fun getSensorManager(context: Context): SensorManager {
            return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }

        fun getSensors(sensorManager: SensorManager): List<Sensor?> {
            return listOf(
                sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            )
        }

        fun hasRequiredSensors(context: Context): Boolean {
            val (proximitySensor, accelerometer, magnetometer) = getSensors(getSensorManager(context))

            return proximitySensor != null && accelerometer != null
        }

        fun hasMagnetometer(context: Context): Boolean {
            val (_, _, magnetometer) = getSensors(getSensorManager(context))

            return magnetometer != null
        }

        fun startSensorListener(context: Context, testMode: Boolean): Boolean {
            // Stop service if running
            stopSensorListener(context)

            // Get enabled features
            val raiseEnabled = raiseFeatureEnabled(context)
            val flipOverEnabled = flipOverFeatureEnabled(context)

            // If no features are enabled, listening makes no sense
            if (!raiseEnabled && !flipOverEnabled) {
                return false
            }

            // Prepare the intent for the sensor event listener
            serviceIntent = Intent(context, RaiseToAnswerSensorEventListener::class.java)
            serviceIntent!!.putExtra("testMode", testMode)

            // Set the features and behaviours
            serviceIntent!!.putExtra(context.getString(R.string.raise_enabled_key), raiseEnabled)
            serviceIntent!!.putExtra(context.getString(R.string.flip_over_enabled_key), flipOverEnabled)
            serviceIntent!!.putExtra(context.getString(R.string.beep_behaviour_enabled_key), beepBehaviourEnabled(context))

            context.startForegroundService(serviceIntent)

            return true
        }

        fun stopSensorListener(context: Context) {
            if (serviceIntent != null) {
                context.stopService(serviceIntent)
                serviceIntent = null
            }
        }
    }
}
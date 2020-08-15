package me.hackerchick.raisetoanswer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log


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

        fun getSensors(sensorManager: SensorManager): List<Sensor> {
            return listOf(
                sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            )
        }

        fun hasWorkingSensors(context: Context): Boolean {
            val (proximitySensor, accelerometer, magnetometer) = getSensors(getSensorManager(context))
            return proximitySensor != null && accelerometer != null && magnetometer != null
        }

        fun startSensorListener(context: Context, testMode: Boolean) {
            Log.w("RAISETOANSWER", "GOT TO LISTENER STARTER")
            // Stop service if running
            stopSensorListener(context)

            // Prepare the intent for the sensor event listener
            serviceIntent = Intent(context, RaiseToAnswerSensorEventListener::class.java)
            serviceIntent!!.putExtra("testMode", testMode)

            // Set the features and behaviours
            serviceIntent!!.putExtra(context.getString(R.string.raise_enabled_key), raiseFeatureEnabled(context))
            serviceIntent!!.putExtra(context.getString(R.string.flip_over_enabled_key), flipOverFeatureEnabled(context))
            serviceIntent!!.putExtra(context.getString(R.string.beep_behaviour_enabled_key), beepBehaviourEnabled(context))

            Log.w("RAISETOANSWER", "PREPARED, STARTING FOREGROUND SERVICE")
            context.startForegroundService(serviceIntent)
            Log.w("RAISETOANSWER", "STARTED FOREGROUND SERVICE")
        }

        fun stopSensorListener(context: Context) {
            if (serviceIntent != null) {
                context.stopService(serviceIntent)
                serviceIntent = null
            }
        }
    }
}
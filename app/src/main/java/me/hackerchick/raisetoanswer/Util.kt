package me.hackerchick.raisetoanswer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.MutableLiveData

class Util {
    companion object {
        var serviceIntent: Intent? = null

        private val debugLog = ArrayList<String>()
        private val debugLogLiveData = MutableLiveData<List<String>>()

        fun log(message: String, testMode: Boolean) {
            Log.d("RaiseToAnswer", message);

            if (testMode) {
                val timeStampedMessage = System.currentTimeMillis().toString() + ": " + message
                debugLog.add(timeStampedMessage)
                debugLogLiveData.postValue(debugLog)
            }
        }

        fun clearLog() {
            debugLog.clear()
            debugLogLiveData.value = debugLog
        }

        fun getLog(): MutableLiveData<List<String>> {
            return debugLogLiveData
        }

        private fun getPrivacyPolicyShownSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.privacy_policy_shown_key), Context.MODE_PRIVATE)
        }

        fun privacyPolicyShown(context: Context): Boolean {
            return getPrivacyPolicyShownSharedPreference(context).getInt(context.getString(R.string.privacy_policy_shown_key), 0) == 1
        }

        fun setPrivacyPolicyShown(context: Context, value: Boolean) {
            with (getPrivacyPolicyShownSharedPreference(context).edit()) {
                putInt(context.getString(R.string.privacy_policy_shown_key), if (value) 1 else 0)
                commit()
            }
        }

        private fun getAnswerFeatureSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.raise_enabled_key), Context.MODE_PRIVATE)
        }

        fun answerFeatureEnabled(context: Context): Boolean {
            return getAnswerFeatureSharedPreference(context).getInt(context.getString(R.string.raise_enabled_key), 1) == 1
        }

        fun setAnswerFeatureEnabled(context: Context, enabled: Boolean) {
            with (getAnswerFeatureSharedPreference(context).edit()) {
                putInt(context.getString(R.string.raise_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        private fun getAnswerAllAnglesFeatureSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.answer_all_angles_enabled_key), Context.MODE_PRIVATE)
        }

        fun answerAllAnglesFeatureEnabled(context: Context): Boolean {
            return getAnswerAllAnglesFeatureSharedPreference(context).getInt(context.getString(R.string.answer_all_angles_enabled_key), 0) == 1
        }

        fun setAnswerAllAnglesFeatureEnabled(context: Context, enabled: Boolean) {
            with (getAnswerAllAnglesFeatureSharedPreference(context).edit()) {
                putInt(context.getString(me.hackerchick.raisetoanswer.R.string.answer_all_angles_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        private fun getDeclineFeatureSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.flip_over_enabled_key), Context.MODE_PRIVATE)
        }

        fun declineFeatureEnabled(context: Context): Boolean {
            return getDeclineFeatureSharedPreference(context).getInt(context.getString(R.string.flip_over_enabled_key), 0) == 1
        }

        fun setDeclineFeatureEnabled(context: Context, enabled: Boolean) {
            with (getDeclineFeatureSharedPreference(context).edit()) {
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

        private fun getVibrateBehaviourSharedPreference(context: Context): SharedPreferences {
            return context.getSharedPreferences(context.getString(R.string.vibrate_behaviour_enabled_key), Context.MODE_PRIVATE)
        }

        fun vibrateBehaviourEnabled(context: Context): Boolean {
            return getVibrateBehaviourSharedPreference(context).getInt(context.getString(R.string.vibrate_behaviour_enabled_key), 0) == 1
        }

        fun setVibrateBehaviourEnabled(context: Context, enabled: Boolean) {
            with (getVibrateBehaviourSharedPreference(context).edit()) {
                putInt(context.getString(me.hackerchick.raisetoanswer.R.string.vibrate_behaviour_enabled_key), if (enabled) 1 else 0)
                commit()
            }
        }

        fun getSensorManager(context: Context): SensorManager {
            return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }

        fun getSensors(sensorManager: SensorManager): List<Sensor?> {
            return listOf(
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            )
        }

        fun hasRequiredSensors(context: Context): Boolean {
            val (proximitySensor, accelerometer, _) = getSensors(getSensorManager(context))

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
            val answerEnabled = answerFeatureEnabled(context)
            val answerAllAnglesEnabled = answerAllAnglesFeatureEnabled(context)
            val declineEnabled = declineFeatureEnabled(context)

            // If no features are enabled, listening makes no sense
            if (!answerEnabled && !declineEnabled) {
                return false
            }

            // Prepare the intent for the sensor event listener
            serviceIntent = Intent(context, RaiseToAnswerSensorEventListener::class.java)
            serviceIntent!!.putExtra("testMode", testMode)

            // Set the features and behaviours
            serviceIntent!!.putExtra(context.getString(R.string.raise_enabled_key), answerEnabled)
            serviceIntent!!.putExtra(context.getString(R.string.answer_all_angles_enabled_key), answerAllAnglesEnabled)
            serviceIntent!!.putExtra(context.getString(R.string.flip_over_enabled_key), declineEnabled)
            serviceIntent!!.putExtra(context.getString(R.string.beep_behaviour_enabled_key), beepBehaviourEnabled(context))
            serviceIntent!!.putExtra(context.getString(R.string.vibrate_behaviour_enabled_key), vibrateBehaviourEnabled(context))

            context.startForegroundService(serviceIntent)

            return true
        }

        fun stopSensorListener(context: Context) {
            if (serviceIntent != null) {
                context.stopService(serviceIntent)
                serviceIntent = null
            }
        }

        fun createIncomingCallForegroundServiceNotificationChannel(context: Context): NotificationChannel {
            val channel = NotificationChannel("incoming_call", context.getString(R.string.incoming_call_service), NotificationManager.IMPORTANCE_LOW)
            val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)

            return channel
        }
    }
}
package me.hackerchick.raisetoanswer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class RaiseToAnswerCallReceiver : BroadcastReceiver() {
    private var mTelephony: TelephonyManager? = null
    private var mPhoneListener: RaiseToAnswerPhoneStateListener? = null

    private var mSensorManager: SensorManager? = null
    private var mProximitySensor: Sensor? = null
    private var mAccelerometer: Sensor? = null

    override fun onReceive(context: Context, intent: Intent?) {
        if (context.getSharedPreferences(context.getString(R.string.app_enabled_key), Context.MODE_PRIVATE).getInt(context.getString(R.string.app_enabled_key), 1) != 1) {
            // Don't do anything if app is set to "disabled"
            return
        }

        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximitySensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mPhoneListener = RaiseToAnswerPhoneStateListener(context, mSensorManager!!, mProximitySensor!!, mAccelerometer!!)

        mTelephony = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        mTelephony!!.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun onDestroy() {
        mTelephony!!.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE)
    }

}

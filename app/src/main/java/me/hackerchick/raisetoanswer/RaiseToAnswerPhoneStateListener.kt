package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast

class RaiseToAnswerPhoneStateListener(context: Context, sensorManager: SensorManager, proximitySensor: Sensor, accelerometer: Sensor) : PhoneStateListener() {
    private var mContext: Context? = null

    private var mSensorEventListener: RaiseToAnswerSensorEventListener? = null

    private var mSensorManager: SensorManager? = null
    private var mProximitySensor: Sensor? = null
    private var mAccelerometer: Sensor? = null

    init {
        mContext = context
        mSensorManager = sensorManager
        mProximitySensor = proximitySensor
        mAccelerometer = accelerometer
    }

    @SuppressLint("MissingPermission")
    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                mSensorEventListener?.stop()
                mSensorEventListener = null
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Toast.makeText(mContext, mContext!!.getString(R.string.hold_to_ear_to_answer), Toast.LENGTH_LONG).show()
                mSensorEventListener = RaiseToAnswerSensorEventListener(mSensorManager!!, mProximitySensor!!, mAccelerometer!!)
                mSensorEventListener!!.waitUntilEarPickup{ ->
                    // Pickup triggered
                    val tm = mContext!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    tm.acceptRingingCall()
                }
            }
        }
    }
}
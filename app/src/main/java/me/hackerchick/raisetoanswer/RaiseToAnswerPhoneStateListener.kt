package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast


class RaiseToAnswerPhoneStateListener(context: Context) : PhoneStateListener() {
    private var mContext: Context? = null

    init {
        mContext = context
    }

    @SuppressLint("MissingPermission")
    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                RaiseToAnswerSensorEventListener.instance!!.waitUntilEarPickup{ ->
                    // Pickup triggered
                    val tm = mContext!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    tm.acceptRingingCall()
                }
                Toast.makeText(mContext, mContext!!.getString(R.string.hold_to_ear_to_answer), Toast.LENGTH_LONG).show()
            }
            else -> {
                RaiseToAnswerSensorEventListener.instance!!.stop()
            }
        }
    }
}
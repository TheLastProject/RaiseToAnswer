package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager


class RaiseToAnswerPhoneStateListener(context: Context) : PhoneStateListener() {
    private var mContext: Context? = null

    init {
        mContext = context
    }

    @SuppressLint("MissingPermission")
    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                RaiseToAnswerSensorEventListener.instance?.waitUntilDesiredState(
                    pickupCallback = {
                        // Pickup triggered
                        val tm = mContext!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        tm.acceptRingingCall()
                    },
                    declineCallback = {
                        // Decline triggered
                        val tm = mContext!!.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                        tm.endCall()
                    }
                )
            }
            else -> {
                // Reported possible NullPointerException in Google Play
                // While I'm not sure why, if the event listener doesn't exist we don't need to stop it anyway
                RaiseToAnswerSensorEventListener.instance?.stop()
            }
        }
    }
}
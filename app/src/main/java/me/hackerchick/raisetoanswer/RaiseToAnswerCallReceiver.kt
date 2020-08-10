package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat.getSystemService

class RaiseToAnswerCallReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission", "NewApi")
    override fun onReceive(context: Context, intent: Intent?) {
        val state = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (TelephonyManager.EXTRA_STATE_RINGING == state) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

            RaiseToAnswerSensorEventListener.instance?.waitUntilDesiredState(
                pickupCallback = {
                    // Pickup triggered
                    tm.acceptRingingCall()
                },
                declineCallback = {
                    // Decline triggered
                    tm.endCall()
                }
            )
        } else {
            RaiseToAnswerSensorEventListener.instance?.stop()
        }
    }
}

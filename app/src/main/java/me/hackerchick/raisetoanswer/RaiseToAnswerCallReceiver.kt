package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class RaiseToAnswerCallReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission", "NewApi")
    override fun onReceive(context: Context, intent: Intent?) {
        val state = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (TelephonyManager.EXTRA_STATE_RINGING == state) {2
            Util.startSensorListener(context, false)
        } else {
            Util.stopSensorListener(context)
        }
    }
}

package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ResultReceiver
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log

class RaiseToAnswerCallReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission", "NewApi")
    override fun onReceive(context: Context, intent: Intent?) {
        Log.w("RAISETOANSWER", "GOT STARTED FROM PHONE STATE CHANGE")

        val state = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.w("RAISETOANSWER", "STATE IS " + state)
        if (TelephonyManager.EXTRA_STATE_RINGING == state) {
            Log.w("RAISETOANSWER", "STARTING LISTENER")
            Util.startSensorListener(context, false)
        } else {
            Util.stopSensorListener(context)
        }
    }
}

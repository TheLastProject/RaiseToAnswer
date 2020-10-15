package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager


class RaiseToAnswerCallReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission", "NewApi")
    override fun onReceive(context: Context, intent: Intent?) {
        val state = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (TelephonyManager.EXTRA_STATE_RINGING == state && audioManager.mode != AudioManager.MODE_IN_CALL) {
            Util.startSensorListener(context, false)
        } else {
            Util.stopSensorListener(context)
        }
    }
}

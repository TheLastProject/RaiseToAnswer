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

    override fun onReceive(context: Context, intent: Intent?) {
        mPhoneListener = RaiseToAnswerPhoneStateListener(context)

        mTelephony = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        mTelephony!!.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun onDestroy() {
        mTelephony?.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE)
    }

}

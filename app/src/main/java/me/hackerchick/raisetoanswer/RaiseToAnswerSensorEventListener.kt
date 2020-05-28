package me.hackerchick.raisetoanswer

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import java.util.*
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt


class RaiseToAnswerSensorEventListener : Service(), SensorEventListener {
    companion object {
        var instance: RaiseToAnswerSensorEventListener? = null
    }

    private val ONGOING_NOTIFICATION_ID = 1
    private val SENSOR_SENSITIVITY = 4

    private var mProximityValue: Float? = null
    private var mInclinationValue: Int? = null

    private val mToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    // First 2 beeps: Good start state found (proximity not near)
    // 3 more beeps: Pickup
    private var resetBeepsDone = 0
    private var pickupBeepsDone = 0
    private var mTimer: Timer? = null

    private var mContext: Context? = null
    private var mSensorManager: SensorManager? = null
    private var mProximitySensor: Sensor? = null
    private var mAccelerometer: Sensor? = null

    override fun onCreate() {
        instance = this
    }

    override fun onBind(p0: Intent?): IBinder? { return null }

    fun bind(context: Context, sensorManager: SensorManager, proximitySensor: Sensor, accelerometer: Sensor) {
        mContext = context
        mSensorManager = sensorManager
        mProximitySensor = proximitySensor
        mAccelerometer = accelerometer

        val pendingIntent: PendingIntent =
            Intent(this, RaiseToAnswerSensorEventListener::class.java).let { notificationIntent ->
                PendingIntent.getActivity(mContext, 0, notificationIntent, 0)
            }

        val channel = NotificationChannel("incoming_call", getString(R.string.incoming_call_service), NotificationManager.IMPORTANCE_LOW)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(mContext, "incoming_call")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(getText(R.string.raise_to_answer_is_enabled))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun disable() {
        stopForeground(true)
    }

    fun waitUntilEarPickup(callback: () -> Unit) {
        mSensorManager!!.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    var proximityValue = mProximityValue
                    var inclinationValue = mInclinationValue

                    if (resetBeepsDone < 2) {
                        if (proximityValue == null || (proximityValue >= SENSOR_SENSITIVITY || proximityValue <= -SENSOR_SENSITIVITY)) {
                            mToneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                            resetBeepsDone += 1
                        } else {
                            resetBeepsDone = 0
                        }

                        return
                    }

                    if (inclinationValue != null && inclinationValue <= 60 && inclinationValue >= 0 &&
                        proximityValue != null && proximityValue >= -SENSOR_SENSITIVITY && proximityValue <= SENSOR_SENSITIVITY) {
                        mToneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                        pickupBeepsDone += 1
                        if (pickupBeepsDone == 3) {
                            callback.invoke()
                            stop()
                        }
                    } else {
                        pickupBeepsDone = 0
                    }
                }
            }, 400, 400
        )
    }

    fun stop() {
        try {
            mTimer?.cancel()
        } catch (_: IllegalStateException) {}
        mSensorManager!!.unregisterListener(this)
        resetBeepsDone = 0
        pickupBeepsDone = 0
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            mProximityValue = event.values[0]
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // https://stackoverflow.com/a/15149421
            val normOfG = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);

            // Normalize the accelerometer vector
            event.values[0] = event.values[0] / normOfG
            event.values[1] = event.values[1] / normOfG
            event.values[2] = event.values[2] / normOfG

            mInclinationValue = Math.toDegrees(
                atan2(
                    event.values[0],
                    event.values[1]
                ).toDouble()
            ).roundToInt()
        }
    }
}

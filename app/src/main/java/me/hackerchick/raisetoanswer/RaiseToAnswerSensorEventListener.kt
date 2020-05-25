package me.hackerchick.raisetoanswer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import java.util.*
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RaiseToAnswerSensorEventListener(sensorManager: SensorManager, proximitySensor: Sensor, accelerometer: Sensor) : SensorEventListener {
    private val SENSOR_SENSITIVITY = 4
    private var mProximityValue: Float? = null
    private var mInclinationValue: Int? = null

    private val mToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    // First 2 beeps: Good start state found (proximity not near)
    // 3 more beeps: Pickup
    private var resetBeepsDone = 0
    private var pickupBeepsDone = 0
    private var mTimer: Timer? = null

    private var mSensorManager: SensorManager? = null
    private var mProximitySensor: Sensor? = null
    private var mAccelerometer: Sensor? = null

    init {
        mSensorManager = sensorManager
        mProximitySensor = proximitySensor
        mAccelerometer = accelerometer
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
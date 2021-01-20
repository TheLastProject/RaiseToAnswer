package me.hackerchick.raisetoanswer

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.telecom.TelecomManager
import android.widget.Toast
import java.util.*
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt


class RaiseToAnswerSensorEventListener : Service(), SensorEventListener {
    private var testMode = false

    private var featureAnswerEnabled = false
    private var featureAnswerAllAnglesEnabled = false
    private var featureDeclineEnabled = false
    private var behaviourBeepEnabled = false
    private var behaviourVibrateEnabled = false

    private var proximitySensorRange = 0f
    private var proximitySensorThreshold = 0f

    private val ONGOING_NOTIFICATION_ID = 1

    private var mAccelerometerValues: FloatArray = FloatArray(3)
    private var mMagnetometerValues: FloatArray = FloatArray(3)
    private var mProximityValue: Float? = null
    private var mInclinationValue: Int? = null

    private var mToneGenerator: ToneGenerator? = null
    private var mVibrator: Vibrator? = null

    // First 2 beeps: Good start state found (proximity not near)
    // 3 more beeps: Pickup / Decline
    private var resetBeepsDone = 0
    private var answerBeepsDone = 0
    private var declineBeepsDone = 0
    private var mTimer: Timer? = null

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    override fun onDestroy() {
        try {
            mTimer?.cancel()
        } catch (_: IllegalStateException) {}
        sensorManager?.unregisterListener(this)
        resetBeepsDone = 0
        answerBeepsDone = 0
        declineBeepsDone = 0

        mToneGenerator?.release()
        mToneGenerator = null

        stopForeground(true)

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? { return null }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        testMode = intent!!.extras!!.getBoolean("testMode")

        featureAnswerEnabled = intent.extras!!.getBoolean(this.getString(R.string.raise_enabled_key))
        featureAnswerAllAnglesEnabled = intent.extras!!.getBoolean(this.getString(R.string.answer_all_angles_enabled_key))
        featureDeclineEnabled = intent.extras!!.getBoolean(this.getString(R.string.flip_over_enabled_key))
        behaviourBeepEnabled = intent.extras!!.getBoolean(this.getString(R.string.beep_behaviour_enabled_key))
        behaviourVibrateEnabled = intent.extras!!.getBoolean(this.getString(R.string.vibrate_behaviour_enabled_key))


        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val channel = NotificationChannel("incoming_call", getString(R.string.incoming_call_service), NotificationManager.IMPORTANCE_LOW)
        val service = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, "incoming_call")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(getText(R.string.raise_to_answer_is_listening_to_sensor_data))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        sensorManager = Util.getSensorManager(this)
        val (proximitySensor, accelerometer, magnetometer) = Util.getSensors(sensorManager!!)
        this.sensorManager = sensorManager
        this.proximitySensor = proximitySensor
        this.accelerometer = accelerometer
        this.magnetometer = magnetometer

        mToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        proximitySensorRange = proximitySensor!!.maximumRange
        proximitySensorThreshold = proximitySensorRange / 2
        if (testMode) {
            Util.log("PROXIMITY SENSOR RANGE DETECTED AS ${proximitySensorRange}")
            Util.log("SETTING PROXIMITY SENSOR THRESHOLD TO ${proximitySensorThreshold}")
        }

        waitUntilDesiredState(magnetometer != null)

        return START_NOT_STICKY
    }

    private fun waitUntilDesiredState(hasMagnetoMeter: Boolean) {
        val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        sensorManager!!.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        if (hasMagnetoMeter) {
            sensorManager!!.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(
            object : TimerTask() {
                @SuppressLint("MissingPermission", "NewApi")
                override fun run() {
                    if (testMode) {
                        Util.log("TIMER RUN START")
                    }

                    var proximityValue = mProximityValue
                    var inclinationValue = mInclinationValue

                    var orientation = FloatArray(3)

                    if (hasMagnetoMeter && !featureAnswerAllAnglesEnabled) {
                        if (mAccelerometerValues.isNotEmpty() && mMagnetometerValues.isNotEmpty()) {
                            var rotationMatrix = FloatArray(9)
                            if (!SensorManager.getRotationMatrix(
                                    rotationMatrix,
                                    null,
                                    mAccelerometerValues,
                                    mMagnetometerValues
                                )
                            ) {
                                return
                            }
                            SensorManager.getOrientation(rotationMatrix, orientation)
                        }

                        if (orientation.isEmpty()) {
                            return
                        }
                    }

                    if (resetBeepsDone < 2) {
                        if (proximityValue == null || (proximityValue >= proximitySensorThreshold)) {
                            if (behaviourBeepEnabled) {
                                mToneGenerator!!.startTone(ToneGenerator.TONE_CDMA_ANSWER, 100)
                            }
                            if (behaviourVibrateEnabled) {
                                mVibrator!!.vibrate(200)
                            }
                            resetBeepsDone += 1
                        } else {
                            resetBeepsDone = 0
                        }

                        if (testMode) {
                            Util.log("RESET BEEP $resetBeepsDone")
                        }

                        return
                    }

                    if (hasMagnetoMeter && !featureAnswerAllAnglesEnabled) {
                        var hasRegistered = false

                        //var azimuth = Math.toDegrees(orientation[0].toDouble()) + 180.0
                        var pitch = Math.toDegrees(orientation[1].toDouble()) + 180.0
                        var roll = Math.toDegrees(orientation[2].toDouble()) + 180.0

                        if (featureAnswerEnabled) {
                            if (inclinationValue != null
                                && inclinationValue in -90..90
                                && proximityValue != null
                                && proximityValue <= proximitySensorThreshold
                                && roll in 45.0..315.0
                            ) {
                                if (behaviourBeepEnabled) {
                                    mToneGenerator!!.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                                }
                                if (behaviourVibrateEnabled) {
                                    mVibrator!!.vibrate(100)
                                }

                                hasRegistered = true
                                answerBeepsDone += 1
                                if (answerBeepsDone == 3) {
                                    pickUpDetected(tm)
                                }
                            } else {
                                answerBeepsDone = 0
                            }

                            if (testMode) {
                                Util.log("ANSWER BEEP $answerBeepsDone")
                            }
                        }

                        if (featureDeclineEnabled && !hasRegistered) {
                            if (pitch in 150.0..210.0
                                && (roll >= 315.0 || roll <= 45.0)
                                && proximityValue == 0.0f
                            ) {
                                if (behaviourBeepEnabled) {
                                    mToneGenerator!!.startTone(ToneGenerator.TONE_PROP_NACK, 100)
                                }
                                if (behaviourVibrateEnabled) {
                                    mVibrator!!.vibrate(500)
                                }

                                declineBeepsDone += 1
                                if (declineBeepsDone == 3) {
                                    declineDetected(tm)
                                }
                            } else {
                                declineBeepsDone = 0
                            }

                            if (testMode) {
                                Util.log("DECLINE BEEP $declineBeepsDone")
                            }
                        }
                    } else {
                        // Use a simpler algorithm if we have no magnetometer
                        // -90 to 0 = Right ear, 0 to 90 = Left ear
                        if (inclinationValue != null && inclinationValue in -90..90
                            && proximityValue != null && proximityValue <= proximitySensorThreshold) {
                            mToneGenerator!!.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                            answerBeepsDone += 1

                            if (answerBeepsDone == 3) {
                                pickUpDetected(tm)
                            }
                        } else {
                            answerBeepsDone = 0
                        }

                        if (testMode) {
                            Util.log("ANSWER BEEP $answerBeepsDone")
                        }
                    }
                }
            }, 400, 400
        )
    }

    @SuppressLint("MissingPermission")
    private fun pickUpDetected(tm: TelecomManager) {
        if (!testMode) {
            tm.acceptRingingCall()
        } else {
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.detected_raise_to_answer),
                    Toast.LENGTH_SHORT
                ).show()
            })

            Util.log("PICKUP DETECTED")
        }
        stopSelf()
    }

    @SuppressLint("NewApi")
    private fun declineDetected(tm: TelecomManager) {
        if (!testMode) {
            tm.endCall()
        } else {
            val handler = Handler(Looper.getMainLooper())
            handler.post(Runnable {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.detected_flip_over),
                    Toast.LENGTH_SHORT
                ).show()
            })

            Util.log("DECLINE DETECTED")
        }
        stopSelf()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            mProximityValue = event.values[0]

            if (testMode) {
                Util.log("PROXIMITY $mProximityValue")
            }
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values

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

            if (testMode) {
                Util.log("INCLINATION $mInclinationValue")
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagnetometerValues = event.values

            if (testMode) {
                Util.log("MAGNETOMETER ${mMagnetometerValues[0]} ${mMagnetometerValues[1]} ${mMagnetometerValues[2]}")
            }
        }
    }
}

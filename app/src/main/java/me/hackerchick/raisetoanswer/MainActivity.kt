package me.hackerchick.raisetoanswer

import android.Manifest
import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer


class MainActivity : AppCompatActivity() {
    private var PERMISSION_REQUEST_READ_PHONE_STATE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Util.hasRequiredSensors(applicationContext)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.could_not_bind_sensor),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            val android9Warning: TextView = findViewById(R.id.missing_support_android_9)
            android9Warning.visibility = View.GONE
        }

        if (Util.hasMagnetometer(applicationContext)) {
            val magnetometerWarning: TextView = findViewById(R.id.missing_support_magnetometer)
            magnetometerWarning.visibility = View.GONE
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS
            ), PERMISSION_REQUEST_READ_PHONE_STATE
        )

        val answerFeature: CheckedTextView = findViewById(R.id.feature_answer)
        val answerAllAnglesFeature: CheckedTextView = findViewById(R.id.feature_answer_all_angles)
        val declineFeature: CheckedTextView = findViewById(R.id.feature_decline)
        val beepBehaviour: CheckedTextView = findViewById(R.id.behaviour_beep)

        answerFeature.setOnClickListener { _->
            setAnswerFeature(!answerFeature.isChecked, true)
        }

        answerAllAnglesFeature.setOnClickListener {
            setAnswerAllAnglesFeatureIfSupported(!answerAllAnglesFeature.isChecked)
        }

        declineFeature.setOnClickListener { _->
            setDeclineFeatureIfSupported(!declineFeature.isChecked)
        }

        beepBehaviour.setOnClickListener {
            setBeepBehaviour(!beepBehaviour.isChecked)
        }

        setAnswerFeature(Util.answerFeatureEnabled(applicationContext), false)
        setAnswerAllAnglesFeatureIfSupported(Util.answerAllAnglesFeatureEnabled(applicationContext))
        setDeclineFeatureIfSupported(Util.declineFeatureEnabled(applicationContext))

        setBeepBehaviour(Util.beepBehaviourEnabled(applicationContext))

        // Debugging
        val debugLog: TextView = findViewById(R.id.debug_log)
        debugLog.setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("RaiseToAnswer Debug Log", Util.getLog().value!!.joinToString(separator = "\n"))
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.debug_log_copied_to_clipboard), Toast.LENGTH_LONG).show()
        }

        val testButton: Button = findViewById(R.id.test_button)
        testButton.setOnClickListener {
            if (!Util.startSensorListener(applicationContext, true)) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.enable_at_least_one_feature),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(applicationContext, getString(R.string.test_started), Toast.LENGTH_SHORT).show()
            Util.clearLog()
            Util.log("TEST STARTED")
        }

        val header: TextView = findViewById(R.id.raise_to_answer_header)
        var debugCounter = 0
        header.setOnClickListener {
            debugCounter += 1

            if (debugCounter == 7) {
                Toast.makeText(this, getString(R.string.debug_mode_activated), Toast.LENGTH_LONG)
                    .show()
                testButton.visibility = View.VISIBLE
                debugLog.visibility = View.VISIBLE

                Util.getLog().observe(this, Observer {
                    try {
                        debugLog.text = it.reversed().joinToString(separator = "\n")
                    } catch (ConcurrentModificationException: Exception) {
                        // We don't care, just skip this update then...
                    }
                })
            }

            return@setOnClickListener
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.permissions_needed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    finish()
                }
                return
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.privacy_policy -> {
                val browserIntent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://thelastproject.github.io/RaiseToAnswer/PRIVACY_POLICY")
                    )
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setAnswerFeature(value: Boolean, propagate: Boolean) {
        val answerFeature: CheckedTextView = findViewById(R.id.feature_answer)
        answerFeature.isChecked = value

        Util.setAnswerFeatureEnabled(applicationContext, value)

        if (propagate) {
            setAnswerAllAnglesFeatureIfSupported(false)
        }
    }

    private fun setAnswerAllAnglesFeatureIfSupported(value: Boolean) {
        val answerAllAnglesFeature: CheckedTextView = findViewById(R.id.feature_answer_all_angles)

        if (!Util.hasMagnetometer(applicationContext)) {
            answerAllAnglesFeature.isEnabled = false
            answerAllAnglesFeature.isChecked = true
            Util.setAnswerAllAnglesFeatureEnabled(applicationContext, true)

            return
        }

        answerAllAnglesFeature.isEnabled = true
        answerAllAnglesFeature.isChecked = value

        // Exclusive with decline
        if (value) {
            setDeclineFeatureIfSupported(false)
        }

        // Makes no sense to have this turned on if answering is turned off
        if (!Util.answerFeatureEnabled(applicationContext)) {
            answerAllAnglesFeature.isEnabled = false
        }

        Util.setAnswerAllAnglesFeatureEnabled(applicationContext, value)
    }

    private fun setDeclineFeatureIfSupported(value: Boolean) {
        val declineFeature: CheckedTextView = findViewById(R.id.feature_decline)

        if (android.os.Build.VERSION.SDK_INT < 28 || !Util.hasMagnetometer(applicationContext)) {
            declineFeature.isEnabled = false
            declineFeature.isChecked = false
            Util.setDeclineFeatureEnabled(applicationContext, false)

            return
        }

        declineFeature.isEnabled = true
        declineFeature.isChecked = value

        // Exclusive with answer all angles
        if (value) {
            setAnswerAllAnglesFeatureIfSupported(false)
        }

        Util.setDeclineFeatureEnabled(applicationContext, value)
    }

    private fun setBeepBehaviour(value: Boolean) {
        val beepBehaviour: CheckedTextView = findViewById(R.id.behaviour_beep)
        beepBehaviour.isChecked = value

        Util.setBeepBehaviourEnabled(applicationContext, value)
    }
}

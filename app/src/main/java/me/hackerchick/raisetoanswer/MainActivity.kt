package me.hackerchick.raisetoanswer

import android.Manifest
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

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS), PERMISSION_REQUEST_READ_PHONE_STATE)

        val testButton: Button = findViewById(R.id.test_button)
        testButton.setOnClickListener {
            if (!Util.startSensorListener(applicationContext, true)) {
                Toast.makeText(applicationContext, getString(R.string.enable_at_least_one_feature), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(applicationContext, getString(R.string.test_started), Toast.LENGTH_SHORT).show()
        }

        val answerFeature: CheckedTextView = findViewById(R.id.feature_answer)
        val answerAllAnglesFeature: CheckedTextView = findViewById(R.id.feature_answer_all_angles)
        val declineFeature: CheckedTextView = findViewById(R.id.feature_decline)
        val beepBehaviour: CheckedTextView = findViewById(R.id.behaviour_beep)

        answerFeature.setOnClickListener { _->
            setAnswerFeature(!answerFeature.isChecked)
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

        setAnswerFeature(Util.answerFeatureEnabled(applicationContext))
        setAnswerAllAnglesFeatureIfSupported(Util.answerAllAnglesFeatureEnabled(applicationContext))
        setDeclineFeatureIfSupported(Util.declineFeatureEnabled(applicationContext))

        setBeepBehaviour(Util.beepBehaviourEnabled(applicationContext))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(applicationContext, getString(R.string.permissions_needed), Toast.LENGTH_SHORT)
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
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://thelastproject.github.io/RaiseToAnswer/PRIVACY_POLICY"))
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setAnswerFeature(value: Boolean) {
        val answerFeature: CheckedTextView = findViewById(R.id.feature_answer)
        answerFeature.isChecked = value

        Util.setAnswerFeatureEnabled(applicationContext, value)
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

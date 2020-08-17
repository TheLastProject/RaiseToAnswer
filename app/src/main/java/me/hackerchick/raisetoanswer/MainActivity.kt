package me.hackerchick.raisetoanswer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckedTextView
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

        if (!Util.hasMagnetometer(applicationContext)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.could_not_bind_magnetometer),
                Toast.LENGTH_LONG
            ).show()
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

        val raiseFeature: CheckedTextView = findViewById(R.id.feature_raise_to_answer)
        val flipOverFeature: CheckedTextView = findViewById(R.id.feature_flip_over_to_decline)
        val beepBehaviour: CheckedTextView = findViewById(R.id.behaviour_beep)

        raiseFeature.setOnClickListener { _->
            setRaiseFeature(!raiseFeature.isChecked)
        }

        flipOverFeature.setOnClickListener { _->
            setFlipOverFeature(!flipOverFeature.isChecked)
        }

        beepBehaviour.setOnClickListener {
            setBeepBehaviour(!beepBehaviour.isChecked)
        }

        setRaiseFeature(Util.raiseFeatureEnabled(applicationContext))

        if (android.os.Build.VERSION.SDK_INT >= 28 && Util.hasMagnetometer(applicationContext)) {
            setFlipOverFeature(Util.flipOverFeatureEnabled(applicationContext))
        } else {
            setFlipOverFeature(false)
            flipOverFeature.isEnabled = false
        }
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

    private fun setRaiseFeature(value: Boolean) {
        val raiseFeature: CheckedTextView = findViewById(R.id.feature_raise_to_answer)
        raiseFeature.isChecked = value

        Util.setRaiseFeatureEnabled(applicationContext, value)
    }

    private fun setFlipOverFeature(value: Boolean) {
        val flipOverFeature: CheckedTextView = findViewById(R.id.feature_flip_over_to_decline)
        flipOverFeature.isChecked = value

        Util.setFlipOverFeatureEnabled(applicationContext, value)
    }

    private fun setBeepBehaviour(value: Boolean) {
        val beepBehaviour: CheckedTextView = findViewById(R.id.behaviour_beep)
        beepBehaviour.isChecked = value

        Util.setBeepBehaviourEnabled(applicationContext, value)
    }
}

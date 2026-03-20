package com.example.mfaapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getMainExecutor

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_LOCATION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start biometric authentication first
        authenticateWithBiometrics()
    }

    private fun authenticateWithBiometrics() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = getMainExecutor(this)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Fingerprint Authentication")
                    .setSubtitle("Scan your fingerprint to continue")
                    .setNegativeButtonText("Cancel")
                    .build()

                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            Toast.makeText(
                                this@MainActivity,
                                "Authentication successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            // After fingerprint success, continue with Wi-Fi setup
                            checkLocationPermission()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(
                                this@MainActivity,
                                "Authentication error: $errString",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(
                                this@MainActivity,
                                "Authentication failed. Try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "No biometric hardware detected", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No fingerprints enrolled", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkLocationPermission() {
        if (LOCATION_PERMS.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, LOCATION_PERMS, REQUEST_LOCATION)
        } else {
            suggestWifi("YourSSID", "YourPassword")
        }
    }

    private fun suggestWifi(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsAppInteractionRequired(true)
                .build()

            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(this, "Wi-Fi suggestion added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Wi-Fi suggestion failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Needs Android 10+", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            suggestWifi("YourSSID", "YourPassword")
        }
    }
}

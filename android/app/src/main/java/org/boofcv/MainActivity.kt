package org.boofcv

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestCameraPermission()
    }

    fun clickedGradient(view: View?) {
        val intent = Intent(this, GradientActivity::class.java)
        startActivity(intent)
    }

    fun clickedQrCode(view: View?) {
        val intent = Intent(this, QrCodeActivity::class.java)
        startActivity(intent)
    }

    /**
     * Newer versions of Android require explicit permission from the user
     */
    private fun requestCameraPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),0)
            // a dialog should open and this dialog will resume when a decision has been made
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dialogNoCameraPermission()
                }
            }
        }
    }

    private fun dialogNoCameraPermission() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Denied access to the camera! Exiting.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id -> System.exit(0) }
        val alert = builder.create()
        alert.show()
    }
}
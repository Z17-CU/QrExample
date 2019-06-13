package com.example.qrmaster

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import com.richard.qrmaster.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

open class MainActivity : AppCompatActivity() {

    private val REQUIRED_SDK_PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val REQUEST_CODE_ASK_PERMISSIONS = 1234
    private var buttonRead: ImageButton? = null
    private var buttonGenerate: ImageButton? = null
    private var result: String? = null
    private var tittle: String? = null
    private var bitmap: Bitmap? = null
    private var text: String? = null
    private var context: Context? = null
    private val IMAGE_DIRECTORY = "/QR_Generated"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        context = this
        checkPermissions()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    result = intent.getStringExtra("result")
                    tittle = intent.getStringExtra("tittle")
                    if (result != null && tittle != null) {
                        showDialog()
                        result = null
                        tittle = null
                    }
                }
            }, IntentFilter("Result")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val byteArray = intent.getByteArrayExtra("bitmap")
                    text = intent.getStringExtra("text")
                    if (byteArray != null && text != null) {
                        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        showDialogGenerate()
                    } else {
                        Toast.makeText(context, "Null image", Toast.LENGTH_SHORT).show()
                    }
                }
            }, IntentFilter("Generate")
        )
    }

    fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(result)
        builder.setTitle(tittle)
        builder.setNegativeButton("Accept", null)
        builder.create().show()
    }

    fun showDialogGenerate() {
        val builder = AlertDialog.Builder(this)
        val imageView = ImageView(context)
        imageView.setImageBitmap(bitmap)
        builder.setView(imageView)
        builder.setNegativeButton("Exit", null)
        builder.setPositiveButton("Save") { _, _ ->
            saveImage(bitmap)
        }
        builder.create().show()
    }

    private fun start() {

        buttonRead = findViewById(R.id._read)
        buttonGenerate = findViewById(R.id._generate)

        buttonRead!!.setOnClickListener {
            val intent = Intent(this@MainActivity, ScanActivity::class.java)
            startActivity(intent)
        }

        buttonGenerate!!.setOnClickListener {
            val intent = Intent(this@MainActivity, GenerateActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val missingPermissions = ArrayList<String>()
        // check all required dynamic permissions
        for (permission in REQUIRED_SDK_PERMISSIONS) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions
                .toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS)
        } else {
            val grantResults = IntArray(REQUIRED_SDK_PERMISSIONS.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                grantResults
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                for (index in permissions.indices.reversed()) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(
                            this, "Required permission '" + permissions[index]
                                    + "' not granted, exiting", Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return
                    }
                }
                // all permissions were granted
                start()
            }
        }
    }

    private fun saveImage(myBitmap: Bitmap?) {
        val bytes = ByteArrayOutputStream()
        myBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = File(
            Environment.getExternalStorageDirectory().toString() + IMAGE_DIRECTORY
        )
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs())
            wallpaperDirectory.mkdirs()
        }

        try {
            if (text!!.length > 12) {
                text = text!!.substring(0, 12)
            }
            val f = File(
                wallpaperDirectory, "$text.jpg"
            )
            f.createNewFile()   //give read write permission
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(
                this,
                arrayOf(f.path),
                arrayOf("image/jpeg"), null
            )
            fo.close()
            Log.d("TAG", "File Saved::--->" + f.absolutePath)
            Toast.makeText(context, "Image saved in: " + f.absolutePath, Toast.LENGTH_LONG).show()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
    }
}

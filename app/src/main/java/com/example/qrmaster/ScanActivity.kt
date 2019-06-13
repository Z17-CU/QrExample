package com.example.qrmaster

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import com.example.qrmaster.PathResolve.getPath
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.richard.qrmaster.R
import kotlinx.android.synthetic.main.qr_reader.*
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private lateinit var mScannerView: ZXingScannerView
    private var readOk = false
    private var tittle: String = ""
    private var bitmap: Bitmap? = null
    private var uri: Uri? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.qr_reader)

        mScannerView = findViewById(R.id._zXingScannerView)
        turnFlash()
        _imagePicker.setOnClickListener { picturePicker() }
        _flash.setOnClickListener {
            mScannerView.flash = !mScannerView.flash
            turnFlash()
        }
    }

    private fun turnFlash() {
        if (mScannerView.flash) {
            _flash.setImageDrawable(this.resources.getDrawable(R.drawable.ic_flash_on))
        } else {
            _flash.setImageDrawable(this.resources.getDrawable(R.drawable.ic_flash_off))
        }
    }

    public override fun onResume() {
        super.onResume()
        mScannerView.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView.startCamera()          // Start camera on resume
    }

    public override fun onPause() {
        super.onPause()
        mScannerView.stopCamera()           // Stop camera on pause
    }

    override fun handleResult(rawResult: Result) {

        //play sound
        val mp = MediaPlayer.create(this, R.raw.beep)
        mp.start()
        //vibrate
        val vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibratorService.vibrate(100)
        //send text
        tittle = rawResult.barcodeFormat.name
        sendBroadCast(rawResult.text)
    }

    private fun sendBroadCast(text: String) {
        val intent = Intent("Result")
        intent.putExtra("result", text)
        intent.putExtra("tittle", tittle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        finish()
    }

    private fun readerFromPicture(resizeBitmap: Bitmap?) {

        val bMap: Bitmap? = resizeBitmap ?: bitmap
        val uri: Uri? = uri

        val progress = Progress(this@ScanActivity)
        var contents: String? = null
        @SuppressLint("StaticFieldLeak")
        val execute = object : AsyncTask<Void, Void, String?>() {
            override fun onPreExecute() {
                super.onPreExecute()
                progress.setMessage("Reading QR")
                progress.show()
            }

            override fun onPostExecute(contents: String?) {
                super.onPostExecute(contents)
                progress.dismiss()
                if (contents != null) {
                    sendBroadCast(contents)
                } else {
                    if (!readOk) {
                        val cropIntent = Intent("com.android.camera.action.CROP")
                        cropIntent.setDataAndType(uri, "image/*")
                        cropIntent.putExtra("return-data", true)
                        startActivityForResult(cropIntent, 8888)
                    } else {
                        showDialog()
                    }
                }
            }

            override fun doInBackground(vararg voids: Void): String? {

                val intArray = IntArray(bMap!!.width * bMap.height)
                bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)

                val source = RGBLuminanceSource(bMap.width, bMap.height, intArray)
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val reader: MultiFormatReader?
                reader = MultiFormatReader()
                try {
                    val result: Result? = reader.decode(bitmap)
                    contents = result!!.text
                    tittle = result.barcodeFormat.name
                    readOk = true
                } catch (e: ReaderException) {
                    e.printStackTrace()
                }
                return contents
            }
        }
        execute.execute()
    }

    private fun showDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Code not found")
        dialog.setMessage("Do you want to retry")
        dialog.setNegativeButton("Cancel", null)
        dialog.setPositiveButton("Retry") { _, _ ->
            readOk = false
            if (bitmap != null && uri != null) {
                readerFromPicture(null)
            }
        }
        dialog.show()
    }

    private fun picturePicker() {
        var chooseFile = Intent(Intent.ACTION_PICK)
        chooseFile.type = "image/*"
        chooseFile = Intent.createChooser(chooseFile, "Imagen")
        startActivityForResult(chooseFile, 9999)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            9999 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data.data
                    if (uri != null) {
                        readOk = false
                        bitmap = BitmapFactory.decodeFile(getPath(this@ScanActivity, uri))
                        this.uri = uri
                        readerFromPicture(null)
                    }
                }
            }
            8888 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val extras = data.extras
                    if (extras != null) {
                        val selectedBitmap = extras.getParcelable<Bitmap>("data")
                        if (selectedBitmap != null) {
                            readOk = true
                            readerFromPicture(selectedBitmap)
                        }
                    }
                }
            }
        }
    }
}

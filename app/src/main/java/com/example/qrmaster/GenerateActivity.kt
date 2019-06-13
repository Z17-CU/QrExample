package com.example.qrmaster

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.richard.qrmaster.R
import kotlinx.android.synthetic.main.activity_generate.*
import java.io.ByteArrayOutputStream

class GenerateActivity : AppCompatActivity() {
    private var bitmap: Bitmap? = null
    private var text: String? = null
    private var btn: ImageButton? = null
    private var isOpened = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        btn = findViewById(R.id._buttonGenerate)

        _editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                _editTextCounter.text = _editText.text.length.toString().plus("/2950")
            }
        })

        btn!!.setOnClickListener {
            if (_editText!!.text.toString().trim { it <= ' ' }.isEmpty()) {
                Toast.makeText(this@GenerateActivity, "Enter your text!", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val progress = Progress(this@GenerateActivity)
                    @SuppressLint("StaticFieldLeak")
                    val execute = object : AsyncTask<Void, Void, Bitmap?>() {
                        override fun onPreExecute() {
                            super.onPreExecute()
                            progress.setMessage("Generating QR")
                            progress.show()
                        }

                        override fun onPostExecute(aVoid: Bitmap?) {
                            super.onPostExecute(aVoid)
                            progress.dismiss()
                            sendBroadCast()
                        }

                        override fun doInBackground(vararg voids: Void): Bitmap? {
                            text = _editText!!.text.toString()
                            if (text != null) {
                                bitmap = TextToImageEncode(text!!)
                            }
                            return bitmap
                        }
                    }
                    execute.execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setListnerToRootView()
    }

    fun setListnerToRootView() {
        val activityRootView = window.decorView.findViewById<View>(android.R.id.content)
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener {
            val heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight()
            if (heightDiff > 50) { // 99% of the time the height diff will be due to a keyboard.
                if (!isOpened) {
                    //btn!!.setVisibility(ImageButton.GONE)
                    //Do two things, make the view top visible and the editText smaller
                }
                Toast.makeText(this, "abierto", Toast.LENGTH_SHORT).show()
                isOpened = true
            } else if (isOpened) {
                Toast.makeText(this, "abierto", Toast.LENGTH_SHORT).show()
                btn!!.setVisibility(ImageButton.VISIBLE)
                isOpened = false
            }
        })
    }

    private fun sendBroadCast() {
        if (bitmap != null) {
            val intent = Intent("Generate")
            val bStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bStream)
            val byteArray = bStream.toByteArray()
            intent.putExtra("bitmap", byteArray)
            intent.putExtra("text", text)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid text", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(WriterException::class)
    private fun TextToImageEncode(Value: String): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                Value,
                BarcodeFormat.QR_CODE,
                QRcodeWidth, QRcodeWidth, null
            )

        } catch (e: Exception) {
            return null
        }

        val bitMatrixWidth = bitMatrix.width

        val bitMatrixHeight = bitMatrix.height

        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)

        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth

            for (x in 0 until bitMatrixWidth) {

                pixels[offset + x] = if (bitMatrix.get(x, y))
                    resources.getColor(R.color.black)
                else
                    resources.getColor(R.color.white)
            }
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444)

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap
    }

    companion object {
        const val QRcodeWidth = 500
    }

}

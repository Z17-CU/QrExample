package com.example.qrmaster

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.richard.qrmaster.R

class Progress @SuppressLint("InflateParams")
constructor(context: Context) {

    private val view: View
    private var msg: TextView? = null
    private var progressBar: ProgressBar? = null
    private var ll: LinearLayout? = null
    private var builder: AlertDialog.Builder? = null
    private var dialog: Dialog? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.progress, null)
        init()
    }

    private fun init() {
        msg = view.findViewById(R.id.msg)
        progressBar = view.findViewById(R.id.loader)
        ll = view.findViewById(R.id.ll)
        builder = AlertDialog.Builder(view.context)
    }

    fun setBackgroundColor() {
        ll!!.setBackgroundColor(android.graphics.Color.DKGRAY)
    }

    fun setProgressColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progressBar!!.indeterminateTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
        }
    }

    fun setMessage(message: String): Progress {
        msg!!.text = message
        return this
    }

    fun setMessageColor(color: Int) {
        msg!!.setTextColor(color)
    }

    fun show() {
        builder!!.setView(view)
        dialog = builder!!.create()
        dialog!!.setCancelable(false)
        dialog!!.show()
    }

    fun dismiss() {
        try {
            dialog!!.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.pollub.samoloty.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import com.vuforia.artest.R

//DONE

class CustomDialogBuilder (context: Context, themeResId: Int = R.style.CustomDialogTheme) : AlertDialog.Builder(context, themeResId) {

    override fun create(): AlertDialog {

        val dialog = super.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnKeyListener { d, keyCode, event ->
            return@setOnKeyListener keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
        }

        return dialog
    }
}
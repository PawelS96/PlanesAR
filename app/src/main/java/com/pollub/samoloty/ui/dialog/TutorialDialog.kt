package com.pollub.samoloty.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_tutorial.view.*

class TutorialDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = CustomDialogBuilder(context!!)
        val root = View.inflate(context!!, R.layout.dialog_tutorial, null)
        root.button_ok.setOnClickListener { dismiss() }
        return builder.setView(root).create()
    }

    companion object {
        val TAG : String = TutorialDialog::class.java.simpleName
    }
}
package com.pollub.samoloty.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.pawels96.autoplayer.ui.dialogs.CustomDialogBuilder
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_victory.view.*
import kotlin.system.exitProcess

class VictoryDialog : DialogFragment() {

    private lateinit var callback: VictoryDialogCallback

    interface VictoryDialogCallback {
        fun onPlayAgain()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is VictoryDialogCallback)
            callback = context
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = CustomDialogBuilder(context!!)

        val root = View.inflate(context!!, R.layout.dialog_victory, null)
        root.button_exit.setOnClickListener { exitProcess(0) }
        root.button_play_again.setOnClickListener {
            callback.onPlayAgain()
            dismiss()
        }

        return builder.setView(root).create()
    }

    companion object {
        val TAG: String = VictoryDialog::class.java.simpleName
    }
}
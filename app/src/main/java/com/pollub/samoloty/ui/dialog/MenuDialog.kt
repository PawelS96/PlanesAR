package com.pollub.samoloty.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.pollub.samoloty.GameMode
import com.pollub.samoloty.SortMode
import com.pollub.samoloty.database.Plane
import com.pollub.samoloty.database.Repository
import com.pollub.samoloty.getFieldForSorting
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_menu.view.*
import kotlinx.android.synthetic.main.dialog_victory.view.*
import kotlinx.android.synthetic.main.dialog_victory.view.button_exit
import kotlinx.android.synthetic.main.list_item_plane.view.*
import kotlin.system.exitProcess

class MenuDialog private constructor() : DialogFragment() {

    private lateinit var callback: GameplayDialogCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is GameplayDialogCallback)
            callback = context
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = CustomDialogBuilder(context!!)

        val root = View.inflate(context!!, R.layout.dialog_menu, null)
        root.button_resume.setOnClickListener { dismiss() }
        root.button_exit.setOnClickListener {
            dismiss()
            callback.onExit()
        }

        if (arguments?.getSerializable("mode") as GameMode == GameMode.MODE_FREE) {

            root.button_change_sorting.setOnClickListener {
                callback.onPlayAgain()
                dismiss()
            }
        }

        else root.button_change_sorting.visibility = View.GONE

        return builder.setView(root).create()
    }

    companion object {
        val TAG: String = MenuDialog::class.java.simpleName

        fun create(gameMode: GameMode) : MenuDialog {
            return MenuDialog().apply { arguments = Bundle().apply { putSerializable("mode", gameMode) } }
        }
    }
}
package com.pollub.samoloty.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.pawels96.autoplayer.ui.dialogs.CustomDialogBuilder
import com.pollub.samoloty.GameManager
import com.pollub.samoloty.GameMode
import com.pollub.samoloty.displayName
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_game_modes.view.*
import kotlinx.android.synthetic.main.dialog_victory.view.*
import kotlinx.android.synthetic.main.list_item_mode.view.*
import kotlinx.coroutines.flow.callbackFlow
import kotlin.system.exitProcess

class GameModeDialog : DialogFragment() {

    private lateinit var callback: GameModeDialogCallback

    interface GameModeDialogCallback {
        fun onModeSelected(mode: GameMode)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is GameModeDialogCallback)
            callback = context
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = CustomDialogBuilder(context!!)

        val root = View.inflate(context!!, R.layout.dialog_game_modes, null)
        root.recycler.adapter = GameModeAdapter(object : GameModeAdapter.GameModeAdapterCallback {

            override fun onClick(mode: GameMode) {
                callback.onModeSelected(mode)
                dismiss()
            }
        })

         return builder.setView(root).create()
    }

    companion object {
        val TAG: String = GameModeDialog::class.java.simpleName
    }

    private class Holder(val root: View) : RecyclerView.ViewHolder(root) {
        var title: TextView = root.name

        fun bindData(mode: GameMode) {
            title.text = mode.displayName()
        }
    }

    private class GameModeAdapter(private val callback : GameModeAdapterCallback)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        interface GameModeAdapterCallback {
            fun onClick(mode: GameMode)
        }

        override fun getItemViewType(position: Int) = R.layout.list_item_mode

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return Holder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        }

        override fun getItemCount(): Int = GameMode.values().size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val mode = GameMode.values()[position]

            (holder as Holder).apply {
                bindData(mode)
                root.setOnClickListener { callback.onClick(mode) }
            }
        }

    }
}

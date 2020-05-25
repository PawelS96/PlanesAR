package com.pollub.samoloty.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.pollub.samoloty.SortMode
import com.pollub.samoloty.displayName
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_game_modes.view.*
import kotlinx.android.synthetic.main.list_item_mode.view.*

class GameModeDialog : DialogFragment() {

    private lateinit var callback: GameModeDialogCallback

    interface GameModeDialogCallback {
        fun onSortModeSelected(mode: SortMode)
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

            override fun onClick(mode: SortMode) {
                callback.onSortModeSelected(mode)
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

        fun bindData(mode: SortMode) {
            title.text = mode.displayName()
        }
    }

    private class GameModeAdapter(private val callback : GameModeAdapterCallback)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        interface GameModeAdapterCallback {
            fun onClick(mode: SortMode)
        }

        override fun getItemViewType(position: Int) = R.layout.list_item_mode

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return Holder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        }

        override fun getItemCount(): Int = SortMode.values().size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val mode = SortMode.values()[position]

            (holder as Holder).apply {
                bindData(mode)
                root.setOnClickListener { callback.onClick(mode) }
            }
        }
    }
}

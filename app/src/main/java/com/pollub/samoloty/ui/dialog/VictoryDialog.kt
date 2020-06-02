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
import com.pollub.samoloty.GameManager
import com.pollub.samoloty.SortMode
import com.pollub.samoloty.database.Plane
import com.pollub.samoloty.database.Repository
import com.pollub.samoloty.getFieldForSorting
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.dialog_victory.view.*
import kotlinx.android.synthetic.main.list_item_plane.view.*
import kotlin.system.exitProcess

interface GameplayDialogCallback {
    fun onPlayAgain()
    fun onExit()
}

class VictoryDialog : DialogFragment() {

    private lateinit var callback: GameplayDialogCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is GameplayDialogCallback)
            callback = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    setStyle(STYLE_NO_TITLE, R.style.DialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = CustomDialogBuilder(context!!)

        val root = View.inflate(context!!, R.layout.dialog_victory, null)
        root.button_exit.setOnClickListener { callback.onExit() }
        root.button_play_again.setOnClickListener {
            dismiss()
            callback.onPlayAgain()
        }
        root.recycler.adapter = PlaneAdapter(GameManager.getCorrectOrder(), GameManager.sortMode)

        return builder.setView(root).create()
    }

    companion object {
        val TAG: String = VictoryDialog::class.java.simpleName
    }
}

 class PlaneHolder(root: View) : RecyclerView.ViewHolder(root) {
    private val img : ImageView = root.target
     private val name: TextView = root.plane_name
     private val info : TextView = root.plane_info

    fun bindData(plane: Plane, mode: SortMode) {

        val context: Context = img.getContext()
        val resName = plane.targetName.replace("-", "_").replace(" ", "_").toLowerCase()
        val id = context.resources.getIdentifier(resName, "drawable", context.packageName)
        img.setImageResource(id)

        name.text = plane.fullName

        when (val planeInfo: Any = plane.getFieldForSorting(mode)){
            is String -> info.text = planeInfo
            else -> info.text = planeInfo.toString()
        }
    }
}

 class PlaneAdapter(val planes: List<Plane>, val mode: SortMode) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int) = R.layout.list_item_plane

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlaneHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
    }

    override fun getItemCount(): Int = planes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val plane = planes[position]

        (holder as PlaneHolder).apply {
            bindData(plane, mode)
        }
    }

}
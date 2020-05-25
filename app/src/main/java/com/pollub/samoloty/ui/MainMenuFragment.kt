package com.pollub.samoloty.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pollub.samoloty.GameMode
import com.pollub.samoloty.ui.dialog.TutorialDialog
import com.pollub.samoloty.ui.dialog.VictoryDialog
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.main_menu.*
import kotlin.system.exitProcess

class MainMenuFragment : Fragment() {

    private lateinit var callback : MainMenuCallback

    interface MainMenuCallback {
        fun onGameModeSelected(gameMode: GameMode)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as MainMenuCallback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_mode1.setOnClickListener { callback.onGameModeSelected(GameMode.MODE_FREE) }
        button_mode2.setOnClickListener { callback.onGameModeSelected(GameMode.MODE_LEVELS) }
        button_exit_main.setOnClickListener { exitProcess(0) }
        button_tutorial.setOnClickListener { TutorialDialog().show(childFragmentManager, TutorialDialog.TAG) }
    }

    companion object {
        val TAG: String = MainMenuFragment::class.java.simpleName
    }
}
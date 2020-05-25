package com.pollub.samoloty.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.pollub.samoloty.*
import com.pollub.samoloty.render.ModelRenderer
import com.pollub.samoloty.ui.dialog.GameModeDialog
import com.pollub.samoloty.ui.dialog.GameplayDialogCallback
import com.pollub.samoloty.ui.dialog.MenuDialog
import com.pollub.samoloty.ui.dialog.VictoryDialog
import com.pollub.samoloty.ui.sidemenu.SampleAppMenuInterface
import com.pollub.samoloty.ui.sidemenu.SideMenu
import com.pollub.samoloty.ui.sidemenu.SideMenuGroup
import com.pollub.samoloty.utils.Timer
import com.vuforia.*
import com.vuforia.artest.R
import kotlinx.android.synthetic.main.bottom_bar.view.*
import kotlinx.android.synthetic.main.camera_overlay.*
import kotlinx.android.synthetic.main.dialog_seekbars.view.*
import kotlinx.android.synthetic.main.loading_screen.*
import kotlinx.android.synthetic.main.main_layout.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.system.exitProcess

class CameraActivity : AppCompatActivity(), Control,
        SampleAppMenuInterface,
        GameplayDialogCallback,
        GameModeDialog.GameModeDialogCallback,
        MainMenuFragment.MainMenuCallback {

    private val testing = false
/*
    fun createDialog(): Dialog {

        val builder = AlertDialog.Builder(this)
        val root = View.inflate(this, R.layout.dialog_seekbars, null)
        val listener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                when (seekBar) {
                    root.barX -> mRenderer!!.x = progress.toFloat()
                    root.barY -> mRenderer!!.y = progress.toFloat()
                    root.barZ -> mRenderer!!.z = progress.toFloat()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }

        with(root) {

            barX.progress = mRenderer!!.x.toInt()
            barY.progress = mRenderer!!.y.toInt()
            barZ.progress = mRenderer!!.z.toInt()

            listOf<SeekBar>(barX, barY, barZ).forEach { it.setOnSeekBarChangeListener(listener) }
        }

        return builder.setView(root).create().apply { window?.setGravity(Gravity.BOTTOM) }
    }
*/
    private lateinit var viewModel: CameraActivityViewModel

    //game state
    private val gameStateManager = GameManager
    private val checkOrderHandler = Handler()
    private var uiState: UiState = UiState.STATE_NONE

    //vuforia
    private var vuforiaAppSession: ArSession? = null
    private var mCurrentDataset: DataSet? = null
    private var mRelocalizationTimer: Timer? = null
    private var mStatusDelayTimer: Timer? = null
    private var mCurrentStatusInfo: Int = 0
    private var shouldStartAR = true

    //loading UI
    private var loadingLayout: ViewGroup? = null

    // side menu
    private var sideMenu: SideMenu? = null
    private var mFocusOptionView: View? = null
    private var mGridOptionView: View? = null
    private var mHintOptionView: View? = null
    private var mFlashOptionView: View? = null
    private var mSettingsAdditionalViews = ArrayList<View>()

    private var isAutoFocusEnabled = true
    private var isHintEnabled = true
    private var isGridEnabled = false
    private var isFlashEnabled = false

    //gameplay UI
    private var mUILayout: RelativeLayout? = null
    private var mGlView: GLView? = null
    private var mGestureDetector: GestureDetector? = null
    private var mRenderer: ModelRenderer? = null

    //error UI
    private var mPopupMessage: PopupMessage? = null
    private var mErrorDialog: AlertDialog? = null

    private val cameraRequestCode = 1
    private var cameraReady = false

    private var gameMode: GameMode = GameMode.MODE_FREE

    //lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOGTAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        checkPermissions()
    }

    override fun onResume() {
        Log.d(LOGTAG, "onResume")
        super.onResume()

        if (cameraReady) {
            //   showProgressIndicator(true)
            vuforiaAppSession?.onResume()
        }
    }

    // Called whenever the device orientation or screen resolution changes
    override fun onConfigurationChanged(config: Configuration) {
        Log.d(LOGTAG, "onConfigurationChanged")
        super.onConfigurationChanged(config)
        vuforiaAppSession?.onConfigurationChanged()
    }

    override fun onPause() {
        Log.d(LOGTAG, "onPause")
        super.onPause()

        mGlView?.run {
            visibility = View.INVISIBLE
            onPause()
        }

        // Turn off the flash
        if (mFlashOptionView != null && isFlashEnabled) {
            // OnCheckedChangeListener is called upon changing the checked state
            setMenuToggle(mFlashOptionView, false);
        }

        vuforiaAppSession?.onPause()
    }

    override fun onDestroy() {
        Log.d(LOGTAG, "onDestroy")
        super.onDestroy()

        try {
            vuforiaAppSession?.stopAR()
        } catch (e: ArException) {
            Log.e(LOGTAG, e.string)
        }

        mRenderer?.clear()
        System.gc()
    }

    //permissions
    private fun checkPermissions() {

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
        else
            onCameraPermissionGranted()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                onCameraPermissionGranted()
            else
                exitProcess(0)
        }
    }

    private fun onCameraPermissionGranted() {
        prepareGameplayUI()
        setState(UiState.STATE_LOADING_AR)
        vuforiaAppSession = ArSession(this)
        vuforiaAppSession?.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        mGestureDetector = GestureDetector(applicationContext, GestureListener(this))

        // Relocalization timer and message
        mPopupMessage = PopupMessage(this, mUILayout!!, mUILayout!!.findViewById(R.id.topbar_layout), false)

        mRelocalizationTimer = object : Timer(10000, 1000) {
            override fun onFinish() {
                vuforiaAppSession?.resetDeviceTracker()
                super.onFinish()
            }
        }

        mStatusDelayTimer = object : Timer(1000, 1000) {
            override fun onFinish() {

                if (mRenderer!!.isTargetCurrentlyTracked) {
                    super.onFinish()
                    return
                }

                mRelocalizationTimer?.apply { if (!isRunning) startTimer() }

                runOnUiThread { mPopupMessage!!.show(getString(R.string.instruct_relocalize)) }

                super.onFinish()
            }
        }

        viewModel = ViewModelProviders.of(this)[CameraActivityViewModel::class.java]

        viewModel.getPlanes().observe(this, Observer {
            setState(UiState.STATE_LOADING_MODELS)
            gameStateManager.setPlanes(it)
        })

        viewModel.getRenderData().observe(this, Observer {
            mRenderer?.setRenderData(it)
            setState(UiState.STATE_MAIN_MENU)
        })

        viewModel.getLoadProgress().observe(this, Observer { list ->
            val progress = "${list[0]} z ${list[1]}"
            loading_progress?.text = progress
        })

        cameraReady = true
    }

    //gameplay and navigation

    private fun onGameCompleted() {
        checkOrderHandler.removeCallbacks(checkOrder)
        mRenderer?.resetCoordinates()
        VictoryDialog().show(supportFragmentManager, VictoryDialog.TAG)
    }

    override fun onPlayAgain() {
        when (gameMode) {
            GameMode.MODE_FREE -> showSortModeSelection()
            GameMode.MODE_LEVELS -> {
                gameStateManager.setRandomSortMode(); showHint()
            }
        }
    }

    override fun onExit() {
        setState(UiState.STATE_MAIN_MENU)
    }

    private fun showSortModeSelection() {
        GameModeDialog().show(supportFragmentManager, GameModeDialog.TAG)
    }

    override fun onGameModeSelected(gameMode: GameMode) {
        showMainMenu(false)
        this.gameMode = gameMode
        setState(UiState.STATE_GAMEPLAY)
        onPlayAgain()
    }

    override fun onSortModeSelected(mode: SortMode) {
        gameStateManager.sortMode = mode

        bottom_bar.visibility = View.VISIBLE
        showHint()

        if (!testing)
            checkOrderHandler.postDelayed(checkOrder, 2000)
    }

    private val checkOrder = object : Runnable {

        override fun run() {

            val targets = mRenderer!!.getSortedTargets()
            Log.d("targetsOrder", targets.toString())
            val isOrderCorrect = gameStateManager.isOrderCorrect(targets)

            Log.d("poprawne ulozenie", isOrderCorrect.toString())

            if (isOrderCorrect) {
                gameStateManager.setCorrectOrder(targets)
                onGameCompleted()
            } else
                checkOrderHandler.postDelayed(this, 2000)
        }
    }

    private fun showHint() {
        if (isHintEnabled || gameMode == GameMode.MODE_LEVELS)
            Snackbar.make(mUILayout!!, gameStateManager.getObjective(), 3000).show()
    }

    private fun showGrid(show: Boolean) {
        isGridEnabled = show
        setMenuToggle(mGridOptionView, show)
        findViewById<Grid>(R.id.grid).visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun initApplicationAR() {
        // Create OpenGL ES view:
        val depthSize = 16
        val stencilSize = 0
        val translucent = Vuforia.requiresAlpha()

        mGlView = GLView(applicationContext)
        mGlView?.init(translucent, depthSize, stencilSize)

        mRenderer = ModelRenderer(this, vuforiaAppSession!!)

        mGlView?.setRenderer(mRenderer)
        mGlView?.preserveEGLContextOnPause = true
    }

    private fun showMainMenu(show: Boolean) {
        if (show) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.frame, MainMenuFragment())
                    .commit()
        } else {
            val fragment = supportFragmentManager.findFragmentByTag(MainMenuFragment.TAG)
            fragment?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
        }
    }

    private fun hideGameplayUI() {
        mGlView?.run {
            visibility = View.INVISIBLE
            onPause()
        }

        vuforiaAppSession?.onPause()
        mRenderer?.setActive(false)
    }

    private fun showGameplayUI() {

        mRenderer?.setActive(true)

        // Now add the GL surface view. It is important
        // that the OpenGL ES surface view gets added
        // BEFORE the camera is started and video
        // background is configured.
        mGlView?.let {

            frame.addView(mGlView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        frame.addView(mUILayout!!.apply { setBackgroundColor(Color.TRANSPARENT) })
        mUILayout?.visibility = View.VISIBLE
        mUILayout?.bringToFront()

        sideMenu = SideMenu(this@CameraActivity,
                this@CameraActivity, "Samoloty",
                mGlView, mUILayout!!, mSettingsAdditionalViews)

        setSideMenuSettings()

        if (shouldStartAR) {
            vuforiaAppSession?.startAR()
            shouldStartAR = false
        } else vuforiaAppSession?.onResume()
    }

    private fun prepareGameplayUI() {
        mUILayout = View.inflate(applicationContext, R.layout.camera_overlay, null) as RelativeLayout
        mUILayout!!.visibility = View.GONE

        mUILayout!!.button_help.setOnClickListener {
            showHint()
            // createDialog().show()
        }
        mUILayout!!.button_more.setOnClickListener {
            MenuDialog.create(gameMode).show(supportFragmentManager, MenuDialog.TAG)
        }

        val topbarLayout = mUILayout!!.findViewById<RelativeLayout>(R.id.topbar_layout)
        topbarLayout.visibility = View.GONE
        mSettingsAdditionalViews.add(topbarLayout)
    }

    private fun startLoadingAnimation() {
        loadingLayout = View.inflate(applicationContext, R.layout.loading_screen, null) as RelativeLayout
        frame.addView(loadingLayout)
    }

    private fun showProgressIndicator(show: Boolean) {
        loading_progress?.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private enum class UiState {
        STATE_NONE,
        STATE_LOADING_AR,
        STATE_LOADING_MODELS,
        STATE_MAIN_MENU,
        STATE_GAMEPLAY
    }

    private fun setState(newState: UiState) {

        when (newState) {

            UiState.STATE_LOADING_AR -> {
                startLoadingAnimation()
            }
            UiState.STATE_LOADING_MODELS -> {
                loading_label.text = "Wczytywanie modeli"
            }

            UiState.STATE_MAIN_MENU -> {

                when (uiState) {

                    UiState.STATE_LOADING_MODELS -> {
                        frame.removeAllViews()
                        showMainMenu(true)
                    }
                    UiState.STATE_GAMEPLAY -> {
                        hideGameplayUI()
                        frame.removeAllViews()
                        showMainMenu(true)
                    }
                }
            }

            UiState.STATE_GAMEPLAY -> {
                showMainMenu(false)
                showGameplayUI()
            }

        }

        uiState = newState
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    //vuforia

    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    override fun onInitARDone(exception: ArException?) {

        if (exception != null) {
            Log.e(LOGTAG, exception.string)
            showInitializationErrorMessage(exception.string)
            return
        }

        initApplicationAR()
        viewModel.loadData(assets)
    }

    private fun showInitializationErrorMessage(message: String) {
        runOnUiThread {
            mErrorDialog?.dismiss()

            AlertDialog.Builder(this@CameraActivity)
                    .setMessage(message)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK)) { _, _ -> finish() }
                    .create()
                    .show()
        }
    }

    override fun doLoadTrackersData(): Boolean {

        val dataSet = "PlanesDatabase.xml"
        val tManager = TrackerManager.getInstance()
        val objectTracker = tManager.getTracker(ObjectTracker.getClassType())
                as ObjectTracker? ?: return false

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet() ?: return false

        if (!mCurrentDataset!!.load(dataSet, STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false

        for (trackable in mCurrentDataset!!.trackables) {
            val name = "Current Dataset : " + trackable.name
            trackable.userData = name
        }
        return true
    }

    override fun doUnloadTrackersData(): Boolean {
        // Indicate if the trackers were unloaded correctly
        var result = true

        val tManager = TrackerManager.getInstance()
        val objectTracker = tManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?
                ?: return false

        if (mCurrentDataset?.isActive == true) {
            if (objectTracker.activeDataSets.at(0) == mCurrentDataset && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false
            }
            mCurrentDataset = null
        }
        return result
    }

    override fun onVuforiaResumed() {
        mGlView?.run {
            visibility = View.VISIBLE
            onResume()
        }
    }

    override fun onVuforiaStarted() {
        mRenderer!!.updateRenderingPrimitives()

        var selectMenuItem = isAutoFocusEnabled

        if (isAutoFocusEnabled) {

            val camera = CameraDevice.getInstance()

            selectMenuItem = if (!camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                // If continuous autofocus mode fails, attempt to set to a different mode
                if (!camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)
                }
                false
            } else {
                false
            }
        }
        setMenuToggle(mFocusOptionView, selectMenuItem)

        showProgressIndicator(false)
    }

    override fun onVuforiaUpdate(state: State) {}

    override fun doInitTrackers(): Boolean {
        // Indicate if the trackers were initialized correctly
        var result = true
        val tManager = TrackerManager.getInstance()
        val tracker = tManager.initTracker(ObjectTracker.getClassType())
        if (tracker == null) {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started")
            result = false
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized")
        }

        return result
    }

    override fun doStartTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType())
        return objectTracker?.start() == true
    }

    override fun doStopTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType())
        objectTracker?.run { stop() } ?: return false
        return true
    }

    override fun doDeinitTrackers(): Boolean {
        val tManager = TrackerManager.getInstance()
        val result = tManager.deinitTracker(ObjectTracker.getClassType())
        tManager.deinitTracker(PositionalDeviceTracker.getClassType())
        return result
    }

    fun checkForRelocalization(statusInfo: Int) {
        if (mCurrentStatusInfo == statusInfo) {
            return
        }

        mCurrentStatusInfo = statusInfo

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING) {
            // If the status is RELOCALIZING, start the timer
            mStatusDelayTimer?.apply { if (!isRunning) startTimer() }
        } else {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            mStatusDelayTimer?.apply { if (isRunning) stopTimer() }
            mRelocalizationTimer?.apply { if (isRunning) stopTimer() }

            runOnUiThread {
                mPopupMessage?.hide()
            }
        }
    }

    //side menu

    private inner class GestureListener(activity: CameraActivity) : GestureDetector.SimpleOnGestureListener() {
        // Used to set autofocus one second after a manual focus is triggered
        private val autofocusHandler = Handler()
        private val activityRef: WeakReference<CameraActivity> = WeakReference(activity)

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // Process Single Tap event to trigger autofocus
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus")

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed({
                if (activityRef.get()!!.isAutoFocusEnabled) {
                    val autofocusResult = CameraDevice.getInstance()
                            .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)

                    if (!autofocusResult)
                        Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus")
                }
            }, 1000L)

            return true
        }
    }

    override fun menuProcess(command: Int): Boolean {
        var result = true

        when (command) {
            CMD_BACK -> finish()
            CMD_GRID -> showGrid(!isGridEnabled)
            CMD_HINT -> isHintEnabled = !isHintEnabled

            CMD_AUTOFOCUS ->

                if (isAutoFocusEnabled) {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)

                    if (result) {
                        isAutoFocusEnabled = false
                    } else {
                        toast(getString(R.string.menu_contAutofocus_error_off))
                        Log.e(LOGTAG, getString(R.string.menu_contAutofocus_error_off))
                    }
                } else {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)

                    if (result) {
                        isAutoFocusEnabled = true
                    } else {
                        toast(getString(R.string.menu_contAutofocus_error_on))
                        Log.e(LOGTAG, getString(R.string.menu_contAutofocus_error_on))
                    }
                }

            CMD_FLASH -> {

                result = CameraDevice.getInstance().setFlashTorchMode(!isFlashEnabled)

                if (result) {
                    isFlashEnabled = !isFlashEnabled
                } else {
                    toast(getString(if (isFlashEnabled) R.string.menu_flash_error_off else R.string.menu_flash_error_on))
                    Log.e(LOGTAG, getString(if (isFlashEnabled) R.string.menu_flash_error_off else R.string.menu_flash_error_on))
                }
            }
        }

        return result
    }

    private fun setSideMenuSettings() {
        val group: SideMenuGroup = sideMenu!!.addGroup(getString(R.string.menu_camera), false)
        mFlashOptionView = group.addSelectionItem("Latarka", CMD_FLASH, isFlashEnabled)
        mFocusOptionView = group.addSelectionItem(getString(R.string.menu_contAutofocus), CMD_AUTOFOCUS, isAutoFocusEnabled)
        mGridOptionView = group.addSelectionItem("Siatka", CMD_GRID, isGridEnabled)
        mHintOptionView = group.addSelectionItem("Podpowiedzi", CMD_HINT, isHintEnabled)

        sideMenu?.attachMenu()
    }

    private fun setMenuToggle(view: View?, value: Boolean) {
        // OnCheckedChangeListener is called upon changing the checked state
        (view as? Switch)?.isChecked = value
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Process the Gestures
        return sideMenu?.processEvent(event) ?: false || mGestureDetector!!.onTouchEvent(event)
    }

    companion object {
        private const val LOGTAG = "CameraActivity"

        private const val CMD_BACK = -1
        private const val CMD_AUTOFOCUS = 2
        private const val CMD_GRID = 3
        private const val CMD_HINT = 4
        private const val CMD_FLASH = 5

    }
}

package com.pollub.samoloty.ui

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.pollub.samoloty.*
import com.pollub.samoloty.render.ModelRenderer
import com.pollub.samoloty.render.RenderData
import com.pollub.samoloty.ui.menu.SampleAppMenu
import com.pollub.samoloty.ui.menu.SampleAppMenuGroup
import com.pollub.samoloty.ui.menu.SampleAppMenuInterface
import com.pollub.samoloty.utils.GLView
import com.pollub.samoloty.utils.LoadingDialogHandler
import com.pollub.samoloty.utils.LoadingDialogHandler.HIDE_LOADING_DIALOG
import com.pollub.samoloty.utils.LoadingDialogHandler.SHOW_LOADING_DIALOG
import com.pollub.samoloty.utils.SampleAppTimer
import com.vuforia.*
import com.vuforia.artest.R
import java.lang.ref.WeakReference
import java.util.*

class CameraActivity : AppCompatActivity(), Control, SampleAppMenuInterface {

    //TODO zrobic pobieranie wspolrzednych z ModelRenderer co kilka sekund
    // i sprawdzac czy dobrze ustawione w GameManager

    private lateinit var viewModel: CameraActivityViewModel
    private val gameStateManager: GameManager = GameManager()

    private var vuforiaAppSession: ArSession? = null
    private var mCurrentDataset: DataSet? = null
    private var mGlView: GLView? = null
    private var mRenderer: ModelRenderer? = null
    private var mGestureDetector: GestureDetector? = null

    // Menu option flags
    private var mSwitchDatasetAsap = false
    private var mContAutofocus = true

    private var mFocusOptionView: View? = null

    private var mUILayout: RelativeLayout? = null

    private var appMenu: SampleAppMenu? = null
    internal var mSettingsAdditionalViews = ArrayList<View>()

    private var mSampleAppMessage: SampleAppMessage? = null
    private var mRelocalizationTimer: SampleAppTimer? = null
    private var mStatusDelayTimer: SampleAppTimer? = null

    private var mCurrentStatusInfo: Int = 0

    private val handler = Handler()

    private fun onGameCompleted() {
        AlertDialog.Builder(this).setMessage("Wygrane").create().show()
    }

    private val checkOrder = object : Runnable {

        override fun run() {

            val targets = mRenderer!!.sortedTargets
            val isOrderCorrect =  gameStateManager.isOrderCorrect(targets)

            if (isOrderCorrect)
                onGameCompleted()
            else
                handler.postDelayed(this, 2000)
        }
    }

    val loadingDialogHandler = LoadingDialogHandler(this)

    // Alert Dialog used to display SDK errors
    private var mErrorDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOGTAG, "onCreate")
        super.onCreate(savedInstanceState)

        vuforiaAppSession = ArSession(this)

        startLoadingAnimation()

        vuforiaAppSession!!.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        mGestureDetector = GestureDetector(applicationContext, GestureListener(this))

        // Relocalization timer and message
        mSampleAppMessage = SampleAppMessage(this, mUILayout!!, mUILayout!!.findViewById(R.id.topbar_layout), false)

        mRelocalizationTimer = object : SampleAppTimer(10000, 1000) {
            override fun onFinish() {
                vuforiaAppSession?.resetDeviceTracker()
                super.onFinish()
            }
        }

        mStatusDelayTimer = object : SampleAppTimer(1000, 1000) {
            override fun onFinish() {
                if (mRenderer!!.isTargetCurrentlyTracked) {
                    super.onFinish()
                    return
                }

                if (!mRelocalizationTimer!!.isRunning) {
                    mRelocalizationTimer!!.startTimer()
                }

                runOnUiThread { mSampleAppMessage!!.show(getString(R.string.instruct_relocalize)) }

                super.onFinish()
            }
        }
        viewModel = ViewModelProviders.of(this)[CameraActivityViewModel::class.java]
        viewModel.getPlanes().observe(this, Observer { gameStateManager.setPlanes(it) })
        viewModel.getRenderData().observe(this, Observer { onDataLoaded(it) })
    }

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
                if (activityRef.get()!!.mContAutofocus) {
                    val autofocusResult = CameraDevice.getInstance()
                            .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)

                    if (!autofocusResult)
                        Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus")
                }
            }, 1000L)

            return true
        }
    }

    override fun onResume() {
        Log.d(LOGTAG, "onResume")
        super.onResume()
        showProgressIndicator(true)
        vuforiaAppSession!!.onResume()
    }

    // Called whenever the device orientation or screen resolution changes
    override fun onConfigurationChanged(config: Configuration) {
        Log.d(LOGTAG, "onConfigurationChanged")
        super.onConfigurationChanged(config)
        vuforiaAppSession!!.onConfigurationChanged()
    }

    override fun onPause() {
        Log.d(LOGTAG, "onPause")
        super.onPause()

        mGlView?.run {
            visibility = View.INVISIBLE
            onPause()
        }

        vuforiaAppSession!!.onPause()
    }

    override fun onDestroy() {
        Log.d(LOGTAG, "onDestroy")
        super.onDestroy()

        try {
            vuforiaAppSession!!.stopAR()
        } catch (e: ArException) {
            Log.e(LOGTAG, e.string)
        }

        mRenderer!!.clear()
        System.gc()
    }

    private fun initApplicationAR() {
        // Create OpenGL ES view:
        val depthSize = 16
        val stencilSize = 0
        val translucent = Vuforia.requiresAlpha()


        mGlView = GLView(applicationContext)
        mGlView!!.init(translucent, depthSize, stencilSize)

        mRenderer = ModelRenderer(this, vuforiaAppSession)

        mGlView!!.setRenderer(mRenderer)
        mGlView!!.preserveEGLContextOnPause = true
    }

    private fun onDataLoaded(data: List<RenderData>) {

        mRenderer!!.setRenderData(data)
        mRenderer!!.setActive(true)

        // Now add the GL surface view. It is important
        // that the OpenGL ES surface view gets added
        // BEFORE the camera is started and video
        // background is configured.
        addContentView(mGlView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Sets the UILayout to be drawn in front of the camera
        mUILayout!!.bringToFront()
        mUILayout!!.setBackgroundColor(Color.TRANSPARENT)

        appMenu = SampleAppMenu(this@CameraActivity,
                this@CameraActivity, "Samoloty",
                mGlView, mUILayout!!, mSettingsAdditionalViews)
        setSampleAppMenuSettings()

        vuforiaAppSession!!.startAR()

        //TODO
        handler.post(checkOrder)
    }

    private fun startLoadingAnimation() {
        mUILayout = View.inflate(applicationContext, R.layout.camera_overlay, null) as RelativeLayout
        mUILayout!!.visibility = View.VISIBLE
        mUILayout!!.setBackgroundColor(Color.BLACK)

        val topbarLayout = mUILayout!!.findViewById<RelativeLayout>(R.id.topbar_layout)
        topbarLayout.visibility = View.GONE

        mSettingsAdditionalViews.add(topbarLayout)

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout!!
                .findViewById(R.id.loading_indicator)

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(SHOW_LOADING_DIALOG)

        // Adds the inflated layout to the view
        addContentView(mUILayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun doLoadTrackersData(): Boolean {
        val tManager = TrackerManager.getInstance()
        val objectTracker = tManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker? ?: return false

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet()

        if (mCurrentDataset == null)
            return false

        if (!mCurrentDataset!!.load(dataSet, STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false

        val trackableList = mCurrentDataset!!.trackables
        for (trackable in trackableList) {
            val name = "Current Dataset : " + trackable.name
            trackable.userData = name
            Log.d(LOGTAG, "UserData:Set the following user data " + trackable.userData)
        }
        return true
    }

    override fun doUnloadTrackersData(): Boolean {
        // Indicate if the trackers were unloaded correctly
        var result = true

        val tManager = TrackerManager.getInstance()
        val objectTracker = tManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker? ?: return false

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
        if (mGlView != null) {
            mGlView!!.visibility = View.VISIBLE
            mGlView!!.onResume()
        }
    }

    override fun onVuforiaStarted() {
        mRenderer!!.updateRenderingPrimitives()

        if (mContAutofocus) {

            val camera = CameraDevice.getInstance()

            if (!camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                // If continuous autofocus mode fails, attempt to set to a different mode
                if (!camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    camera.setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)
                }

                setMenuToggle(mFocusOptionView, false)
            } else {
                setMenuToggle(mFocusOptionView, true)
            }
        } else {
            setMenuToggle(mFocusOptionView, false)
        }

        showProgressIndicator(false)
    }

    private fun showProgressIndicator(show: Boolean) {
        val message = if (show) SHOW_LOADING_DIALOG else HIDE_LOADING_DIALOG
        loadingDialogHandler.sendEmptyMessage(message)
    }

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

    // Called every frame
    override fun onVuforiaUpdate(state: State) {
        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false
            val tm = TrackerManager.getInstance()
            val ot = tm.getTracker(ObjectTracker.getClassType()) as ObjectTracker?
            if (ot == null || mCurrentDataset == null || ot.activeDataSets.at(0) == null) {
                Log.d(LOGTAG, "Failed to swap datasets")
                return
            }

            doUnloadTrackersData()
            doLoadTrackersData()
        }
    }

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Process the Gestures
        return appMenu != null && appMenu!!.processEvent(event) || mGestureDetector!!.onTouchEvent(event)
    }

    private fun setSampleAppMenuSettings() {
        val group: SampleAppMenuGroup = appMenu!!.addGroup(getString(R.string.menu_camera), true)
        mFocusOptionView = group.addSelectionItem(getString(R.string.menu_contAutofocus), CMD_AUTOFOCUS, mContAutofocus)
        appMenu!!.attachMenu()
    }

    private fun setMenuToggle(view: View?, value: Boolean) {
        // OnCheckedChangeListener is called upon changing the checked state
        (view as Switch).isChecked = value
    }

    // In this function you can define the desired behavior for each menu option
    // Each case corresponds to a menu option
    override fun menuProcess(command: Int): Boolean {
        var result = true

        when (command) {
            CMD_BACK -> finish()

            CMD_AUTOFOCUS ->

                if (mContAutofocus) {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)

                    if (result) {
                        mContAutofocus = false
                    } else {
                        toast(getString(R.string.menu_contAutofocus_error_off))
                        Log.e(LOGTAG, getString(R.string.menu_contAutofocus_error_off))
                    }
                } else {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)

                    if (result) {
                        mContAutofocus = true
                    } else {
                        toast(getString(R.string.menu_contAutofocus_error_on))
                        Log.e(LOGTAG, getString(R.string.menu_contAutofocus_error_on))
                    }
                }
        }

        return result
    }

    fun checkForRelocalization(statusInfo: Int) {
        if (mCurrentStatusInfo == statusInfo) {
            return
        }

        mCurrentStatusInfo = statusInfo

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING) {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer!!.isRunning) {
                mStatusDelayTimer!!.startTimer()
            }
        } else {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer!!.isRunning) {
                mStatusDelayTimer!!.stopTimer()
            }

            if (mRelocalizationTimer!!.isRunning) {
                mRelocalizationTimer!!.stopTimer()
            }

            runOnUiThread {
                mSampleAppMessage?.hide()
            }
        }
    }

    private fun clearSampleAppMessage() {
        runOnUiThread {
            mSampleAppMessage?.hide()
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val LOGTAG = "CameraActivity"

        private val CMD_BACK = -1
        private val CMD_AUTOFOCUS = 2

        const val dataSet = "PlanesDatabase.xml"
    }
}

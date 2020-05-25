/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/
package com.pollub.samoloty.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.util.Pair
import com.pollub.samoloty.ArSession
import com.pollub.samoloty.render.shader.CubeShaders
import com.pollub.samoloty.ui.CameraActivity
import com.pollub.samoloty.utils.MathUtils
import com.vuforia.*
import java.lang.ref.WeakReference
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(activity: CameraActivity, session: ArSession) : RendererControl, GLSurfaceView.Renderer {

    private val activityRef: WeakReference<CameraActivity>
    private var shaderProgramID = 0
    private var vertexHandle = 0
    private var textureCoordHandle = 0
    private var mvpMatrixHandle = 0
    private var texSampler2DHandle = 0
    private val mVideoRenderer: VideoRenderer
    private val vuforiaAppSession: ArSession

    var isTargetCurrentlyTracked = false
        private set

    private val coordinates: MutableMap<String, Float> = HashMap()
    private var dataMap: MutableMap<String, RenderData> = HashMap()

    fun setRenderData(dataList: List<RenderData>) {
        dataMap = dataList.associateBy { it.targetName }.toMutableMap()
    }

    fun resetCoordinates() {
        coordinates.clear()
    }

    fun getSortedTargets() = coordinates.entries.sortedBy { it.value }.map { it.key }

    fun clear() = dataMap.clear()

    fun updateRenderingPrimitives() = mVideoRenderer.updateRenderingPrimitives()

    fun setActive(active: Boolean) = mVideoRenderer.setActive(active)

    override fun renderFrame(state: State, projectionMatrix: FloatArray) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mVideoRenderer.renderVideoBackground()

        // Set the device pose matrix as identity
        var devicePoseMatrix = MathUtils.Matrix44FIdentity()
        var modelMatrix: Matrix44F
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW) // Back camera

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.deviceTrackableResult != null) {
            val statusInfo = state.deviceTrackableResult.statusInfo
            val trackerStatus = state.deviceTrackableResult.status
            activityRef.get()?.checkForRelocalization(statusInfo)
            if (trackerStatus != TrackableResult.STATUS.NO_POSE) {
                modelMatrix = Tool.convertPose2GLMatrix(state.deviceTrackableResult.pose)
                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = MathUtils.Matrix44FTranspose(MathUtils.Matrix44FInverse(modelMatrix))
            }
        }

        val trackableResultList = state.trackableResults

        // Determine if target is currently being tracked
        setIsTargetCurrentlyTracked(trackableResultList)

        for (result in trackableResultList) {

            if (result.isOfType(ImageTargetResult.getClassType())) {
                modelMatrix = Tool.convertPose2GLMatrix(result.pose)
                val name = result.trackable.name
                coordinates[name] = modelMatrix.data[12]
                renderModel(dataMap[name]!!, projectionMatrix, devicePoseMatrix.data, modelMatrix.data)
                RenderUtils.checkGLError("Image Targets renderFrame")
            }
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    }

    override fun initRendering() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, if (Vuforia.requiresAlpha()) 0.0f else 1.0f)

        for (data in dataMap.values) {
            val t = data.texture
            GLES20.glGenTextures(1, t.mTextureID, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0])
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData)
        }

        shaderProgramID = RenderUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER)

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition")
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix")
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D")
    }

    private val ALIGN_VERTICAL = 0
    private val ALIGN_FLAT = 1

    private val displayMode = ALIGN_FLAT

    var x = 0f
    var y = 90f
    var z = 90f

    private fun renderModel(renderData: RenderData,
                            projectionMatrix: FloatArray, viewMatrix: FloatArray, modelMatrix: FloatArray) {

        var (_, model, texture, scale, rotation, multiplier) = renderData

        val modelViewProjection = FloatArray(16)
        scale *= OBJECT_BASE_SCALE


        when (displayMode) {
            ALIGN_FLAT -> {

                Matrix.rotateM(modelMatrix, 0, x, 1f, 0f, 0f)
                Matrix.rotateM(modelMatrix, 0, y, 0f, 1f, 0f)
                Matrix.rotateM(modelMatrix, 0, z, 0f, 0f, 1f)

            }
            ALIGN_VERTICAL -> Matrix.rotateM(modelMatrix, 0, 180f + rotation, 0f, 0f, 0f)
        }

        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID)
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.getVertices())
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.getTexCoords())
        GLES20.glEnableVertexAttribArray(vertexHandle)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.mTextureID[0])
        GLES20.glUniform1i(texSampler2DHandle, 0)
        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, (model.getVertexCount() * multiplier).toInt())
        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }

    private fun setIsTargetCurrentlyTracked(trackableResultList: TrackableResultList) {
        for (result in trackableResultList) {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ImageTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType())) {
                val currentStatus = result.status
                val currentStatusInfo = result.statusInfo
                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL) {
                    isTargetCurrentlyTracked = true
                    return
                }
            }
        }
        isTargetCurrentlyTracked = false
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated")
        // Call Vuforia function to (re)initialize rendering after first use
// or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated()
        mVideoRenderer.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged")
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height)
        // RenderingPrimitives to be updated when some rendering change is done
        onConfigurationChanged()
    }

    override fun onDrawFrame(gl: GL10) { // Call our function to render content from SampleAppRenderer class
        mVideoRenderer.render()
    }

    private fun onConfigurationChanged() {
        mVideoRenderer.onConfigurationChanged()
    }

    companion object {
        private const val LOGTAG = "ImageTargetRenderer"
        private const val OBJECT_BASE_SCALE = 0.025f
    }

    init {
        Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS.toLong(), 5)
        activityRef = WeakReference(activity)
        vuforiaAppSession = session
        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
// the device mode AR/VR and stereo mode
        mVideoRenderer = VideoRenderer(this, activity, vuforiaAppSession.videoMode,
                0.01f, 5f)
    }
}
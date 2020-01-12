/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.pollub.samoloty.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;
import com.pollub.samoloty.ArSession;
import com.pollub.samoloty.render.shader.CubeShaders;
import com.pollub.samoloty.ui.CameraActivity;
import com.pollub.samoloty.utils.MathUtils;
import com.vuforia.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vuforia.HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS;

public class ModelRenderer implements RendererControl, GLSurfaceView.Renderer {

    private static final String LOGTAG = "ImageTargetRenderer";

    private final WeakReference<CameraActivity> mActivityRef;

    private CameraActivity getActivity() {
        return mActivityRef.get();
    }

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private VideoRenderer mVideoRenderer;
    private ArSession vuforiaAppSession;

    private boolean mIsTargetCurrentlyTracked = false;

    private static final float OBJECT_SCALE_FLOAT = 0.023f;

    //klucz - nazwa targetu, wartość - współrzędna X
    private Map<String, Float> coordinates = new HashMap<>();

    private Map<String, Pair<Model, Texture>> dataMap = new HashMap<>();

    public void setRenderData(List<RenderData> dataList) {
        dataList.forEach(data -> {
            Pair<Model, Texture> pair = new Pair<>(data.getModel(), data.getTexture());
            dataMap.put(data.getTargetName(), pair);
        });
    }

    public List<String> getSortedTargets() {

        return new ArrayList<>(coordinates.entrySet())
                .stream()
                .sorted((a, b) -> Float.compare(a.getValue(), b.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void clear() {
        dataMap.clear();
        dataMap = null;
    }

    public ModelRenderer(CameraActivity activity, ArSession session) {

        Vuforia.setHint(HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 3);

        mActivityRef = new WeakReference<>(activity);
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mVideoRenderer = new VideoRenderer(this, getActivity(), vuforiaAppSession.getVideoMode(),
                0.01f, 5f);
    }

    public void updateRenderingPrimitives() {
        mVideoRenderer.updateRenderingPrimitives();
    }

    public void setActive(boolean active) {
        mVideoRenderer.setActive(active);
    }

    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mVideoRenderer.renderVideoBackground();

        // Set the device pose matrix as identity
        Matrix44F devicePoseMatrix = MathUtils.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null) {
            int statusInfo = state.getDeviceTrackableResult().getStatusInfo();
            int trackerStatus = state.getDeviceTrackableResult().getStatus();

            getActivity().checkForRelocalization(statusInfo);

            if (trackerStatus != TrackableResult.STATUS.NO_POSE) {
                modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = MathUtils.Matrix44FTranspose(MathUtils.Matrix44FInverse(modelMatrix));
            }
        }

        TrackableResultList trackableResultList = state.getTrackableResults();

        // Determine if target is currently being tracked
        setIsTargetCurrentlyTracked(trackableResultList);

        // Iterate through trackable results and render any augmentations
        for (TrackableResult result : trackableResultList) {
            Trackable trackable = result.getTrackable();

            if (result.isOfType(ImageTargetResult.getClassType())) {
                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

                float x = modelMatrix.getData()[12];
                String name = trackable.getName();
                coordinates.put(name, x);

                Model model = dataMap.get(name).first;
                Texture texture = dataMap.get(name).second;

                renderModel(model, texture, projectionMatrix,
                        devicePoseMatrix.getData(), modelMatrix.getData());

                RenderUtils.checkGLError("Image Targets renderFrame");
            }
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void initRendering() {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        for (Pair<Model, Texture> data : dataMap.values()) {

            Texture t = data.second;

            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = RenderUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

    }

    private void renderModel(Model model, Texture texture, float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix) {
        float[] modelViewProjection = new float[16];

        Matrix.rotateM(modelMatrix, 0, 180, 0, 1, 0);
        Matrix.translateM(modelMatrix, 0, 0, 0, -0.2f);
        Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.getVertexCount() * 5);

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    private void setIsTargetCurrentlyTracked(TrackableResultList trackableResultList) {
        for (TrackableResult result : trackableResultList) {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ImageTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType())) {
                int currentStatus = result.getStatus();
                int currentStatusInfo = result.getStatusInfo();

                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL) {
                    mIsTargetCurrentlyTracked = true;
                    return;
                }
            }
        }
        mIsTargetCurrentlyTracked = false;
    }

    public boolean isTargetCurrentlyTracked() {
        return mIsTargetCurrentlyTracked;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mVideoRenderer.onSurfaceCreated();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        onConfigurationChanged();
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // Call our function to render content from SampleAppRenderer class
        mVideoRenderer.render();
    }


    private void onConfigurationChanged() {
        mVideoRenderer.onConfigurationChanged();
    }
}


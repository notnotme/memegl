/*
 * Copyright 2017 Uncorked Studios Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This code is forked from https://github.com/spaceLenny/recordablesurfaceview
 */
package com.notnotme.memegl.renderer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaRecorder
import android.opengl.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Used to record video of the content of a SurfaceView, backed by a GL render loop.
 *
 *
 * Intended as a near-drop-in replacement for [GLSurfaceView], but reliant on callbacks
 * instead of an explicit [GLSurfaceView.Renderer].
 *
 *
 *
 * **Note:** Currently, GLRecorderSurfaceView does not record video on the emulator
 * due to a dependency on [MediaRecorder].
 */
class GLRecorderSurfaceView : SurfaceView {

    companion object {
        const val TAG = "GLRecorderSurfaceView"

        /**
         * The renderer only renders when the surface is created, or when @link{requestRender} is
         * called.
         */
        const val RENDERMODE_WHEN_DIRTY = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        /**
         * The renderer is called continuously to re-render the scene.
         */
        const val RENDERMODE_CONTINUOUSLY = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    private val mSurface = MediaCodec.createPersistentInputSurface()
    private val mRenderMode = AtomicInteger(RENDERMODE_CONTINUOUSLY)
    private val mIsRecording = AtomicBoolean(false)
    private val mHasGLContext = AtomicBoolean(false)
    private val mSizeChange = AtomicBoolean(false)
    private val mStartRecorder = AtomicBoolean(false)
    private val mRenderRequested = AtomicBoolean(false)
    private val mRendererCallbacks = AtomicReference<RendererCallbacks?>(null)

    private var mMediaRecorder: MediaRecorder? = null
    private var mARRenderThread: ARRenderThread? = null

    private var mWidth = 0
    private var mHeight = 0
    private var mRecorderWidth = 0
    private var mRecorderHeight = 0

    init {
        mARRenderThread = ARRenderThread().also {
            holder.addCallback(it)
        }
    }

    /**
     * @param context -
     */
    constructor(context: Context?) : super(context)

    /**
     * @param context -
     * @param attrs   -
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    /**
     * @param context      -
     * @param attrs        -
     * @param defStyleAttr -
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * @param context      -
     * @param attrs        -
     * @param defStyleAttr -
     * @param defStyleRes  -
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    var renderMode: Int
        /**
         * @return int representing the current render mode of this object
         * @see GLRecorderSurfaceView.RENDERMODE_WHEN_DIRTY
         *
         * @see GLRecorderSurfaceView.RENDERMODE_CONTINUOUSLY
         */
        get() = mRenderMode.get()
        /**
         * Set the rendering mode. When renderMode is [GLRecorderSurfaceView.RENDERMODE_CONTINUOUSLY],
         * the renderer is called repeatedly to re-render the scene. When renderMode is [ ][GLRecorderSurfaceView.RENDERMODE_WHEN_DIRTY], the renderer only rendered when the surface is
         * created, or when [GLRecorderSurfaceView.requestRender] is called. Defaults to [ ][GLRecorderSurfaceView.RENDERMODE_CONTINUOUSLY].
         *
         *
         * Using [GLRecorderSurfaceView.RENDERMODE_WHEN_DIRTY] can improve battery life and
         * overall system performance by allowing the GPU and CPU to idle when the view does not need
         * to
         * be updated.
         */
        set(mode) = mRenderMode.set(mode)

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to [ ][GLRecorderSurfaceView.RENDERMODE_WHEN_DIRTY],  so that frames are only rendered on demand.
     * May be called from any thread.
     *
     *
     * Must not be called before a renderer has been set.
     */
    fun requestRender() {
        mRenderRequested.set(true)
    }

    /**
     * Iitializes the [MediaRecorder] ad relies on its lifecycle and requirements.
     *
     * @param saveToFile      the File object to record into. Assumes the calling program has
     * permission to write to this file
     * @param width    the Width of the display
     * @param height   the Height of the display
     * @param errorListener   optional [MediaRecorder.OnErrorListener] for recording state callbacks
     * @param infoListener    optional [MediaRecorder.OnInfoListener] for info callbacks
     * @see MediaRecorder
     */
    @Throws(IOException::class)
    fun initRecorder(
        saveToFile: File,
        width: Int,
        height: Int,
        errorListener: MediaRecorder.OnErrorListener?,
        infoListener: MediaRecorder.OnInfoListener?
    ) {
        mRecorderWidth = width
        mRecorderHeight = height
        mMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("deprecation")
            MediaRecorder()
        }.also {
            // see https://developer.android.com/guide/topics/media/media-formats
            it.setOnInfoListener(infoListener)
            it.setOnErrorListener(errorListener)
            it.setInputSurface(mSurface)
            it.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            it.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            it.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            it.setVideoFrameRate(30)
            it.setAudioSamplingRate(44_100)
            it.setAudioEncodingBitRate(96_000)
            it.setVideoEncodingBitRate(1_500_000)
            it.setVideoSize(width, height)
            it.setOutputFile(saveToFile.absolutePath)
            it.prepare()
        }
    }

    /**
     * @return true if the recording started successfully and false if not
     * @see MediaRecorder.start
     */
    fun startRecording() {
        mMediaRecorder?.let {
            try {
                it.start()
                mStartRecorder.set(true)
                mIsRecording.set(true)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error: ${e.message}")
                it.reset()
                it.release()
            }
        }
    }

    /**
     * Stops the [MediaRecorder] and sets the internal state of this object to 'Not
     * recording'
     * It is important to call this before attempting to play back the video that has been
     * recorded.
     *
     * @return true if the recording stopped successfully and false if not
     * @throws IllegalStateException if not recording when called
     */
    @Throws(IllegalStateException::class)
    fun stopRecording() {
        mMediaRecorder?.let {
            try {
                mIsRecording.set(false)
                it.stop()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error: ${e.message}")
            } finally {
                it.reset()
                it.release()
            }
        }
        mMediaRecorder = null
    }

    var rendererCallbacks: RendererCallbacks?
        /**
         * Returns the reference (if any) to the [RendererCallbacks]
         *
         * @return the callbacks if registered
         * @see RendererCallbacks
         */
        get() = mRendererCallbacks.get()
        /**
         * Add a [RendererCallbacks] object to handle rendering. Not setting one of these is not
         * necessarily an error, but is usually necessary.
         *
         * @param surfaceRendererCallbacks - the object to call back to
         */
        set(surfaceRendererCallbacks) = mRendererCallbacks.set(surfaceRendererCallbacks)

    /**
     * Queue a runnable to be run on the GL rendering thread.
     *
     * @param runnable - the runnable to queue
     */
    fun queueEvent(runnable: Runnable) {
        mARRenderThread?.let {
            synchronized(it.mRunnableQueueLock) {
                it.mRunnableQueue.add(runnable)
            }
        }
    }

    fun stop() {
        mARRenderThread?.let {
            it.mLoop.set(false)
            try {
                it.join()
                Log.d(TAG, "Thread stopped $mARRenderThread")
            } catch (e: Exception) {
                Log.e(TAG, "Thread stop: ${e.message}")
            }
        }
    }

    /**
     * Lifecycle events for the SurfaceView and renderer. These callbacks (unless specified)
     * are executed on the GL thread.
     */
    interface RendererCallbacks {
        /**
         * The surface has been created and bound to the GL context.
         *
         *
         * A GL context is guaranteed to exist when this function is called.
         */
        fun onSurfaceCreated()

        /**
         * The surface has changed width or height.
         *
         *
         * This callback will only be called when there is a change to either or both params
         *
         * @param width  width of the surface
         * @param height height of the surface
         */
        fun onSurfaceChanged(width: Int, height: Int)

        /**
         * Called just before the GL Context is torn down.
         */
        fun onSurfaceDestroyed()

        /**
         * Render call.
         */
        fun onDrawFrame()

        /**
         * Called just before frame recording.
         */
        fun onStartRecorder(width: Int, height: Int)

        /**
         * Render call. Called when recording.
         */
        fun onRecordFrame()
    }

    private inner class ARRenderThread : Thread(), SurfaceHolder.Callback2 {
        val mLoop = AtomicBoolean(false)
        val mRunnableQueue = LinkedList<Runnable>()
        val mRunnableQueueLock = Any()
        var mEGLDisplay: EGLDisplay? = null
        var mEGLContext: EGLContext? = null
        var mEGLSurface: EGLSurface? = null
        var mEGLSurfaceMedia: EGLSurface? = null
        var config = intArrayOf(
            EGL14.EGL_RED_SIZE, 5,
            EGL14.EGL_GREEN_SIZE, 6,
            EGL14.EGL_BLUE_SIZE, 5,
            EGL14.EGL_ALPHA_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            0x3142, 1,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_NONE
        )

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                config[10] = EGLExt.EGL_RECORDABLE_ANDROID
            }
        }

        fun chooseEglConfig(eglDisplay: EGLDisplay?): EGLConfig? {
            val configsCount = intArrayOf(0)
            val configs = arrayOfNulls<EGLConfig>(1)
            EGL14.eglChooseConfig(eglDisplay, config, 0, configs, 0, configs.size, configsCount, 0)
            return configs[0]
        }

        override fun run() {
            Log.d(TAG, "Thread started $mARRenderThread")

            if (mHasGLContext.get()) {
                return
            }
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

            val version = IntArray(2)
            EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)

            val eglConfig = chooseEglConfig(mEGLDisplay)
            mEGLContext = EGL14.eglCreateContext(
                mEGLDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, eglConfig, this@GLRecorderSurfaceView, surfaceAttribs, 0)
            mEGLSurfaceMedia = EGL14.eglCreateWindowSurface(mEGLDisplay, eglConfig, mSurface, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
            //EGL14.eglSwapInterval(mEGLDisplay, 0)

            // guarantee to only report surface as created once GL context
            // associated with the surface has been created, and call on the GL thread
            // NOT the main thread but BEFORE the codec surface is attached to the GL context
            mRendererCallbacks.get()?.onSurfaceCreated()
            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

            mHasGLContext.set(true)
            mLoop.set(true)
            while (mLoop.get()) {
                var shouldRender = false

                //we're just rendering when requested, so check that no one
                //has requested and if not, just continue
                if (mRenderMode.get() == RENDERMODE_WHEN_DIRTY) {
                    if (mRenderRequested.get()) {
                        mRenderRequested.set(false)
                        shouldRender = true
                    }
                } else {
                    shouldRender = true
                }

                synchronized(mRunnableQueueLock) {
                    if (mRunnableQueue.isNotEmpty()) {
                        mRunnableQueue.iterator().run {
                            while (hasNext()) {
                                next().run()
                                remove()
                            }
                        }
                    }
                }

                if (mSizeChange.get()) {
                    mRendererCallbacks.get()?.onSurfaceChanged(mWidth, mHeight)
                    mSizeChange.set(false)
                }

                if (shouldRender && mEGLSurface != null && mEGLSurface != EGL14.EGL_NO_SURFACE) {
                    mRendererCallbacks.get()?.let {
                        it.onDrawFrame()
                        EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)

                        if (mIsRecording.get()) {
                            // If we are recording,
                            // - notify the renderer that recording will start
                            if (mStartRecorder.get()) {
                                mRendererCallbacks.get()?.onStartRecorder(mRecorderWidth, mRecorderHeight)
                                mStartRecorder.set(false)
                            }

                            // - Make EGLMediaSurface current
                            // - Call onRecordFrame
                            // - Make old surface current
                            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurfaceMedia, mEGLSurfaceMedia, mEGLContext)
                            it.onRecordFrame()
                            EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurfaceMedia)
                            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
                        }
                    }
                }
            }

            cleanup()
        }

        private fun cleanup() {
            Log.d(TAG, "Cleanup")

            synchronized(mRunnableQueueLock) {
                if (mRunnableQueue.isNotEmpty()) {
                    Log.w(TAG, "Runnable queue is not empty, removing ${mRunnableQueue.size} element(s)")
                    mRunnableQueue.iterator().run {
                        while (hasNext()) {
                            next().run()
                            remove()
                        }
                    }
                }
            }

            mRendererCallbacks.run {
                get()?.onSurfaceDestroyed()
                set(null)
            }

            if (mEGLDisplay != null) {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

                if (mEGLSurface != null) {
                    EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
                    mEGLSurface = null
                }

                if (mEGLSurfaceMedia != null) {
                    EGL14.eglDestroySurface(mEGLDisplay, mEGLSurfaceMedia)
                    mEGLSurfaceMedia = null
                }

                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
                mHasGLContext.set(false)
                mEGLContext = null

                EGL14.eglReleaseThread()
                EGL14.eglTerminate(mEGLDisplay)
                mEGLDisplay = null
            }

            mSurface.release()
        }

        override fun surfaceRedrawNeeded(surfaceHolder: SurfaceHolder) {}

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            if (!isAlive && !isInterrupted && state != State.TERMINATED) {
                start()
            }
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, width: Int, height: Int) {
            if (mWidth != width) {
                mWidth = width
                mSizeChange.set(true)
            }
            if (mHeight != height) {
                mHeight = height
                mSizeChange.set(true)
            }
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            surfaceHolder.removeCallback(this@ARRenderThread)
        }
    }

}

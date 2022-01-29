/*
 * Meme Présidents, swap a président face with yours.
 * Copyright (C) 2022  Romain Graillot
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.notnotme.memegl.fragment.camera

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.google.android.material.color.MaterialColors
import com.google.android.material.math.MathUtils
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.notnotme.memegl.Mask
import com.notnotme.memegl.renderer.GLRecorderSurfaceView
import com.notnotme.memegl.renderer.graphic.*
import com.notnotme.memegl.renderer.graphic.PolySpriteBuffer.Companion.Sprite
import com.notnotme.memegl.renderer.graphic.PolySpriteBuffer.Companion.setPosition
import com.notnotme.memegl.renderer.graphic.PolySpriteBuffer.Companion.setSTUV
import com.notnotme.memegl.renderer.graphic.PolySpriteBuffer.Companion.setScale
import com.notnotme.memegl.renderer.graphic.PolySpriteBuffer.Companion.setSize
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

open class PolyRenderer(
    private var surfaceView: GLRecorderSurfaceView?,
    private val imageZoom: Boolean,
    private val width: Int,
    private val height: Int,
    private val isFrontCamera: Boolean
) : GLRecorderSurfaceView.RendererCallbacks {

    companion object {
        const val TAG = "PolyRenderer"

        private const val LERP_VALUE = 4.0f // * delta time
        private const val CIRCLE_PRECISION = 16 // segments from 360°
        private const val STEP_SIZE = 360.0f / CIRCLE_PRECISION

        // Eyes and Mouth sprite size.
        // This also map directly to camera texture size, whatever the lens zoom is
        // That mean eye and mouth will not have the same texture content between devices.
        // TODO: Find a simple way to normalize texture mapping. ML Kit Contour detection may help.
        private val eyeSize = Size(200.0F, 140.0f)
        private val mouthSize = Size(180.0f, 270.0f)

        private data class LandmarkSpriteInfo(
            var position: PointF = PointF(),
            var texturePosition: PointF = PointF(),
            var textureNewPosition: PointF = PointF(),
            var size: Size = Size(),
            var scale: Scale = Scale(),
            var orientation: Float = 0.0f
        )

        private data class SpriteHolder(
            var leftEye: LandmarkSpriteInfo,
            var rightEye: LandmarkSpriteInfo,
            var mouth: LandmarkSpriteInfo,
            var mask: Sprite,
            var framebuffer: Sprite,
            var draw: Boolean,
            var textureScale: Float,
            var textureOrientation: Float
        )
    }

    private lateinit var cameraSurfaceTexture: SurfaceTexture
    private val cameraTexture = Texture()
    private val updateCameraTexture = AtomicBoolean(true)
    private val maskTexture = Texture()
    private val offScreenFrameBuffer = FrameBuffer()
    private val offscreenMVP = FloatArray(16)
    private val renderMVP = FloatArray(16)
    private val recordMVP = FloatArray(16)
    private val spriteBuffer = PolySpriteBuffer()

    private val spriteHolder = SpriteHolder(
        leftEye = LandmarkSpriteInfo(
            position = PointF(Float.MIN_VALUE, Float.MAX_VALUE),
            size = eyeSize
        ),
        rightEye = LandmarkSpriteInfo(
            position = PointF(Float.MIN_VALUE, Float.MAX_VALUE),
            size = eyeSize
        ),
        mouth = LandmarkSpriteInfo(
            position = PointF(Float.MIN_VALUE, Float.MAX_VALUE),
            size = mouthSize
        ),
        mask = Sprite(
            position = Position(width / 2.0f, height / 2.0f),
            size = Size(1.0f, 1.0f)
        ),
        framebuffer = Sprite(
            size = Size(width.toFloat(), height.toFloat()),
            // Rendered surface result in a inverted texture, fix it now.
            texture = STUV(0.0f, 1.0f, 1.0f, 0.0f)
        ),
        draw = false,
        textureScale = 1.0f,
        textureOrientation = 0.0f
    )

    private var lastRenderTime = 0L
    private var backgroundColor = Color()
    private var screenWidth = 0
    private var screenHeight = 0
    private var recorderWidth = 0
    private var recorderHeight = 0

    fun setUpdateTexture(update: Boolean) {
        updateCameraTexture.set(update)
    }

    fun getCameraSurfaceTexture(): SurfaceTexture {
        return cameraSurfaceTexture
    }

    override fun onSurfaceCreated() {
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        spriteBuffer.initialize()
        offScreenFrameBuffer.create(width, height)
        cameraTexture.createOES(width, height, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE)
        maskTexture.createRGB565(1, 1, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE)

        Matrix.orthoM(offscreenMVP, 0, 0.0f, offScreenFrameBuffer.width.toFloat(), offScreenFrameBuffer.height.toFloat(), 0.0f, 0.0f, 1.0f)

        cameraSurfaceTexture = SurfaceTexture(cameraTexture.textureId)
        surfaceView?.let {
            // Set the background color of the renderer equals to the system theme
            val color = MaterialColors.getColor(it.context, android.R.attr.windowBackground, android.graphics.Color.BLACK)
            backgroundColor = Color(color.red, color.green, color.blue, color.alpha)
        }
    }

    override fun onSurfaceDestroyed() {
        cameraSurfaceTexture.release()
        cameraTexture.destroy()
        maskTexture.destroy()
        offScreenFrameBuffer.destroy()
        spriteBuffer.destroy()
        surfaceView = null
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        Matrix.orthoM(renderMVP, 0, 0.0f, screenWidth.toFloat(), screenHeight.toFloat(), 0.0f, 0.0f, 1.0f)
    }

    override fun onDrawFrame() {
        val now = System.currentTimeMillis()
        val delta = (now - lastRenderTime) / 1000.0f
        lastRenderTime = now

        // Render camera and sprites in a offscreen framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offScreenFrameBuffer.frameBufferId)
        GLES20.glViewport(0, 0, offScreenFrameBuffer.width, offScreenFrameBuffer.height)
        GLES20.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Render the face mask
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture.textureId)
        spriteBuffer.run {
            bind()
            spriteShader.setTexture(0)
            spriteShader.setMatrix(offscreenMVP)

            reset()
            putSprite(spriteHolder.mask)
            render()
        }

        if (spriteHolder.draw) {
            if (updateCameraTexture.get()) {
                // Update the camera texture with latest data when available (this will also bind the texture)
                cameraSurfaceTexture.updateTexImage()
            } else {
                // Don't update the texture and just bind it
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture.textureId)
            }

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendEquationSeparate(GLES20.GL_FUNC_ADD, GLES20.GL_FUNC_ADD)
            GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            spriteBuffer.run {
                bindOES()
                spriteShaderOES.setTexture(0)
                spriteShaderOES.setMatrix(offscreenMVP)

                reset()
                computeUserSpriteTextureCenter(spriteHolder.leftEye, delta)
                drawUserLandmark(spriteHolder.leftEye, spriteHolder.textureOrientation, spriteHolder.textureScale)

                computeUserSpriteTextureCenter(spriteHolder.rightEye, delta)
                drawUserLandmark(spriteHolder.rightEye, spriteHolder.textureOrientation, spriteHolder.textureScale)

                computeUserSpriteTextureCenter(spriteHolder.mouth, delta)
                drawUserLandmark(spriteHolder.mouth, spriteHolder.textureOrientation, spriteHolder.textureScale)
                render()
            }            
        }

        // Render offscreen buffer on the device screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
        GLES20.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offScreenFrameBuffer.textureId)
        GLES20.glDisable(GLES20.GL_BLEND)

        spriteHolder.framebuffer.run {
            val scale = when (imageZoom) {
                true -> screenWidth / size.w   // Needed to fill the entire screen space in height
                else -> screenHeight / size.h     // Needed to fill the entire screen space in width
            }

            // Keep aspect ratio by applying same scaling value to X and Y and center on screen
            setScale(scale, scale)
            setPosition(screenWidth * 0.5f, screenHeight * 0.5f)
        }

        spriteBuffer.run {
            bind()
            spriteShader.setTexture(0)
            spriteShader.setMatrix(renderMVP)
            reset()
            putSprite(spriteHolder.framebuffer)
            render()
        }
    }

    override fun onStartRecorder(width: Int, height: Int) {
        recorderWidth = width
        recorderHeight = height
        Matrix.orthoM(recordMVP, 0, 0.0f, width.toFloat(), height.toFloat(), 0.0f, 0.0f, 1.0f)
    }

    override fun onRecordFrame() {
        // Render offscreen buffer directly, don't redraw everything
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, recorderWidth, recorderHeight)
        GLES20.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offScreenFrameBuffer.textureId)
        GLES20.glDisable(GLES20.GL_BLEND)

        spriteHolder.framebuffer.run {
            val scale = when (imageZoom) {
                true -> recorderWidth / size.w   // Needed to fill the entire screen space in height
                else -> recorderHeight / size.h     // Needed to fill the entire screen space in width
            }

            // Keep aspect ratio by applying same scaling value to X and Y and center on screen
            setScale(scale, scale)
            setPosition(recorderWidth * 0.5f, recorderHeight * 0.5f)
        }

        spriteBuffer.run {
            bind()
            spriteShader.setTexture(0)
            spriteShader.setMatrix(recordMVP)
            reset()
            putSprite(spriteHolder.framebuffer)
            render()
        }
    }

    private fun drawUserLandmark(spriteInfo: LandmarkSpriteInfo, orientation: Float, textureScale: Float) {
        val texelWidth = 1.0f / cameraTexture.height
        val texelHeight = 1.0f / cameraTexture.width

        val width = spriteInfo.size.w * 0.5f
        val height = spriteInfo.size.h * 0.5f
        val widthInTexture = (spriteInfo.size.w * texelWidth) * 0.5f
        val heightInTexture = (spriteInfo.size.h * texelHeight) * 0.5f

        // Depending if the front or back camera is used, texture orientation
        // is mirrored and we need to adjust it
        val xPositionInTexture = when {
            isFrontCamera -> 1.0f - spriteInfo.texturePosition.x / cameraTexture.height
            else -> spriteInfo.texturePosition.x / cameraTexture.height
        }

        val yPositionInTexture = when {
            isFrontCamera -> spriteInfo.texturePosition.y / cameraTexture.width
            else -> 1.0f - spriteInfo.texturePosition.y / cameraTexture.width
        }

        var step = 0.0f
        while (step < CIRCLE_PRECISION) {
            val angle = step * STEP_SIZE
            val s = FastMath.sin(angle)
            val c = FastMath.cos(angle)
            val s1 = FastMath.sin(angle + STEP_SIZE)
            val c1 = FastMath.cos(angle + STEP_SIZE)

            spriteBuffer.putVertex(
                0.0f,
                0.0f,
                xPositionInTexture,
                yPositionInTexture,
                1.0f,
                1.0f,
                1.0f,
                // Saturate the alpha component of the center vertex. This allow for a greater blending of the contour
                // and a better transparency in the middle of the sprite.
                1.8f,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                orientation + spriteInfo.orientation,
                spriteInfo.position.x,
                spriteInfo.position.y
            )

            spriteBuffer.putVertex(
                s * width,
                c * height,
                xPositionInTexture + (s * widthInTexture) * textureScale,
                yPositionInTexture + (c * heightInTexture) * textureScale,
                1.0f,
                1.0f,
                1.0f,
                0.0f,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                orientation + spriteInfo.orientation,
                spriteInfo.position.x,
                spriteInfo.position.y
            )

            spriteBuffer.putVertex(
                s1 * width,
                c1 * height,
                xPositionInTexture + (s1 * widthInTexture) * textureScale,
                yPositionInTexture + (c1 * heightInTexture) * textureScale,
                1.0f,
                1.0f,
                1.0f,
                0.0f,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                orientation + spriteInfo.orientation,
                spriteInfo.position.x,
                spriteInfo.position.y
            )

            step++
        }
    }

    fun updateUserLandmarks(orientation: Float, scale: Float, face: Face?) {
        spriteHolder.draw = face != null
        spriteHolder.textureOrientation = orientation
        spriteHolder.textureScale = scale
        if (updateCameraTexture.get()) {
            face?.getLandmark(FaceLandmark.LEFT_EYE)?.let { landmark ->
                spriteHolder.leftEye.textureNewPosition.set(
                    landmark.position.x,
                    landmark.position.y
                )
            }
            face?.getLandmark(FaceLandmark.RIGHT_EYE)?.let { landmark ->
                spriteHolder.rightEye.textureNewPosition.set(
                    landmark.position.x,
                    landmark.position.y
                )
            }
            face?.getLandmark(FaceLandmark.MOUTH_LEFT)?.let { llandmark ->
                face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.let { rlandmark ->
                    spriteHolder.mouth.textureNewPosition.set(
                        MathUtils.lerp(llandmark.position.x, rlandmark.position.x, 0.5f),
                        MathUtils.lerp(llandmark.position.y, rlandmark.position.y, 0.5f)
                    )
                }
            }
        }
    }

    fun setMask(mask: Mask, bitmap: Bitmap, face: Face) {
        surfaceView?.queueEvent {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture.textureId)
            spriteHolder.mask.setSize(bitmap.width.toFloat(), bitmap.height.toFloat())
            maskTexture.setPixels(bitmap, true)

            // Add an offset to each position to center it on the render surface, to match
            // the landmark position of the current mask
            val offsetX = (spriteHolder.framebuffer.size.w * 0.5f) - (spriteHolder.mask.size.w * 0.5f)
            val offsetY = (spriteHolder.framebuffer.size.h * 0.5f) - (spriteHolder.mask.size.h * 0.5f)

            face.getLandmark(FaceLandmark.RIGHT_EYE)?.let { landmark ->
                spriteHolder.rightEye.apply {
                    scale = mask.scale
                    orientation = face.headEulerAngleZ
                    position.set(
                        landmark.position.x + offsetX,
                        landmark.position.y + offsetY
                    )
                }
            }

            face.getLandmark(FaceLandmark.LEFT_EYE)?.let { landmark ->
                spriteHolder.leftEye.apply {
                    scale = mask.scale
                    orientation = face.headEulerAngleZ
                    position.set(
                        landmark.position.x + offsetX,
                        landmark.position.y + offsetY
                    )
                }
            }

            face.getLandmark(FaceLandmark.MOUTH_LEFT)?.let { llandmark ->
                face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.let { rlandmark ->
                    spriteHolder.mouth.apply {
                        scale = mask.scale
                        orientation = face.headEulerAngleZ
                        // Interpolate horizontal and vertical mouth position to find where to put the sprite
                        position.set(
                            ((llandmark.position.x + rlandmark.position.x) * 0.5f) + offsetX,
                            ((llandmark.position.y + rlandmark.position.y) * 0.5f) + offsetY
                        )
                    }
                }
            }
        }
    }

    fun getBitmap(onBitmapReady: (bitmap: Bitmap) -> Unit) {
        surfaceView?.queueEvent {
            val imageWidth = when {
                spriteHolder.mask.size.w > offScreenFrameBuffer.width -> offScreenFrameBuffer.width
                else -> spriteHolder.mask.size.w.toInt()
            }
            val imageHeight = when {
                spriteHolder.mask.size.h > offScreenFrameBuffer.height -> offScreenFrameBuffer.height
                else -> spriteHolder.mask.size.h.toInt()
            }

            val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            val buffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4)

            // We don't take the whole framebuffer, instead, we crop to the image size
            // from the center of it
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offScreenFrameBuffer.frameBufferId)
            GLES20.glReadPixels(
                (spriteHolder.mask.position.x - (imageWidth * 0.5f)).toInt(),
                (spriteHolder.mask.position.y - (imageHeight * 0.5f)).toInt(),
                imageWidth,
                imageHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )

            bitmap.copyPixelsFromBuffer(buffer)

            // We also need to mirror the result bitmap in the Y axis
            val matrix = android.graphics.Matrix().also {
                it.postScale(1.0f, -1.0f)
            }

            // If we don't get the same bitmap, we recycle the old one
            val resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (!resultBitmap.sameAs(bitmap)) {
                bitmap.recycle()
            }

            onBitmapReady(resultBitmap)
        }
    }

    private fun computeUserSpriteTextureCenter(spriteInfo: LandmarkSpriteInfo, delta: Float) {
        if (updateCameraTexture.get()) {
            spriteInfo.apply {
                //val distance = MathUtils.dist(spriteInfo.position.x, spriteInfo.position.y, textureTargetPositionX, textureTargetPositionY)
                when (texturePosition.x) {
                    0.0f /* || distance > 256 */ -> {
                        texturePosition.set(
                            textureNewPosition.y,
                            textureNewPosition.x
                        )
                    }
                    else -> {
                        // If we moved just a bit, move the sprites to the right place using a linear interpolation
                        texturePosition.set(
                            MathUtils.lerp(texturePosition.x, textureNewPosition.y, LERP_VALUE * delta),
                            MathUtils.lerp(texturePosition.y, textureNewPosition.x, LERP_VALUE * delta)
                        )
                    }
                }
            }
        }
    }

}

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
package com.notnotme.memegl.renderer.graphic

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PolySpriteBuffer {

    companion object {
        const val TAG = "PolySpriteBuffer"

        const val PI180 = (Math.PI / 180.0f).toFloat()
        const val CAPACITY = 256 // 256 is enough for our use case. // Short.MAX_VALUE

        const val POSITION_PER_VERTEX = 2 // X,Y
        const val TEXTURE_PER_VERTEX = 2 // U,V
        const val COLOR_PER_VERTEX = 4 // R,G,B,A
        const val TRANSLATE_PER_VERTEX = 2 // TX,TY
        const val SCALE_PER_VERTEX = 2 // SX,SY
        const val ROTATE_PER_VERTEX = 1 // Angle

        const val BYTES_PER_POSITION = POSITION_PER_VERTEX * Float.SIZE_BYTES
        const val BYTES_PER_TEXTURE = TEXTURE_PER_VERTEX * Float.SIZE_BYTES
        const val BYTES_PER_COLOR = COLOR_PER_VERTEX * Float.SIZE_BYTES
        const val BYTES_PER_TRANSLATE = TRANSLATE_PER_VERTEX * Float.SIZE_BYTES
        const val BYTES_PER_SCALE = SCALE_PER_VERTEX * Float.SIZE_BYTES
        const val BYTES_PER_ROTATE = ROTATE_PER_VERTEX * Float.SIZE_BYTES

        const val BYTES_PER_VERTEX = (BYTES_PER_POSITION + BYTES_PER_TEXTURE + BYTES_PER_COLOR
                + BYTES_PER_TRANSLATE + BYTES_PER_SCALE + BYTES_PER_ROTATE)

        data class Sprite(
            var position: Position = Position(),
            var size: Size = Size(),
            var texture: STUV = STUV(),
            var color: Color = Color(),
            var scale: Scale = Scale(),
            var rotation: Float = 0.0f
        )

        fun Sprite.setSTUV(s: Float, t: Float, u: Float, v: Float) {
            apply {
                texture.s = s; texture.t = t; texture.u = u; texture.v = v
            }
        }

        fun Sprite.setColor(r: Float, g: Float, b: Float, a: Float) {
            apply {
                color.r = r; color.g = g; color.b = b; color.a = a
            }
        }

        fun Sprite.setScale(xScale: Float, yScale: Float) {
            apply {
                scale.x = xScale; scale.y = yScale
            }
        }

        fun Sprite.setSize(width: Float, height: Float) {
            apply {
                size.w = width; size.h = height
            }
        }

        fun Sprite.setPosition(x: Float, y: Float) {
            apply {
                position.x = x; position.y = y
            }
        }
    }

    private val vertexPositionOffset: FloatBuffer
    private val vertexTextureOffset: FloatBuffer
    private val vertexColorOffset: FloatBuffer
    private val vertexTranslateOffset: FloatBuffer
    private val vertexScaleOffset: FloatBuffer
    private val vertexRotationOffset: FloatBuffer
    private var numberOfVertices = 0

    private val vertexBuffer: ByteBuffer = ByteBuffer.allocateDirect(CAPACITY * BYTES_PER_VERTEX)
        .order(ByteOrder.nativeOrder())
        .also {
            vertexPositionOffset = it.asFloatBuffer();
            it.position(BYTES_PER_POSITION)

            vertexTextureOffset = it.asFloatBuffer();
            it.position(BYTES_PER_POSITION + BYTES_PER_TEXTURE)

            vertexColorOffset = it.asFloatBuffer();
            it.position(BYTES_PER_POSITION + BYTES_PER_TEXTURE + BYTES_PER_COLOR)

            vertexTranslateOffset = it.asFloatBuffer();
            it.position(BYTES_PER_POSITION + BYTES_PER_TEXTURE + BYTES_PER_COLOR + BYTES_PER_TRANSLATE)

            vertexScaleOffset = it.asFloatBuffer();
            it.position(BYTES_PER_POSITION + BYTES_PER_TEXTURE + BYTES_PER_COLOR + BYTES_PER_TRANSLATE + BYTES_PER_SCALE)

            vertexRotationOffset = it.asFloatBuffer()
            it.position(0)
        }

    private val _spriteShader = SpriteShader()
    val spriteShader get() = _spriteShader

    private val _spriteShaderOES = SpriteShaderOES()
    val spriteShaderOES get() = _spriteShaderOES

    /**
     * Initialize the resources used by the SpriteBatch
     */
    fun initialize() {
        _spriteShader.create()
        _spriteShaderOES.create()
    }

    /**
     * Release the resources used by the SpriteBatch
     */
    fun destroy() {
        _spriteShader.destroy()
        _spriteShaderOES.destroy()
        vertexBuffer.clear()
    }

    /**
     * Bind the SpriteBuffer
     */
    fun bind() {
        bind(spriteShader)
    }

    /**
     * Bind the SpriteBuffer to be used with camera texture
     */
    fun bindOES() {
        bind(spriteShaderOES)
    }

    /**
     * Bind the SpriteBuffer using a SpriteShaderBase
     */
    fun bind(spriteShaderBase: SpriteShaderBase) {
        GLES20.glUseProgram(spriteShaderBase.programId)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexPositionLocation)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexTextureLocation)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexColorLocation)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexTranslateLocation)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexScaleLocation)
        GLES20.glEnableVertexAttribArray(spriteShaderBase.vertexRotationLocation)

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexPositionLocation,
            POSITION_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexPositionOffset
        )

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexTextureLocation,
            TEXTURE_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexTextureOffset
        )

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexColorLocation,
            COLOR_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexColorOffset
        )

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexTranslateLocation,
            TRANSLATE_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexTranslateOffset
        )

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexScaleLocation,
            SCALE_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexScaleOffset
        )

        GLES20.glVertexAttribPointer(
            spriteShaderBase.vertexRotationLocation,
            ROTATE_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            BYTES_PER_VERTEX,
            vertexRotationOffset
        )
    }

    /**
     * Prepare updating the buffer for a new frame
     */
    fun reset() {
        numberOfVertices = 0
    }

    /**
     * Put  a Sprite into the buffer
     */
    fun putSprite(sprite: Sprite) {
        putVertex(
            -sprite.size.w, -sprite.size.h,
            sprite.texture.s, sprite.texture.t,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )

        putVertex(
            -sprite.size.w, sprite.size.h,
            sprite.texture.s, sprite.texture.v,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )

        putVertex(
            sprite.size.w, sprite.size.h,
            sprite.texture.u, sprite.texture.v,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )

        putVertex(
            sprite.size.w, sprite.size.h,
            sprite.texture.u, sprite.texture.v,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )

        putVertex(
            sprite.size.w, -sprite.size.h,
            sprite.texture.u, sprite.texture.t,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )

        putVertex(
            -sprite.size.w, -sprite.size.h,
            sprite.texture.s, sprite.texture.t,
            sprite.color.r, sprite.color.g, sprite.color.b, sprite.color.a,
            sprite.scale.x, sprite.scale.y,
            sprite.rotation,
            sprite.position.x, sprite.position.y
        )
    }


    /**
     * Put  a Vertex into the buffer
     */
    fun putVertex(
        x: Float,
        y: Float,
        s: Float,
        t: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        sx: Float,
        sy: Float,
        rotation: Float,
        tx: Float,
        ty: Float
    ) {
        if (numberOfVertices >= CAPACITY) {
            Log.e(TAG, "PolySpriteBuffer overflow")
            return
        }

        vertexBuffer.apply {
            putFloat(x)
            putFloat(y)
            putFloat(s)
            putFloat(t)
            putFloat(r)
            putFloat(g)
            putFloat(b)
            putFloat(a)
            putFloat(tx)
            putFloat(ty)
            putFloat(sx)
            putFloat(sy)
            putFloat(rotation * PI180)
        }

        numberOfVertices++
    }

    /**
     * Render the buffer.
     */
    fun render() {
        vertexBuffer.position(0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numberOfVertices)
    }

}

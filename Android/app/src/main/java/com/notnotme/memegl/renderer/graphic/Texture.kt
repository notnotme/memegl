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

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils

class Texture {

    companion object {
        const val TAG = "Texture"
    }

    private var _width: Int = 0
    val width get() = _width

    private var _height: Int = 0
    val height get() = _height

    private var _textureId: IntArray = intArrayOf(-1)
    val textureId get() = _textureId[0]

    /**
     * Create an empty OES texture
     * @param filter One of the value GL_TEXTURE_*_FILTER
     * @param repeat One of the values GL_TEXTURE_WRAP_*
     */
    fun createOES(width: Int, height: Int,filter: Int = GLES20.GL_NEAREST, repeat: Int = GLES20.GL_REPEAT) {
        _width = width
        _height = height
        generate(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, filter, repeat)
    }

    /**
     * Create an empty RGB 565 texture
     * @param width The width of the texture in pixels
     * @param height The height of the texture in pixels
     * @param filter One of the value GL_TEXTURE_*_FILTER
     * @param repeat One of the values GL_TEXTURE_WRAP_*
     */
    fun createRGB565(width: Int, height: Int, filter: Int = GLES20.GL_NEAREST, repeat: Int = GLES20.GL_REPEAT) {
        _width = width
        _height = height
        generate(GLES20.GL_TEXTURE_2D, filter, repeat)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null)
    }

    /**
     * Create an empty RGBA 8888 texture
     * @param width The width of the texture in pixels
     * @param height The height of the texture in pixels
     * @param filter One of the value GL_TEXTURE_*_FILTER
     * @param repeat One of the values GL_TEXTURE_WRAP_*
     */
    fun createRGBA8888(width: Int, height: Int, filter: Int = GLES20.GL_NEAREST, repeat: Int = GLES20.GL_REPEAT) {
        _width = width
        _height = height
        generate(GLES20.GL_TEXTURE_2D, filter, repeat)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    }

    /**
     * Upload a bitmap into the texture.
     * You need to bind texture before use
     */
    fun setPixels(bitmap: Bitmap, recycle: Boolean) {
        _width = bitmap.width
        _height = bitmap.height

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        if (recycle) {
            bitmap.recycle()
        }
    }

    /**
     * Release the texture resource
     */
    fun destroy() {
        GLES20.glDeleteTextures(1, _textureId, 0)
        _textureId[0] = -1
    }

    /**
     * Generate a new texture
     * @param filter One of the value GL_TEXTURE_*_FILTER
     * @param repeat One of the values GL_TEXTURE_WRAP_*
     */
    private fun generate(target: Int, filter: Int = GLES20.GL_NEAREST, repeat: Int = GLES20.GL_REPEAT) {
        // Create the texture
        GLES20.glGenTextures(1, _textureId, 0)
        GLES20.glBindTexture(target, _textureId[0])
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, filter)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, filter)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, repeat)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, repeat)
    }

}

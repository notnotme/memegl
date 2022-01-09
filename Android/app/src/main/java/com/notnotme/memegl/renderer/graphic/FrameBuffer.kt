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

class FrameBuffer {

    companion object {
        const val TAG = "FrameBuffer"
    }

    private val _texture: Texture = Texture()
    val textureId get() = _texture.textureId

    val width get() = _texture.width

    val height get() = _texture.height

    private var _frameBufferId: IntArray = intArrayOf(-1)
    val frameBufferId get() = _frameBufferId[0]

    private var _renderBufferId: IntArray = intArrayOf(-1)
    val renderBufferId get() = _renderBufferId[0]

    fun create(width: Int, height: Int) {
        _texture.createRGB565(width, height, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glGenFramebuffers(1, _frameBufferId, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, _frameBufferId[0])

        GLES20.glGenRenderbuffers(1, _renderBufferId, 0)
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, _renderBufferId[0])

        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, _renderBufferId[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, _texture.textureId, 0)

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            error("Incomplete FrameBuffer")
        }
    }

    fun destroy() {
        GLES20.glDeleteFramebuffers(1, _frameBufferId, 0)
        GLES20.glDeleteRenderbuffers(1, _renderBufferId, 0)
        _frameBufferId[0] = -1
        _renderBufferId[0] = -1
        _texture.destroy()
    }

}
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

abstract class SpriteShaderBase : ProgramShader() {

    companion object {
        const val TAG = "SpriteShaderBase"
    }

    var matrixUniform: Int = -1
        protected set

    var textureUniform: Int = -1
        protected set

    var vertexPositionLocation: Int = -1
        protected set

    var vertexColorLocation: Int = -1
        protected set

    var vertexTextureLocation: Int = -1
        protected set

    var vertexTranslateLocation: Int = -1
        protected set

    var vertexScaleLocation: Int = -1
        protected set

    var vertexRotationLocation: Int = -1
        protected set

    /**
     * Set the matrix to use in the shader
     * The ProgramShader need to be in use before using this function
     * @param mat A FloatArray of 16 element that represent a 4x4 matrix
     */
    fun setMatrix(mat: FloatArray) {
        GLES20.glUniformMatrix4fv(matrixUniform, 1, false, mat, 0)
    }

    /**
     * Tell the ProgramShader to use the textureId as image source to render the sprites
     * The ProgramShader need to be in use before using this function
     * @param textureId The textureId to use for drawing. Must be bind to slot #0
     */
    fun setTexture(textureId: Int) {
        GLES20.glUniform1i(textureUniform, textureId)
    }

}
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

open class SpriteShaderOES : SpriteShader() {

    companion object {
        const val TAG = "SpriteShaderOES"

        const val fragmentShaderSource =
            """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_texCoords;
            varying vec4 v_color;
            uniform samplerExternalOES  u_texture;
            
            void main()
            {
                gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
            }
            """
    }

    override fun create() {
        create(vertexShaderSource, fragmentShaderSource)

        // Get the program uniforms/attributes location
        matrixUniform = GLES20.glGetUniformLocation(programId, "uMat")
        textureUniform = GLES20.glGetUniformLocation(programId, "u_texture")
        vertexPositionLocation = GLES20.glGetAttribLocation(programId, "vPosition")
        vertexColorLocation = GLES20.glGetAttribLocation(programId, "vColor")
        vertexTextureLocation = GLES20.glGetAttribLocation(programId, "vTexture")
        vertexTranslateLocation = GLES20.glGetAttribLocation(programId, "vTranslate")
        vertexScaleLocation = GLES20.glGetAttribLocation(programId, "vScale")
        vertexRotationLocation = GLES20.glGetAttribLocation(programId, "vRotate")
    }

}
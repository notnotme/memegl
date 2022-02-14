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

open class SpriteShader : SpriteShaderBase() {

    companion object {
        const val TAG = "SpriteShader"

        const val vertexShaderSource =
            """
            precision mediump float;
            uniform mat4 uMat;
            attribute vec2 vPosition;
            attribute vec4 vColor;
            attribute vec2 vTexture;
            attribute vec2 vTranslate;
            attribute vec2 vScale;
            attribute float vRotate;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            
            void main() {
                mat3 rotate_mat = mat3
                    (cos(vRotate), -sin(vRotate), 0.0,
                     sin(vRotate), cos(vRotate), 0.0,
                     0.0, 0.0, 1.0);
                
                mat3 scale_mat = mat3
                    (vScale.x, 0.0, 0.0,
                     0.0, vScale.y, 0.0,
                     0.0, 0.0, 1.0);
                
                mat3 translate_mat = mat3
                    (1.0, 0.0, 0.0,
                     0.0, 1.0, 0.0,
                     vTranslate.x, vTranslate.y, 0.0);
                
                vec3 transformed = translate_mat * rotate_mat * scale_mat * vec3(vPosition, 1.0);
                gl_Position = uMat * vec4(transformed, 1.0);
                v_color = vColor;
                v_texCoords = vTexture;
            }
            """

        const val fragmentShaderSource =
            """
            precision mediump float;
            varying vec2 v_texCoords;
            varying vec4 v_color;
            uniform sampler2D u_texture;
            
            void main()
            {
                gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
            }
            """
    }

    /**
     * Create the SpriteShader by calling create(src,src)
     */
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
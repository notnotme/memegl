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

abstract class ProgramShader {

    companion object {
        const val TAG = "ProgramShader"
    }

    private var _programId: Int = -1
    val programId get() = _programId

    /**
     * Inherit and override this function to load your own shader's sources
     */
    abstract fun create()

    /**
     * Create a ProgramShader from vertex and fragment source code
     * @param vertexSrc Vertex shader source code
     * @param fragmentSrc Fragment shader source code
     */
    protected fun create(vertexSrc: String, fragmentSrc: String) {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)

        _programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(_programId, vertexShader)
        GLES20.glAttachShader(_programId, fragmentShader)
        GLES20.glLinkProgram(_programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(_programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val error = GLES20.glGetProgramInfoLog(_programId)
            GLES20.glDeleteProgram(_programId)
            error("Could not link program: $error")
        }

        // Shader are not needed anymore
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }

    /**
     * Destroy this ProgramShader and release resources
     */
    fun destroy() {
        GLES20.glDeleteProgram(_programId)
        _programId = -1
    }

    /**
     * Compile a shader of specified type
     * @param type The type of Shader to compile
     * @param source The source code of the shader
     */
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Could not compile shader ${String.format("0X%4X", type)} : $error")
        }

        return shader
    }

}
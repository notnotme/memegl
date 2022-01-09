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

import OpenGLES
import GLKit

final class SpriteProgram {

    private static let vertexShaderSource =
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
            mat3 scale_mat = mat3
                (vScale.x,  0.0,            0.0,
                 0.0,           vScale.y,   0.0,
                 0.0,           0.0,            1.0);
                 
            mat3 rotate_mat = mat3
                (cos(vRotate), sin(vRotate), 0.0,
                -sin(vRotate), cos(vRotate), 0.0,
                 0.0,          0.0,          1.0);
                 
            mat4 translate_mat = mat4
                (1.0,          0.0,          0.0, 0.0,
                 0.0,          1.0,          0.0, 0.0,
                 0.0,          0.0,          1.0, 0.0,
                 vTranslate.x, vTranslate.y, 0.0, 1.0);
                
            gl_Position  = uMat * translate_mat * vec4(rotate_mat * scale_mat * vec3(vPosition.xy, 0.0), 1.0);
            v_color      = vColor;
            v_texCoords  = vTexture;
        }
        """
    
    private static let fragmentShaderSource =
        """
        precision mediump float;
        varying vec2 v_texCoords;
        varying vec4 v_color;
        uniform sampler2D u_texture;
        
        void main()
        {
          // Let's do color swizzling to ease video encoding
          gl_FragColor = v_color * texture2D(u_texture, v_texCoords).bgra;
        }
        """
    
    private var programShader = ProgramShader()
    private var _matrixUniform: GLint = -1
    private var _textureUniform: GLint = -1
    private var _vertexPositionLocation: GLint = -1
    private var _vertexColorLocation: GLint = -1
    private var _vertexTextureLocation: GLint = -1
    private var _vertexTranslateLocation: GLint = -1
    private var _vertexScaleLocation: GLint = -1
    private var _vertexRotationLocation: GLint = -1

    public var id: GLuint {
        get {
            return programShader.id
        }
    }
    
    public var matrixUniform: GLint {
        get {
            return _matrixUniform
        }
    }

    public var textureUniform: GLint {
        get {
            return _textureUniform
        }
    }
    
    public var vertexPositionLocation: GLint {
        get {
            return _vertexPositionLocation
        }
    }
    
    public var vertexColorLocation: GLint {
        get {
            return _vertexColorLocation
        }
    }
    
    public var vertexTextureLocation: GLint {
        get {
            return _vertexTextureLocation
        }
    }
    
    public var vertexTranslateLocation: GLint {
        get {
            return _vertexTranslateLocation
        }
    }
    
    public var vertexScaleLocation: GLint {
        get {
            return _vertexScaleLocation
        }
    }
    
    public var vertexRotationLocation: GLint {
        get {
            return _vertexRotationLocation
        }
    }
    
    public func create() {
        programShader.create()
        programShader.linkProgram(
            Self.vertexShaderSource,
            Self.fragmentShaderSource
        )

        let programId = programShader.id
        _matrixUniform = glGetUniformLocation(programId, "uMat")
        _textureUniform = glGetUniformLocation(programId, "u_texture")
        _vertexPositionLocation = glGetAttribLocation(programId, "vPosition")
        _vertexColorLocation = glGetAttribLocation(programId, "vColor")
        _vertexTextureLocation = glGetAttribLocation(programId, "vTexture")
        _vertexTranslateLocation = glGetAttribLocation(programId, "vTranslate")
        _vertexScaleLocation = glGetAttribLocation(programId, "vScale")
        _vertexRotationLocation = glGetAttribLocation(programId, "vRotate")
    }
    
    public func destroy() {
        programShader.destroy()
    }

    public func setMatrix(_ mat: inout GLKMatrix4) {
        glUniformMatrix4fv(_matrixUniform, 1, GLboolean(GL_FALSE), &mat.m.0)
    }

    public func setTexture(_ textureId: GLint) {
        glUniform1i(_textureUniform, textureId)
    }
    
}

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

struct ProgramShader {

    private var _id: GLuint = 0
    
    public var id: GLuint {
        get {
            return _id
        }
    }

}

extension ProgramShader {
    
    public mutating func create() {
        _id = glCreateProgram()
    }
    
    public mutating func destroy() {
        glDeleteProgram(_id)
        _id = 0
    }
    
    func linkProgram(_ vertexSource: String, _ fragmentSource: String) {
        let vertexShader = Self.compileShader(GLenum(GL_VERTEX_SHADER), vertexSource)
        let fragmentShader = Self.compileShader(GLenum(GL_FRAGMENT_SHADER), fragmentSource)
        defer {
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
        }

        glAttachShader(_id, vertexShader)
        glAttachShader(_id, fragmentShader)
        glLinkProgram(_id)
        
        var linked = GLint(0)
        glGetProgramiv(_id, GLenum(GL_LINK_STATUS), &linked)
        
        guard linked != GL_FALSE else {
            var logLength = GLint(0)
            glGetProgramiv(_id, GLenum(GL_INFO_LOG_LENGTH), &logLength)
            
            var infoLog: [GLchar] = [GLchar](repeating: 0, count: Int(logLength))
            glGetProgramInfoLog(_id, logLength, nil, &infoLog)
            
            let error = NSString(
                bytes: infoLog,
                length: Int(logLength),
                encoding: String.Encoding.ascii.rawValue
            )

            fatalError("Could not compile program shader : \(error ?? "Unknown error")")
        }
    }
    
    static internal func compileShader(_ type: GLenum, _ source: String) -> GLuint {
        let shader = glCreateShader(type)
        var sourceC = (source as NSString).utf8String
        glShaderSource(shader, 1, &sourceC, nil)
        glCompileShader(shader)

        var compiled = GLint(0)
        glGetShaderiv(shader, GLenum(GL_COMPILE_STATUS), &compiled)
        
        guard compiled != GL_FALSE else {
            var logLength = GLint(0)
            glGetShaderiv(shader, GLenum(GL_INFO_LOG_LENGTH), &logLength)
            
            var infoLog: [GLchar] = [GLchar](repeating: 0, count: Int(logLength))
            glGetShaderInfoLog(shader, logLength, nil, &infoLog)
            
            let error = NSString(
                bytes: infoLog,
                length: Int(logLength),
                encoding: String.Encoding.ascii.rawValue
            )

            glDeleteShader(shader)
            fatalError("Could not compile shader : \(error ?? "Unknown error")")
        }

       return shader
    }

}

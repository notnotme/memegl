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
import CoreGraphics

struct Texture {
    
    private var _id: GLuint = 0
    private var _width: Int = 0
    private var _height: Int = 0

    public var id: GLuint {
        get {
            return _id
        }
    }
    
    public var width: Int {
        get {
            return _width
        }
    }
    
    public var height: Int {
        get {
            return _height
        }
    }
    
}

extension Texture {

    private mutating func generate(_ target: GLenum, _ filter: GLint, _ wrap: GLint) {
        glGenTextures(1, &_id)
        glBindTexture(target, _id)
        glTexParameteri(target, GLenum(GL_TEXTURE_MIN_FILTER), filter)
        glTexParameteri(target, GLenum(GL_TEXTURE_MAG_FILTER), filter)
        glTexParameteri(target, GLenum(GL_TEXTURE_WRAP_S), wrap)
        glTexParameteri(target, GLenum(GL_TEXTURE_WRAP_T), wrap)
        
        guard _id != 0 else {
            fatalError("Can't generate OpenGL texture.")
        }
    }
    
    public mutating func create(_ filter: GLint, _ wrap: GLint) {
        generate(GLenum(GL_TEXTURE_2D), GLint(filter), GLint(wrap))
    }
    
    public mutating func destroy() {
        glDeleteTextures(1, &_id)
        _id = 0
    }
    
    public mutating func setPixels(_ image: CGImage) {
        _width = image.width
        _height = image.height

        guard let dataProvider = image.dataProvider else {
            fatalError("Nil image.dataProvider.")
        }
        
        let pixelData = CFDataCreateMutableCopy(
            kCFAllocatorDefault,
            0,
            dataProvider.data
        )
        
        // Don't stall
        
        glTexImage2D(
            GLenum(GL_TEXTURE_2D),
            0,
            GL_RGBA,
            GLsizei(_width),
            GLsizei(_height),
            0,
            GLenum(GL_RGBA),
            GLenum(GL_UNSIGNED_BYTE),
            nil
        )
        
        glTexSubImage2D(
            GLenum(GL_TEXTURE_2D),
            0,
            GLint(0),
            GLint(0),
            GLint(_width),
            GLint(_height),
            GLenum(GL_RGBA),
            GLenum(GL_UNSIGNED_BYTE),
            CFDataGetMutableBytePtr(pixelData)
        )
    }
        
}

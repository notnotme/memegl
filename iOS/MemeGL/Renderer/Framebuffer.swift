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
import CoreMedia

struct Framebuffer {

    private var _buffer: CVPixelBuffer?
    private var _texture: CVOpenGLESTexture?
    private var _framebufferId: GLuint = 0
    private var _renderbufferId: GLuint = 0
    private var _width = 0
    private var _height = 0

    public var textureId: GLuint {
        get {
            if let texture = _texture {
                return CVOpenGLESTextureGetName(texture)
            }
            return 0
        }
    }

    public var buffer: CVPixelBuffer? {
        get {
            return _buffer
        }
    }
    
    public var framebufferId: GLuint {
        get {
            return _framebufferId
        }
    }
    
    public var renderbufferId: GLuint {
        get {
            return _renderbufferId
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

extension Framebuffer {
    
    public mutating func create(_ width: Int,  _ height: Int, _ cache: CVOpenGLESTextureCache) {
        var error: CVReturn
        
        error = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32BGRA,
            [
                // Make it compatible with OpenGL ES otherwise
                // The CVPixelBuffer will not be updated.
                kCVPixelBufferOpenGLESCompatibilityKey as String : true,
                kCVPixelBufferIOSurfacePropertiesKey as String : [
                    kCVPixelBufferIOSurfaceOpenGLESTextureCompatibilityKey as String : true
                ]
            ] as CFDictionary,
            &_buffer
        )
        
        guard
            _buffer != nil,
            error == 0 else {
            fatalError("Can't create pixel buffer.")
        }
        
        error = CVOpenGLESTextureCacheCreateTextureFromImage(
            kCFAllocatorDefault,
            cache,
            _buffer!,
            nil,
            GLenum(GL_TEXTURE_2D),
            GL_RGBA,
            GLsizei(width),
            GLsizei(height),
            GLenum(GL_RGBA),
            GLenum(GL_UNSIGNED_BYTE),
            0,
            &_texture
        )
        
        guard
            _texture != nil,
            error == 0 else {
            fatalError("Can't create texture from cache.")
        }

        glBindTexture(GLenum(GL_TEXTURE_2D), textureId)
        glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_S), GLint(GL_CLAMP_TO_EDGE))
        glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_T), GLint(GL_CLAMP_TO_EDGE))
        glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_MIN_FILTER), GLint(GL_LINEAR))
        glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_MAG_FILTER), GLint(GL_LINEAR))

        glGenRenderbuffers(1, &_renderbufferId)
        glBindRenderbuffer(GLenum(GL_RENDERBUFFER), _renderbufferId)
        glRenderbufferStorage(
            GLenum(GL_RENDERBUFFER),
            GLenum(GL_DEPTH_COMPONENT16),
            GLsizei(width),
            GLsizei(height)
        )

        glGenFramebuffers(1, &_framebufferId)
        glBindFramebuffer(GLenum(GL_FRAMEBUFFER), _framebufferId)
        glFramebufferTexture2D(
            GLenum(GL_FRAMEBUFFER),
            GLenum(GL_COLOR_ATTACHMENT0),
            GLenum(GL_TEXTURE_2D),
            textureId,
            0
        )
        glFramebufferRenderbuffer(
            GLenum(GL_FRAMEBUFFER),
            GLenum(GL_DEPTH_ATTACHMENT),
            GLenum(GL_RENDERBUFFER),
            _renderbufferId
        )
        
        guard glCheckFramebufferStatus(GLenum(GL_FRAMEBUFFER)) == GLenum(GL_FRAMEBUFFER_COMPLETE) else {
            fatalError("Incomplete Framebuffer")
        }
        
        _width = width
        _height = height
    }
    
    public mutating func destroy() {
        glDeleteFramebuffers(1, &_framebufferId)
        glDeleteRenderbuffers(1, &_renderbufferId)
        _framebufferId = 0
        _renderbufferId = 0
    }
    
}

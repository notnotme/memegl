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

final class PolySpriteBuffer {

    static let PI180 = Float.pi / 180.0
    static let CAPACITY = 256

    private struct Vertex {
        var px: Float = 0 // Position X
        var py: Float = 0 // Position Y
        var ts: Float = 0 // Texture S
        var tt: Float = 0 // Texture T
        var cr: Float = 0 // Color R
        var cg: Float = 0 // Color G
        var cb: Float = 0 // Color B
        var ca: Float = 0 // Color A
        var sx: Float = 0 // Scale X
        var sy: Float = 0 // Scale Y
        var tx: Float = 0 // Translation X
        var ty: Float = 0 // Translation Y
        var rot: Float = 0 // Rotation
    }
        
    private var buffer: UnsafeMutablePointer<Vertex>?
    private var spriteProgram = SpriteProgram()
    private var count = 0

    func create() {
        let vertexBuffer = UnsafeMutablePointer<Vertex>.allocate(capacity: Self.CAPACITY)
        vertexBuffer.initialize(repeating: Vertex(), count: Self.CAPACITY)
        buffer = vertexBuffer
        spriteProgram.create()
    }
    
    func destroy() {
        spriteProgram.destroy()
        if let vertexBuffer = buffer {
            vertexBuffer.deinitialize(count: Self.CAPACITY)
            vertexBuffer.deallocate()
        }
    }
    
    func bind() {
        let vertexPosition = GLuint(spriteProgram.vertexPositionLocation)
        let texturePosition = GLuint(spriteProgram.vertexTextureLocation)
        let colorPosition = GLuint(spriteProgram.vertexColorLocation)
        let scalePosition = GLuint(spriteProgram.vertexScaleLocation)
        let translatePosition = GLuint(spriteProgram.vertexTranslateLocation)
        let rotationPosition = GLuint(spriteProgram.vertexRotationLocation)
        let stride = GLsizei(MemoryLayout<Vertex>.stride)

        glUseProgram(spriteProgram.id)
        glEnableVertexAttribArray(vertexPosition)
        glEnableVertexAttribArray(texturePosition)
        glEnableVertexAttribArray(colorPosition)
        glEnableVertexAttribArray(scalePosition)
        glEnableVertexAttribArray(translatePosition)
        glEnableVertexAttribArray(rotationPosition)

        guard let vertexBuffer = buffer else {
            NSLog("Nil vertex buffer.")
            return
        }
        
        glVertexAttribPointer(
            vertexPosition,
            2,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].px
        )
        
        glVertexAttribPointer(
            texturePosition,
            2,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].ts
        )
        
        glVertexAttribPointer(
            colorPosition,
            4,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].cr
        )
                
        glVertexAttribPointer(
            scalePosition,
            2,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].sx
        )
        
        glVertexAttribPointer(
            translatePosition,
            2,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].tx
        )

        glVertexAttribPointer(
            rotationPosition,
            1,
            GLenum(GL_FLOAT),
            GLboolean(GL_FALSE),
            stride,
            &vertexBuffer[0].rot
        )
    }
    
    public func setMatrix(_ mat: inout GLKMatrix4) {
        spriteProgram.setMatrix(&mat)
    }

    public func setTexture(_ textureId: GLint) {
        spriteProgram.setTexture(textureId)
    }
    
    func begin() {
        count = 0
    }
    
    func putSprite(_ sprite: inout Sprite) {
        let sizeX = Float(sprite.size.x) * 0.5
        let sizeY = Float(sprite.size.y) * 0.5
            
        putVertex(
            -sizeX, -sizeY,
            sprite.texture.x, sprite.texture.y,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )

        putVertex(
            -sizeX, sizeY,
            sprite.texture.x, sprite.texture.w,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )

        putVertex(
            sizeX, sizeY,
            sprite.texture.z, sprite.texture.w,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )

        putVertex(
            sizeX, sizeY,
            sprite.texture.z, sprite.texture.w,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )

        putVertex(
            sizeX, -sizeY,
            sprite.texture.z, sprite.texture.y,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )

        putVertex(
            -sizeX, -sizeY,
            sprite.texture.x, sprite.texture.y,
            sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w,
            sprite.scale.x, sprite.scale.y,
            sprite.position.x, sprite.position.y,
            sprite.rotation
        )
    }
    
    func putVertex(_ px: Float, _ py: Float, _ ts: Float, _ tt: Float, _ cr: Float, _ cg: Float, _ cb: Float, _ ca: Float, _ sx: Float, _ sy: Float, _ tx: Float, _ ty: Float, _ rotation: Float) {
        guard let vertexBuffer = buffer else {
            fatalError("Nil vertex buffer.")
        }

        guard count < Self.CAPACITY else {
            NSLog("Vertex capacity overflow.")
            return
        }
        
        vertexBuffer[count].px = px
        vertexBuffer[count].py = py
        vertexBuffer[count].ts = ts
        vertexBuffer[count].tt = tt
        vertexBuffer[count].cr = cr
        vertexBuffer[count].cg = cg
        vertexBuffer[count].cb = cb
        vertexBuffer[count].ca = ca
        vertexBuffer[count].sx = sx
        vertexBuffer[count].sy = sy
        vertexBuffer[count].tx = tx
        vertexBuffer[count].ty = ty
        vertexBuffer[count].rot = rotation * Self.PI180
        
        count += 1
    }
    
    func render() {
        glDrawArrays(GLenum(GL_TRIANGLES), 0, GLsizei(count))
    }
    
}

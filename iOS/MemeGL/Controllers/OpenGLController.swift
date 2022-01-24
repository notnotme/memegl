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

import GLKit
import OpenGLES
import AVFoundation
import Vision
import VideoToolbox

class OpenGLController: GLKViewRecordableController {

    /// An alias to be used when face detection need to be updated in
    /// parent controllers
    typealias FaceDetectedReceiver = (_ detected: Bool) -> Void
    
    // Drawing surface and sprite size never change size
    private static let FRAMEBUFFER_WIDTH = 720
    private static let FRAMEBUFFER_HEIGHT = 1280
    private static let SPRITE_EYE_SIZE = Vector2<UInt>(200, 140)
    private static let SPRITE_MOUTH_SIZE = Vector2<UInt>(180, 270)

    // This help rendering landmark sprites as they use a circle shape
    private static let CIRCLE_PRECISION = Float(16.0) // segments from 360°
    private static let STEP_SIZE = Float(360.0) / CIRCLE_PRECISION

    // Used to control landmark texture speed adjustment
    private static let LERP_VALUE = Float(0.5)

    /// Store information about the face landmark sprites that we need to draw
    private struct LandmarkSpriteInfo {
        var position: Vector2<Float> = Vector2(
            Float.infinity,
            Float.infinity
        )
        var texturePosition: Vector2<Float> = Vector2(
            0.0,
            0.0
        )
        var textureNewPosition: Vector2<Float> = Vector2(
            0.0,
            0.0
        )
        var size: Vector2<UInt> = Vector2(
            0,
            0
        )
        var scale: Vector2<Float> = Vector2(
            1,
            1
        )
        var orientation: Float = 0.0
        var textureScale: Float = 1.0
    }
    
    /// Holder to keep sprites
    private struct SpriteHolder {
        var leftEye: LandmarkSpriteInfo
        var rightEye: LandmarkSpriteInfo
        var mouth: LandmarkSpriteInfo
        var mask: Sprite
        var framebuffer: Sprite
    }

    private let userFaceRequestHandler = VNSequenceRequestHandler()
    private let maskFaceRequestHandler = VNSequenceRequestHandler()
    private let spriteBuffer = PolySpriteBuffer()
    private let cachedPath = UIBezierPath()

    private var faceDetectedReceiver: FaceDetectedReceiver?
    private var cropFramebufferSprite = true
    private var faceDetected = false
    private var framebuffer = Framebuffer()
    private var maskTexture = Texture()
    private var backgroundColor = Vector4<Float>(0)
        
    private var spriteHolder = SpriteHolder(
        leftEye: LandmarkSpriteInfo(
            size: SPRITE_EYE_SIZE
        ),
        rightEye: LandmarkSpriteInfo(
            size: SPRITE_EYE_SIZE
        ),
        mouth: LandmarkSpriteInfo(
            size: SPRITE_MOUTH_SIZE
        ),
        mask: Sprite(
            position: Vector2(
                Float(FRAMEBUFFER_WIDTH) * 0.5,
                Float(FRAMEBUFFER_HEIGHT) * 0.5
            )
        ),
        framebuffer: Sprite(
            size: Vector2(
                FRAMEBUFFER_WIDTH,
                FRAMEBUFFER_HEIGHT
            ),
            texture: Vector4(0, 0, 1, 1)
        )
    )
    
    var textureScale = Float(1.0)

    override func viewDidLoad() {
        super.viewDidLoad()
        preferredFramesPerSecond = 30
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // Check user preference to set fullscreen image or not
        cropFramebufferSprite = UserDefaults.standard.bool(
            forKey: PreferencesController.fullImageKey
        )
    }
    
    override func glkView(_ view: GLKView, drawIn rect: CGRect) {
        super.glkView(view, drawIn: rect)

        let screenWidth = Float(view.drawableWidth)
        let screenHeight = Float(view.drawableHeight)
        let framebufferWidth = Float(framebuffer.width)
        let framebufferHeight = Float(framebuffer.height)
        
        glClearColor(
            GLfloat(backgroundColor.x),
            GLfloat(backgroundColor.y),
            GLfloat(backgroundColor.z),
            GLfloat(backgroundColor.w)
        )
        
        // Offscreen render - As resolution never change we may move this matrix as member of OpenGLController
        var offscreenMartix = GLKMatrix4MakeOrtho(0, framebufferWidth, 0, framebufferHeight, 0, -1)
        spriteBuffer.setMatrix(&offscreenMartix)

        glBindFramebuffer(GLenum(GL_FRAMEBUFFER), framebuffer.framebufferId)
        glViewport(0, 0, GLsizei(framebuffer.width), GLsizei(framebuffer.height))
        glClear(GLbitfield(GL_COLOR_BUFFER_BIT))
        glDisable(GLenum(GL_BLEND))
        glBindTexture(GLenum(GL_TEXTURE_2D), maskTexture.id)

        spriteBuffer.begin()
        spriteBuffer.putSprite(&spriteHolder.mask)
        spriteBuffer.render()

        if faceDetected {
            glBindTexture(GLenum(GL_TEXTURE_2D), cameraTexture.id)
            glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_S), GLint(GL_CLAMP_TO_EDGE))
            glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_T), GLint(GL_CLAMP_TO_EDGE))
            
            glEnable(GLenum(GL_BLEND))
            glBlendEquationSeparate(GLenum(GL_FUNC_ADD), GLenum(GL_FUNC_ADD))
            glBlendFuncSeparate(GLenum(GL_SRC_ALPHA), GLenum(GL_ONE_MINUS_SRC_ALPHA), GLenum(GL_ONE), GLenum(GL_ONE_MINUS_SRC_ALPHA))

            // Compensate device orientation and camera rotation by
            // rotating the sprite
            var orientation: Float {
                guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
                    return 0
                }
                switch (scene.interfaceOrientation) {
                case .unknown, .portrait: return 90
                case .landscapeLeft: return 180
                case .landscapeRight: return 0
                case .portraitUpsideDown: return 270
                @unknown default:
                    return 0
                }
            }
            
            spriteBuffer.begin()
            computeUserSpriteTextureCenter(&spriteHolder.leftEye)
            computeUserSpriteTextureCenter(&spriteHolder.rightEye)
            computeUserSpriteTextureCenter(&spriteHolder.mouth)

            drawSprite(&spriteHolder.leftEye, textureScale, orientation)
            drawSprite(&spriteHolder.rightEye, textureScale, orientation)
            drawSprite(&spriteHolder.mouth, textureScale, orientation)
            spriteBuffer.render()
        }
        
        // View render
        glFlush()
        view.bindDrawable()

        var screenMatrix = GLKMatrix4MakeOrtho(0, screenWidth, screenHeight, 0, 0, 1)
        spriteBuffer.setMatrix(&screenMatrix)

        glViewport(0, 0, GLsizei(screenWidth), GLsizei(screenHeight))
        glClear(GLbitfield(GL_COLOR_BUFFER_BIT))
        glDisable(GLenum(GL_BLEND))
        glBindTexture(GLenum(GL_TEXTURE_2D), framebuffer.textureId)

        var scale: Float {
            if cropFramebufferSprite {
                return screenWidth / framebufferWidth
            } else {
                return screenHeight / framebufferHeight
            }
        }
        
        spriteHolder.framebuffer.scale.x = scale
        spriteHolder.framebuffer.scale.y = scale
        spriteHolder.framebuffer.position.x = screenWidth * 0.5
        spriteHolder.framebuffer.position.y = screenHeight * 0.5

        spriteBuffer.begin()
        spriteBuffer.putSprite(&spriteHolder.framebuffer)
        spriteBuffer.render()
        
        // Capture if needed
        capture(framebuffer.buffer!)
    }
    
    override func startOpenGL(_ context: EAGLContext) {
        super.startOpenGL(context)
 
        spriteBuffer.create()
        spriteBuffer.bind()
        spriteBuffer.setTexture(0)
       
        framebuffer.create(Self.FRAMEBUFFER_WIDTH, Self.FRAMEBUFFER_HEIGHT, textureCache!)
        maskTexture.create(GL_LINEAR, GL_CLAMP_TO_EDGE)
        
        // Get system background color to clear the background with
        // We may cache this in global members
        var r = CGFloat(0)
        var g = CGFloat(0)
        var b = CGFloat(0)
        var a = CGFloat(0)
        UIColor.systemBackground.getRed(&r, green: &g, blue: &b, alpha: &a)
        backgroundColor.x = Float(r)
        backgroundColor.y = Float(g)
        backgroundColor.z = Float(b)
        backgroundColor.w = Float(a)
        
        // setup OpenGL for 2D rendering
        glDisable(GLenum(GL_DEPTH_TEST))
        glDepthMask(GLboolean(GL_FALSE))
        glEnable(GLenum(GL_TEXTURE_2D))
        glActiveTexture(GLenum(GL_TEXTURE0))
    }
    
    override func stopOpenGL() {
        super.stopOpenGL()
        
        spriteBuffer.destroy()
        maskTexture.destroy()
        framebuffer.destroy()
    }
    
    override func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        super.captureOutput(output, didOutput: sampleBuffer, from: connection)
                
        // Detect user faces and landmarks
        do {
            let faceRequest = VNDetectFaceRectanglesRequest()
            faceRequest.preferBackgroundProcessing = false

            let landmarksRequest = VNDetectFaceLandmarksRequest(
                completionHandler: didDetectUserFace
            )
            
            landmarksRequest.constellation = .constellation65Points
            landmarksRequest.preferBackgroundProcessing = false

            try userFaceRequestHandler.perform(
                [faceRequest, landmarksRequest],
                on: sampleBuffer,
                orientation: .leftMirrored
            )
        } catch {
            NSLog("Failed to perform image request: \(error.localizedDescription)")
        }
    }
    
    func setFaceDetectedReceiver(_ receiver: FaceDetectedReceiver?) {
        faceDetectedReceiver = receiver
    }

    func changePresident(_ president: President) {
        // Queue a runnable to be executed on the OpenGL thread: update texture
        // and sprite size information. This must be done on the OpenGL thread
        queueRunnable { [self] in
            guard let cgImage = president.image.cgImage  else {
                // This mmust never fail as we use predefined images
                fatalError("Failed to perform image request.")
            }
          
            spriteHolder.mask.size.x = cgImage.width
            spriteHolder.mask.size.y = cgImage.height
            
            spriteHolder.leftEye.scale = president.imageScale
            spriteHolder.rightEye.scale = president.imageScale
            spriteHolder.mouth.scale = president.imageScale

            glBindTexture(GLenum(GL_TEXTURE_2D), maskTexture.id)
            maskTexture.setPixels(cgImage)
            
            // Trigger a face detection on our image so we can place the
            // user sprite landmarks on top of the face mask
            let faceRequest = VNDetectFaceRectanglesRequest()
            faceRequest.preferBackgroundProcessing = false

            let landmarksRequest = VNDetectFaceLandmarksRequest(
                completionHandler: didDetectMaskFace
            )
            
            landmarksRequest.constellation = .constellation76Points
            landmarksRequest.preferBackgroundProcessing = false

            do {
                try maskFaceRequestHandler.perform(
                    [faceRequest, landmarksRequest],
                    on: cgImage,
                    orientation: .downMirrored
                )
            } catch {
                fatalError("Error while performing face detection: \(error.localizedDescription)")
            }
        }
    }
    
    /// Create a UIImage  using the offscreen framebuffer content
    func createUIImage() -> UIImage {
        // Calculate image bounds
        var imageWidth: Int {
            if spriteHolder.mask.size.x > framebuffer.width {
                return framebuffer.width
            } else {
                return spriteHolder.mask.size.x
            }
        }
        
        var imageHeight: Int {
            if spriteHolder.mask.size.y > framebuffer.height {
                return framebuffer.height
            } else {
                return spriteHolder.mask.size.y
            }
        }

        var posX: Int {
            return Int(spriteHolder.mask.position.x - (Float(imageWidth) * 0.5))
        }
        
        var posY: Int {
            return Int(spriteHolder.mask.position.y - (Float(imageHeight) * 0.5))
        }

        // Ensure pixelBuffer is available
        guard let pixelBuffer = framebuffer.buffer else {
            fatalError("Nil Framebuffer buffer.")
        }

        let ciImage = CIImage(
            cvPixelBuffer: pixelBuffer,
            options: nil
        )
        
        // Take only the imlage rect and skip borders
        let rect = CGRect(
            x: posX,
            y: posY,
            width: imageWidth,
            height: imageHeight
        )
                    
        let context = CIContext()

        guard let cgImage = context.createCGImage(ciImage, from: rect) else {
            fatalError("Can't create CGImage.")
        }

        return UIImage(cgImage: cgImage)
    }
    
    func startRecording(_ url: URL) {
        startRecording(url, framebuffer.width, framebuffer.height)
    }

    private func getCenterOfPoints(_ points: [CGPoint]) -> (x: Float, y: Float) {
        cachedPath.removeAllPoints()

        if points.count > 0 {
            cachedPath.move(to: points.first!)
            for (index, point) in points.enumerated() {
                if index == 0 {
                    continue
                }
                cachedPath.addLine(to: point)
            }
            cachedPath.close()
        }
        
        let center = cachedPath.bounds
        return (Float(center.midX), Float(center.midY))
    }
    
    private func computeUserSpriteTextureCenter(_ spriteInfo: inout LandmarkSpriteInfo) {
        if spriteInfo.texturePosition.x == 0.0 {
            spriteInfo.texturePosition.x = spriteInfo.textureNewPosition.x
            spriteInfo.texturePosition.y = spriteInfo.textureNewPosition.y
        } else {
            // If we moved just a bit, move the sprites to the right place using a linear interpolation
            spriteInfo.texturePosition.x = Self.lerp(
                spriteInfo.texturePosition.x,
                spriteInfo.textureNewPosition.x,
                Self.LERP_VALUE
            )
            spriteInfo.texturePosition.y = Self.lerp(
                spriteInfo.texturePosition.y,
                spriteInfo.textureNewPosition.y,
                Self.LERP_VALUE
            )
        }
    }

    private func drawSprite(_ spriteInfo: inout LandmarkSpriteInfo, _ textureScale: Float, _ orientation: Float) {
        let texelWidth = 1.0 / Float(cameraTexture.width)
        let texelHeight = 1.0 / Float(cameraTexture.height)

        let width = Float(spriteInfo.size.x) * 0.5
        let height = Float(spriteInfo.size.y) * 0.5
        let widthInTexture = (Float(spriteInfo.size.x) * texelWidth) * 0.5
        let heightInTexture = (Float(spriteInfo.size.y) * texelHeight) * 0.5

        let xPositionInTexture = 1.0 - spriteInfo.texturePosition.y / Float(cameraTexture.width)
        let yPositionInTexture = spriteInfo.texturePosition.x / Float(cameraTexture.height)

        // Draw a sprite with N vertices doing a circle shape
        var step = Float(0.0)
        while (step < Self.CIRCLE_PRECISION) {
            let angle = step * Self.STEP_SIZE
            let s = sinf(GLKMathDegreesToRadians(angle))
            let c = cosf(GLKMathDegreesToRadians(angle))
            let s1 = sinf(GLKMathDegreesToRadians(angle + Self.STEP_SIZE))
            let c1 = cosf(GLKMathDegreesToRadians(angle + Self.STEP_SIZE))

            spriteBuffer.putVertex(
                0.0,
                0.0,
                xPositionInTexture,
                yPositionInTexture,
                1,
                1,
                1,
                // Saturate the alpha component of the center. This allow for a greater blending of the contour
                // and a better transparency in the border of the sprite.
                1.8,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                spriteInfo.position.x,
                spriteInfo.position.y,
                orientation - spriteInfo.orientation
            )

            spriteBuffer.putVertex(
                s * width,
                c * height,
                xPositionInTexture + (s * widthInTexture) * textureScale,
                yPositionInTexture + (c * heightInTexture) * textureScale,
                1,
                1,
                1,
                0.0,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                spriteInfo.position.x,
                spriteInfo.position.y,
                orientation - spriteInfo.orientation
            )

            spriteBuffer.putVertex(
                s1 * width,
                c1 * height,
                xPositionInTexture + (s1 * widthInTexture) * textureScale,
                yPositionInTexture + (c1 * heightInTexture) * textureScale,
                1,
                1,
                1,
                0.0,
                spriteInfo.scale.x,
                spriteInfo.scale.y,
                spriteInfo.position.x,
                spriteInfo.position.y,
                orientation - spriteInfo.orientation
            )

            step += 1
        }
    }
    
    private func didDetectMaskFace(_ request: VNRequest, _ error: Error?) {
        guard
            let array = request.results,
            array.count > 0 else {
            // This should never happen in release as we use
            // predefined images
            fatalError("Mask face not detected.")
        }
            
        // Position the landmark sprites on top of the mask landmarks
        let ob = array.first as! VNFaceObservation
        let faceRotation = GLKMathRadiansToDegrees(ob.roll?.floatValue ?? 0.0)
        let imageSize = CGSize(
            width: spriteHolder.mask.size.x,
            height: spriteHolder.mask.size.y
        )

        // all images are centered in the framebuffer so we also need to offset the positions
        let offsetX = (Float(framebuffer.width) * 0.5) - (Float(imageSize.width) * 0.5)
        let offsetY = (Float(framebuffer.height) * 0.5) - (Float(imageSize.height) * 0.5)
        
        if let leftEye = ob.landmarks?.leftEye {
            let center = getCenterOfPoints(
                leftEye.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.leftEye.position.x = center.x + offsetX
            spriteHolder.leftEye.position.y = center.y + offsetY
            spriteHolder.leftEye.orientation = faceRotation
        }
        
        if let rightEye = ob.landmarks?.rightEye {
            let center = getCenterOfPoints(
                rightEye.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.rightEye.position.x = center.x + offsetX
            spriteHolder.rightEye.position.y = center.y + offsetY
            spriteHolder.rightEye.orientation = faceRotation
        }
        
        if let outerLips = ob.landmarks?.outerLips {
            let center = getCenterOfPoints(
                outerLips.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.mouth.position.x = center.x + offsetX
            spriteHolder.mouth.position.y = center.y + offsetY
            spriteHolder.mouth.orientation = faceRotation
        }
    }
    
    private func didDetectUserFace(_ request: VNRequest, _ error: Error?) {
        guard
            let array = request.results,
            array.count > 0 else {
            faceDetected = false
            faceDetectedReceiver?(faceDetected)
            return
        }
        
        faceDetected = true
        faceDetectedReceiver?(faceDetected)

        // Update user landmark texture coordinate so the user sprite show them.
        // This interpolate each value with the old to smooth the movement
        // and add a bit of stabilization to the picture
        let ob = array.first as! VNFaceObservation
        let imageSize = CGSize(
            width: cameraTexture.height,
            height: cameraTexture.width
        )
        
        if let leftEye = ob.landmarks?.leftEye {
            let textureCenter = getCenterOfPoints(
                leftEye.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.leftEye.textureNewPosition.x = textureCenter.x
            spriteHolder.leftEye.textureNewPosition.y = textureCenter.y
        }
        
        if let rightEye = ob.landmarks?.rightEye {
            let textureCenter = getCenterOfPoints(
                rightEye.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.rightEye.textureNewPosition.x = textureCenter.x
            spriteHolder.rightEye.textureNewPosition.y = textureCenter.y
        }
        
        if let outerLips = ob.landmarks?.outerLips {
            let textureCenter = getCenterOfPoints(
                outerLips.pointsInImage(imageSize: imageSize)
            )
            spriteHolder.mouth.textureNewPosition.x = textureCenter.x
            spriteHolder.mouth.textureNewPosition.y = textureCenter.y
        }
    }
        
    /// Non precise lerp, from wikiki
    /// https://en.wikipedia.org/wiki/Linear_interpolation
    private static func lerp(_ start: Float, _ end: Float, _ t: Float) -> Float {
        return start + t * (end - start)
    }
    
}


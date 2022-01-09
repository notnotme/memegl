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
import AVFoundation

/**
    A GLKViewPlusController that provide a camera image texture and
    microphone audio buffers when activated.
    This allow inherited classes to have audio in addition to camera stream
    to eventually ease recording to files as everything is ready in the CaptureSession.
 */
class GLKViewCameraController : GLKViewPlusController, AVCaptureVideoDataOutputSampleBufferDelegate, AVCaptureAudioDataOutputSampleBufferDelegate {
    
    /// An alias that is called when camera is started/stoped
    typealias CameraCallback = () -> Void
    
    /// Store information about the OpenGL texture created by the camera output
    internal struct DummyTexture {
        var id: GLuint = 0
        var width: Int = 0
        var height: Int = 0
    }

    /// Camera texture holder
    internal var cameraTexture = DummyTexture()

    /// Video capture output configuration
    internal var videoCaptureOutput = AVCaptureVideoDataOutput()

    /// Video capture output configuration
    internal var audioCaptureOutput = AVCaptureAudioDataOutput()

    /// A texture cache used to keep memory under control
    /// while creating or updating a textures from camera
    internal var textureCache: CVOpenGLESTextureCache?

    /// Capture session to manage the camera devices
    private let captureSession = AVCaptureSession()
    
    /// A Queue used to handle video capture results
    private let videoSessionQueue = DispatchQueue(
        label: "VideoSessionQueue",
        qos: .userInitiated
    )

    /// A Queue used to handle audio capture results
    private let audioSessionQueue = DispatchQueue(
        label: "AudioSessionQueue",
        qos: .userInitiated
    )
    
    /// Video capture input configuration
    private var videoCaptureInput: AVCaptureDeviceInput?

    /// Audio capture input configuration
    private var audioCaptureInput: AVCaptureDeviceInput?
    
    /// Store the current used camera position
    private var _currentCamera = AVCaptureDevice.Position.front
    
    /// Property that return the current camera position used by the controller
    var currentCamera: AVCaptureDevice.Position {
        get {
            return _currentCamera
        }
    }
    
    /// Return true if the camera and microphone is reading data.
    var isCameraOpen: Bool {
        get {
            return captureSession.isRunning
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if isCameraOpen {
            stopCamera {
                NSLog("Force stop camera because view did disappear.")
            }
        }
    }
    
    override func startOpenGL(_ context: EAGLContext) {
        super.startOpenGL(context)
        
        // This create a texture cache, we need it
        // to generate some OpenGL ES texture managed
        // by GLKit to avoid manipulating pixels directly.
        guard CVOpenGLESTextureCacheCreate(
            kCFAllocatorDefault,
            nil,
            context,
            nil,
            &textureCache
        ) == 0 else {
            fatalError("Can't create texture cache.")
        }
        
        // Configure video and audio output
        videoCaptureOutput.alwaysDiscardsLateVideoFrames = true
        videoCaptureOutput.setSampleBufferDelegate(self, queue: videoSessionQueue)
        videoCaptureOutput.videoSettings = [
            kCVPixelBufferPixelFormatTypeKey as String : kCVPixelFormatType_32BGRA,
        ]
        
        audioCaptureOutput.setSampleBufferDelegate(self, queue: audioSessionQueue)
    }

    /**
        Start the camera and microphone in the specified position and execute the allback.
        This does not check for permission to open the devices, so you probably
        want to ask the user for permissions before using this func
     */
    func startCamera(_ position: AVCaptureDevice.Position? = nil, _ callback: CameraCallback? = nil) {
        // Get the camera device according to position and
        // create the input and output configuration
        guard let camera = getCameraDevice(position ?? currentCamera) else {
            fatalError("Can't find camera device.")
        }
        
        guard let microphone = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInMicrophone,
            for: AVMediaType.audio,
            position: .unspecified
        ) else {
            fatalError("Can't find microphone device.")
        }

        do {
            NSLog("Open camera and microphone...")
            videoCaptureInput = try AVCaptureDeviceInput(device: camera)
            audioCaptureInput = try AVCaptureDeviceInput(device: microphone)
        }
        catch {
            fatalError("Can't open camera or microphone: \(error.localizedDescription)")
        }
        
        captureSession.beginConfiguration()
        
        if captureSession.canAddInput(videoCaptureInput!) {
            captureSession.addInput(videoCaptureInput!)
        } else {
            fatalError("Can't add video capture input.")
        }

        if captureSession.canAddInput(audioCaptureInput!) {
            captureSession.addInput(audioCaptureInput!)
        } else {
            fatalError("Can't add audio capture input.")
        }

        if captureSession.canAddOutput(videoCaptureOutput) {
            captureSession.addOutput(videoCaptureOutput)
        } else {
            fatalError("Can't add video capture output.")
        }
        
        if captureSession.canAddOutput(audioCaptureOutput) {
            captureSession.addOutput(audioCaptureOutput)
        } else {
            fatalError("Can't add audio capture output.")
        }
        
        // Commit configuration and enable 720p session
        captureSession.commitConfiguration()
        captureSession.sessionPreset = AVCaptureSession.Preset(
            rawValue: AVCaptureSession.Preset.iFrame1280x720.rawValue
        )
        
        // Enable simple video stabilization if possible
        for connection in captureSession.connections.enumerated() {
            if connection.element.isVideoStabilizationSupported {
                connection.element.preferredVideoStabilizationMode = AVCaptureVideoStabilizationMode.standard
            }
        }

        NSLog("Starting capture...")
        captureSession.startRunning()
        _currentCamera = position ?? currentCamera
        
        callback?()
    }
    
    /// Stop the camera and microphone and execute the callback when finished
    func stopCamera(_ callback: CameraCallback? = nil) {
        if captureSession.isRunning {
            NSLog("Stoping capture...")
            captureSession.stopRunning()
            
            if let input = videoCaptureInput {
                captureSession.removeInput(input)
            }

            if let input = audioCaptureInput {
                captureSession.removeInput(input)
            }

            captureSession.removeOutput(videoCaptureOutput)
            captureSession.removeOutput(audioCaptureOutput)
        }
        
        callback?()
    }
    
    /**
        Called after each camera image update, or audio buffer update, and used to update
        the camera texture used in the OpenGL rendering func. The audio is left untouched  but is accessible
        to inherited classes.
     */
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        // Actually we only use the video stream and let the inherited classes use the audio stream if needed
        if output.isEqual(videoCaptureOutput) {
            // We must have a valid pixelBuffer
            guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
                NSLog("Nil pixelBuffer")
                return
            }
            
            // The texture cache must be created
            guard let cache = textureCache else {
                NSLog("Nil textureCache.")
                return
            }
            
            // Copy the pixelBuffer to an OpenGL texture
            var cvTexture: CVOpenGLESTexture?
            let width = CVPixelBufferGetWidth(pixelBuffer)
            let height = CVPixelBufferGetHeight(pixelBuffer)
            let error = CVOpenGLESTextureCacheCreateTextureFromImage(
                kCFAllocatorDefault,
                cache,
                pixelBuffer,
                nil,
                GLenum(GL_TEXTURE_2D),
                GL_RGBA,
                GLsizei(width),
                GLsizei(height),
                GLenum(GL_BGRA),
                GLenum(GL_UNSIGNED_BYTE),
                0,
                &cvTexture
            )

            // The texture must exists
            guard
                let texture = cvTexture,
                error == 0 else {
                NSLog("CVOpenGLESTextureCacheCreateTextureFromImage failed (error: \(error))")
                return
            }
            
            /*
             Update our internal DummyTexture struct and flush the texture cache now
             
             Note that before using the camera texture, wrap parameters must be configured, ex:
             Otherwise the texture stay black.
             
             glBindTexture(GLenum(GL_TEXTURE_2D), cameraTexture.id)
             glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_S), GLint(GL_CLAMP_TO_EDGE))
             glTexParameteri(GLenum(GL_TEXTURE_2D), GLenum(GL_TEXTURE_WRAP_T), GLint(GL_CLAMP_TO_EDGE))
             */
            
            cameraTexture.id = CVOpenGLESTextureGetName(texture)
            cameraTexture.width = width
            cameraTexture.height = height
            CVOpenGLESTextureCacheFlush(cache, 0)
        }
    }

    /// Called when an image can't be processed by the captureOutput because the queue is full of unprocessed images.
    func captureOutput(_ output: AVCaptureOutput, didDrop sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        NSLog("captureOutput didDrop.")
    }

    /// Get the first camera device found for the requested postiion, or nil
    private func getCameraDevice(_ position: AVCaptureDevice.Position) -> AVCaptureDevice? {
        let deviceDescoverySession = AVCaptureDevice.DiscoverySession.init(
            deviceTypes: [AVCaptureDevice.DeviceType.builtInWideAngleCamera],
            mediaType: AVMediaType.video,
            position: position
        )

        for device in deviceDescoverySession.devices {
            if device.position == position {
                return device
            }
        }

        return nil
    }
    
}

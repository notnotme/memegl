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
    A GLKView that can encode the rendered frames and audio to a video file.
 */
class GLKViewRecordableController: GLKViewCameraController {
    
    /// A Callback to be used when recording stop.
    typealias WritterEndCallback = () -> Void
    
    enum RecordStatus {
        case started
        case recording
        case stopped
    }
    
    var status: RecordStatus {
        get {
            return recordStatus
        }
    }
    
    private var assetWritter: AVAssetWriter?
    private var frameWritter: AVAssetWriterInputPixelBufferAdaptor?
    private var audioInput: AVAssetWriterInput?
    private var currentTime: CMTime?
    private var updateFrame = false
    private var recordStatus = RecordStatus.stopped

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if status == .recording {
            stopRecording {
                NSLog("Forced recording to stop because view did disappear.")
            }
        }
    }
    
    func startRecording(_ url: URL, _ width: Int, _ height: Int) {
        guard isCameraOpen else {
            fatalError("Camera must be activated before recording.")
        }
        
        // Try to delete any old meme before creating another
        do {
            try? FileManager.default.removeItem(at: url)
            assetWritter = try AVAssetWriter(outputURL: url, fileType: .mp4)
        } catch {
            fatalError("Can't initialize asset writter \(error.localizedDescription)")
        }

        guard let assetWritter = assetWritter else {
            fatalError("Nil assetWritter.")
        }

        assetWritter.shouldOptimizeForNetworkUse = true
        
        // Configure a video input
        let videoInput = AVAssetWriterInput(
            mediaType: .video,
            outputSettings: [
                AVVideoCodecKey : AVVideoCodecType.h264,
                AVVideoWidthKey : width,
                AVVideoHeightKey : height,
                AVVideoCompressionPropertiesKey : [
                    AVVideoExpectedSourceFrameRateKey : preferredFramesPerSecond,
                    AVVideoProfileLevelKey : AVVideoProfileLevelH264BaselineAutoLevel
                ]
            ]
        )
         
        videoInput.expectsMediaDataInRealTime = true
        
        // Create an adaptor to be able to submit CVPixelBuffers
        // directly to the videoInput
        frameWritter = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: videoInput,
            sourcePixelBufferAttributes: nil
        )
        
        // Configure the audio input
        audioInput = AVAssetWriterInput(
            mediaType: .audio,
            outputSettings: [
                AVNumberOfChannelsKey : 1,
                AVSampleRateKey : 44100,
                AVEncoderBitRateKey : 96000,
                AVFormatIDKey : kAudioFormatMPEG4AAC
            ]
        )

        audioInput!.expectsMediaDataInRealTime = true
        
        if assetWritter.canAdd(videoInput) {
            assetWritter.add(videoInput)
        } else {
            fatalError("No video input added.")
        }

        if assetWritter.canAdd(audioInput!) {
            assetWritter.add(audioInput!)
        } else {
            fatalError("No audio input added.")
        }
                
        if !assetWritter.startWriting() {
            fatalError(assetWritter.error?.localizedDescription ?? "Unknown error.")
        }

        recordStatus = .started
    }
    
    func stopRecording(_ callback: @escaping WritterEndCallback) {
        recordStatus = .stopped

        guard let videoWritter = assetWritter else {
            return
        }

        guard let frameWritter = frameWritter else {
            return
        }

        if videoWritter.status == .writing {
            audioInput?.markAsFinished()
            frameWritter.assetWriterInput.markAsFinished()
            videoWritter.endSession(atSourceTime: currentTime!)
            videoWritter.finishWriting() {
                callback()
            }
        } else {
            callback()
        }
    }
    
    func capture(_ buffer: CVPixelBuffer) {
        guard recordStatus == .recording, updateFrame else {
            return
        }
                
        guard let frameWritter = frameWritter else {
            NSLog("Nil frameWritter.")
            return
        }
        
        guard frameWritter.assetWriterInput.isReadyForMoreMediaData else {
            NSLog("Video input writter not ready for media data.")
            return
        }

        if !frameWritter.append(buffer, withPresentationTime: currentTime!) {
            NSLog("Problem appending pixel buffer at time: %lld", currentTime!.value)
        }
        
        updateFrame = false
    }
    
    override func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        super.captureOutput(output, didOutput: sampleBuffer, from: connection)
        
        switch (recordStatus) {
            case .started:
                if output.isEqual(videoCaptureOutput) {
                    // Session must start at the presentation timestamp
                    // otherwise the file will be invalid and will output no sound
                    // on some video players.
                    currentTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
                    assetWritter!.startSession(atSourceTime: currentTime!)
                    
                    updateFrame = true
                    recordStatus = .recording
                }
            break
            case .recording:
                if output.isEqual(audioCaptureOutput) {
                    if let audioInput = audioInput {
                        guard audioInput.isReadyForMoreMediaData else {
                            NSLog("Audio input writter not ready for media data.")
                            return
                        }
                        
                        if !audioInput.append(sampleBuffer) {
                            NSLog("Audio input writter fail to append media data.")
                        }
                    }
                } else {
                    // Keep timestamp in sync with video buffer
                    currentTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
                    updateFrame = true
                }
            break
            default:
            break
        }
    }
    
}

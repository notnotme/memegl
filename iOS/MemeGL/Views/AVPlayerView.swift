//
//  AVPlayerView.swift
//  MemeGL
//
// I took the code as it on a thread laying on Stackoverflow.
// See answers from @hbk and @iKK https://stackoverflow.com/a/61425000
// Thanks to the contributors of this thread.

import AVFoundation
import UIKit

class AVPlayerView: UIView {

    var player: AVPlayer? {
        get {
            return playerLayer.player
        }
        set {
            playerLayer.player = newValue
        }
    }

    var playerLayer: AVPlayerLayer {
        return layer as! AVPlayerLayer
    }

    override static var layerClass: AnyClass {
        return AVPlayerLayer.self
    }
    
}

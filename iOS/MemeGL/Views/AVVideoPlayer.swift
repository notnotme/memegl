//
//  VideoPlayer.swift
//  MemeGL
//
// I took the code as it on a thread laying on Stackoverflow.
// See answers from @hbk and @iKK https://stackoverflow.com/a/61425000
// Thanks to the contributors of this thread.

import AVFoundation
import Foundation

protocol AVVideoPlayerDelegate {
    func downloadedProgress(_ progress: Double)
    func readyToPlay()
    func didUpdateProgress(_ progress: Double)
    func didFinishPlayItem()
    func didFailPlayToEnd()
}

let videoContext: UnsafeMutableRawPointer? = nil

class AVVideoPlayer : NSObject {

    private var assetPlayer: AVPlayer?
    private var playerItem: AVPlayerItem?
    private var urlAsset: AVURLAsset?
    private var videoOutput: AVPlayerItemVideoOutput?
    private var playerView: AVPlayerView?

    private var assetDuration = 0.0
    private var autoRepeatPlay = true
    private var autoPlay = true

    var delegate: AVVideoPlayerDelegate?

    var playerRate: Float = 1.0 {
        didSet {
            if let player = assetPlayer {
                player.rate = playerRate > 0 ? playerRate : 0.0
            }
        }
    }

    var volume: Float = 1.0 {
        didSet {
            if let player = assetPlayer {
                player.volume = volume > 0 ? volume : 0.0
            }
        }
    }

    // MARK: - Init

    convenience init(_ urlAsset: URL, _ view: AVPlayerView, _ startAutoPlay: Bool = true, _ repeatAfterEnd: Bool = true) {
        self.init()

        playerView = view
        autoPlay = startAutoPlay
        autoRepeatPlay = repeatAfterEnd

        if let playView = playerView, let playerLayer = playView.layer as? AVPlayerLayer {
            playerLayer.videoGravity = AVLayerVideoGravity.resizeAspect
        }
        
        initialSetupWithURL(urlAsset)
        prepareToPlay()
    }

    override init() {
        super.init()
    }

    // MARK: - Public

    func isPlaying() -> Bool {
        if let player = assetPlayer {
            return player.rate > 0
        } else {
            return false
        }
    }

    func seekToPosition(seconds:Float64) {
        if let player = assetPlayer {
            pause()
            if let timeScale = player.currentItem?.asset.duration.timescale {
                player.seek(to: CMTimeMakeWithSeconds(seconds, preferredTimescale: timeScale)) { [self] (complete) in
                    play()
                }
            }
        }
    }

    func pause() {
        if let player = assetPlayer {
            player.pause()
        }
    }

    func play() {
        if let player = assetPlayer {
            if player.currentItem?.status == .readyToPlay {
                player.play()
                player.rate = playerRate
            }
        }
    }

    func cleanUp() {
        if let item = playerItem {
            item.removeObserver(self, forKeyPath: "status")
            item.removeObserver(self, forKeyPath: "loadedTimeRanges")
        }
        NotificationCenter.default.removeObserver(self)
        assetPlayer = nil
        playerItem = nil
        urlAsset = nil
    }

    // MARK: - Private

    private func prepareToPlay() {
        let keys = ["tracks"]
        if let asset = urlAsset {
            asset.loadValuesAsynchronously(forKeys: keys) {
                DispatchQueue.main.async { [self] in
                    startLoading()
                }
            }
        }
    }

    private func startLoading() {
        var error: NSError?
        guard let asset = urlAsset else {
            fatalError("No urlAsset to play.")
        }
        
        let status = asset.statusOfValue(forKey: "tracks", error: &error)

        if status == AVKeyValueStatus.loaded {
            assetDuration = CMTimeGetSeconds(asset.duration)

            let videoOutputOptions = [
                kCVPixelBufferPixelFormatTypeKey as String : Int(kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange)
            ]
            
            videoOutput = AVPlayerItemVideoOutput(pixelBufferAttributes: videoOutputOptions)
            playerItem = AVPlayerItem(asset: asset)

            if let item = playerItem {
                item.addObserver(
                    self,
                    forKeyPath: "status",
                    options: .initial,
                    context: videoContext
                )
                
                item.addObserver(
                    self,
                    forKeyPath: "loadedTimeRanges",
                    options: [.new, .old],
                    context: videoContext
                )

                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(playerItemDidReachEnd),
                    name: NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                    object: nil
                )
                
                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(didFailedToPlayToEnd),
                    name: NSNotification.Name.AVPlayerItemFailedToPlayToEndTime,
                    object: nil
                )

                if let output = videoOutput {
                    item.add(output)

                    item.audioTimePitchAlgorithm = AVAudioTimePitchAlgorithm.varispeed
                    assetPlayer = AVPlayer(playerItem: item)

                    if let player = assetPlayer {
                        player.rate = playerRate
                    }

                    addPeriodicalObserver()
                    if let playView = playerView, let layer = playView.layer as? AVPlayerLayer {
                        layer.player = assetPlayer
                        print("player created")
                    }
                }
            }
        }
    }

    private func addPeriodicalObserver() {
        let timeInterval = CMTimeMake(value: 1, timescale: 1)

        if let player = assetPlayer {
            player.addPeriodicTimeObserver(forInterval: timeInterval, queue: DispatchQueue.main) { (time) in
                self.playerDidChangeTime(time)
            }
        }
    }

    private func playerDidChangeTime(_ time: CMTime) {
        if let player = assetPlayer {
            let timeNow = CMTimeGetSeconds(player.currentTime())
            let progress = timeNow / assetDuration

            delegate?.didUpdateProgress(progress)
        }
    }

    @objc private func playerItemDidReachEnd() {
        delegate?.didFinishPlayItem()

        if let player = assetPlayer {
            player.seek(to: CMTime.zero)
            if autoRepeatPlay == true {
                play()
            }
        }
    }

    @objc private func didFailedToPlayToEnd() {
        delegate?.didFailPlayToEnd()
    }

    private func playerDidChangeStatus(_ status: AVPlayer.Status) {
        if status == .failed {
            print("Failed to load video")
        } else if status == .readyToPlay, let player = assetPlayer {
            volume = player.volume
            delegate?.readyToPlay()

            if autoPlay == true && player.rate == 0.0 {
                play()
            }
        }
    }

    private func moviewPlayerLoadedTimeRangeDidUpdated(_ ranges: Array<NSValue>) {
        var maximum: TimeInterval = 0
        for value in ranges {
            let range: CMTimeRange = value.timeRangeValue
            let currentLoadedTimeRange = CMTimeGetSeconds(range.start) + CMTimeGetSeconds(range.duration)
            if currentLoadedTimeRange > maximum {
                maximum = currentLoadedTimeRange
            }
        }
        
        let progress = assetDuration == 0 ? 0.0 : Double(maximum) / assetDuration
        delegate?.downloadedProgress(progress)
    }

    deinit {
        cleanUp()
    }

    private func initialSetupWithURL(_ url: URL) {
        let options = [AVURLAssetPreferPreciseDurationAndTimingKey : true]
        urlAsset = AVURLAsset(url: url, options: options)
    }

    // MARK: - Observations
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if context == videoContext {
            if let key = keyPath {
                if key == "status", let player = assetPlayer {
                    playerDidChangeStatus(player.status)
                } else if key == "loadedTimeRanges", let item = playerItem {
                    moviewPlayerLoadedTimeRangeDidUpdated(item.loadedTimeRanges)
                }
            }
        }
    }
    
}

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

import UIKit

class ShareController: UIViewController {

    enum MediaType {
        case video
        case image
    }
    
    @IBOutlet weak var imageView: UIImageView?
    @IBOutlet weak var playerView: AVPlayerView?

    private var mediaURL: URL?
    private var mediaType: MediaType?
    private var videoPlayer: AVVideoPlayer?
    private var activityViewController: UIActivityViewController?

    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard
            let mediaURL = mediaURL,
            let mediaType = mediaType else {
            fatalError("ShareController not ready. Did you call configure?")
        }
        
        activityViewController = UIActivityViewController(
            activityItems: [mediaURL],
            applicationActivities: nil
        )
        
        switch (mediaType) {
        case .image:
            if let imageView = imageView {
                imageView.image = UIImage(contentsOfFile: mediaURL.path)
            }
        break
        case .video:
            if let playerView = playerView {
                videoPlayer = AVVideoPlayer(mediaURL, playerView)
            }
        break
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // Observe if we go background/foreground
        // to stop/start video playback
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(willResignActiveNotification),
            name: UIApplication.willResignActiveNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(didBecomeActiveNotification),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }
        
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)

        // Remove observers
        NotificationCenter.default.removeObserver(
            self,
            name: UIApplication.willResignActiveNotification,
            object: nil
        )

        NotificationCenter.default.removeObserver(
            self,
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )

        if let videoPlayer = videoPlayer {
            videoPlayer.pause()
            videoPlayer.cleanUp()
        }
    }
    
    @IBAction func shareButtonClick(_ sender: UIButton) {
        guard let activityViewController = activityViewController else {
            NSLog("Nil activityViewController.")
            return
        }

        // iPads need an anchor view to be set
        if UIDevice.current.userInterfaceIdiom != .phone {
            if let popoverController = activityViewController.popoverPresentationController {
                popoverController.sourceView = sender
            }
        }

        present(activityViewController, animated: true)
    }
    
    func configure(_ type: MediaType, _ url: URL) {
        mediaType = type
        mediaURL = url
    }

    @objc private func willResignActiveNotification() {
        if let videoPlayer = videoPlayer, videoPlayer.isPlaying() {
            videoPlayer.pause()
        }
    }

    @objc private func didBecomeActiveNotification() {
        if let videoPlayer = videoPlayer, !videoPlayer.isPlaying() {
            videoPlayer.play()
        }
    }

}

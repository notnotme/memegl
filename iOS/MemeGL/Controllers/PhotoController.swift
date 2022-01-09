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
import AVFoundation

class PhotoController: UIViewController, UICollectionViewDelegate {

    static let appStoreId = 1602390330
    static let mainStoryboardName = "Main"
    static let glControllerSeg = "GLKViewSeg"
    static let aboutControllerIdentifier = "AboutController"
    static let preferencesControllerIdentifier = "PreferencesController"
    static let shareControllerIdentifier = "ShareController"

    @IBOutlet weak var buttonMenu: UIBarButtonItem?
    @IBOutlet weak var presidentCollectionView: UICollectionView?
    @IBOutlet weak var cameraButton: UIButton?
    @IBOutlet weak var photoButton: UIButton?
    @IBOutlet weak var glkViewContainer: UIView?
    @IBOutlet weak var noFaceDetectedView: UIView?
    @IBOutlet weak var loaderView: UIActivityIndicatorView?
    @IBOutlet weak var maskSelectorButton: UIButton?
    @IBOutlet weak var zoomSlider: UISlider?
    @IBOutlet weak var cameraModeButton: UIButton?
    @IBOutlet weak var cameraModeSwitchContainer: UIView?
    
    private let presidentDataSource = PresidentDataSource()
    private var glContainerGestureRecognizer : UITapGestureRecognizer?
    private var glController: OpenGLController?
    private var maskSelectorVisible = false
    private var isVideoMode = false

    private var mainMenu: UIMenu {
        return UIMenu(
            title: "",
            children: [
                UIAction(
                    title: NSLocalizedString("menu.share", comment: ""),
                    image: nil,
                    handler: { [self] (_) in
                        
                    shareApplication()
                }),
                UIAction(
                    title: NSLocalizedString("menu.store", comment: ""),
                    image: nil,
                    handler: { [self] (_) in
                        
                    openAppStore()
                }),
                UIAction(
                    title: NSLocalizedString("menu.preferences", comment: ""),
                    image: nil,
                    handler: { [self] (_) in
                        
                    guard let storyboard = storyboard else {
                        fatalError("Nil storyboard.")
                    }
                    
                    let preferencesController = storyboard.instantiateViewController(identifier: Self.preferencesControllerIdentifier)
                    navigationController?.pushViewController(preferencesController, animated: true)
                }),
                UIAction(
                    title: NSLocalizedString("menu.about", comment: ""),
                    image: nil,
                    handler: { [self] (_) in
                        
                    guard let storyboard = storyboard else {
                        fatalError("Nil storyboard.")
                    }

                    let aboutController = storyboard.instantiateViewController(identifier: Self.aboutControllerIdentifier)
                    navigationController?.pushViewController(aboutController, animated: true)
                })
            ]
        )
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == Self.glControllerSeg {
            if let controller = segue.destination as? OpenGLController {
                // Let's capture the controller of the GLKView that will be presented
                glController = controller
            } else {
                fatalError("Can't find OpenGLController.")
            }
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        guard let glController = glController else {
            fatalError("Nil glController.")
        }

        glContainerGestureRecognizer = UITapGestureRecognizer(
            target: self,
            action: #selector(didTapGLContainer)
        )
        
        let president = Int.random(in: 0..<President.data.count)

        // Offset mask selector by its size to hide in at the begining
        // I don't know how to to it from Storyboard
        if let presidentCollectionView = presidentCollectionView {
            presidentCollectionView.transform = CGAffineTransform(
                translationX: 0,
                y: 200 // View is not yet inflated, so take the value from the storyboard
            )
            
            // Disallow multiple selection and set data source and load a president
            presidentCollectionView.allowsMultipleSelection = false
            presidentCollectionView.dataSource = presidentDataSource
            presidentCollectionView.delegate = self
            presidentCollectionView.selectItem(
                at: IndexPath.init(row: president, section: 0),
                animated: false,
                scrollPosition: UICollectionView.ScrollPosition.centeredHorizontally
            )
        }
        
        // First load ignore selectItem so we need to load the president now
        glController.changePresident(President.data[president])

        // Check back camera availability
        cameraButton?.isHidden = !isBackCameraAvailable()
       
        // Add menu - When added from Storyboard, it don't work ?
        buttonMenu?.menu = mainMenu
    }
        
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // Add a tap gesture recognizer on the opengl container
        // to hide the mask selector when it is showing
        if UserDefaults.standard.bool(forKey: PreferencesController.maskSelectorKey) {
            if let glContainerGestureRecognizer = glContainerGestureRecognizer {
                glkViewContainer?.addGestureRecognizer(glContainerGestureRecognizer)
            }
        }
        
        guard let glController = glController else {
            fatalError("Nil glController.")
        }
         
        var counter = 0
        glController.setFaceDetectedReceiver { [self] detected in
            counter += 1
            if counter == 5 {
                // Wait at least 5 frames
                counter = 0
                DispatchQueue.main.sync {
                    // This can make the view flicker so we may
                    // find another solution
                    // FIXME: Remove counter hack
                    noFaceDetectedView?.isHidden = detected
                }
            }
        }
        
        // Observe if we go background/foreground
        // to stop/start video playback
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(willResignActiveNotification),
            name: UIApplication.willResignActiveNotification,
            object: nil
        )
        
        checkCameraPermissions()
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        // Remove observers
        NotificationCenter.default.removeObserver(
            self,
            name: UIApplication.willResignActiveNotification,
            object: nil
        )
        
        // Remove the tap gesture if needed
        if UserDefaults.standard.bool(forKey: PreferencesController.maskSelectorKey) {
            if let glContainerGestureRecognizer = glContainerGestureRecognizer {
                glkViewContainer?.removeGestureRecognizer(glContainerGestureRecognizer)
            }
        }
        
        // Remove the face detection receiver and Stop camera 
        guard let glController = glController else {
            fatalError("Nil glController.")
        }
        
        glController.setFaceDetectedReceiver(nil)
        glController.stopCamera()
        
        // Hide the loader view
        if let loaderView = loaderView {
            if loaderView.isAnimating {
                loaderView.stopAnimating()
            }
        }
        
        cameraButton?.isEnabled = true
        photoButton?.isEnabled = true
        maskSelectorButton?.isEnabled = true
        cameraModeButton?.isEnabled = true
    }
    
    @IBAction func photoButtonClick(_ sender: UIButton) {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }

        // Start and show the loader view
        loaderView?.startAnimating()
        cameraButton?.isEnabled = false
        photoButton?.isEnabled = false
        maskSelectorButton?.isEnabled = false
        cameraModeButton?.isEnabled = false
        
        if isVideoMode {
            // Get a temporary file to store the video
            let directory = FileManager.default.temporaryDirectory
            guard let tmpFile = NSURL.fileURL(withPathComponents: [directory.path, "meme.mp4"]) else {
                fatalError("Can't get temporary file.")
            }
            
            if glController.status == .recording {
                stopVideo(tmpFile)
            } else {
                startVideo(tmpFile)
            }
        } else {
            takePhoto()
        }
    }
    
    @IBAction func cameraButtonClick(_ sender: UIButton) {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }
        
        loaderView?.startAnimating()
        photoButton?.isEnabled = false
        maskSelectorButton?.isEnabled = false
        cameraButton?.isEnabled = false
        cameraModeButton?.isEnabled = false
        
        glController.stopCamera { [self] in
            switch (glController.currentCamera) {
            case .unspecified, .back:
                glController.startCamera(.front) {
                    sender.setImage(
                        UIImage(named: "outline_camera_rear_black_24pt"),
                        for: .normal
                    )
                }
            break
            case .front:
                glController.startCamera(.back) {
                    sender.setImage(
                        UIImage(named: "outline_camera_front_black_24pt"),
                        for: .normal
                    )
                }
            break
            @unknown default:
                NSLog("Unknwon camera position requested.")
            }
            
            loaderView?.stopAnimating()
            photoButton?.isEnabled = true
            maskSelectorButton?.isEnabled = true
            cameraButton?.isEnabled = true
            cameraModeButton?.isEnabled = true
        }
    }
    
    @IBAction func maskButtonClick(_ sender: UIButton) {
        toggleMaskSelector()
    }
    
    @IBAction func togglePhotoVideo(_ sender: UIButton) {
        toggleCameraMode()
    }
    
    @IBAction func changeZoom(_ sender: UISlider) {
        guard let glController = glController else {
            fatalError("Nil glController")
        }

        let inverseValue = (sender.minimumValue + (sender.maximumValue - sender.value))
        glController.textureScale = inverseValue * 0.01
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }

        let president = President.data[indexPath.row]
        glController.changePresident(president)
        toggleMaskSelector()
    }
    
    private func toggleCameraMode() {
        if isVideoMode {
            let text = NSAttributedString(
                string: NSLocalizedString("photo", comment: ""),
                attributes: [
                    NSAttributedString.Key.font: UIFont.boldSystemFont(ofSize: 11)
                ]
            )

            cameraModeButton?.setAttributedTitle(text, for: .normal)
            cameraModeButton?.setImage(UIImage(named: "outline_photo_camera_black_24pt"), for: .normal)
        } else {
            let text = NSAttributedString(
                string: NSLocalizedString("video", comment: ""),
                attributes: [
                    NSAttributedString.Key.font: UIFont.boldSystemFont(ofSize: 11)
                ]
            )

            cameraModeButton?.setAttributedTitle(text, for: .normal)
            cameraModeButton?.setImage(UIImage(named: "outline_videocam_black_24pt"), for: .normal)
        }
        
        UIView.animate(
            withDuration: 0.2,
            delay: 0,
            options: .curveEaseIn,
            animations: { [self] in
                guard let cameraModeButton = cameraModeButton else {
                    return
                }
                guard let cameraModeSwitchContainer = cameraModeSwitchContainer else {
                    return
                }

                if isVideoMode {
                    cameraModeButton.transform = CGAffineTransform(
                        translationX: 0,
                        y: 0
                    )
                } else {
                    cameraModeButton.transform = CGAffineTransform(
                        translationX: cameraModeSwitchContainer.frame.width - cameraModeButton.frame.width,
                        y: 0
                    )
                }
                isVideoMode.toggle()
            },
            completion: nil
        )
    }
    
    private func checkCameraPermissions() {
        // Check permission
        switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized:
                NSLog("Camera permission ok.")
                checkMicrophonePermission()
            return
            case .notDetermined:
                // Request access
                AVCaptureDevice.requestAccess(for: .video) { [self] granted in
                    if granted {
                        NSLog("Camera permission ok.")
                        checkMicrophonePermission()
                    } else {
                        NSLog("Camera permission nok.")
                        showUsageDialogFor(.video)
                    }
                }
            // Error
            case .denied:
                NSLog("Camera permission nok.")
                showUsageDialogFor(.video)
            return
            case .restricted:
                NSLog("Camera permission nok.")
                showUsageDialogFor(.video)
            return
            @unknown default:
                fatalError("Unknown permission state for .video")
        }
    }
    
    private func checkMicrophonePermission() {
        // Check permission
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
            case .authorized:
                guard let glController = glController else {
                    fatalError("Nil glController.")
                }
                NSLog("Microphone permission ok.")
                glController.startCamera()
            return
            case .notDetermined:
                // Request access
                AVCaptureDevice.requestAccess(for: .video) { [self] granted in
                    guard let glController = glController else {
                        fatalError("Nil glController.")
                    }

                    if granted {
                        NSLog("Microphone permission ok.")
                        glController.startCamera()
                    } else {
                        NSLog("Microphone permission nok.")
                        showUsageDialogFor(.audio)
                    }
                }
            // Error
            case .denied:
                NSLog("Microphone permission nok.")
                showUsageDialogFor(.audio)
            return
            case .restricted:
                NSLog("Microphone permission nok.")
                showUsageDialogFor(.audio)
            return
            @unknown default:
                fatalError("Unknown permission state for .audio")
        }
    }
    
    private func showUsageDialogFor(_ usage: AVMediaType) {
        // Create new Alert
        var dialog: UIAlertController {
            switch (usage) {
            case .video:
                return UIAlertController(
                    title: NSLocalizedString("alert.cameraAccess.title", comment: ""),
                    message: NSLocalizedString("alert.cameraAccess.message", comment: ""),
                    preferredStyle: .alert
                )
            case .audio:
                return UIAlertController(
                    title: NSLocalizedString("alert.microphoneAccess.title", comment: ""),
                    message: NSLocalizedString("alert.microphoneAccess.message", comment: ""),
                    preferredStyle: .alert
                )
            default:
                fatalError("Can't show usage dialog: Unknon usage.")
            }
        }

        let action = UIAlertAction(
            title: NSLocalizedString("button.ok", comment: ""),
            style: .default,
            handler: { [self] (_) in
                openAppSettings()
            }
        )

        dialog.addAction(action)
        
        DispatchQueue.main.async { [self] in
            present(dialog, animated: true, completion: nil)
        }
    }
    

    /// Hide or Show the lmask selector
    private func toggleMaskSelector() {
        // Depending on the state of the mask selector
        // we select a different curve animation
        var options: UIView.AnimationOptions {
            if (maskSelectorVisible) {
                return .curveEaseIn
            } else {
                return .curveEaseOut
            }
        }
        
        UIView.animate(
            withDuration: 0.2,
            delay: 0,
            options: options,
            animations: { [self] in
                guard let presidentCollectionView = presidentCollectionView else {
                    return
                }
                if maskSelectorVisible {
                    presidentCollectionView.transform = CGAffineTransform(
                        translationX: 0,
                        y: presidentCollectionView.contentSize.height
                    )
                } else {
                    presidentCollectionView.transform = CGAffineTransform(
                        translationX: 0,
                        y: 0
                    )
                }
                maskSelectorVisible.toggle()
            },
            completion: nil
        )
    }
    
    private func takePhoto() {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }

        // Camera shutter sound taken from
        // https://iphonedev.wiki/index.php/AudioServices
        let systemSoundID: SystemSoundID = 1108 // shutter
        AudioServicesPlaySystemSound(systemSoundID)
        
        let image = glController.createUIImage()
        guard let data = image.jpegData(compressionQuality: 0.95) else {
            fatalError("Can't get jpegData from image.")
        }

        // Create a temporary file to store the image
        let directory = FileManager.default.temporaryDirectory
        guard let tmpFile = NSURL.fileURL(withPathComponents: [directory.path, "meme.jpg"]) else {
            fatalError("Can't get temporary file.")
        }

        do {
            try data.write(to: tmpFile)
        } catch {
            fatalError("Can't write png file on disk.")
        }
        
        // This is not needed but this stop the camera texture update
        // and make the view appealing
        glController.stopCamera { [self] in
            DispatchQueue.main.async {
                guard let storyboard = storyboard else {
                    fatalError("Nil storyboard.")
                }

                let shareController = storyboard.instantiateViewController(identifier: Self.shareControllerIdentifier) as ShareController
                shareController.configure(.image, tmpFile)
                navigationController?.pushViewController(shareController, animated: true)
            }
        }
    }
    
    private func stopVideo(_ url: URL) {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }
        
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            glController.stopRecording {
                // Play sound after recording end to avoid capturing it
                // in the tesulting video
                // Camera shutter sound taken from
                // https://iphonedev.wiki/index.php/AudioServices
                let systemSoundID: SystemSoundID = 1118 // end video
                AudioServicesPlaySystemSound(systemSoundID)
                
                // We must be sure that the camera is stopped before going on
                glController.stopCamera {
                    DispatchQueue.main.async {
                        guard let storyboard = storyboard else {
                            fatalError("Nil storyboard.")
                        }
                        let shareController = storyboard.instantiateViewController(identifier: Self.shareControllerIdentifier) as ShareController
                        shareController.configure(.video, url)
                        navigationController?.pushViewController(shareController, animated: true)
                    }
                }
            }
        }
    }
    
    private func startVideo(_ url: URL) {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }

        // Play sound and delay the recording to avoid
        // having the shutter sound included in the resulting video
        // Camera shutter sound taken from
        // https://iphonedev.wiki/index.php/AudioServices
        let systemSoundID: SystemSoundID = 1117 // start video
        AudioServicesPlaySystemSound(systemSoundID)

        let deadline = DispatchTime.now().advanced(
            by: DispatchTimeInterval.milliseconds(500)
        )
        
        DispatchQueue.global(qos: .userInitiated).asyncAfter(
            deadline: deadline) {
            glController.startRecording(url)
            DispatchQueue.main.async { [self] in
                // Hide the loader while recording
                loaderView?.stopAnimating()
                photoButton?.isEnabled = true
            }
        }
    }
    
    private func shareApplication() {
        let activityViewController = UIActivityViewController(
            activityItems: ["https://itunes.apple.com/app/\(Self.appStoreId)"],
            applicationActivities: nil
        )
        
        if UIDevice.current.userInterfaceIdiom != .phone {
            // iPads need the anchor to be set
            if let popoverController = activityViewController.popoverPresentationController {
                popoverController.sourceView = photoButton
            }
        }
        
        present(activityViewController, animated: true)
    }
    
    private func openAppStore() {
        if let url = URL(string: "itms-apps://apple.com/app/\(Self.appStoreId)") {
            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            } else {
                NSLog("Can't open app store via menu.")
            }
        }
    }
    
    private func openAppSettings() {
        if let appSettings = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(appSettings, options: [:], completionHandler: nil)
        }
    }
    
    /// Return true if the device has a back camera
    /// Return true on macbook air m1 (?)
    private func isBackCameraAvailable() -> Bool {
        let discovery = AVCaptureDevice.DiscoverySession.init(
            deviceTypes: [.builtInWideAngleCamera],
            mediaType: AVMediaType.video,
            position: .back)
        
        return !discovery.devices.isEmpty
    }
    
    @objc private func didTapGLContainer() {
        if maskSelectorVisible {
            toggleMaskSelector()
        }
    }
    
    @objc private func willResignActiveNotification() {
        guard let glController = glController else {
            fatalError("Nil glController.")
        }
                
        if glController.status == .recording {
            glController.stopRecording { [self] in
                NSLog("Forced stop recording because app become inactive.")
                DispatchQueue.main.sync {
                    cameraButton?.isEnabled = true
                    photoButton?.isEnabled = true
                    maskSelectorButton?.isEnabled = true
                    cameraModeButton?.isEnabled = true
                }
            }
        }
    }
    
}

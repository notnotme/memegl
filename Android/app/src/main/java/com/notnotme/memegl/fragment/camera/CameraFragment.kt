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
package com.notnotme.memegl.fragment.camera

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Gravity
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.notnotme.memegl.Mask
import com.notnotme.memegl.R
import com.notnotme.memegl.databinding.FragmentCameraBinding
import com.notnotme.memegl.fragment.camera.CameraFragmentViewModel.Companion.CameraMode
import com.notnotme.memegl.fragment.camera.CameraFragmentViewModel.Companion.CameraType
import com.notnotme.memegl.fragment.share.ShareFragmentViewModel.Companion.MediaType
import com.notnotme.memegl.renderer.GLRecorderSurfaceView
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@androidx.camera.core.ExperimentalGetImage
class CameraFragment : Fragment(R.layout.fragment_camera) {

    companion object {
        const val TAG = "CameraFragment"

        private const val CAMERA_PREVIEW_WIDTH = 720
        private const val CAMERA_PREVIEW_HEIGHT = 1280

        private const val TEMP_FILENAME_IMAGE = "meme.png"
        private const val TEMP_FILENAME_VIDEO = "meme.mp4"

        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val MICRO_PERMISSION = Manifest.permission.RECORD_AUDIO
    }

    private val viewModel: CameraFragmentViewModel by viewModels()
    private var permissionRequest: ActivityResultLauncher<Array<String>>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var binding: FragmentCameraBinding? = null
    private var surfaceView: GLRecorderSurfaceView? = null

    private var renderer: PolyRenderer? = null
    private var dialog: Dialog? = null
    private var videoFile: File? = null

    private val faceDetectionNoTracking by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.3f)
                .build()
        )
    }

    private val faceDetectionTracking by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.3f)
                .enableTracking()
                .build()
        )
    }

    private val shutterSound by lazy {
        MediaActionSound()
    }

    private val analyzerExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val backPressHandler by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.isMaskSelectorVisible.value == true -> {
                        viewModel.switchMaskSelectorVisibility()
                    }
                    viewModel.recording.value == true
                            && binding?.cameraButton?.isEnabled == true -> {
                        stopRecorder(true)
                    }
                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val autoCloseSelector = preferenceManager.getBoolean(
            getString(R.string.preference_auto_close_mask_selector_key),
            true
        )

        binding = FragmentCameraBinding.bind(view).also {
            it.cameraModeSwitch.run {
                setOnClickListener {
                    viewModel.switchCameraMode()
                }
            }

            it.maskSelectorButton.run {
                setOnClickListener {
                    viewModel.switchMaskSelectorVisibility()
                }
                isEnabled = false
            }

            it.cameraSelectorButton.run {
                setOnClickListener {
                    viewModel.switchCameraType()
                }
                isEnabled = false
            }

            it.cameraScale.run {
                value = viewModel.cameraScale.value!!
                addOnChangeListener { slider, value, _ ->
                    val inverseValue = (slider.valueFrom + (slider.valueTo - value))
                    viewModel.setCameraScale(inverseValue)
                }
            }

            it.cameraButton.run {
                setOnClickListener {
                    when (viewModel.cameraMode.value!!) {
                        CameraMode.PHOTO -> takePhoto()
                        CameraMode.VIDEO -> {
                            when (viewModel.recording.value!!) {
                                false -> requestRecorder()
                                true -> stopRecorder(false)
                            }
                        }
                    }
                }
                isEnabled = false
            }

            it.maskSelector.run {
                setHasFixedSize(true)
                adapter = MaskAdapter { mask ->
                    if (autoCloseSelector) {
                        viewModel.switchMaskSelectorVisibility()
                    }
                    viewModel.setSelectedMask(mask)
                }
            }

            it.surfaceContainer.run {
                setOnClickListener {
                    if (viewModel.isMaskSelectorVisible.value == true) {
                        viewModel.switchMaskSelectorVisibility()
                    }
                }
            }

            viewModel.producedFile.observe(viewLifecycleOwner, { event ->
                event.getContentIfNotHandled()?.let { file ->
                    when (viewModel.cameraMode.value!!) {
                        CameraMode.VIDEO -> navigateToShare(file.absolutePath, MediaType.VIDEO)
                        CameraMode.PHOTO -> navigateToShare(file.absolutePath, MediaType.PHOTO)
                    }
                }
            })

            viewModel.cameraMode.observe(viewLifecycleOwner, { mode ->
                showCameraModeSwitch(mode)
            })

            viewModel.isUserFaceVisible.observe(viewLifecycleOwner, { visible ->
                it.noFaceBanner.visibility = if (visible) View.GONE else View.VISIBLE
            })

            viewModel.isMaskSelectorVisible.observe(viewLifecycleOwner, { visible ->
                showMaskSelector(visible)
            })

            viewModel.error.observe(viewLifecycleOwner, { event ->
                event.getContentIfNotHandled()?.let { message ->
                    showError(message)
                }
            })

            viewModel.mask.observe(viewLifecycleOwner, { mask ->
                val maskAdapter = it.maskSelector.adapter as MaskAdapter
                maskAdapter.setSelected(mask.ordinal)
                loadMask(mask)
            })
        }

        // We override the back button behavior in some case
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_preferences -> {
            navigateToPreferences()
            true
        }
        R.id.action_about_us -> {
            navigateToAbout()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        super.onAttach(context)
    }

    override fun onDetach() {
        permissionRequest?.unregister()
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        configureCameraSelectorButton()
        viewModel.cameraType.observe(viewLifecycleOwner, {
            binding?.cameraSelectorButton?.isEnabled = false
            requestCameraPermission(it)
        })
    }

    override fun onPause() {
        if (viewModel.recording.value == true) {
            stopRecorder(true)
        }

        viewModel.cameraType.removeObservers(viewLifecycleOwner)
        cameraProvider?.unbindAll()
        surfaceView?.stop()

        super.onPause()
    }

    override fun onDestroyView() {
        binding?.surfaceContainer?.removeView(surfaceView)
        binding = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        hideDialog()

        shutterSound.release()
        analyzerExecutor.shutdown()

        super.onDestroy()
    }

    private fun showCameraModeSwitch(mode: CameraMode) {
        binding?.let {

            TransitionManager.beginDelayedTransition(it.cameraModeContainer,
                AutoTransition().also { transition ->
                    transition.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                    transition.interpolator = DecelerateInterpolator()
                })

            with(it.cameraModeSwitch.layoutParams as FrameLayout.LayoutParams) {
                if (mode == CameraMode.PHOTO) {
                    it.cameraModeSwitch.text = getString(R.string.photo)
                    it.cameraModeSwitch.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_photo)
                    gravity = Gravity.START
                } else if (mode == CameraMode.VIDEO) {
                    it.cameraModeSwitch.text = getString(R.string.video)
                    it.cameraModeSwitch.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_video)
                    gravity = Gravity.END
                }
            }

        }
    }

    private fun showMaskSelector(visible: Boolean) {
        binding?.let {

            TransitionManager.beginDelayedTransition(it.maskSelector,
                ChangeBounds().also { transition ->
                    transition.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                    transition.interpolator = DecelerateInterpolator()
                    transition.excludeChildren(it.surfaceContainer, true)
                    transition.excludeChildren(it.noFaceBanner, true)
                    transition.excludeChildren(it.progressBar, true)
                    transition.excludeChildren(it.bottomLayout, true)
                })

            ConstraintSet().also { constraint ->
                constraint.clone(requireContext(), R.layout.fragment_camera)
                constraint.applyTo(it.root)
            }

            with(it.maskSelector.layoutParams as ConstraintLayout.LayoutParams) {
                if (visible) {
                    topToBottom = ConstraintLayout.LayoutParams.UNSET
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                } else {
                    bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    topToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
            }
        }
    }

    private fun takePhoto() {
        binding?.let {
            it.cameraButton.isEnabled = false
            it.cameraScale.isEnabled = false
            it.cameraSelectorButton.isEnabled = false
            it.cameraModeSwitch.isEnabled = false
            it.maskSelectorButton.isEnabled = false
            it.progressBar.visibility = View.VISIBLE
        }

        renderer?.run {
            lifecycleScope.launch(Dispatchers.IO) {
                shutterSound.play(MediaActionSound.SHUTTER_CLICK)
            }

            // Stop updating the texture and sprites now
            // because we take a screenshot of the framebuffer
            setUpdateTexture(false)

            getBitmap {
                lifecycleScope.launch(Dispatchers.IO) {
                    val context = requireContext()
                    val tempFile = File(context.filesDir, TEMP_FILENAME_IMAGE)
                    try {
                        FileOutputStream(tempFile).use { out ->
                            it.compress(Bitmap.CompressFormat.PNG, 90, out)
                            it.recycle()
                        }

                        viewModel.setProducedFile(tempFile)
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            binding?.let {
                                it.cameraButton.isEnabled = true
                                it.cameraSelectorButton.isEnabled = true
                                it.cameraScale.isEnabled = true
                                it.cameraModeSwitch.isEnabled = true
                                it.maskSelectorButton.isEnabled = true
                                it.progressBar.visibility = View.GONE
                            }
                            showError("${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun requestRecorder() {
        requestMicroPermission()
    }

    private fun stopRecorder(interrupted: Boolean) {
        binding?.let {
            it.cameraButton.isEnabled = false
            it.cameraButton.setImageResource(R.drawable.ic_lens)
            if (!interrupted) {
                // We need to show the loader
                it.progressBar.visibility = View.VISIBLE
            }
        }

        surfaceView?.run {
            lifecycleScope.launch(Dispatchers.IO) {
                stopRecording()
                shutterSound.play(MediaActionSound.STOP_VIDEO_RECORDING)

                viewModel.setRecorderStatus(false)
                if (!interrupted) {
                    videoFile?.let { file ->
                        viewModel.setProducedFile(file)
                    }
                }

                delay(TimeUnit.SECONDS.toMillis(1) / 2)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    binding?.let {
                        it.cameraButton.isEnabled = true
                        it.cameraScale.isEnabled = true
                        it.cameraModeSwitch.isEnabled = true
                        it.cameraSelectorButton.isEnabled = true
                        it.maskSelectorButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun startRecorder() {
        binding?.let {
            it.cameraButton.isEnabled = false
            it.progressBar.visibility = View.VISIBLE
            it.cameraModeSwitch.isEnabled = false
            it.cameraScale.isEnabled = false
            it.cameraSelectorButton.isEnabled = false
            it.maskSelectorButton.isEnabled = false
        }

        shutterSound.play(MediaActionSound.START_VIDEO_RECORDING)
        surfaceView?.run {
            lifecycleScope.launch(Dispatchers.IO) {
                // Wait a bit before recording, because we don't want the sutter sound to be
                // audible in the video file playback
                delay(TimeUnit.SECONDS.toMillis(1) / 2)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@launch
                }

                val context = requireContext()
                videoFile = File(context.filesDir, TEMP_FILENAME_VIDEO).also {
                    initRecorder(it, CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT,
                        { _, what, extra -> Log.e(TAG, "Error: $what $extra") },
                        { _, what, extra -> Log.e(TAG, "Info: $what $extra") })

                    startRecording()
                }

                viewModel.setRecorderStatus(true)
                withContext(Dispatchers.Main) {
                    binding?.let {
                        it.progressBar.visibility = View.GONE
                        it.cameraButton.setImageResource(R.drawable.ic_stop)
                    }
                }

                delay(TimeUnit.SECONDS.toMillis(1) / 2)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    binding?.cameraButton?.isEnabled = true
                }
            }
        }
    }

    private fun navigateToShare(imagePath: String, mediaType: MediaType) {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        val action = CameraFragmentDirections.actionCameraFragmentToShareFragment(imagePath, mediaType)
        navController.navigate(action)
    }

    private fun navigateToPreferences() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        val action = CameraFragmentDirections.actionCameraFragmentToPreferenceFragment()
        navController.navigate(action)
    }

    private fun navigateToAbout() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        val action = CameraFragmentDirections.actionCameraFragmentToAboutFragment()
        navController.navigate(action)
    }

    private fun showErrorCameraPermissions() {
        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.permission_camera_denied)
            .setCancelable(false)
            .setPositiveButton(R.string.finish) { _, _ ->
                requireActivity().finish()
            }.show()
    }

    private fun showErrorMicroPermissions() {
        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.permission_micro_denied)
            .setCancelable(false)
            .setPositiveButton(R.string.finish, null)
            .show()
    }

    private fun showCameraPermissionRationale() {
        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.permission_camera_rationale)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showApplicationDetails()
            }.setNegativeButton(R.string.finish) { _, _ ->
                showErrorCameraPermissions()
            }.show()
    }

    private fun showMicroPermissionRationale() {
        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.permission_micro_rationale)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showApplicationDetails()
            }.setNegativeButton(android.R.string.cancel) { _, _ ->
                showErrorMicroPermissions()
            }.show()
    }

    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")

        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.error_loading_file)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun hideDialog() {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission(cameraType: CameraType) {
        val showRationale = shouldShowRequestPermissionRationale(CAMERA_PERMISSION)
        val permissionRequested = viewModel.isCameraPermissionRequested()
        when {
            hasPermission(CAMERA_PERMISSION) -> startRenderer(cameraType) {
                loadMask(viewModel.mask.value!!)
                startCamera(cameraType)
            }
            !permissionRequested && !showRationale -> {
                viewModel.setCameraPermissionRequested(true)
                permissionRequest?.launch(arrayOf(CAMERA_PERMISSION))
            }
            else -> showCameraPermissionRationale()
        }
    }

    private fun requestMicroPermission() {
        val showRationale = shouldShowRequestPermissionRationale(MICRO_PERMISSION)
        val permissionRequested = viewModel.isMicroPermissionRequested()
        when {
            hasPermission(MICRO_PERMISSION) -> {
                startRecorder()
            }
            !permissionRequested && !showRationale -> {
                viewModel.setMicroPermissionRequested(true)
                permissionRequest?.launch(arrayOf(MICRO_PERMISSION))
            }
            else -> showMicroPermissionRationale()
        }
    }

    private fun showApplicationDetails() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
            it.flags += Intent.FLAG_ACTIVITY_NEW_TASK
            it.data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun configureCameraSelectorButton() {
        val context = requireContext()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProviderFuture.get().also { provider ->
                if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    // If back camera is available, show the button that allow
                    // switching between Front and Back
                    binding?.let {
                        if (it.cameraSelectorButton.visibility != View.VISIBLE) {
                            TransitionManager.beginDelayedTransition(it.bottomLayout,
                                AutoTransition().also { transition ->
                                    transition.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                                    transition.interpolator = LinearInterpolator()
                                })

                            it.cameraSelectorButton.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startCamera(cameraType: CameraType) {
        Log.d(TAG, "Starting camera...")

        val context = requireContext()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get().also {
                val size = Size(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT)
                val preview = Preview.Builder()
                    .setTargetResolution(size)
                    .setTargetRotation(Surface.ROTATION_0)
                    .also { builder ->
                        Camera2Interop.Extender(builder)
                            // Let the library manage itself the control mode (auto focus, exposure, etc)
                            .setCaptureRequestOption(
                                CaptureRequest.CONTROL_MODE,
                                CameraMetadata.CONTROL_MODE_AUTO
                            )
                            // We want at least the camera fps to be betwwen 20 and 40
                            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(20, 40))
                            // Try to enable image stabilization
                            .setCaptureRequestOption(
                                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                            )
                    }
                    .build()
                    .also { preview ->
                        preview.setSurfaceProvider { request ->
                            Log.d(TAG, "Creating surface...")
                            val resolution = request.resolution
                            val cameraSurface = renderer?.getCameraSurfaceTexture()?.also { surface ->
                                surface.setDefaultBufferSize(resolution.width, resolution.height)
                            }
                            request.provideSurface(
                                Surface(cameraSurface),
                                ContextCompat.getMainExecutor(requireContext()),
                                { result ->
                                    result.surface.release()
                                    Log.d(TAG, "Surface released.")
                                }
                            )
                        }
                    }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(preview.targetRotation)
                    .setTargetResolution(size)
                    .build()
                    .also { analysis ->
                        val successListener = object : OnSuccessListener<List<Face>> {
                            var rotation = 0.0f
                            override fun onSuccess(faces: List<Face>) {
                                val face = when (faces.isNotEmpty()) {
                                    true -> faces[0]
                                    else -> null
                                }
                                viewModel.setUserFaceVisibleHint(face != null)
                                renderer?.updateUserLandmarks(rotation, viewModel.cameraScale.value!!, face)
                            }
                        }

                        val completeListener = object : OnCompleteListener<List<Face>> {
                            var imageProxy: ImageProxy? = null
                            override fun onComplete(faces: Task<List<Face>>) {
                                imageProxy?.close()
                            }
                        }

                        analysis.setAnalyzer(analyzerExecutor, { imageProxy ->
                            when (val image = imageProxy.image) {
                                null -> imageProxy.close()
                                else -> {
                                    val inputImageRotation = imageProxy.imageInfo.rotationDegrees
                                    val inputImage = InputImage.fromMediaImage(image, inputImageRotation)

                                    successListener.rotation = -inputImageRotation.toFloat()
                                    completeListener.imageProxy = imageProxy

                                    faceDetectionTracking.process(inputImage)
                                        .addOnSuccessListener(successListener)
                                        .addOnCompleteListener(completeListener)
                                }
                            }
                        })
                    }

                try {

                    val cameraSelector = when (cameraType) {
                        CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                        CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val cameraSelectorIcon = when (cameraType) {
                        CameraType.FRONT -> R.drawable.ic_camera_back
                        CameraType.BACK -> R.drawable.ic_camera_front
                    }

                    binding?.cameraSelectorButton?.icon = ContextCompat.getDrawable(requireContext(), cameraSelectorIcon)

                    it.unbindAll()
                    it.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startRenderer(cameraType: CameraType, onSurfaceCreated: () -> Unit) {
        binding?.let {
            it.surfaceContainer.removeView(surfaceView)

            surfaceView?.stop()
            surfaceView = GLRecorderSurfaceView(requireContext()).also { view ->
                val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val imageZoom = preferenceManager.getBoolean(
                    getString(R.string.preference_image_zoom_key),
                    false
                )

                renderer = object : PolyRenderer(view, imageZoom, CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT, cameraType == CameraType.FRONT) {
                    override fun onSurfaceCreated() {
                        super.onSurfaceCreated()
                        onSurfaceCreated()
                    }
                }
                view.rendererCallbacks = renderer
            }

            it.surfaceContainer.addView(surfaceView)
        }
    }

    private fun loadMask(mask: Mask) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.progressBar.visibility = View.VISIBLE
                it.cameraButton.isEnabled = false
                it.cameraSelectorButton.isEnabled = false
                it.maskSelectorButton.isEnabled = false
            }

            withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeResource(resources, mask.bitmapRes)
                val faceToReplaceInputImage = InputImage.fromBitmap(bitmap, 0)

                // This face detection must never fail as we use predefined images
                faceDetectionNoTracking.process(faceToReplaceInputImage)
                    .addOnSuccessListener { faces ->
                        when (val r = renderer) {
                            null -> bitmap.recycle()
                            else -> r.setMask(mask, bitmap, faces[0])
                        }

                        binding?.let {
                            it.cameraButton.isEnabled = true
                            it.cameraModeSwitch.isEnabled = true
                            it.cameraScale.isEnabled = true
                            it.cameraSelectorButton.isEnabled = true
                            it.maskSelectorButton.isEnabled = true
                            it.progressBar.visibility = View.GONE
                        }
                    }
            }
        }
    }

}

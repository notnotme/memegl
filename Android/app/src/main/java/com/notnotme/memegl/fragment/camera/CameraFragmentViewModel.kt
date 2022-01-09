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

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.notnotme.memegl.Event
import com.notnotme.memegl.Mask
import kotlinx.parcelize.Parcelize
import java.io.File

class CameraFragmentViewModel(state: SavedStateHandle) : ViewModel() {

    companion object {
        const val TAG = "CameraFragmentViewModel"

        @Parcelize
        enum class CameraMode : Parcelable {
            PHOTO, VIDEO
        }

        @Parcelize
        enum class CameraType : Parcelable {
            FRONT, BACK
        }

        private const val KEY_SELECTED_MASK = "selectedMask"
        private const val KEY_CAMERA_MODE = "cameraMode"
        private const val KEY_CAMERA_TYPE = "cameraType"
        private const val KEY_CAMERA_SCALE = "cameraScale"
        private const val KEY_MASK_SELECTOR_OPEN = "maskSelectorOpen"

        private val DEFAULT_MASK = Mask.values().random()
        private val DEFAULT_CAMERA_MODE = CameraMode.PHOTO
        private val DEFAULT_CAMERA_SCALE = 1.0f
        private val DEFAULT_CAMERA_TYPE = CameraType.FRONT
    }

    private val _mask = state.getLiveData(KEY_SELECTED_MASK, DEFAULT_MASK)
    val mask: LiveData<Mask> get() = _mask

    private val _cameraMode = state.getLiveData(KEY_CAMERA_MODE, DEFAULT_CAMERA_MODE)
    val cameraMode: LiveData<CameraMode> get() = _cameraMode

    private val _cameraType = state.getLiveData(KEY_CAMERA_TYPE, DEFAULT_CAMERA_TYPE)
    val cameraType: LiveData<CameraType> get() = _cameraType

    private val _cameraScale = state.getLiveData(KEY_CAMERA_SCALE, DEFAULT_CAMERA_SCALE)
    val cameraScale: LiveData<Float> get() = _cameraScale

    private val _recording = MutableLiveData(false)
    val recording: LiveData<Boolean> get() = _recording

    private val _userFaceVisible = MutableLiveData(false)
    val isUserFaceVisible: LiveData<Boolean> get() = _userFaceVisible

    private val _maskSelectorVisible = state.getLiveData(KEY_MASK_SELECTOR_OPEN, false)
    val isMaskSelectorVisible: LiveData<Boolean> get() = _maskSelectorVisible

    private val _producedFile = MutableLiveData<Event<File>>()
    val producedFile: LiveData<Event<File>> get() = _producedFile

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> get() = _error

    private var cameraPermissionRequested = false
    private var microPermissionRequested = false

    fun isCameraPermissionRequested() = cameraPermissionRequested

    fun isMicroPermissionRequested() = microPermissionRequested

    fun setCameraPermissionRequested(requested: Boolean) {
        cameraPermissionRequested = requested
    }

    fun setMicroPermissionRequested(requested: Boolean) {
        microPermissionRequested = requested
    }

    fun setRecorderStatus(recording: Boolean) {
        _recording.postValue(recording)
    }

    fun setUserFaceVisibleHint(visible: Boolean) {
        if (_userFaceVisible.value == visible) {
            return
        }

        _userFaceVisible.postValue(visible)
    }

    fun setProducedFile(file: File) {
        _producedFile.postValue(Event(file))
    }

    fun setCameraScale(scale: Float) {
        _cameraScale.postValue(scale)
    }

    fun switchCameraMode() {
        when (_cameraMode.value!!) {
            CameraMode.PHOTO -> _cameraMode.postValue(CameraMode.VIDEO)
            CameraMode.VIDEO -> _cameraMode.postValue(CameraMode.PHOTO)
        }
    }

    fun switchCameraType() {
        when (_cameraType.value!!) {
            CameraType.FRONT -> _cameraType.postValue(CameraType.BACK)
            CameraType.BACK -> _cameraType.postValue(CameraType.FRONT)
        }
    }

    fun switchMaskSelectorVisibility() {
        _maskSelectorVisible.postValue(_maskSelectorVisible.value != true)
    }

    fun setSelectedMask(mask: Mask) {
        when (_mask.value!!) {
            mask -> return
            else -> _mask.postValue(mask)
        }
    }

}

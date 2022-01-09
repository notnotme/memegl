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
package com.notnotme.memegl.fragment.share

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.notnotme.memegl.Event
import java.io.File

class ShareFragmentViewModel : ViewModel() {

    companion object {
        const val TAG = "ShareFragmentViewModel"

        enum class MediaType(val contentType: String) {
            PHOTO("image/png"),
            VIDEO("video/mp4")
        }

        data class Media(
            val uri: Uri,
            val type: MediaType
        )
    }

    private val _media = MutableLiveData<Event<Media>>()
    val media: LiveData<Event<Media>> get() = _media

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> get() = _error

    fun load(mediaType: MediaType, fileToLoad: File) {
        _media.postValue(
            Event(
                Media(
                    uri = fileToLoad.toUri(),
                    type = mediaType
                )
            )
        )
    }

}

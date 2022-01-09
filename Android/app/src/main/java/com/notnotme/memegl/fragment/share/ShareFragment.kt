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

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.notnotme.memegl.BuildConfig
import com.notnotme.memegl.R
import com.notnotme.memegl.databinding.FragmentShareBinding
import com.notnotme.memegl.fragment.share.ShareFragmentViewModel.Companion.MediaType
import java.io.File

class ShareFragment : Fragment(R.layout.fragment_share) {

    companion object {
        const val TAG = "ShareFragment"

        private enum class DisplayChild(val displayChild: Int) {
            PHOTO (0),
            VIDEO (1)
        }
    }

    private val viewModel: ShareFragmentViewModel by viewModels()
    private var binding: FragmentShareBinding? = null
    private var dialog: Dialog? = null
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = ShareFragmentArgs.fromBundle(requireArguments())

        binding = FragmentShareBinding.bind(view).also {
            it.shareButton.setOnClickListener {
                if (!BuildConfig.DEMO_MODE) {
                    shareContent(args.mediaType, File(args.filename))
                } else {
                    toast?.cancel()
                    toast = Toast.makeText(requireContext(), "DEMO MODE", Toast.LENGTH_SHORT).also { toast ->
                        toast.show()
                    }
                }
            }

            it.video.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                it.video.seekTo(0)
                it.video.start()
            }

            viewModel.media.observe(viewLifecycleOwner, { event ->
                event.peekContent().let { media ->
                    when (media.type) {
                        MediaType.PHOTO -> {
                            it.viewSwitcher.displayedChild = DisplayChild.PHOTO.displayChild
                            it.image.setImageURI(media.uri)
                        }
                        MediaType.VIDEO -> {
                            it.viewSwitcher.displayedChild = DisplayChild.VIDEO.displayChild
                            it.video.setVideoURI(media.uri)
                        }
                    }
                }
            })

            viewModel.error.observe(viewLifecycleOwner, { event ->
                event.getContentIfNotHandled()?.let { message ->
                    showError(message)
                }
            })
        }

        viewModel.load(args.mediaType, File(args.filename))
    }

    override fun onPause() {
        super.onPause()
        toast?.cancel()
    }

    override fun onDestroyView() {
        binding?.video?.suspend()
        binding = null
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_preferences -> {
            navigateToPreferences(); true
        }
        R.id.action_about_us -> {
            navigateToAbout(); true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun shareContent(mediaType: MediaType, file: File) {
        val context = requireContext()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        try {
            ShareCompat.IntentBuilder(context)
                .setStream(uri)
                .setType(mediaType.contentType)
                .startChooser()
        } catch (exception: Exception) {
            Log.e(TAG, "Error: ${exception.message}")
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "File cannot be loaded: $message}")

        hideDialog()
        dialog = AlertDialog.Builder(requireContext())
            .setMessage(R.string.error_loading_file)
            .setCancelable(false)
            .setPositiveButton(R.string.back) { _, _ ->
                requireActivity().onBackPressed()
            }.show()
    }

    private fun navigateToPreferences() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        val action = ShareFragmentDirections.actionShareFragmentToPreferenceFragment()
        navController.navigate(action)
    }

    private fun navigateToAbout() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        val action = ShareFragmentDirections.actionShareFragmentToAboutFragment()
        navController.navigate(action)
    }

    private fun hideDialog() {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

}

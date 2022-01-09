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

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notnotme.memegl.Mask
import com.notnotme.memegl.databinding.ItemMaskBinding
import com.notnotme.memegl.fragment.camera.MaskAdapter.Companion.ViewHolder

class MaskAdapter(private val onItemClick: (mask: Mask) -> Unit) : RecyclerView.Adapter<ViewHolder>() {

    companion object {
        const val TAG = "MaskAdapter"

        class ViewHolder(val binding: ItemMaskBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private val items = Mask.values()
    private var selected = -1

    fun setSelected(selected: Int) {
        val oldSelected = this.selected
        this.selected = selected
        notifyItemChanged(oldSelected, false)
        notifyItemChanged(selected, true)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ).also {
        it.binding.root.setOnClickListener { _ ->
            onItemClick(items[it.adapterPosition])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with (holder.binding) {
            val context = root.context
            root.isChecked = selected == position
            text.text = context.getString(item.stringRes)
            mask.setImageBitmap(BitmapFactory.decodeResource(context.resources, item.thumbnailRes))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        with (holder.binding) {
            if (payloads.isNotEmpty()) {
                root.isChecked = payloads[0] as Boolean
            }
        }
    }

}

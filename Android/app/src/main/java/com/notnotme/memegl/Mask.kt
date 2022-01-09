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
package com.notnotme.memegl

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.notnotme.memegl.renderer.graphic.Scale
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Mask(
    @DrawableRes val thumbnailRes: Int, // This a thumbnail of the full size image but resized to be 200 pixels width
    @DrawableRes val bitmapRes: Int, // This is the full size image that show on screen, below your landmarks. Maximum size allowed is 720x1280 pixels
    @StringRes val stringRes: Int, // This is a string resource used to show the name of the president in the UI
    val scale: Scale // A scaling value used to adjust the landmark sprite size to the picture.
) : Parcelable {
    ARTHAUD     (R.mipmap.thumb_arthaud,         R.mipmap.arthaud,       R.string.name_arthaud,         Scale(1.5f)),
    DAIGNANT    (R.mipmap.thumb_dupont_aignan,   R.mipmap.dupont_aignan, R.string.name_dupont_aignan,   Scale(1.8f)),
    HIDALGO     (R.mipmap.thumb_hidalgo,         R.mipmap.hidalgo,       R.string.name_hidalgo,         Scale(1.8f)),
    JADOT       (R.mipmap.thumb_jadot,           R.mipmap.jadot,         R.string.name_jadot,           Scale(1.8f)),
    LEPEN       (R.mipmap.thumb_lepen,           R.mipmap.lepen,         R.string.name_lepen,           Scale(1.8f)),
    MELENCHON   (R.mipmap.thumb_melenchon,       R.mipmap.melenchon,     R.string.name_melenchon,       Scale(2.3f)),
    MONTEBOURG  (R.mipmap.thumb_montebourg,      R.mipmap.montebourg,    R.string.name_montebourg,      Scale(1.3f)),
    PECRESSE    (R.mipmap.thumb_pecresse,        R.mipmap.pecresse,      R.string.name_pecresse,        Scale(2.0f)),
    POUTOU      (R.mipmap.thumb_poutou,          R.mipmap.poutou,        R.string.name_poutou,          Scale(1.8f)),
    ROUSSEL     (R.mipmap.thumb_roussel,         R.mipmap.roussel,       R.string.name_roussel,         Scale(1.7f)),
    ZEMMOUR     (R.mipmap.thumb_zemmour,         R.mipmap.zemmour,       R.string.name_zemmour,         Scale(2.0f)),
    MACRON      (R.mipmap.thumb_macron,          R.mipmap.macron,        R.string.name_macron,          Scale(2.3f))
}

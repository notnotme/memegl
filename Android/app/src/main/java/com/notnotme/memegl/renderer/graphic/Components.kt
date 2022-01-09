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
package com.notnotme.memegl.renderer.graphic

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represent a texture coordinate that delimit a rectangle
 * s,t: Up/Left corner
 * u,v: Down/Right corner
 */
@Parcelize
data class STUV(
    var s: Float = 0.0f,
    var t: Float = 0.0f,
    var u: Float = 1.0f,
    var v: Float = 1.0f
) : Parcelable

/**
 * Represent a position in a virtual space
 * x,y: horizontal and vertical pixel position of the sprite
 * originX,originY: The origin of the sprite (0,0 mean center). In pixels (max: Sprite's width and height)
 */
@Parcelize
data class Position(
    var x: Float = 0.0f,
    var y: Float = 0.0f
) : Parcelable

/**
 * Represent a color
 * r,g,b,a: Red, Green, Blue, and Alpha component value of the color
 */
@Parcelize
data class Color(
    var r: Float = 1.0f,
    var g: Float = 1.0f,
    var b: Float = 1.0f,
    var a: Float = 1.0f
) : Parcelable {
    constructor(r: Int, g: Int, b: Int, a: Int)
            : this(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f)

    fun set(r: Int, g: Int, b: Int, a: Int) {
        this.r = r / 255.0f
        this.g = g / 255.0f
        this.b = b / 255.0f
        this.a = a / 255.0f
    }
}


/**
 * Represent a scaling value
 * x,y: Horizontal and vertical scale (can be negative)
 */
@Parcelize
data class Scale(
    var x: Float = 1.0f,
    var y: Float = 1.0f
) : Parcelable {
    constructor(value: Float)
            : this(value, value)
}

/**
 * Represent a size
 * w,h: width and height
 */
@Parcelize
data class Size(
    var w: Float = 16.0f,
    var h: Float = 16.0f
) : Parcelable {
    constructor(value: Float)
            : this(value, value)
}

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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FastMath {

    private val FAST_SIN by lazy { Array(360) { sin(it * PI / 180.0).toFloat() } }
    private val FAST_COS by lazy { Array(360) { cos(it * PI / 180.0).toFloat() } }

    fun sin(degree: Float): Float = FAST_SIN[degree.toInt() % 360]
    fun cos(degree: Float): Float = FAST_COS[degree.toInt() % 360]

}
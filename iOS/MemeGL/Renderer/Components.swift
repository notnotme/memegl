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
 
struct Vector2<T: Numeric> {
    
    var x: T = T.zero
    var y: T = T.zero
    
    init(_ val: T) {
        self.x = val
        self.y = val
    }
    
    init(_ x: T, _ y: T) {
        self.x = x
        self.y = y
    }
    
}

struct Vector3<T: Numeric> {
    
    var x: T = T.zero
    var y: T = T.zero
    var z: T = T.zero

    init(_ val: T) {
        self.x = val
        self.y = val
        self.z = val
    }

    init(_ x: T, _ y: T, _ z: T) {
        self.x = x
        self.y = y
        self.z = z
    }
    
}

struct Vector4<T: Numeric> {
    var x: T = T.zero
    var y: T = T.zero
    var z: T = T.zero
    var w: T = T.zero

    init(_ val: T) {
        self.x = val
        self.y = val
        self.z = val
        self.w = val
    }

    init(_ x: T, _ y: T, _ p: T, _ q: T) {
        self.x = x
        self.y = y
        self.z = p
        self.w = q
    }
    
}


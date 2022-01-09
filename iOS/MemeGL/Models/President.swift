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

struct President {
    let name: String // The president name to show in the UI
    let image: UIImage // This is the full size image that show on screen, below your landmarks. Maximum size allowed is 720x1280 pixels
    let thumbnail: UIImage // This a thumbnail of the full size image but resized to be 200 pixels width
    let imageScale: Vector2<Float> // A scaling value used to adjust the landmark sprite size to the picture.
}

extension President {
    
    static let data = [
        President(
            name: "N. ARTHAUD",
            image: UIImage(named: "arthaud")!,
            thumbnail: UIImage(named: "thumb_arthaud")!,
            imageScale: Vector2(0.5)
        ),
        President(
            name: "F. DUPONT-AIGNAN",
            image: UIImage(named: "dupont_aignan")!,
            thumbnail: UIImage(named: "thumb_dupont_aignan")!,
            imageScale: Vector2(0.65)
        ),
        President(
            name: "A. HIDALGO",
            image: UIImage(named: "hidalgo")!,
            thumbnail: UIImage(named: "thumb_hidalgo")!,
            imageScale: Vector2(0.65)
        ),
        President(
            name: "Y. JADOT",
            image: UIImage(named: "jadot")!,
            thumbnail: UIImage(named: "thumb_jadot")!,
            imageScale: Vector2(0.6)
        ),
        President(
            name: "M. LEPEN",
            image: UIImage(named: "lepen")!,
            thumbnail: UIImage(named: "thumb_lepen")!,
            imageScale: Vector2(0.6)
        ),
        President(
            name: "J.-L. MELENCHON",
            image: UIImage(named: "melenchon")!,
            thumbnail: UIImage(named: "thumb_melenchon")!,
            imageScale: Vector2(0.85)
        ),
        President(
            name: "A. MONTEBOURG",
            image: UIImage(named: "montebourg")!,
            thumbnail: UIImage(named: "thumb_montebourg")!,
            imageScale: Vector2(0.5)
        ),
        President(
            name: "V. PECRESSE",
            image: UIImage(named: "pecresse")!,
            thumbnail: UIImage(named: "thumb_pecresse")!,
            imageScale: Vector2(0.8)
        ),
        President(
            name: "P. POUTOU",
            image: UIImage(named: "poutou")!,
            thumbnail: UIImage(named: "thumb_poutou")!,
            imageScale: Vector2(0.6)
        ),
        President(
            name: "F. ROUSSEL",
            image: UIImage(named: "roussel")!,
            thumbnail: UIImage(named: "thumb_roussel")!,
            imageScale: Vector2(0.65)
        ),
        President(
            name: "E. ZEMMOUR",
            image: UIImage(named: "zemmour")!,
            thumbnail: UIImage(named: "thumb_zemmour")!,
            imageScale: Vector2(0.7)
        ),
        President(
            name: "E. MACRON",
            image: UIImage(named: "macron")!,
            thumbnail: UIImage(named: "thumb_macron")!,
            imageScale: Vector2(1.0)
        )

    ]
    
}

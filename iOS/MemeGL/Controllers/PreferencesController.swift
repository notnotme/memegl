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

class PreferencesController: UIViewController {
    
    static let maskSelectorKey = "MaskSelectorKey"
    static let fullImageKey = "FullImageKey"

    @IBOutlet weak var maskSelector: UISwitch?
    @IBOutlet weak var fullImage: UISwitch?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if let maskSelector = maskSelector {
            maskSelector.isOn = UserDefaults.standard.bool(
                forKey: Self.maskSelectorKey
            )
        }

        if let fullImage = fullImage {
            fullImage.isOn = UserDefaults.standard.bool(
                forKey: Self.fullImageKey
            )
        }
    }
    
    @IBAction func fullImageSwitch(_ sender: UISwitch) {
        UserDefaults.standard.set(
            sender.isOn,
            forKey: Self.fullImageKey
        )
    }

    @IBAction func maskSelectorSwitch(_ sender: UISwitch) {
        UserDefaults.standard.set(
            sender.isOn,
            forKey: Self.maskSelectorKey
        )
    }
        
}

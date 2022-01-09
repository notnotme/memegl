## Meme Présidents (MemeGL)

Make memes using your device cameras to surimpose your face landmarks on a presidential poster. Take a photo or a video and share it.

## Important

To be able to run both the iOS and Android version from this repository, you will need to provide your own pictures. **This repository does not host the presidents images** used in production to avoid it being closed for copyright reason.

To do so just open the project in Xcode or Android studio and look where the red lines are. Here is a shortcut for [Android](https://github.com/notnotme/memegl/blob/main/Android/app/src/main/java/com/notnotme/memegl/Mask.kt) and [iOS](https://github.com/notnotme/memegl/blob/main/iOS/MemeGL/Models/President.swift). Edit the source and place your image in "mipmap-nodpi" for the Android version. Do as usual for iOS.

You may provide more than one resolution for the thumbnails if you want, but the full images that are loaded in the OpenGL context must be unique with a maximum size of 720x1280 pixels.

This code target iOS >= 15.0 and Android >= 23.

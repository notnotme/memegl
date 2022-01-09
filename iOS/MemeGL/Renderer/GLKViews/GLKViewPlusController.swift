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

import GLKit

/**
    A controller that automatically startand stop an OpenGL ES 2 context and provide
    a function allowing to run some code on the OpenGL thread from another threads.
 */
class GLKViewPlusController : GLKViewController {
    
    /// A alias for queued function to be run on the OpenGL thread
    typealias GLRunnable = () -> Void

    /// A list that store some GLRunnable
    private var runnableQueue : [GLRunnable] = []
    
    /// A lock object to synchronize the runnable queue
    private let runnableLock = NSLock()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Create openGL ES 2 context
        guard let context = EAGLContext(api: .openGLES2) else {
            fatalError("Failed to initialize OpenGLES2 context.")
        }

        // Set OpenGL context current in this thread and setup the rendering
        EAGLContext.setCurrent(context)
        startOpenGL(context)

        // Start updating the view
        let glView = view as! GLKView
        glView.delegate = self
    }
        
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        
        // Time to release OpenGL stuff
        stopOpenGL()
    }
    
    /**
        You must call super when you override it with your ownw rendering code,
        otherwise the runnable queue will never be executed and cleared.
     */
    override func glkView(_ view: GLKView, drawIn rect: CGRect) {
        super.glkView(view, drawIn: rect)
        
        // Execute and empty the runnable queue while we are in the OpenGL context
        runnableLock.lock()
        for i in (0 ..< runnableQueue.count).reversed() {
            runnableQueue[i]()
            runnableQueue.remove(at: i)
        }
        runnableLock.unlock()
    }
        
    /**
        Create the OpenGL context and setup the view to use the RGBA8888 color format.
        This also set the background color of the GLKView to transparent.
     */
    func startOpenGL(_ context: EAGLContext) {
        NSLog("Setup OpenGL...")
        
        let glView = view as! GLKView
        glView.drawableColorFormat = .RGBA8888
        glView.drawableDepthFormat = .format24
        glView.drawableStencilFormat = .format8
        glView.backgroundColor = UIColor.clear
        
        // Finally connect the context passed in parameter
        // to the view's context
        glView.context = context
    }
    
    /**
        Stop the OpenGL context
        Actually this does nothing since EAGLContext seem to be auto managed
        and you should not call this function directly.
        But if you need to free some data before the context got deleted you
        probably want to do it here bu overriding this func.
     */
    func stopOpenGL() {
        NSLog("Cleanup OpenGL...")
        // We do nothing here but the inherited classes
        // should release the resources by overriding this func
    }
    
    /// Add a GLRunnable to be executed before the next rendered frame.
    final func queueRunnable(runnable: @escaping GLRunnable) {
        runnableLock.lock()
        runnableQueue.append(runnable)
        runnableLock.unlock()
    }
    
}

package com.notnotme.memegl

import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.notnotme.memegl.fragment.camera.CameraFragment
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @Test
    fun testTakePhoto() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.cameraButton)).perform(click())
        onView(withId(R.id.cameraButton)).check { view, _ -> !view.isEnabled }
        onView(withId(R.id.cameraModeSwitch)).check { view, _ -> !view.isEnabled }
        onView(withId(R.id.maskSelectorButton)).check { view, _ -> !view.isEnabled }
        onView(withId(R.id.progressBar)).check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    @Test
    fun testTakeVideo() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.cameraModeSwitch)).perform(click())
        onView(withId(R.id.cameraButton)).perform(click())
        onView(withId(R.id.cameraButton)).check { view, _ -> view.isEnabled }
        onView(withId(R.id.cameraModeSwitch)).check { view, _ -> !view.isEnabled }
        onView(withId(R.id.maskSelectorButton)).check { view, _ -> !view.isEnabled }
        onView(withId(R.id.cameraButton)).perform(click())
    }

}
package com.notnotme.memegl

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.notnotme.memegl.fragment.camera.CameraFragment
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@MediumTest
class CameraFragmentTest {

    @Test
    fun testMaskSelectorOpen() {
        launchFragmentInContainer<CameraFragment>(null, R.style.Theme_MyApp)

        onView(withId(R.id.maskSelectorButton)).perform(click())
        onView(withId(R.id.maskSelector)).check { view, _ ->
            (view.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom == ConstraintLayout.LayoutParams.PARENT_ID
            &&
            (view.layoutParams as ConstraintLayout.LayoutParams).topToBottom == ConstraintLayout.LayoutParams.UNSET
        }
    }

    @Test
    fun testMaskSelectorClose_clickOutside() {
        launchFragmentInContainer<CameraFragment>(null, R.style.Theme_MyApp)

        onView(withId(R.id.maskSelectorButton)).perform(click())
        onView(withId(R.id.surfaceContainer)).perform(click())

        onView(withId(R.id.maskSelector)).check { view, _ ->
            (view.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom == ConstraintLayout.LayoutParams.UNSET
            &&
            (view.layoutParams as ConstraintLayout.LayoutParams).topToBottom == ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

    @Test
    fun testMaskSelectorClose_backButton() {
        launchFragmentInContainer<CameraFragment>(null, R.style.Theme_MyApp)
        Thread.sleep(3000)

        onView(withId(R.id.maskSelectorButton)).perform(click())
        Espresso.pressBack()
        onView(withId(R.id.maskSelector)).check { view, _ ->
            (view.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom == ConstraintLayout.LayoutParams.UNSET
                    &&
                    (view.layoutParams as ConstraintLayout.LayoutParams).topToBottom == ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

}
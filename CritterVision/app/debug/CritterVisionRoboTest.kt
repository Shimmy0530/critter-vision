package com.example.crittervision

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*

/**
 * Robo test for CritterVision app
 * Tests camera functionality, filter switching, and UI interactions
 * 
 * To run this test:
 * ./gradlew connectedAndroidTest
 * 
 * Or specifically:
 * ./gradlew connectedDebugAndroidTest --tests="com.example.crittervision.CritterVisionRoboTest"
 */
@RunWith(AndroidJUnit4::class)
class CritterVisionRoboTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    private lateinit var device: UiDevice
    private lateinit var context: Context

    companion object {
        private const val PACKAGE_NAME = "com.example.crittervision"
        private const val TIMEOUT = 5000L
        private const val FILTER_SWITCH_DELAY = 2000L
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
        
        // Grant camera permission if needed
        grantCameraPermissionIfNeeded()
        
        // Start the app
        startApp()
    }

    @Test
    fun testAppLaunchesSuccessfully() {
        // Verify the app launches and main activity is visible
        val previewView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/previewView"))
        assertTrue("Camera preview should be visible", previewView.waitForExists(TIMEOUT))
        
        // Verify filter buttons are present
        val dogButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"))
        val catButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/catVisionButton"))
        val birdButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/birdVisionButton"))
        val originalButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/originalVisionButton"))
        
        assertTrue("Dog vision button should be visible", dogButton.exists())
        assertTrue("Cat vision button should be visible", catButton.exists())
        assertTrue("Bird vision button should be visible", birdButton.exists())
        assertTrue("Original vision button should be visible", originalButton.exists())
    }

    @Test
    fun testFilterTextViewExists() {
        // Verify the active filter text view is present and shows default text
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        assertTrue("Filter text view should exist", filterTextView.waitForExists(TIMEOUT))
        
        val text = filterTextView.text
        assertTrue("Filter text should contain 'Human Vision'", text.contains("Human Vision"))
    }

    @Test
    fun testDogVisionFilter() {
        // Click dog vision button
        val dogButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"))
        assertTrue("Dog button should exist", dogButton.waitForExists(TIMEOUT))
        dogButton.click()
        
        // Wait for filter to apply
        device.waitForIdle()
        Thread.sleep(FILTER_SWITCH_DELAY)
        
        // Verify filter text updates
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        val text = filterTextView.text
        assertTrue("Filter text should show Dog Vision", text.contains("Dog Vision"))
        
        // Verify toast appears
        val toast = device.findObject(UiSelector().textContains("Dog Vision"))
        assertTrue("Dog vision toast should appear", toast.waitForExists(TIMEOUT))
    }

    @Test
    fun testCatVisionFilter() {
        // Click cat vision button
        val catButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/catVisionButton"))
        assertTrue("Cat button should exist", catButton.waitForExists(TIMEOUT))
        catButton.click()
        
        // Wait for filter to apply
        device.waitForIdle()
        Thread.sleep(FILTER_SWITCH_DELAY)
        
        // Verify filter text updates
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        val text = filterTextView.text
        assertTrue("Filter text should show Cat Vision", text.contains("Cat Vision"))
        
        // Verify toast appears
        val toast = device.findObject(UiSelector().textContains("Cat Vision"))
        assertTrue("Cat vision toast should appear", toast.waitForExists(TIMEOUT))
    }

    @Test
    fun testBirdVisionFilter() {
        // Click bird vision button
        val birdButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/birdVisionButton"))
        assertTrue("Bird button should exist", birdButton.waitForExists(TIMEOUT))
        birdButton.click()
        
        // Wait for filter to apply
        device.waitForIdle()
        Thread.sleep(FILTER_SWITCH_DELAY)
        
        // Verify filter text updates
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        val text = filterTextView.text
        assertTrue("Filter text should show Bird Vision", text.contains("Bird Vision"))
        
        // Verify toast appears
        val toast = device.findObject(UiSelector().textContains("Bird Vision"))
        assertTrue("Bird vision toast should appear", toast.waitForExists(TIMEOUT))
    }

    @Test
    fun testOriginalVisionFilter() {
        // First switch to a different filter
        val dogButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"))
        dogButton.click()
        Thread.sleep(FILTER_SWITCH_DELAY)
        
        // Then switch back to original
        val originalButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/originalVisionButton"))
        assertTrue("Original button should exist", originalButton.waitForExists(TIMEOUT))
        originalButton.click()
        
        // Wait for filter to apply
        device.waitForIdle()
        Thread.sleep(FILTER_SWITCH_DELAY)
        
        // Verify filter text updates
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        val text = filterTextView.text
        assertTrue("Filter text should show Human Vision", text.contains("Human Vision"))
        
        // Verify toast appears
        val toast = device.findObject(UiSelector().textContains("Human Vision"))
        assertTrue("Human vision toast should appear", toast.waitForExists(TIMEOUT))
    }

    @Test
    fun testFilterSwitching() {
        // Test rapid filter switching to ensure no crashes
        val buttons = listOf(
            device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton")),
            device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/catVisionButton")),
            device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/birdVisionButton")),
            device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/originalVisionButton"))
        )
        
        // Click each button multiple times
        repeat(3) {
            buttons.forEach { button ->
                if (button.exists()) {
                    button.click()
                    Thread.sleep(500) // Short delay between clicks
                    device.waitForIdle()
                }
            }
        }
        
        // Verify app is still responsive
        val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        assertTrue("Filter text view should still exist after rapid switching", filterTextView.exists())
    }

    @Test
    fun testCameraPreviewIsActive() {
        // Wait for camera to initialize
        Thread.sleep(3000)
        
        // Verify preview view exists and is visible
        val previewView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/previewView"))
        assertTrue("Camera preview should be visible", previewView.exists())
        assertTrue("Camera preview should be clickable/active", previewView.isClickable || previewView.isEnabled)
    }

    @Test
    fun testUIElementsAreAccessible() {
        // Test that all UI elements are accessible for users with disabilities
        val dogButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"))
        val catButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/catVisionButton"))
        val birdButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/birdVisionButton"))
        val originalButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/originalVisionButton"))
        
        // Verify buttons are clickable
        assertTrue("Dog button should be clickable", dogButton.isClickable)
        assertTrue("Cat button should be clickable", catButton.isClickable)
        assertTrue("Bird button should be clickable", birdButton.isClickable)
        assertTrue("Original button should be clickable", originalButton.isClickable)
        
        // Verify buttons have text content
        assertTrue("Dog button should have text", dogButton.text.isNotEmpty())
        assertTrue("Cat button should have text", catButton.text.isNotEmpty())
        assertTrue("Bird button should have text", birdButton.text.isNotEmpty())
        assertTrue("Original button should have text", originalButton.text.isNotEmpty())
    }

    @Test
    fun testAppRotation() {
        // Test app behavior during device rotation
        val originalFilterText = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        val originalText = originalFilterText.text
        
        // Rotate to landscape
        device.setOrientationLeft()
        Thread.sleep(2000) // Wait for rotation to complete
        
        // Verify UI elements still exist
        val filterTextAfterRotation = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
        assertTrue("Filter text should exist after rotation", filterTextAfterRotation.waitForExists(TIMEOUT))
        
        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(2000)
        
        // Verify app is still functional
        val dogButton = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"))
        assertTrue("Dog button should exist after rotation", dogButton.exists())
    }

    @Test
    fun testMemoryLeaks() {
        // Test for potential memory leaks by switching filters many times
        val buttons = listOf(
            UiSelector().resourceId("$PACKAGE_NAME:id/dogVisionButton"),
            UiSelector().resourceId("$PACKAGE_NAME:id/catVisionButton"),
            UiSelector().resourceId("$PACKAGE_NAME:id/birdVisionButton"),
            UiSelector().resourceId("$PACKAGE_NAME:id/originalVisionButton")
        )
        
        // Perform many filter switches
        repeat(20) { iteration ->
            buttons.forEach { selector ->
                val button = device.findObject(selector)
                if (button.exists()) {
                    button.click()
                    Thread.sleep(200)
                    device.waitForIdle()
                }
            }
            
            // Check if app is still responsive every 5 iterations
            if (iteration % 5 == 0) {
                val filterTextView = device.findObject(UiSelector().resourceId("$PACKAGE_NAME:id/activeFilterTextView"))
                assertTrue("App should remain responsive after $iteration iterations", filterTextView.exists())
            }
        }
    }

    private fun grantCameraPermissionIfNeeded() {
        // Handle camera permission dialog if it appears
        val allowButton = device.findObject(UiSelector().text("Allow").className("android.widget.Button"))
        if (allowButton.waitForExists(3000)) {
            allowButton.click()
        }
        
        // Alternative text for permission dialog
        val allowButtonAlt = device.findObject(UiSelector().text("ALLOW").className("android.widget.Button"))
        if (allowButtonAlt.waitForExists(1000)) {
            allowButtonAlt.click()
        }
    }

    private fun startApp() {
        val intent = Intent().apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(PACKAGE_NAME, "com.example.crittervision.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        
        // Wait for app to launch
        device.wait(Until.hasObject(UiSelector().packageName(PACKAGE_NAME)), TIMEOUT)
    }
}
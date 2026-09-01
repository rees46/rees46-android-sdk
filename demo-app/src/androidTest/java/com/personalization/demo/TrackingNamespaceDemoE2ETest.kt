package com.personalization.demo

import android.Manifest
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Taps every button of the demo's tracking section and reads the result line the handler writes.
 *
 * The handlers call `sdk.tracking.*` against the real API, so a green run means both that the demo
 * is wired to the namespace and that every method works end to end.
 */
@RunWith(AndroidJUnit4::class)
class TrackingNamespaceDemoE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    companion object {
        /**
         * Grant POST_NOTIFICATIONS before the first activity launch, so the system permission
         * dialog never pauses the activity — Espresso fails with NoActivityResumedException while
         * it is up. Same guard as [MultiInstanceE2ETest].
         */
        @JvmStatic
        @BeforeClass
        fun grantNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        private const val RESULT_TIMEOUT_MS = 20_000L
        private const val POLL_INTERVAL_MS = 100L
    }

    @Test
    fun productView_reportsSuccess() = tapAndExpect(R.id.btnTrackingProductView, "productView OK")

    @Test
    fun categoryView_reportsSuccess() = tapAndExpect(R.id.btnTrackingCategoryView, "categoryView OK")

    @Test
    fun search_reportsSuccess() = tapAndExpect(R.id.btnTrackingSearch, "search OK")

    @Test
    fun addToCart_reportsSuccess() = tapAndExpect(R.id.btnTrackingAddToCart, "addToCart OK")

    @Test
    fun syncCart_reportsSuccess() = tapAndExpect(R.id.btnTrackingSyncCart, "syncCart OK")

    @Test
    fun removeFromCart_reportsSuccess() =
        tapAndExpect(R.id.btnTrackingRemoveFromCart, "removeFromCart OK")

    @Test
    fun addToFavorites_reportsSuccess() =
        tapAndExpect(R.id.btnTrackingAddToFavorites, "addToFavorites OK")

    @Test
    fun syncFavorites_reportsSuccess() =
        tapAndExpect(R.id.btnTrackingSyncFavorites, "syncFavorites OK")

    @Test
    fun removeFromFavorites_reportsSuccess() =
        tapAndExpect(R.id.btnTrackingRemoveFromFavorites, "removeFromFavorites OK")

    @Test
    fun setSource_reportsSuccess() = tapAndExpect(R.id.btnTrackingSetSource, "setSource OK")

    private fun tapAndExpect(buttonId: Int, expected: String) {
        onView(withId(buttonId)).perform(scrollTo(), click())
        awaitResult(expected)
        onView(withId(R.id.tvTrackingNamespaceResult)).check(matches(withText(expected)))
    }

    /** The call is asynchronous — poll the result line instead of sleeping a fixed amount. */
    private fun awaitResult(expected: String) {
        val deadline = System.currentTimeMillis() + RESULT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            var current: String? = null
            activityRule.scenario.onActivity { activity ->
                current = activity
                    .findViewById<android.widget.TextView>(R.id.tvTrackingNamespaceResult)
                    .text
                    .toString()
            }
            if (current == expected) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }
}

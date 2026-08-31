package com.personalization.stories

import com.personalization.api.OnApiCallbackListener
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.trackingSource.SetTrackingSourceUseCase
import com.personalization.stories.models.Story
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [StoriesManager.loadStories], the stateless data path introduced when [StoriesView] took
 * over its own loading. The manager no longer holds a view — it fetches, parses and hands the
 * result back, so a caller (the view) can render several blocks independently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoriesManagerLoadTest {

    private lateinit var sendNetworkMethodUseCase: SendNetworkMethodUseCase
    private lateinit var setTrackingSourceUseCase: SetTrackingSourceUseCase
    private lateinit var storiesManager: StoriesManager

    @Before
    fun setUp() {
        sendNetworkMethodUseCase = mockk(relaxed = true)
        setTrackingSourceUseCase = mockk(relaxed = true)
        storiesManager = StoriesManager(setTrackingSourceUseCase, sendNetworkMethodUseCase)
    }

    @Test
    fun `loadStories requests the block for the given code`() {
        storiesManager.loadStories("block-code") { }

        verify { sendNetworkMethodUseCase.getAsync("stories/block-code", any(), any()) }
    }

    @Test
    fun `loadStories parses the response and hands the stories back`() {
        val listener = slot<OnApiCallbackListener>()
        every { sendNetworkMethodUseCase.getAsync(any(), any(), capture(listener)) } just Runs

        var delivered: List<Story>? = null
        storiesManager.loadStories("block-code") { delivered = it }

        listener.captured.onSuccess(
            JSONObject(
                """
                {"stories":[
                  {"id":1,"name":"First","avatar":"a","slides":[]},
                  {"id":2,"name":"Second","avatar":"b","slides":[]}
                ]}
                """.trimIndent()
            )
        )

        assertEquals(2, delivered?.size)
        assertEquals("First", delivered?.get(0)?.name)
        assertEquals("Second", delivered?.get(1)?.name)
    }

    @Test
    fun `loadStories delivers an empty list for an empty block`() {
        val listener = slot<OnApiCallbackListener>()
        every { sendNetworkMethodUseCase.getAsync(any(), any(), capture(listener)) } just Runs

        var delivered: List<Story>? = null
        storiesManager.loadStories("block-code") { delivered = it }

        listener.captured.onSuccess(JSONObject("""{"stories":[]}"""))

        assertEquals(0, delivered?.size)
    }

    @Test
    fun `loadStories does not deliver on a null response`() {
        val listener = slot<OnApiCallbackListener>()
        every { sendNetworkMethodUseCase.getAsync(any(), any(), capture(listener)) } just Runs

        var called = false
        storiesManager.loadStories("block-code") { called = true }

        listener.captured.onSuccess(null)

        assertEquals(false, called)
    }
}

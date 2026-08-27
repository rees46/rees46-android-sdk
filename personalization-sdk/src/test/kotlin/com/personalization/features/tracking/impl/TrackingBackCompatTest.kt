package com.personalization.features.tracking.impl

import com.personalization.sdk.domain.models.RecommendedBy
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.stories.StoriesManager
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
 * Guards the upgrade path for the one existing entry point the namespace rewired: story tracking
 * used to take an `Int` story id and build its request inline, and now delegates to the string
 * overload the namespace speaks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackingBackCompatTest {

    private lateinit var sendNetworkMethodUseCase: SendNetworkMethodUseCase
    private lateinit var setRecommendedByUseCase: SetRecommendedByUseCase
    private lateinit var storiesManager: StoriesManager

    @Before
    fun setUp() {
        sendNetworkMethodUseCase = mockk(relaxed = true)
        setRecommendedByUseCase = mockk(relaxed = true)
        storiesManager = StoriesManager(setRecommendedByUseCase, sendNetworkMethodUseCase)
    }

    @Test
    fun legacyIntStoryId_producesTheSameRequestAsBefore() {
        storiesManager.trackStory(
            event = "view",
            code = "main_stories",
            storyId = 42,
            slideId = "3"
        )

        val body = capturedStoriesBody()
        assertEquals("view", body.getString("event"))
        assertEquals(42, body.getInt("story_id"))
        assertEquals("3", body.getString("slide_id"))
        assertEquals("main_stories", body.getString("code"))
    }

    @Test
    fun legacyCall_stillAttributesTheNextEventToStories() {
        val stored = slot<RecommendedBy>()

        storiesManager.trackStory(
            event = "click",
            code = "main_stories",
            storyId = 42,
            slideId = "3"
        )

        verify { setRecommendedByUseCase.invoke(capture(stored)) }
        assertEquals(RecommendedBy.TYPE.STORIES, stored.captured.type)
        assertEquals("main_stories", stored.captured.code)
    }

    @Test
    fun nonNumericStoryId_isSentAsAString() {
        storiesManager.trackStory(
            event = "view",
            code = "main_stories",
            storyId = "story-a",
            slideId = "3"
        )

        assertEquals("story-a", capturedStoriesBody().getString("story_id"))
    }

    private fun capturedStoriesBody(): JSONObject {
        val bodies = mutableListOf<JSONObject>()
        verify {
            sendNetworkMethodUseCase.postAsync(
                StoriesManager.TRACK_STORIES_METHOD,
                capture(bodies),
                any()
            )
        }
        return bodies.last()
    }
}

package com.personalization.features.trackEvent.impl

import com.personalization.api.OnApiCallbackListener
import com.personalization.api.managers.InAppNotificationManager
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.GetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.userSettings.GetUserSettingsValueUseCase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import com.personalization.sdk.domain.usecases.trackingSource.GetTrackingSourceUseCase
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackEventManagerImplTrackEventTest {

    private lateinit var sendNetworkMethodUseCase: SendNetworkMethodUseCase
    private lateinit var inAppNotificationManager: InAppNotificationManager
    private lateinit var getRecommendedByUseCase: GetRecommendedByUseCase
    private lateinit var setRecommendedByUseCase: SetRecommendedByUseCase
    private lateinit var getUserSettingsValueUseCase: GetUserSettingsValueUseCase
    private lateinit var impl: TrackEventManagerImpl

    @Before
    fun setUp() {
        sendNetworkMethodUseCase = mockk(relaxed = true)
        inAppNotificationManager = mockk(relaxed = true)
        getRecommendedByUseCase = mockk(relaxed = true)
        setRecommendedByUseCase = mockk(relaxed = true)
        getUserSettingsValueUseCase = mockk(relaxed = true)

        every { getRecommendedByUseCase.invoke() } returns null

        impl = TrackEventManagerImpl(
            getRecommendedByUseCase,
            setRecommendedByUseCase,
            sendNetworkMethodUseCase,
            inAppNotificationManager,
            getUserSettingsValueUseCase,
            mockk<GetTrackingSourceUseCase>(relaxed = true).also {
                every { it.invoke() } returns null
            }
        )
    }

    @Test
    fun trackEvent_reservedKeyInCustomFields_doesNotPost_invokesOnError() {
        var capturedCode = 0
        var capturedMessage: String? = null
        val listener = mockk<OnApiCallbackListener>()
        every { listener.onError(any(), any()) } answers {
            capturedCode = invocation.args[0] as Int
            capturedMessage = invocation.args[1] as String?
        }

        impl.trackEvent(
            event = "demo_event",
            time = null,
            category = null,
            label = null,
            value = null,
            customFields = mapOf("shop_id" to "x"),
            listener = listener
        )

        verify(exactly = 0) { sendNetworkMethodUseCase.postAsync(any(), any(), any()) }
        verify(exactly = 1) { listener.onError(any(), any()) }
        assertEquals(TrackCustomEventPayloadHelper.CLIENT_VALIDATION_ERROR_CODE, capturedCode)
        assertTrue(capturedMessage!!.contains("shop_id"))
        assertTrue(capturedMessage!!.contains("trackEvent:"))
    }

    @Test
    fun trackEvent_multipleReservedKeys_errorListsSortedKeys() {
        var capturedMessage: String? = null
        val listener = mockk<OnApiCallbackListener>()
        every { listener.onError(any(), any()) } answers {
            capturedMessage = invocation.args[1] as String?
        }

        impl.trackEvent(
            event = "e",
            customFields = mapOf("event" to "bad", "payload" to "bad"),
            listener = listener
        )

        verify(exactly = 0) { sendNetworkMethodUseCase.postAsync(any(), any(), any()) }
        assertTrue(capturedMessage!!.contains("event, payload"))
    }

    @Test
    fun trackEvent_validCustomFields_postsPushCustom_mergesTopLevelAndPayload() {
        val listener = mockk<OnApiCallbackListener>(relaxed = true)
        val paramsSlot = slot<JSONObject>()
        every {
            sendNetworkMethodUseCase.postAsync(any(), capture(paramsSlot), any())
        } just Runs

        impl.trackEvent(
            event = "my_event",
            time = 123_456,
            category = "cat",
            label = "lab",
            value = 100,
            customFields = mapOf("demo_custom_key" to "ios_demo_app", "n" to 42),
            listener = listener
        )

        verify(exactly = 1) { sendNetworkMethodUseCase.postAsync("push/custom", any(), any()) }

        val body = paramsSlot.captured
        assertEquals("my_event", body.getString("event"))
        assertEquals(123_456, body.getInt("time"))
        assertEquals("cat", body.getString("category"))
        assertEquals("lab", body.getString("label"))
        assertEquals(100, body.getInt("value"))
        assertEquals("ios_demo_app", body.getString("demo_custom_key"))
        assertEquals(42, body.getInt("n"))

        val payload = body.getJSONObject("payload")
        assertEquals(2, payload.length())
        assertEquals("ios_demo_app", payload.getString("demo_custom_key"))
        assertEquals(42, payload.getInt("n"))
    }

    @Test
    fun trackEvent_onlyNullCustomFieldValues_omitsPayload() {
        val paramsSlot = slot<JSONObject>()
        every {
            sendNetworkMethodUseCase.postAsync(any(), capture(paramsSlot), any())
        } just Runs

        impl.trackEvent(
            event = "e",
            customFields = mapOf("k" to null),
            listener = null
        )

        val body = paramsSlot.captured
        assertEquals("e", body.getString("event"))
        assertFalse(body.has("payload"))
    }
}

package com.personalization.features.tracking.impl

import com.personalization.Params
import com.personalization.Params.TrackEvent
import com.personalization.api.managers.InAppNotificationManager
import com.personalization.api.managers.TrackingApi
import com.personalization.api.models.purchase.PurchaseItemRequest
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.models.tracking.TrackingItem
import com.personalization.api.models.tracking.TrackingSource
import com.personalization.api.models.tracking.TrackingSourceType
import com.personalization.api.params.ProductItemParams
import com.personalization.features.trackEvent.impl.TrackEventManagerImpl
import com.personalization.sdk.domain.models.RecommendedBy
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.GetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.userSettings.GetUserSettingsValueUseCase
import com.personalization.stories.StoriesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the `tracking` namespace end to end: every method is driven through the real
 * [TrackEventManagerImpl] / [StoriesManager], and the request body they hand to the network layer
 * is what the assertions read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackingApiImplTest {

    private lateinit var sendNetworkMethodUseCase: SendNetworkMethodUseCase
    private lateinit var setRecommendedByUseCase: SetRecommendedByUseCase
    private lateinit var storiesManager: StoriesManager
    private lateinit var trackEventManager: TrackEventManagerImpl
    private lateinit var tracking: TrackingApi

    @Before
    fun setUp() {
        sendNetworkMethodUseCase = mockk(relaxed = true)
        setRecommendedByUseCase = mockk(relaxed = true)
        val getRecommendedByUseCase = mockk<GetRecommendedByUseCase>(relaxed = true)
        every { getRecommendedByUseCase.invoke() } returns null

        trackEventManager = TrackEventManagerImpl(
            getRecommendedByUseCase,
            setRecommendedByUseCase,
            sendNetworkMethodUseCase,
            mockk<InAppNotificationManager>(relaxed = true),
            mockk<GetUserSettingsValueUseCase>(relaxed = true)
        )
        storiesManager = StoriesManager(setRecommendedByUseCase, sendNetworkMethodUseCase)
        tracking = TrackingApiImpl(trackEventManager, storiesManager, setRecommendedByUseCase)
    }

    // region events

    @Test
    fun productView_postsViewWithTheItem() {
        tracking.productView("sku-1")

        val body = capturedBody(path = "push")
        assertEquals("view", body.getString("event"))
        assertEquals("sku-1", body.getJSONArray("items").getJSONObject(0).getString("id"))
    }

    @Test
    fun categoryView_postsCategoryId() {
        tracking.categoryView("women-shoes")

        val body = capturedBody(path = "push")
        assertEquals("category", body.getString("event"))
        assertEquals("women-shoes", body.getString("category_id"))
    }

    @Test
    fun search_postsQueryAndResults() {
        tracking.search(query = "boots", results = listOf("sku-1", "sku-2"))

        val body = capturedBody(path = "push")
        assertEquals("search", body.getString("event"))
        assertEquals("boots", body.getString("search_query"))
        assertEquals("sku-1,sku-2", body.getString("results"))
    }

    @Test
    fun search_withoutResults_omitsThem() {
        tracking.search(query = "boots")

        val body = capturedBody(path = "push")
        assertFalse(body.has("results"))
    }

    @Test
    fun addToCart_carriesQuantityAndPrice() {
        tracking.addToCart(TrackingItem(id = "sku-1", quantity = 3, price = 49.9))

        val item = capturedBody(path = "push").getJSONArray("items").getJSONObject(0)
        assertEquals("sku-1", item.getString("id"))
        assertEquals("3", item.getString("amount"))
        assertEquals("49.9", item.getString("price"))
    }

    @Test
    fun syncCart_marksTheCartAsFull() {
        tracking.syncCart(
            listOf(
                TrackingItem(id = "sku-1", quantity = 2, price = 10.0),
                TrackingItem(id = "sku-2")
            )
        )

        val body = capturedBody(path = "push")
        assertEquals("cart", body.getString("event"))
        assertTrue(body.getBoolean("full_cart"))
        assertEquals(2, body.getJSONArray("items").length())
    }

    @Test
    fun removeFromCart_postsRemoveEvent() {
        tracking.removeFromCart("sku-1")

        assertEquals("remove_from_cart", capturedBody(path = "push").getString("event"))
    }

    @Test
    fun favorites_postWishEvents() {
        tracking.addToFavorites("sku-1")
        assertEquals("wish", capturedBody(path = "push").getString("event"))

        tracking.removeFromFavorites("sku-1")
        assertEquals("remove_wish", capturedBody(path = "push").getString("event"))

        tracking.syncFavorites(listOf("sku-1", "sku-2"))
        val synced = capturedBody(path = "push")
        assertEquals("wish", synced.getString("event"))
        assertTrue(synced.getBoolean("full_wish"))
        assertEquals(2, synced.getJSONArray("items").length())
    }

    @Test
    fun custom_postsToTheCustomEndpoint() {
        tracking.custom(
            event = "checkout_step",
            time = 1000,
            category = "checkout",
            label = "delivery",
            value = 2,
            customFields = mapOf("delivery_type" to "courier")
        )

        val body = capturedBody(path = "push/custom")
        assertEquals("checkout_step", body.getString("event"))
        assertEquals(1000, body.getInt("time"))
        assertEquals("courier", body.getString("delivery_type"))
        assertEquals("courier", body.getJSONObject("payload").getString("delivery_type"))
    }

    @Test
    fun purchase_postsTheOrder() {
        tracking.purchase(
            PurchaseTrackingRequest(
                orderId = "order-1",
                orderPrice = 100.0,
                items = listOf(PurchaseItemRequest(id = "sku-1", amount = 1, price = 100.0))
            )
        )

        val body = capturedBody(path = "push")
        assertEquals("purchase", body.getString("event"))
        assertEquals("order-1", body.getString("order_id"))
    }

    // endregion

    // region stories

    @Test
    fun storyView_postsToTheStoriesEndpoint() {
        tracking.storyView(storyId = "42", slideId = "3", code = "main_stories")

        val body = capturedBody(path = StoriesManager.TRACK_STORIES_METHOD)
        assertEquals("view", body.getString("event"))
        assertEquals(42, body.getInt("story_id"))
        assertEquals("3", body.getString("slide_id"))
        assertEquals("main_stories", body.getString("code"))
    }

    @Test
    fun storyClick_withoutCode_fallsBackToTheLoadedBlock() {
        storiesManager.requestStories("loaded_block", mockk(relaxed = true))

        tracking.storyClick(storyId = "42", slideId = "3")

        val body = capturedBody(path = StoriesManager.TRACK_STORIES_METHOD)
        assertEquals("click", body.getString("event"))
        assertEquals("loaded_block", body.getString("code"))
    }

    @Test
    fun storyView_withoutAnyCode_isDropped() {
        tracking.storyView(storyId = "42", slideId = "3")

        verify(exactly = 0) {
            sendNetworkMethodUseCase.postAsync(
                StoriesManager.TRACK_STORIES_METHOD,
                any(),
                any()
            )
        }
    }

    // endregion

    // region attribution

    @Test
    fun source_isSentAsRecommendedBy() {
        tracking.productView(
            itemId = "sku-1",
            source = TrackingSource(TrackingSourceType.DYNAMIC, "popular")
        )

        val body = capturedBody(path = "push")
        assertEquals("dynamic", body.getString("recommended_by"))
        assertEquals("popular", body.getString("recommended_code"))
    }

    @Test
    fun storedSource_isSentAsItsWireValue() {
        val getRecommendedByUseCase = mockk<GetRecommendedByUseCase>(relaxed = true)
        every { getRecommendedByUseCase.invoke() } returns
            RecommendedBy(RecommendedBy.TYPE.RECOMMENDATION, "popular")
        val manager = TrackEventManagerImpl(
            getRecommendedByUseCase,
            setRecommendedByUseCase,
            sendNetworkMethodUseCase,
            mockk<InAppNotificationManager>(relaxed = true),
            mockk<GetUserSettingsValueUseCase>(relaxed = true)
        )

        TrackingApiImpl(manager, storiesManager, setRecommendedByUseCase).productView("sku-1")

        val body = capturedBody(path = "push")
        assertEquals("dynamic", body.getString("recommended_by"))
        assertEquals("popular", body.getString("recommended_code"))
    }

    @Test
    fun setSource_storesItForTheNextEvent() {
        val stored = slot<RecommendedBy>()
        every { setRecommendedByUseCase.invoke(capture(stored)) } returns Unit

        tracking.setSource(TrackingSource(TrackingSourceType.FULL_SEARCH, "boots"))

        assertEquals(RecommendedBy.TYPE.FULL_SEARCH, stored.captured.type)
        assertEquals("boots", stored.captured.code)
    }

    // endregion

    // region the namespace builds what the previous API built

    @Test
    fun namespaceAndLegacyCallsProduceTheSameBody() {
        assertSameBody(
            legacy = { it.track(TrackEvent.VIEW, Params().put(ProductItemParams("sku-1"))) },
            namespace = { it.productView("sku-1") }
        )
        assertSameBody(
            legacy = { it.track(TrackEvent.CATEGORY, Params().put(Params.Parameter.CATEGORY_ID, "cat-1")) },
            namespace = { it.categoryView("cat-1") }
        )
        assertSameBody(
            legacy = { it.track(TrackEvent.SEARCH, Params().put(Params.Parameter.SEARCH_QUERY, "boots")) },
            namespace = { it.search("boots") }
        )
        assertSameBody(
            legacy = {
                it.track(
                    TrackEvent.CART,
                    Params().put(
                        ProductItemParams("sku-1").set(ProductItemParams.PARAMETER.AMOUNT, 2)
                    )
                )
            },
            namespace = { it.addToCart(TrackingItem(id = "sku-1", quantity = 2)) }
        )
        assertSameBody(
            legacy = { it.track(TrackEvent.REMOVE_FROM_CART, Params().put(ProductItemParams("sku-1"))) },
            namespace = { it.removeFromCart("sku-1") }
        )
        assertSameBody(
            legacy = { it.track(TrackEvent.WISH, Params().put(ProductItemParams("sku-1"))) },
            namespace = { it.addToFavorites("sku-1") }
        )
        assertSameBody(
            legacy = { it.track(TrackEvent.REMOVE_FROM_WISH, Params().put(ProductItemParams("sku-1"))) },
            namespace = { it.removeFromFavorites("sku-1") }
        )
    }

    private fun assertSameBody(
        legacy: (TrackEventManagerImpl) -> Unit,
        namespace: (TrackingApi) -> Unit
    ) {
        legacy(trackEventManager)
        val legacyBody = capturedBody(path = "push").toString()

        namespace(tracking)
        val namespaceBody = capturedBody(path = "push").toString()

        assertEquals(legacyBody, namespaceBody)
    }

    // endregion

    /**
     * The body of the most recent post to [path]. Captured into a list rather than a slot: some
     * tests post several times, and mockk refuses slot capture for a repeated call.
     */
    private fun capturedBody(path: String): JSONObject {
        val bodies = mutableListOf<JSONObject>()
        verify {
            sendNetworkMethodUseCase.postAsync(
                path,
                capture(bodies),
                any()
            )
        }
        return bodies.last()
    }

    @Test
    fun trackingItem_defaultsToOneUnitAndNoPrice() {
        val item = TrackingItem(id = "sku-1")

        assertEquals(1, item.quantity)
        assertNull(item.price)
        assertNull(item.fashionSize)
    }
}

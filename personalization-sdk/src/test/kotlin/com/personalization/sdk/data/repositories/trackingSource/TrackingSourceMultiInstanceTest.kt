package com.personalization.sdk.data.repositories.trackingSource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.personalization.PreferencesPartition
import com.personalization.sdk.data.repositories.preferences.PreferencesDataSourceImpl
import com.personalization.sdk.domain.models.StoredTrackingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Multi-instance isolation for the attribution `tracking.setSource(...)` stores.
 *
 * Two shops can be initialized at once, and each SDK instance builds its own Dagger graph, so it
 * gets its own [PreferencesDataSourceImpl] pointed at its own partition. These tests go through the
 * real derivation — `PreferencesPartition.keyFor(shopId)` over real Robolectric SharedPreferences —
 * rather than two stand-ins, so the wiring from shop id to stored source is what is under test.
 *
 * Mirror of iOS `TrackingSourceStoreTests`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackingSourceMultiInstanceTest {

    private lateinit var context: Context

    private val shopA = "shop-a"
    private val shopB = "shop-b"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Robolectric keeps SharedPreferences across tests in one process.
        for (shop in listOf(shopA, shopB)) {
            context.getSharedPreferences(PreferencesPartition.keyFor(shop), Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    /** The store an instance for [shopId] would end up with after `SDK.initialize`. */
    private fun storeFor(shopId: String): TrackingSourceDataSourceImpl {
        val preferences = PreferencesDataSourceImpl()
        preferences.initialize(
            context = context,
            preferencesKey = PreferencesPartition.keyFor(shopId),
            legacyPreferencesKey = null,
            shopId = shopId
        )
        return TrackingSourceDataSourceImpl(preferences)
    }

    @Test
    fun `a source set for one shop is invisible to the other`() {
        val a = storeFor(shopA)
        val b = storeFor(shopB)

        a.setSource(StoredTrackingSource(type = "dynamic", code = "shop-a-block"))

        assertEquals(StoredTrackingSource("dynamic", "shop-a-block"), a.getSource())
        assertNull("shop B was coloured by shop A's attribution", b.getSource())
    }

    @Test
    fun `each shop keeps its own source at the same time`() {
        val a = storeFor(shopA)
        val b = storeFor(shopB)

        a.setSource(StoredTrackingSource(type = "dynamic", code = "block-a"))
        b.setSource(StoredTrackingSource(type = "stories", code = "block-b"))

        assertEquals(StoredTrackingSource("dynamic", "block-a"), a.getSource())
        assertEquals(StoredTrackingSource("stories", "block-b"), b.getSource())
    }

    @Test
    fun `clearing one shop's source leaves the other's alone`() {
        val a = storeFor(shopA)
        val b = storeFor(shopB)
        a.setSource(StoredTrackingSource(type = "dynamic", code = "block-a"))
        b.setSource(StoredTrackingSource(type = "bulk", code = "block-b"))

        // Expiring shop A's window is the only way its source goes away.
        context.getSharedPreferences(PreferencesPartition.keyFor(shopA), Context.MODE_PRIVATE)
            .edit()
            .putLong("tracking_source_saved_at", System.currentTimeMillis() - (49L * 60 * 60 * 1000))
            .commit()

        assertNull(a.getSource())
        assertEquals(StoredTrackingSource("bulk", "block-b"), b.getSource())
    }

    /** A shop's source is written to disk, so a new instance for the same shop still sees it. */
    @Test
    fun `a source survives the instance that set it`() {
        storeFor(shopA).setSource(StoredTrackingSource(type = "chain", code = "block-a"))

        val freshInstance = storeFor(shopA)

        assertEquals(StoredTrackingSource("chain", "block-a"), freshInstance.getSource())
    }

    /**
     * A host that passes its own `preferencesKey` opts out of per-shop partitioning — two shops
     * sharing one key deliberately share one store. Pinned so the opt-out stays visible.
     */
    @Test
    fun `a host-supplied preferences key is used verbatim`() {
        context.getSharedPreferences("host_own_key", Context.MODE_PRIVATE).edit().clear().commit()
        fun hostStore(): TrackingSourceDataSourceImpl {
            val preferences = PreferencesDataSourceImpl()
            preferences.initialize(context, "host_own_key", null, null)
            return TrackingSourceDataSourceImpl(preferences)
        }

        hostStore().setSource(StoredTrackingSource(type = "dynamic", code = "shared"))

        assertEquals(StoredTrackingSource("dynamic", "shared"), hostStore().getSource())
        assertNull("the per-shop partition must stay clean", storeFor(shopA).getSource())
    }
}

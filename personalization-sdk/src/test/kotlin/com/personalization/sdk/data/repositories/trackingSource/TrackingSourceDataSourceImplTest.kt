package com.personalization.sdk.data.repositories.trackingSource

import com.personalization.sdk.data.repositories.preferences.PreferencesDataSource
import com.personalization.sdk.domain.models.StoredTrackingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The store behind `tracking.setSource(...)`. Port of iOS `TrackingSourceStoreTests`: the source
 * outlives a cold start and expires after 48h.
 */
class TrackingSourceDataSourceImplTest {

    private lateinit var preferences: FakePreferences
    private lateinit var dataSource: TrackingSourceDataSourceImpl

    @Before
    fun setUp() {
        preferences = FakePreferences()
        dataSource = TrackingSourceDataSourceImpl(preferences)
    }

    @Test
    fun aStoredSource_isReadBack() {
        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "popular"))

        assertEquals(StoredTrackingSource("dynamic", "popular"), dataSource.getSource())
    }

    @Test
    fun nothingStored_readsAsNoSource() {
        assertNull(dataSource.getSource())
    }

    /** Preferences survive process death, so a source set yesterday still applies today. */
    @Test
    fun aSourceStoredBeforeARestart_isStillThere() {
        dataSource.setSource(StoredTrackingSource(type = "stories", code = "main_stories"))

        val afterRestart = TrackingSourceDataSourceImpl(preferences)

        assertEquals(StoredTrackingSource("stories", "main_stories"), afterRestart.getSource())
    }

    @Test
    fun aSourceOlderThan48Hours_isDropped() {
        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "stale"))
        preferences.values["tracking_source_saved_at"] =
            System.currentTimeMillis() - (49L * 60 * 60 * 1000)

        assertNull(dataSource.getSource())
        assertNull("an expired source is cleared, not re-read", preferences.values["tracking_source_code"])
    }

    @Test
    fun aSourceJustInsideTheWindow_survives() {
        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "fresh"))
        preferences.values["tracking_source_saved_at"] =
            System.currentTimeMillis() - (47L * 60 * 60 * 1000)

        assertEquals(StoredTrackingSource("dynamic", "fresh"), dataSource.getSource())
    }

    @Test
    fun settingASecondSource_replacesTheFirstAndRestartsTheWindow() {
        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "first"))
        dataSource.setSource(StoredTrackingSource(type = "bulk", code = "second"))

        assertEquals(StoredTrackingSource("bulk", "second"), dataSource.getSource())
    }

    /**
     * Storage is partitioned per shop, so an attribution set for one instance must not colour
     * another's events. Mirrors iOS `test_sourceSetForOneShop_isNotVisibleToAnother`.
     */
    @Test
    fun aSourceSetForOneShop_isNotVisibleToAnother() {
        val otherShop = TrackingSourceDataSourceImpl(FakePreferences())

        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "shop-a-block"))

        assertEquals(StoredTrackingSource("dynamic", "shop-a-block"), dataSource.getSource())
        assertNull("a source set for one shop leaked into another", otherShop.getSource())
    }

    /** A half-written record is not an attribution. */
    @Test
    fun aRecordMissingItsCode_readsAsNoSource() {
        dataSource.setSource(StoredTrackingSource(type = "dynamic", code = "popular"))
        preferences.values["tracking_source_code"] = ""

        assertNull(dataSource.getSource())
    }

    /** Minimal in-memory stand-in; only the four methods the store touches are meaningful. */
    private class FakePreferences : PreferencesDataSource {
        val values = mutableMapOf<String, Any?>()

        override fun getValue(field: String, defaultValue: String): String =
            values[field] as? String ?: defaultValue

        override fun getValue(field: String, defaultValue: Long): Long =
            values[field] as? Long ?: defaultValue

        override fun <T> saveValue(field: String, value: T) {
            values[field] = value
        }

        override fun removeValue(field: String) {
            values.remove(field)
        }

        override fun initialize(
            context: android.content.Context,
            preferencesKey: String,
            legacyPreferencesKey: String?,
            shopId: String?
        ) = Unit

        override fun getPushToken(provider: String): String = ""
        override fun savePushToken(provider: String, value: String) = Unit
        override fun getLastPushTokenDate(provider: String): Long = 0L
        override fun saveLastPushTokenDate(provider: String, value: Long) = Unit
    }
}

package com.personalization.sdk.data.repositories.trackingSource

import com.personalization.sdk.data.repositories.preferences.PreferencesDataSource
import com.personalization.sdk.domain.models.StoredTrackingSource
import javax.inject.Inject

/**
 * Preferences-backed store for the attribution `tracking.setSource(...)` sets.
 *
 * Port of iOS `TrackingSourceStoreImpl`, down to the 48h window: the source survives a cold start
 * and colours every event until it expires. It lives in the shop's own preferences partition, so
 * two instances in one app do not share an attribution.
 */
class TrackingSourceDataSourceImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : TrackingSourceDataSource {

    override fun setSource(source: StoredTrackingSource) {
        preferencesDataSource.saveValue(TYPE_FIELD, source.type)
        preferencesDataSource.saveValue(CODE_FIELD, source.code)
        preferencesDataSource.saveValue(SAVED_AT_FIELD, System.currentTimeMillis())
    }

    override fun getSource(): StoredTrackingSource? {
        val savedAt = preferencesDataSource.getValue(SAVED_AT_FIELD, NEVER_SAVED)
        if (savedAt <= NEVER_SAVED || System.currentTimeMillis() - savedAt > TTL_MILLIS) {
            // Expired, or never set. Drop the values so a later read is cheap — same as iOS.
            preferencesDataSource.removeValue(TYPE_FIELD)
            preferencesDataSource.removeValue(CODE_FIELD)
            return null
        }

        val type = preferencesDataSource.getValue(TYPE_FIELD, EMPTY)
        val code = preferencesDataSource.getValue(CODE_FIELD, EMPTY)
        if (type.isEmpty() || code.isEmpty()) return null

        return StoredTrackingSource(type = type, code = code)
    }

    private companion object {
        const val TYPE_FIELD = "tracking_source_type"
        const val CODE_FIELD = "tracking_source_code"
        const val SAVED_AT_FIELD = "tracking_source_saved_at"

        const val NEVER_SAVED = 0L
        const val EMPTY = ""

        /** How long a stored source keeps colouring events. Same window as iOS. */
        const val TTL_MILLIS = 48L * 60 * 60 * 1000
    }
}

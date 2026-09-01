package com.personalization.sdk.data.repositories.trackingSource

import com.personalization.sdk.domain.models.StoredTrackingSource
import com.personalization.sdk.domain.repositories.TrackingSourceRepository
import javax.inject.Inject

class TrackingSourceRepositoryImpl @Inject constructor(
    private val trackingSourceDataSource: TrackingSourceDataSource
) : TrackingSourceRepository {

    override fun setSource(source: StoredTrackingSource) {
        trackingSourceDataSource.setSource(source)
    }

    override fun getSource(): StoredTrackingSource? = trackingSourceDataSource.getSource()
}

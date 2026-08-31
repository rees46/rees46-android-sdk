package com.personalization.sdk.domain.usecases.trackingSource

import com.personalization.sdk.domain.models.StoredTrackingSource
import com.personalization.sdk.domain.repositories.TrackingSourceRepository
import javax.inject.Inject

class SetTrackingSourceUseCase @Inject constructor(
    private val trackingSourceRepository: TrackingSourceRepository
) {

    operator fun invoke(type: String, code: String) {
        trackingSourceRepository.setSource(StoredTrackingSource(type = type, code = code))
    }
}

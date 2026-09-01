package com.personalization.sdk.domain.usecases.trackingSource

import com.personalization.sdk.domain.models.StoredTrackingSource
import com.personalization.sdk.domain.repositories.TrackingSourceRepository
import javax.inject.Inject

class GetTrackingSourceUseCase @Inject constructor(
    private val trackingSourceRepository: TrackingSourceRepository
) {

    operator fun invoke(): StoredTrackingSource? = trackingSourceRepository.getSource()
}

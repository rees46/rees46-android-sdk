package com.personalizatio.domain.features.notification.usecase

import com.personalizatio.data.repository.notification.SourceRepository
import javax.inject.Inject

class UpdateSourceUseCase @Inject constructor(
    private val sourceRepository: SourceRepository
) {

    operator fun invoke(
        type: String,
        id: String
    ) = sourceRepository.update(
            type = type,
            id = id
        )
}

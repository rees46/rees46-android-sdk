package com.personalizatio.domain.features.preferences.usecase

import android.content.SharedPreferences
import com.personalizatio.domain.repositories.PreferencesRepository
import javax.inject.Inject

class InitPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    operator fun invoke(sharedPreferences: SharedPreferences) {
        preferencesRepository.init(sharedPreferences)
    }
}

package com.personalizatio.domain.usecases.preferences

import com.personalizatio.domain.repositories.PreferencesRepository
import javax.inject.Inject

class GetPreferencesValueUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    fun getSidLastActTime(defaultValue: Long = 0L): Long = preferencesRepository.getSidLastActTime(defaultValue)

    fun getSid(defaultValue: String? = null) = preferencesRepository.getSid(defaultValue)

    fun getDid(defaultValue: String? = null) = preferencesRepository.getDid(defaultValue)

    fun getToken(defaultValue: String? = null) = preferencesRepository.getToken(defaultValue)

    fun getLastPushTokenDate(defaultValue: Long = 0L): Long = preferencesRepository.getLastPushTokenDate(defaultValue)

    fun getSegment(defaultValue: String) = preferencesRepository.getSegment(defaultValue)
}

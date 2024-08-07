package com.personalization.domain.usecases.preferences

import com.personalization.domain.repositories.PreferencesRepository
import javax.inject.Inject

class GetPreferencesValueUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    fun getSidLastActTime(): Long = preferencesRepository.getSidLastActTime()

    fun getSid(): String = preferencesRepository.getSid()

    fun getDid(): String = preferencesRepository.getDid()

    fun getToken(): String = preferencesRepository.getToken()

    fun getLastPushTokenDate(): Long = preferencesRepository.getLastPushTokenDate()

    fun getSegment(): String = preferencesRepository.getSegment()
}

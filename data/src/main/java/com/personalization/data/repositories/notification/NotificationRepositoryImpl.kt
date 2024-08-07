package com.personalization.data.repositories.notification

import com.personalization.data.mappers.NotificationMapper
import com.personalization.domain.models.NotificationSource
import com.personalization.domain.repositories.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDataSource: NotificationDataSource,
    private val notificationMapper: NotificationMapper
) : NotificationRepository {

    override fun getNotificationSource(timeDuration: Int): NotificationSource? {
        val notificationSourceDto = notificationDataSource.getNotificationSourceDto()

        if (!isTimeCorrect(notificationSourceDto.time, timeDuration)) {
            return null
        }

        return notificationMapper.toNotificationSource(notificationSourceDto)
    }

    override fun updateNotificationSource(type: String, id: String) {
        notificationDataSource.saveType(type)
        notificationDataSource.saveId(id)
        notificationDataSource.saveTime(System.currentTimeMillis())
    }

    private fun isTimeCorrect(time: Long, timeDuration: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        return time > 0 && (time + timeDuration) > currentTime
    }
}

package com.rees46.sdk

import android.content.Context
import com.personalizatio.BuildConfig
import com.personalizatio.SDK
import com.personalizatio.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class REES46 private constructor() : SDK() {

    companion object {

        private const val TAG: String = "REES46"
        private const val DEBUG_API_URL: String = "http://dev.api.rees46.com:8000/"
        private const val RELEASE_API_URL: String = "https://api.rees46.ru/"
        private const val PREFERENCES_KEY: String = "rees46.sdk"
        private const val PLATFORM_ANDROID: String = "android"
        private const val NOTIFICATION_TYPE = "REES46_NOTIFICATION_TYPE"
        private const val NOTIFICATION_ID = "REES46_NOTIFICATION_ID"

        private val API_URL: String = when {
            BuildConfig.DEBUG -> DEBUG_API_URL
            else -> RELEASE_API_URL
        }
        private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

        fun getInstance(): SDK = instance

        /**
         * Initialize api
         * @param context application context
         * @param shopId Shop key
         */
        fun initialize(
            context: Context,
            shopId: String,
            shopSecretKey: String,
            apiHost: String? = null,
            autoSendPushToken: Boolean = true
        ) {
            val apiUrl = apiHost?.let { "https://$it/" } ?: API_URL

            val sdk = getInstance()

            sdk.initialize(
                context = context,
                shopId = shopId,
                shopSecretKey = shopSecretKey,
                apiUrl = apiUrl,
                tag = TAG,
                preferencesKey = PREFERENCES_KEY,
                stream = PLATFORM_ANDROID,
                notificationType = NOTIFICATION_TYPE,
                notificationId =  NOTIFICATION_ID,
                autoSendPushToken = autoSendPushToken
            )

            sdk.setOnMessageListener { data: Map<String, String> ->
                coroutineScope.launch {
                    val images = withContext(Dispatchers.IO) {
                        NotificationHelper.loadBitmaps(urls = data[NotificationHelper.NOTIFICATION_IMAGES])
                    }
                    NotificationHelper.createNotification(
                        context = context,
                        data = data,
                        images = images,
                        currentIndex = 0
                    )
                }
            }
        }
    }
}

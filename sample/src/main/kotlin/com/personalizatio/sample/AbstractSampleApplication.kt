package com.personalizatio.sample

import android.app.Application
import android.util.Log
import com.personalizatio.SDK
import com.personalizatio.notification.NotificationHelper
import com.personalizatio.notification.NotificationHelper.createNotification
import com.personalizatio.notification.NotificationHelper.loadBitmaps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AbstractSampleApplication<out T : SDK> internal constructor(
    private val sdk: SDK
) : Application() {
    protected abstract val shopId: String?
    protected abstract val shopSecretKey: String?
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    protected abstract fun initialize()

    override fun onCreate() {
        super.onCreate()

        // Demo shop
        initialize()
        sdk.getSid { sid -> Log.d("APP", "sid: $sid") }
        sdk.setOnMessageListener { data: Map<String, String> ->
            coroutineScope.launch {
                val images = withContext(Dispatchers.IO) {
                    loadBitmaps(urls = data[NotificationHelper.NOTIFICATION_IMAGES])
                }
                createNotification(
                    context = applicationContext,
                    data = data,
                    images = images,
                    currentIndex = 0
                )
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        coroutineScope.cancel()
    }
}
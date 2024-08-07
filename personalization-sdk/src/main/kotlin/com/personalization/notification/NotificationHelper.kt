package com.personalization.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.personalization.R
import com.personalization.SDK
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NotificationHelper {

    const val TAG = "NotificationHelper"

    private const val NOTIFICATION_CHANNEL = "notification_channel"
    const val ACTION_PREVIOUS_IMAGE = "ACTION_PREVIOUS_IMAGE"
    const val CURRENT_IMAGE_INDEX = "current_image_index"
    const val ACTION_NEXT_IMAGE = "ACTION_NEXT_IMAGE"
    const val NOTIFICATION_IMAGES = "images"
    const val NOTIFICATION_TITLE = "title"
    const val NOTIFICATION_BODY = "body"

    var notificationType: String = "NOTIFICATION_TYPE"
    var notificationId: String = "NOTIFICATION_ID"

    private val requestCodeGenerator = RequestCodeGenerator

    fun createNotification(
        context: Context,
        data: Map<String, String?>,
        images: List<Bitmap>?,
        currentIndex: Int
    ) {
        val intent = createNotificationIntent(
            context = context,
            data = data,
            currentIndex = currentIndex
        )

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCodeGenerator.generateRequestCode(
                action = intent.action.orEmpty(),
                currentIndex = currentIndex
            ),
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(data[NOTIFICATION_TITLE])
            .setContentText(data[NOTIFICATION_BODY])
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!images.isNullOrEmpty() && currentIndex >= 0 && currentIndex < images.size) {
            val currentImage = images[currentIndex]

            notificationBuilder.setLargeIcon(currentImage)
                .setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(currentImage)
                )

            if (currentIndex > 0) {
                val prevIntent = createNotificationIntent(
                    context = context,
                    data = data,
                    currentIndex = currentIndex - 1
                )
                val prevPendingIntent = PendingIntent.getBroadcast(
                    /* context = */ context,
                    /* requestCode = */ requestCodeGenerator.generateRequestCode(
                        action = prevIntent.action.orEmpty(),
                        currentIndex = currentIndex - 1
                    ),
                    /* intent = */ prevIntent,
                    /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    /* action = */ NotificationCompat.Action.Builder(
                        /* icon = */ android.R.drawable.ic_media_previous,
                        /* title = */ context.getString(R.string.notification_button_back),
                        /* intent = */ prevPendingIntent
                    ).build()
                )
            }

            if (currentIndex < images.size - 1) {
                val nextIntent = createNotificationIntent(
                    context = context,
                    data = data,
                    currentIndex = currentIndex + 1
                )
                val nextPendingIntent = PendingIntent.getBroadcast(
                    /* context = */ context,
                    /* requestCode = */ requestCodeGenerator.generateRequestCode(
                        action = nextIntent.action.orEmpty(),
                        currentIndex = currentIndex + 1
                    ),
                    /* intent = */ nextIntent,
                    /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    /* action = */ NotificationCompat.Action.Builder(
                        /* icon = */ android.R.drawable.ic_media_next,
                        /* title = */ context.getString(R.string.notification_button_forward),
                        /* intent = */ nextPendingIntent
                    ).build()
                )
            }
        } else {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(data[NOTIFICATION_BODY])
            )
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        when {
            notificationManager != null -> notificationManager.notify(
                /* id = */ notificationId.hashCode(),
                /* notification = */ notificationBuilder.build()
            )
            else -> SDK.error("NotificationManager not available")
        }
    }

    private fun createNotificationIntent(
        context: Context,
        data: Map<String, String?>,
        currentIndex: Int
    ): Intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
        putExtra(NOTIFICATION_IMAGES, data[NOTIFICATION_IMAGES])
        putExtra(NOTIFICATION_TITLE, data[NOTIFICATION_TITLE])
        putExtra(NOTIFICATION_BODY, data[NOTIFICATION_BODY])
        putExtra(notificationType, data[notificationType])
        putExtra(notificationId, data[notificationId])
        putExtra(CURRENT_IMAGE_INDEX, currentIndex)

        action = when (currentIndex) {
            0 -> ACTION_NEXT_IMAGE
            else -> ACTION_PREVIOUS_IMAGE
        }
    }

    suspend fun loadBitmaps(urls: String?): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        if (urls != null) {
            val urlArray = urls.split(",").toTypedArray()
            withContext(Dispatchers.IO) {
                for (url in urlArray) {
                    try {
                        val inputStream: InputStream = URL(url).openStream()
                        bitmaps.add(BitmapFactory.decodeStream(inputStream))
                    } catch (ioException: IOException) {
                        SDK.error("Error caught in load bitmaps", ioException)
                    }
                }
            }
        }
        return bitmaps
    }
}

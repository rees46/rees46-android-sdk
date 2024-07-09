package com.personalizatio.features.notifications

import com.google.gson.Gson
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.NetworkManager
import com.personalizatio.api.managers.NotificationsManager
import com.personalizatio.api.params.NotificationChannels
import com.personalizatio.api.params.NotificationTypes
import com.personalizatio.api.responses.notifications.GetAllNotificationsResponse
import org.json.JSONObject
import java.util.EnumSet

internal class NotificationsManagerImpl(private val networkManager: NetworkManager) :
    NotificationsManager {

    override fun getAllNotifications(
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        dateFrom: String,
        types: NotificationTypes,
        channels: NotificationChannels,
        page: Int?,
        limit: Int?,
        onGetAllNotifications: (GetAllNotificationsResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        val type = getEnumsString(types)
        val channel = getEnumsString(channels)

        getAllNotifications(email, phone, loyaltyId, externalId, dateFrom, type, channel, page, limit, onGetAllNotifications, onError)
    }

    override fun getAllNotifications(
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        dateFrom: String,
        type: String,
        channel: String,
        page: Int?,
        limit: Int?,
        onGetAllNotifications: (GetAllNotificationsResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getAllNotifications(email, phone, loyaltyId, externalId, dateFrom, type, channel, page, limit, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val getAllNotificationsResponse = Gson().fromJson(it.toString(), GetAllNotificationsResponse::class.java)
                    onGetAllNotifications(getAllNotificationsResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getAllNotifications(
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        dateFrom: String,
        type: String,
        channel: String,
        page: Int?,
        limit: Int?,
        listener: OnApiCallbackListener?
    ) {
        val params = com.personalizatio.Params()
        if (email != null) {
            params.put(GetAllNotificationsParameter.EMAIL, email)
        }
        if (phone != null) {
            params.put(GetAllNotificationsParameter.PHONE, phone)
        }
        if (externalId != null) {
            params.put(GetAllNotificationsParameter.EXTERNAL_ID, externalId)
        }
        if (loyaltyId != null) {
            params.put(GetAllNotificationsParameter.LOYALTY_ID, loyaltyId)
        }
        params.put(GetAllNotificationsParameter.DATE_FROM, dateFrom)
        params.put(GetAllNotificationsParameter.TYPE, type)
        params.put(GetAllNotificationsParameter.CHANNEL, channel)
        if (page != null) {
            params.put(GetAllNotificationsParameter.PAGE, page)
        }
        if (limit != null) {
            params.put(GetAllNotificationsParameter.LIMIT, limit)
        }

        networkManager.getSecretAsync(GET_ALL_NOTIFICATIONS_REQUEST, params.build(), listener)
    }

    private inline fun <reified T : Enum<T>> getEnumsString(enums: EnumSet<T>): String {
        val string = StringBuilder()

        for (enum in enumValues<T>()) {
            if(enums.contains(enum)) {
                if(string.isNotEmpty()) {
                    string.append(',')
                }
                string.append(enum.toString())
            }
        }

        return string.toString()
    }

    companion object {
        const val GET_ALL_NOTIFICATIONS_REQUEST = "notifications"
    }
}

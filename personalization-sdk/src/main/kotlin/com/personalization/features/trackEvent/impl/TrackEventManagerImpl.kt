package com.personalization.features.trackEvent.impl

import android.util.Log
import com.personalization.SDK
import com.personalization.Params
import com.personalization.Params.TrackEvent
import com.personalization.api.OnApiCallbackListener
import com.personalization.api.managers.InAppNotificationManager
import com.personalization.api.managers.TrackEventManager
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.params.ProductItemParams
import com.personalization.sdk.data.mappers.popup.PopupDtoMapper
import com.personalization.sdk.data.models.params.SdkInitializationParams.PARAM_POPUP
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.GetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.trackingSource.GetTrackingSourceUseCase
import com.personalization.sdk.domain.usecases.userSettings.GetUserSettingsValueUseCase
import com.personalization.sdk.data.models.params.UserBasicParams
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

internal class TrackEventManagerImpl @Inject constructor(
    val getRecommendedByUseCase: GetRecommendedByUseCase,
    val setRecommendedByUseCase: SetRecommendedByUseCase,
    private val sendNetworkMethodUseCase: SendNetworkMethodUseCase,
    private val inAppNotificationManager: InAppNotificationManager,
    private val getUserSettingsValueUseCase: GetUserSettingsValueUseCase,
    private val getTrackingSourceUseCase: GetTrackingSourceUseCase
) : TrackEventManager {

    override fun track(event: TrackEvent, productId: String) {
        track(event, Params().put(ProductItemParams(productId)), null)
    }

    override fun track(
        event: TrackEvent,
        params: Params,
        listener: OnApiCallbackListener?
    ) {
        params.put(EVENT_PARAMETER, event.value)
        val lastRecommendedBy = getRecommendedByUseCase()
        if (lastRecommendedBy != null) {
            params.put(lastRecommendedBy)
            setRecommendedByUseCase(null)
        }

        val internalListener = object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    handlePopup(response)
                }
                listener?.onSuccess(response)
            }

            override fun onError(code: Int, msg: String?) {
                listener?.onError(code, msg)
            }
        }

        val body = params.build()
        attachStoredSource(body)

        sendNetworkMethodUseCase.postAsync(
            PUSH_REQUEST,
            body,
            internalListener
        )
    }

    override fun trackPurchase(request: PurchaseTrackingRequest, listener: OnApiCallbackListener?) {
        val jsonResult = PurchaseTrackingJsonBuilder.buildOrError(request)
        if (jsonResult.isFailure) {
            listener?.onError(
                PurchaseTrackingJsonBuilder.CLIENT_VALIDATION_ERROR_CODE,
                jsonResult.exceptionOrNull()?.message,
            )
            return
        }
        val body = jsonResult.getOrNull()!!
        if (request.recommendedBy == null) {
            val lastRecommendedBy = getRecommendedByUseCase()
            if (lastRecommendedBy != null) {
                PurchaseTrackingJsonBuilder.mergeInto(body, Params().put(lastRecommendedBy).build())
                setRecommendedByUseCase(null)
            }
        }

        val internalListener = object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    handlePopup(response)
                }
                listener?.onSuccess(response)
            }

            override fun onError(code: Int, msg: String?) {
                listener?.onError(code, msg)
            }
        }

        attachStoredSource(body)

        sendNetworkMethodUseCase.postAsync(
            PUSH_REQUEST,
            body,
            internalListener,
        )
    }

    override fun trackEvent(
        event: String,
        time: Int?,
        category: String?,
        label: String?,
        value: Int?,
        customFields: Map<String, Any?>?,
        listener: OnApiCallbackListener?
    ) {
        val effectiveCustom = TrackCustomEventPayloadHelper.effectiveCustomFields(customFields)
        TrackCustomEventPayloadHelper.validateNoReservedKeyCollisions(effectiveCustom)?.let { msg ->
            listener?.onError(TrackCustomEventPayloadHelper.CLIENT_VALIDATION_ERROR_CODE, msg)
            return
        }

        val params = Params()
        params.put(EVENT_PARAMETER, event)
        if (time != null) {
            params.put(TIME_PARAMETER, time)
        }
        if (category != null) {
            params.put(CATEGORY_PARAMETER, category)
        }
        if (label != null) {
            params.put(LABEL_PARAMETER, label)
        }
        if (value != null) {
            params.put(VALUE_PARAMETER, value)
        }

        val body = params.build()
        if (effectiveCustom.isNotEmpty()) {
            try {
                val payload = JSONObject()
                for ((key, fieldValue) in effectiveCustom) {
                    TrackCustomEventPayloadHelper.putJsonValue(body, key, fieldValue)
                    TrackCustomEventPayloadHelper.putJsonValue(payload, key, fieldValue)
                }
                body.put(PAYLOAD_PARAMETER, payload)
            } catch (e: JSONException) {
                listener?.onError(
                    TrackCustomEventPayloadHelper.CLIENT_VALIDATION_ERROR_CODE,
                    "trackEvent: failed to build custom fields payload: ${e.message}"
                )
                return
            }
        }

        postCustomEvent(body, listener)
    }

    @Deprecated("Use trackEvent for iOS-aligned custom events.")
    override fun customTrack(
        event: String,
        email: String?,
        phone: String?,
        loyaltyId: String?,
        externalId: String?,
        category: String?,
        label: String?,
        value: Int?,
        listener: OnApiCallbackListener?
    ) {
        val params = Params()
        params.put(EVENT_PARAMETER, event)
        if (email != null) {
            params.put(EMAIL_PARAMETER, email)
        }
        if (phone != null) {
            params.put(PHONE_PARAMETER, phone)
        }
        if (loyaltyId != null) {
            params.put(LOYALTY_ID_PARAMETER, loyaltyId)
        }
        if (externalId != null) {
            params.put(EXTERNAL_ID_PARAMETER, externalId)
        }
        if (category != null) {
            params.put(CATEGORY_PARAMETER, category)
        }
        if (label != null) {
            params.put(LABEL_PARAMETER, label)
        }
        if (value != null) {
            params.put(VALUE_PARAMETER, value)
        }

        postCustomEvent(params.build(), listener)
    }

    override fun trackPopupShown(popupId: Int, listener: OnApiCallbackListener?) {
        val params = Params()
        params.put(UserBasicParams.SHOP_ID, getUserSettingsValueUseCase.getShopId())
        params.put(UserBasicParams.DID, getUserSettingsValueUseCase.getDid())
        params.put(UserBasicParams.SID, getUserSettingsValueUseCase.getSid())
        params.put("popup", popupId.toString())

        sendNetworkMethodUseCase.postAsync(
            POPUP_SHOWN_PATH,
            params.build(),
            listener
        )
    }

    /**
     * Adds the source a host set with `tracking.setSource(...)`, if it is still inside its window.
     *
     * Port of iOS `TrackEventServiceImpl`: the source rides in its own `source` object and stays on
     * every request until it expires — it is not spent by the first one. Attached to `push` and
     * `push/custom`; `popup/showed` carries no attribution, on either platform.
     */
    private fun attachStoredSource(body: JSONObject) {
        val source = getTrackingSourceUseCase() ?: return
        try {
            body.put(
                SOURCE_PARAMETER,
                JSONObject()
                    .put(SOURCE_FROM_PARAMETER, source.type)
                    .put(SOURCE_CODE_PARAMETER, source.code)
            )
        } catch (e: JSONException) {
            Log.e(SDK.TAG, e.message, e)
        }
    }

    private fun postCustomEvent(json: JSONObject, listener: OnApiCallbackListener?) {
        attachStoredSource(json)
        val internalListener = object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    handlePopup(response)
                }
                listener?.onSuccess(response)
            }

            override fun onError(code: Int, msg: String?) {
                listener?.onError(code, msg)
            }
        }

        sendNetworkMethodUseCase.postAsync(
            CUSTOM_PUSH_REQUEST,
            json,
            internalListener
        )
    }

    private fun handlePopup(response: JSONObject) {
        val popUpData = response.optJSONObject(PARAM_POPUP)?.let { PopupDtoMapper.map(it) }
        if (popUpData != null) {
            inAppNotificationManager.shopPopUp(popupDto = popUpData)
        }
    }

    companion object {
        private const val CUSTOM_PUSH_REQUEST = "push/custom"
        private const val PUSH_REQUEST = "push"
        private const val POPUP_SHOWN_PATH = "popup/showed"

        private const val EVENT_PARAMETER = "event"
        private const val TIME_PARAMETER = "time"
        private const val PAYLOAD_PARAMETER = "payload"
        private const val EMAIL_PARAMETER = "email"
        private const val PHONE_PARAMETER = "phone"
        private const val LOYALTY_ID_PARAMETER = "loyalty_id"
        private const val EXTERNAL_ID_PARAMETER = "external_id"
        private const val CATEGORY_PARAMETER = "category"
        private const val LABEL_PARAMETER = "label"
        private const val VALUE_PARAMETER = "value"

        private const val SOURCE_PARAMETER = "source"
        private const val SOURCE_FROM_PARAMETER = "from"
        private const val SOURCE_CODE_PARAMETER = "code"
    }
}

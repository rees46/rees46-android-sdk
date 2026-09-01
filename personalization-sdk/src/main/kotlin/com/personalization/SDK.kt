package com.personalization

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentManager
import com.google.firebase.messaging.RemoteMessage
import com.personalization.Params.InternalParameter
import com.personalization.Params.TrackEvent
import com.personalization.api.OnApiCallbackListener
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.managers.CartManager
import com.personalization.api.managers.CategoryManager
import com.personalization.api.managers.CollectionManager
import com.personalization.api.managers.InAppNotificationManager
import com.personalization.api.managers.LoyaltyManager
import com.personalization.api.managers.OrdersManager
import com.personalization.api.managers.ProductsManager
import com.personalization.api.managers.ProfileManager
import com.personalization.api.managers.PredictManager
import com.personalization.api.managers.RecommendationManager
import com.personalization.api.managers.SearchManager
import com.personalization.api.managers.TrackEventManager
import com.personalization.api.managers.TrackingApi
import com.personalization.api.params.ProfileParams
import com.personalization.di.AppModule
import com.personalization.di.DaggerSdkComponent
import com.personalization.features.notification.data.mapper.toNotificationData
import com.personalization.features.notification.presentation.helpers.NotificationHelper
import com.personalization.handlers.notifications.NotificationHandler
import com.personalization.push.PushTokenManager
import com.personalization.sdk.domain.repositories.NPSRepository
import com.personalization.sdk.domain.usecases.network.AddTaskToQueueUseCase
import com.personalization.sdk.domain.usecases.network.InitNetworkUseCase
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.notification.GetAllNotificationsUseCase
import com.personalization.sdk.domain.usecases.preferences.GetPreferencesValueUseCase
import com.personalization.sdk.domain.usecases.preferences.InitPreferencesUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.userSettings.GetUserSettingsValueUseCase
import com.personalization.sdk.domain.usecases.userSettings.InitUserSettingsUseCase
import com.personalization.sdk.domain.usecases.userSettings.InitializeAdvertisingIdUseCase
import com.personalization.stories.StoriesManager
import com.personalization.stories.views.StoriesView
import com.personalization.utils.DomainFormattingUtils.formatApiDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import org.json.JSONException
import org.json.JSONObject

private const val GAID_KEY = "google_advertising_id"

open class SDK {

    internal lateinit var context: Context

    private var onMessageListener: OnMessageListener? = null
    private var onPushTokenListener: OnPushTokenListener? = null
    private var search: Search = Search(JSONObject())

    /**
     * True once initialize() has wired the Dagger graph. A push can reach the SDK in a process
     * where initialize() never ran (FCM cold start, or the host integrated the SDK incorrectly),
     * so the push path checks this flag to avoid crashing the host app with
     * UninitializedPropertyAccessException.
     */
    @Volatile
    private var isSdkInitialized: Boolean = false

    @Inject
    internal lateinit var initializeAdvertisingIdUseCase: InitializeAdvertisingIdUseCase

    @Inject
    lateinit var notificationHandler: NotificationHandler

    @Inject
    lateinit var registerManager: RegisterManager

    @Inject
    lateinit var pushTokenManager: PushTokenManager

    @Inject
    lateinit var storiesManager: StoriesManager

    @Inject
    lateinit var recommendationManager: RecommendationManager

    @Inject
    lateinit var productsManager: ProductsManager

    @Inject
    lateinit var cartManager: CartManager

    /**
     * Standard tracking events, grouped: `sdk.tracking.productView(itemId)`,
     * `sdk.tracking.addToCart(item)`, and the rest.
     */
    @Inject
    lateinit var tracking: TrackingApi

    @Deprecated(
        message = "Use the tracking namespace: sdk.tracking.",
        replaceWith = ReplaceWith("tracking"),
        level = DeprecationLevel.WARNING
    )
    @Inject
    lateinit var trackEventManager: TrackEventManager

    @Inject
    lateinit var searchManager: SearchManager

    @Inject
    lateinit var predictManager: PredictManager

    @Inject
    lateinit var ordersManager: OrdersManager

    @Inject
    lateinit var loyaltyManager: LoyaltyManager

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var categoryManager: CategoryManager

    @Inject
    lateinit var collectionManager: CollectionManager

    @Inject
    lateinit var inAppNotificationManager: InAppNotificationManager

    @Inject
    lateinit var initPreferencesUseCase: InitPreferencesUseCase

    @Inject
    lateinit var initUserSettingsUseCase: InitUserSettingsUseCase

    @Inject
    lateinit var initNetworkUseCase: InitNetworkUseCase

    @Inject
    lateinit var getPreferencesValueUseCase: GetPreferencesValueUseCase

    @Inject
    lateinit var getUserSettingsValueUseCase: GetUserSettingsValueUseCase

    @Inject
    lateinit var addTaskToQueueUseCase: AddTaskToQueueUseCase

    @Inject
    lateinit var sendNetworkMethodUseCase: SendNetworkMethodUseCase

    @Inject
    lateinit var setRecommendedByUseCase: SetRecommendedByUseCase

    @Inject
    lateinit var getAllNotificationsUseCase: GetAllNotificationsUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var NPSRepository: NPSRepository

    /**
     * @param shopId Shop key
     */
    @Deprecated(
        message = "Use Rees46.initialize(context, Rees46Config(...)) — the unified, multi-instance " +
            "entry point. Reach instances with Rees46.getInstance(shopId).",
        replaceWith = ReplaceWith(
            "Rees46.initialize(context, Rees46Config(shopId = shopId))",
            "com.personalization.Rees46",
            "com.personalization.Rees46Config"
        )
    )
    fun initialize(
        context: Context,
        shopId: String,
        apiDomain: String = "api.rees46.ru",
        tag: String = TAG,
        preferencesKey: String = PreferencesPartition.LEGACY_KEY,
        stream: String = ANDROID,
        autoSendPushToken: Boolean = true,
        needReInitialization: Boolean = false,
        addTrailingSlash: Boolean = true
    ) = initializeInternal(
        context = context,
        shopId = shopId,
        apiDomain = apiDomain,
        tag = tag,
        preferencesKey = preferencesKey,
        stream = stream,
        autoSendPushToken = autoSendPushToken,
        needReInitialization = needReInitialization,
        addTrailingSlash = addTrailingSlash,
        sendProfileOnInit = true
    )

    /**
     * Light bring-up for handling a push in a (possibly cold) background process: keeps the persisted
     * did (no `/init`), does not re-register the push token (it is already on the server — that is why
     * the push arrived), and skips the GAID + profile/set call. Just enough to track the delivery and
     * reach the message listener. Used by [Rees46.handlePush] to materialize a lazily-registered shop
     * without the full startup work. The public [initialize] contract is unchanged.
     */
    internal fun initializeForPush(context: Context, config: Rees46Config) = initializeInternal(
        context = context,
        shopId = config.shopId,
        apiDomain = config.apiDomain,
        tag = config.tag,
        preferencesKey = PreferencesPartition.LEGACY_KEY,
        stream = config.stream,
        autoSendPushToken = false,
        needReInitialization = false,
        addTrailingSlash = config.addTrailingSlash,
        sendProfileOnInit = false
    )

    private fun initializeInternal(
        context: Context,
        shopId: String,
        apiDomain: String,
        tag: String,
        preferencesKey: String,
        stream: String,
        autoSendPushToken: Boolean,
        needReInitialization: Boolean,
        addTrailingSlash: Boolean,
        // When false, skip the background GAID + profile/set call (the push-context bring-up).
        sendProfileOnInit: Boolean
    ) {

        val sdkComponent = DaggerSdkComponent.factory().create(
            appModule = AppModule(applicationContext = context)
        )

        val baseUrl = apiDomain?.let {
            formatApiDomain(
                apiDomain = it,
                addTrailingSlash = addTrailingSlash
            )
        }

        sdkComponent.inject(sdk = this)
        this.context = context
        TAG = tag

        onPushTokenListener?.let { pushTokenManager.setOnPushTokenListener(it) }

        // A host that leaves preferencesKey at its default gets a per-shop partition instead of the
        // single shared file, plus a one-time migration out of the legacy file so an existing
        // install keeps its did/sid. A host that passes its own key keeps using it verbatim, with no
        // derivation and no migration.
        val usingDefaultPartition = preferencesKey == PreferencesPartition.LEGACY_KEY
        val effectivePreferencesKey =
            if (usingDefaultPartition) PreferencesPartition.keyFor(shopId) else preferencesKey

        initPreferencesUseCase.invoke(
            context = context,
            preferencesKey = effectivePreferencesKey,
            legacyPreferencesKey = if (usingDefaultPartition) PreferencesPartition.LEGACY_KEY else null,
            shopId = shopId
        )

        notificationHandler.initialize(context = context)

        initUserSettingsUseCase.invoke(
            shopId = shopId,
            stream = stream
        )

        initNetworkUseCase(url = baseUrl)

        registerManager.initialize(
            context = context,
            contentResolver = context.contentResolver,
            autoSendPushToken = autoSendPushToken,
            needReInitialization = needReInitialization
        )

        // Register this instance so pushes delivered to the (process-global) messaging services are
        // routed back to every initialized SDK instance, not just one, and so the documented
        // `SDK().initialize()` usage and hosts reading SDK.instance get a working instance. register
        // both adds this to the fan-out set and makes it the current default.
        SdkRegistry.register(shopId = shopId, sdk = this)
        isSdkInitialized = true

        if (sendProfileOnInit) {
            CoroutineScope(Dispatchers.IO).launch {
                val advertisingId = initializeAdvertisingIdUseCase.invoke()

                profile(
                    data = ProfileParams
                        .Builder()
                        .put(GAID_KEY, advertisingId)
                        .build(),
                    listener = object : OnApiCallbackListener() {
                        override fun onSuccess(response: JSONObject?) {
                            debug("Profile GAID sent successfully: $advertisingId")
                        }

                        override fun onError(code: Int, msg: String?) {
                            warn("Profile GAID send failed: $code | $msg")
                        }
                    }
                )
            }
        }

    }

    private fun initNetworkUseCase(url: String?) {
        if (url != null) {
            initNetworkUseCase.invoke(
                baseUrl = url
            )
        }
    }

    @Deprecated(
        message = "The view loads itself: declare app:shop_id (or none for the default instance) on " +
            "the XML StoriesView, or use the Compose StoriesWidget. Kept working for existing hosts.",
        replaceWith = ReplaceWith("")
    )
    fun initializeStoriesView(storiesView: StoriesView) {
        storiesView.attach(this)
    }

    fun initializeFragmentManager(fragmentManager: FragmentManager) {
        inAppNotificationManager.initFragmentManager(fragmentManager = fragmentManager)
    }

    /**
     * @param listener
     */
    fun stories(code: String, listener: OnApiCallbackListener) {
        storiesManager.requestStories(code, listener)
    }

    /**
     * Show stories block by code
     *
     * @param code Stories block code
     */
    fun showStories(code: String) {
        storiesManager.showStories(context.mainLooper, code)
    }


    /**
     * Triggers a story event
     *
     * @param event Event
     * @param code Stories block code
     * @param storyId Story ID
     * @param slideId Slide ID
     */
    /**
     * No `replaceWith`: which method replaces this one depends on [event], and a quick-fix that always
     * picked `storyView` would silently turn a tracked click into a view.
     */
    @Deprecated(
        message = "Use the tracking namespace: sdk.tracking.storyView(storyId, slideId, code) for " +
            "\"view\" and sdk.tracking.storyClick(storyId, slideId, code) for \"click\".",
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    fun trackStory(event: String, code: String, storyId: Int, slideId: String) {
        if (::storiesManager.isInitialized) {
            storiesManager.trackStory(
                event = event, code = code, storyId = storyId, slideId = slideId
            )
        } else {
            Log.i(TAG, "storiesManager is not initialized")
        }
    }

    /**
     * Update profile data
     *
     * @param data profile data
     */
    fun profile(data: ProfileParams, listener: OnApiCallbackListener? = null) {
        sendAsync(SET_PROFILE_FIELD, data.toJson(), listener)
    }

    /**
     * Return the session ID
     */
    fun getSid(): String = getUserSettingsValueUseCase.getSid()

    /**
     * Return the Advertising ID
     */
    fun getAdvertisingId(): String = getUserSettingsValueUseCase.getAdvertisingId()

    /**
     * Returns the session ID
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("getSid(): String")
    )
    fun getSid(listener: Consumer<String?>) {
        listener.accept(getSid())
    }

    /**
     * @param extras from data notification
     */
    fun notificationClicked(extras: Bundle?) {
        notificationHandler.notificationClicked(
            extras = extras,
            sendAsync = { method, params ->
                sendNetworkMethodUseCase.postAsync(
                    method = method,
                    params = params
                )
            }
        )
    }

    /**
     * Asynchronous execution of a request if did is not specified and initialization has not been completed
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("networkManager.postAsync(method, params, listener)")
    )
    fun sendAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendNetworkMethodUseCase.postAsync(method, params, listener)
    }

    /**
     * Asynchronous execution of a request if did is not specified and initialization has not been completed
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("networkManager.getAsync(method, params, listener)")
    )
    fun getAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendNetworkMethodUseCase.getAsync(method, params, listener)
    }

    /**
     * @param listener Event on message receive
     * @deprecated Per-instance and must be set on every shop; a shop it was never set on silently
     * shows nothing. Use the process-global [Rees46.setOnMessageListener], which fires for every shop
     * with its `shopId`. This one still works and fires alongside the global listener.
     */
    @Deprecated(
        "Use Rees46.setOnMessageListener { shopId, data -> ... } — one process-global listener covers " +
            "every shop instead of wiring one per instance."
    )
    fun setOnMessageListener(listener: OnMessageListener) {
        onMessageListener = listener
    }

    /**
     * Registers a callback invoked when a push token is received or refreshed for a provider
     * (FCM and/or HMS).
     *
     * This is the recommended way for the host app to obtain push tokens: the SDK captures
     * token issuance and refresh from its own messaging services automatically, including
     * tokens that a provider delivers only asynchronously (e.g. HMS via `onNewToken`, which
     * cannot be retrieved synchronously). May be called before or after [initialize].
     */
    fun setOnPushTokenListener(listener: OnPushTokenListener) {
        onPushTokenListener = listener
        if (::pushTokenManager.isInitialized) {
            pushTokenManager.setOnPushTokenListener(listener)
        }
    }

    /**
     * Returns the last known push token for [provider] from the local cache, or null if no
     * token has been received yet. For live updates use [setOnPushTokenListener].
     */
    fun getPushToken(provider: PushProvider): String? = pushTokenManager.getToken(provider)

    /**
     * Registers a push [token] for the given [provider] with the rees46 backend.
     *
     * In the default setup you normally do NOT need to call this. The SDK ships its own
     * messaging services for FCM and HMS, captures token issuance and refresh automatically,
     * sends the token to the backend, and reports it to the host via [setOnPushTokenListener].
     *
     * Call this only if your app owns the messaging service (you declare your own
     * FirebaseMessagingService / HmsMessageService) and want to forward the token manually.
     * Always pass the [PushProvider] that issued the token: the backend stores it per provider,
     * and FCM and HMS tokens are not interchangeable.
     *
     * @param token    the push token issued by [provider]
     * @param provider the provider that issued the token (FCM or HMS)
     * @param listener optional callback with the backend registration result
     */
    fun setPushToken(
        token: String,
        provider: PushProvider,
        listener: OnApiCallbackListener? = null
    ) {
        pushTokenManager.sendToken(token = token, provider = provider, listener = listener)
    }

    /**
     * Registers an FCM push token with the rees46 backend.
     *
     * @deprecated This method predates multi-provider support. It has no [PushProvider]
     * parameter and therefore always assumes the token is an FCM token. Now that both FCM and
     * HMS are supported the provider must be explicit, otherwise an HMS token would be
     * registered as FCM (the backend keeps them separate and they are not interchangeable).
     * Use [setPushToken] with an explicit [PushProvider] instead. Equivalent to
     * `setPushToken(token, PushProvider.FCM, listener)`.
     *
     * @param token    an FCM push token
     * @param listener optional callback with the backend registration result
     */
    @Deprecated(
        message = "Assumes an FCM token; the provider cannot be specified. Use " +
            "setPushToken(token, provider, listener) with an explicit PushProvider — " +
            "FCM and HMS tokens are stored separately and are not interchangeable.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("setPushToken(token, PushProvider.FCM, listener)")
    )
    fun setPushTokenNotification(token: String, listener: OnApiCallbackListener?) =
        setPushToken(token = token, provider = PushProvider.FCM, listener = listener)

    /**
     * Releases this SDK instance so it no longer receives push tokens delivered to the
     * process-global messaging services. Call this for short-lived instances to avoid leaking
     * them through the active-instances registry. Apps that keep a single long-lived instance
     * for the whole process usually do not need it.
     */
    fun release() {
        SdkRegistry.unregister(this)
    }

    /**
     * Quick search
     *
     * @param query Search phrase
     * @param type Search type
     * @param listener Callback
     */
    @Deprecated(
        "This class will be removed in future versions. Use searchManager.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "searchManager.searchInstant(...) or searchManager.searchFull(...)"
        )
    )
    fun search(query: String, type: SearchParams.TYPE, listener: OnApiCallbackListener) {
        search(query, type, SearchParams(), listener)
    }

    /**
     * Quick search
     *
     * @param query Search phrase
     * @param type Search type
     * @param params Additional parameters for the request
     * @param listener v
     */
    @Deprecated(
        "This class will be removed in future versions. Use searchManager.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "searchManager.searchInstant(...) or searchManager.searchFull(...)"
        )
    )
    fun search(
        query: String,
        type: SearchParams.TYPE,
        params: SearchParams,
        listener: OnApiCallbackListener
    ) {
        if (search != null) {
            params.put(InternalParameter.SEARCH_TYPE, type.value)
                .put(InternalParameter.SEARCH_QUERY, query)
            getAsync(SEARCH_FIELD, params.build(), listener)
        } else {
            warn("Search not initialized")
        }
    }

    @Deprecated(
        "This class will be removed in future versions. Use searchManager.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "searchManager.searchBlank(...)"
        )
    )
    fun searchBlank(listener: OnApiCallbackListener) {
        if (search != null) {
            if (search?.blank == null) {
                getAsync(BLANK_SEARCH_FIELD, Params().build(), object : OnApiCallbackListener() {
                    override fun onSuccess(response: JSONObject?) {
                        search?.blank = response
                        listener.onSuccess(response)
                    }

                    override fun onError(code: Int, msg: String?) {
                        listener.onError(code, msg)
                    }
                })
            } else {
                listener.onSuccess(search?.blank)
            }
        } else {
            warn("Search not initialized")
        }
    }

    fun review(
        rate: Int,
        channel: String,
        category: String,
        orderId: String? = null,
        comment: String? = null,
        listener: OnApiCallbackListener? = null
    ) = NPSRepository.review(
        rate = rate,
        channel = channel,
        category = category,
        orderId = orderId,
        comment = comment,
        listener = listener
    )

    /**
     * Request a dynamic block of recommendations
     *
     * @param recommender_code Recommendation block code
     * @param listener Callback
     */
    @Deprecated(
        "This method will be removed in future versions. Use recommendationManager.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "recommendationManager.getRecommendation(recommender_code, ...)"
        )
    )
    fun recommend(recommender_code: String, listener: OnApiCallbackListener) {
        recommendationManager.getRecommendation(recommender_code, Params(), listener)
    }

    /**
     * Request a dynamic block of recommendations
     *
     * @param code Code of the dynamic block of recommendations
     * @param params Parameters for the request
     * @param listener Callback
     */
    @Deprecated(
        "This method will be removed in future versions. Use recommendationManager.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "recommendationManager.getRecommendation(code, ...)"
        )
    )
    fun recommend(code: String, params: Params, listener: OnApiCallbackListener) {
        recommendationManager.getRecommendation(code, params, listener)
    }

    /**
     * Event tracking
     *
     * @param event Event type
     * @param itemId Product ID
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "trackEventManager.track(event, itemId)"
        )
    )
    @Suppress("DEPRECATION")
    fun track(event: TrackEvent, itemId: String) {
        trackEventManager.track(event, itemId)
    }

    /**
     * Event tracking
     *
     * @param event Event type
     * @param params Parameters for the request
     * @param listener Callback
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "trackEventManager.track(event, params, listener)"
        )
    )
    @Suppress("DEPRECATION")
    fun track(event: TrackEvent, params: Params, listener: OnApiCallbackListener? = null) {
        trackEventManager.track(event, params, listener)
    }

    /**
     * Custom event tracking.
     *
     * @param event Event key
     * @param time Optional UNIX time in seconds
     * @param category Event category
     * @param label Event label
     * @param value Event value (sent as string in JSON)
     * @param customFields Optional map merged at top level and under `payload`
     * @param listener Callback
     */
    @Deprecated(
        message = "Use the tracking namespace: sdk.tracking.custom(...).",
        replaceWith = ReplaceWith(
            "tracking.custom(event = event, time = time, category = category, label = label, value = value, customFields = customFields, listener = listener)"
        ),
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    fun trackEvent(
        event: String,
        time: Int? = null,
        category: String? = null,
        label: String? = null,
        value: Int? = null,
        customFields: Map<String, Any?>? = null,
        listener: OnApiCallbackListener? = null
    ) {
        trackEventManager.trackEvent(
            event = event,
            time = time,
            category = category,
            label = label,
            value = value,
            customFields = customFields,
            listener = listener
        )
    }

    /**
     * Strict purchase tracking (`push`, event = `purchase`).
     *
     * Prefer this over [track] with [TrackEvent.PURCHASE] and manual [Params] assembly.
     */
    @Deprecated(
        message = "Use the tracking namespace: sdk.tracking.purchase(request, listener = listener).",
        replaceWith = ReplaceWith("tracking.purchase(request = request, listener = listener)"),
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    fun trackPurchase(
        request: PurchaseTrackingRequest,
        listener: OnApiCallbackListener? = null,
    ) {
        trackEventManager.trackPurchase(request, listener)
    }

    /**
     * Tracking custom events
     *
     * @param event Event key
     * @param category Event category
     * @param label Event label
     * @param value Event value
     * @param listener Callback
     */
    @Deprecated(
        "This method will be removed in future versions. Use trackEvent.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "trackEvent(event = event, category = category, label = label, value = value, listener = listener)",
            imports = []
        )
    )
    fun track(
        event: String,
        category: String? = null,
        label: String? = null,
        value: Int? = null,
        listener: OnApiCallbackListener? = null
    ) {
        trackEventManager.trackEvent(
            event = event,
            time = null,
            category = category,
            label = label,
            value = value,
            customFields = null,
            listener = listener
        )
    }

    /**
     * Returns the device ID
     *
     * @return String
     */
    fun getDid(): String? {
        return getUserSettingsValueUseCase.getDid()
    }

    /**
     * Signs up for price reduction
     *
     * @param id Product ID
     * @param currentPrice Current price
     * @param email Email, if available
     * @param phone Phone, if available
     */
    fun subscribeForPriceDrop(
        id: String,
        currentPrice: Double,
        email: String? = null,
        phone: String? = null,
        listener: OnApiCallbackListener? = null
    ) {
        val params = Params()
        params.put(Params.Parameter.ITEM, id)
        params.put(Params.Parameter.PRICE, currentPrice.toString())
        if (email != null) {
            params.put(InternalParameter.EMAIL, email)
        }
        if (phone != null) {
            params.put(InternalParameter.PHONE, phone)
        }
        sendAsync(SUBSCRIPTION_SUBSCRIBE_PRICE, params.build(), listener)
    }

    /**
     * Subscribes for price reduction
     *
     * @param itemIds Product identifiers
     * @param email Email, if available
     * @param phone Phone, if available
     */
    fun unsubscribeForPriceDrop(
        itemIds: Array<String>,
        email: String? = null,
        phone: String? = null,
        listener: OnApiCallbackListener? = null
    ) {
        val params = JSONObject()
        try {
            params.put(ITEM_IDS_FIELD, java.lang.String.join(", ", *itemIds))
            if (email != null) {
                params.put(InternalParameter.EMAIL.value, email)
            }
            if (phone != null) {
                params.put(InternalParameter.PHONE.value, phone)
            }
            sendAsync(SUBSCRIPTION_UNSUBSCRIBE_PRICE, params, listener)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
        }
    }

    /**
     * Signs for product availability
     *
     * @param id Product ID
     * @param email Email, if available
     * @param phone Phone, if available
     */
    fun subscribeForBackInStock(
        id: String,
        email: String? = null,
        phone: String? = null,
        properties: JSONObject? = null,
        listener: OnApiCallbackListener? = null
    ) {
        val params = Params()
        params.put(Params.Parameter.ITEM, id)
        if (properties != null) {
            params.put(InternalParameter.PROPERTIES, properties)
        }
        if (email != null) {
            params.put(InternalParameter.EMAIL, email)
        }
        if (phone != null) {
            params.put(InternalParameter.PHONE, phone)
        }
        sendAsync(SUBSCRIPTION_SUBSCRIBE, params.build(), listener)
    }

    /**
     * Subscribes to product availability
     *
     * @param itemIds Product ID
     * @param email Email, if available
     * @param phone Phone, if available
     */
    @JvmOverloads
    fun unsubscribeForBackInStock(
        itemIds: Array<String>,
        email: String? = null,
        phone: String? = null,
        listener: OnApiCallbackListener? = null
    ) {
        val params = JSONObject()
        try {
            params.put(ITEM_IDS_FIELD, java.lang.String.join(", ", *itemIds))
            if (email != null) {
                params.put(InternalParameter.EMAIL.value, email)
            }
            if (phone != null) {
                params.put(InternalParameter.PHONE.value, phone)
            }
            sendAsync(SUBSCRIPTION_UNSUBSCRIBE, params, listener)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
        }
    }

    /**
     * Manage subscriptions
     *
     * @param email
     * @param phone
     * @param subscriptions
     * @param listener
     */
    fun manageSubscription(
        email: String?,
        phone: String?,
        subscriptions: HashMap<String, Boolean>,
        listener: OnApiCallbackListener? = null
    ) {
        manageSubscription(
            email = email,
            phone = phone,
            externalId = null,
            loyaltyId = null,
            telegramId = null,
            subscriptions = subscriptions,
            listener = listener
        )
    }

    /**
     * Manage subscriptions
     *
     * @param email
     * @param phone
     * @param externalId
     * @param loyaltyId
     * @param telegramId
     * @param subscriptions
     * @param listener
     */
    @JvmOverloads
    fun manageSubscription(
        email: String?,
        phone: String?,
        externalId: String?,
        loyaltyId: String?,
        telegramId: String?,
        subscriptions: HashMap<String, Boolean>,
        listener: OnApiCallbackListener? = null
    ) {
        try {
            val params = JSONObject()
            for ((key, value) in subscriptions) {
                params.put(key, value)
            }
            if (email != null) {
                params.put(InternalParameter.EMAIL.value, email)
            }
            if (phone != null) {
                params.put(InternalParameter.PHONE.value, phone)
            }
            if (externalId != null) {
                params.put(InternalParameter.EXTERNAL_ID.value, externalId)
            }
            if (loyaltyId != null) {
                params.put(InternalParameter.LOYALTY_ID.value, loyaltyId)
            }
            if (telegramId != null) {
                params.put(InternalParameter.TELEGRAM_ID.value, telegramId)
            }
            sendAsync(SUBSCRIPTION_MANAGE, params, listener)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
        }
    }

    /**
     * Returns the current segment for A/B testing
     */
    fun getSegment(): String = getUserSettingsValueUseCase.getSegmentForABTesting()

    /**
     * Add user to a segment
     *
     * @param segmentId
     * @param email
     * @param phone
     */
    fun addToSegment(
        segmentId: String, email: String?, phone: String?, listener: OnApiCallbackListener? = null
    ) {
        segmentMethod(ADD_FIELD, segmentId, email, phone, listener)
    }

    /**
     * Remove user from a segment
     *
     * @param segment_id
     * @param email
     * @param phone
     */
    fun removeFromSegment(
        segment_id: String, email: String?, phone: String?, listener: OnApiCallbackListener? = null
    ) {
        segmentMethod(REMOVE_FIELD, segment_id, email, phone, listener)
    }

    /**
     * Get user segments
     *
     * @param listener
     */
    fun getCurrentSegment(listener: OnApiCallbackListener) {
        getAsync(SEGMENT_GET_FIELD, JSONObject(), listener)
    }

    private fun segmentMethod(
        method: String,
        segmentId: String?,
        email: String?,
        phone: String?,
        listener: OnApiCallbackListener?
    ) {
        try {
            val params = JSONObject()
            if (segmentId != null) {
                params.put(SEGMENT_ID_FIELD, segmentId)
            }
            if (email != null) {
                params.put(SEGMENT_EMAIL_FIELD, email)
            }
            if (phone != null) {
                params.put(SEGMENT_PHONE_FIELD, phone)
            }
            sendAsync("$SEGMENTS_FIELD/$method", params, listener)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
        }
    }

    /**
     * @param data from data notification
     */
    fun notificationReceived(data: Map<String, String>) {
        if (!isSdkInitialized) {
            warn(
                "notificationReceived() called before SDK.initialize(); ignoring. " +
                    "Initialize SDK.instance in Application.onCreate so push delivery works in a cold process."
            )
            return
        }
        val params = JSONObject()
        try {
            val type = data[TYPE_FIELD]
            if (type != null) {
                params.put(TYPE_FIELD, type)
            }
            val id = data[ID_FIELD]
            if (id != null) {
                params.put(CODE_FIELD, id)
            }
            if (params.length() > 0) {
                sendNetworkMethodUseCase.postAsync(TRACK_RECEIVED, params)
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
        }
    }

    private fun receiveMessage(data: Map<String, String>) {
        // Track the delivery on this instance's network and fire the deprecated per-instance listener.
        // The process-global listener (Rees46.setOnMessageListener) is dispatched by the router
        // ([Rees46.handlePush] / the [onMessage] companion) instead, so the notification shows even for a
        // shop that has no live instance yet.
        notificationReceived(data = data)
        onMessageListener?.onMessage(data = data.toNotificationData())
    }

    /** Entry point for [Rees46.handlePush]: track a received push on the already-resolved instance. */
    internal fun onPushReceived(data: Map<String, String>) = receiveMessage(data = data)

    companion object {

        var TAG = "SDK"

        /**
         * Optional debug hook for observing all SDK HTTP traffic (requests and responses).
         * Process-global; set it from a debug/QA build to mirror the SDK's network calls.
         * See [NetworkLogger]. Invoked on network threads — keep implementations fast and thread-safe.
         */
        @Volatile
        @JvmStatic
        var networkLogger: NetworkLogger? = null

        private const val SUBSCRIPTION_UNSUBSCRIBE_PRICE =
            "subscriptions/unsubscribe_from_product_price"
        private const val SUBSCRIPTION_UNSUBSCRIBE =
            "subscriptions/unsubscribe_from_product_available"
        private const val SUBSCRIPTION_SUBSCRIBE_PRICE = "subscriptions/subscribe_for_product_price"
        private const val SUBSCRIPTION_SUBSCRIBE = "subscriptions/subscribe_for_product_available"
        private const val SUBSCRIPTION_MANAGE = "subscriptions/manage"
        private const val PERSONALIZATION_SDK = "Personalizatio SDK "
        private const val ANDROID: String = "android"
        private const val BLANK_SEARCH_FIELD = "search/blank"
        private const val SEGMENT_GET_FIELD = "segments/get"
        private const val TRACK_RECEIVED = "track/received"
        private const val SET_PROFILE_FIELD = "profile/set"
        private const val SEGMENT_ID_FIELD = "segment_id"
        private const val SEGMENT_EMAIL_FIELD = "email"
        private const val SEGMENT_PHONE_FIELD = "email"
        private const val ITEM_IDS_FIELD = "item_ids"
        private const val SEGMENTS_FIELD = "segments"
        private const val SEARCH_FIELD = "search"
        private const val REMOVE_FIELD = "remove"
        private const val CODE_FIELD = "code"
        private const val TYPE_FIELD = "type"
        private const val ADD_FIELD = "add"
        private const val ID_FIELD = "id"

        /**
         * The SDK that receives push callbacks (see [onMessage]): whichever SDK was last passed to
         * [initialize], so the documented `SDK().initialize()` usage routes incoming pushes to that
         * initialized instance (otherwise the push hit a different, lazily-created object that was
         * never initialized). Falls back to a lazily-created instance if a push is delivered before
         * initialize() ran — that instance is guarded and won't crash the host. Backed by
         * [SdkRegistry], which owns the routing state for the coming multi-instance support.
         */
        val instance: SDK
            get() = SdkRegistry.currentOrLazy()

        fun userAgent(): String {
            return PERSONALIZATION_SDK + BuildConfig.FLAVOR.uppercase(Locale.getDefault()) + ", v" + BuildConfig.VERSION_NAME
        }

        /**
         * @param message Message
         */
        fun debug(message: String) {
            Log.d(TAG, message)
        }

        /**
         * @param message Message
         */
        fun warn(message: String?) {
            Log.w(TAG, message.toString())
        }

        /**
         * @param message Error message
         */
        fun error(message: String?) {
            Log.e(TAG, message.toString())
        }

        /**
         * @param message Error message
         */
        fun error(message: String?, e: Throwable?) {
            Log.e(TAG, message, e)
        }

        /**
         * Routes a push payload to the SDK so it is tracked as received and forwarded to the host's
         * [OnMessageListener] for display. Called by the SDK's messaging services.
         *
         * Both providers deliver the payload as a `data` map (title/body/icon/…): this FCM overload
         * unwraps [RemoteMessage.getData], while the map overload is used by [HmsMessagingService]
         * so Huawei data-messages are shown the same way as FCM ones.
         *
         * @param remoteMessage an FCM message
         */
        fun onMessage(remoteMessage: RemoteMessage) {
            onMessage(remoteMessage.data)
        }

        /**
         * Routes a push `data` payload among the currently-live shops. See the [RemoteMessage] overload
         * for the FCM entry point.
         *
         * The payload's `shop_id` names the target, and a single-shop app resolves with no `shop_id`. An
         * unknown `shop_id` — or none while several shops are live — is dropped, not delivered (and
         * tracked as received) against the wrong shop. A push arriving before any `initialize()` has
         * nowhere to route and is likewise dropped.
         *
         * This resolves against live shops only. The SDK's messaging services route through
         * [Rees46.handlePush] instead, which additionally materializes a registered-but-pending shop the
         * push targets — needed for a data-only push to reach a lazily-registered shop in a cold process.
         *
         * @param data the push data payload (title/body/icon/…)
         */
        fun onMessage(data: Map<String, String>) {
            val shopId = PushTargetResolver.resolve(
                payloadShopId = data[PreferencesPartition.SHOP_ID_FIELD],
                liveShopIds = SdkRegistry.shopIds()
            )
            val target = shopId?.let { SdkRegistry.byShopId(it) }
            if (target == null || shopId == null) {
                warn(
                    "onMessage: push dropped — no shop resolves it " +
                        "(shop_id=${data[PreferencesPartition.SHOP_ID_FIELD]}, live=${SdkRegistry.shopIds()})."
                )
                return
            }
            // Show via the process-global listener, then track received on the instance.
            SdkRegistry.dispatchMessage(shopId = shopId, data = data.toNotificationData())
            target.receiveMessage(data = data)
        }

        /**
         * Entry point used by the SDK's messaging services to report a token delivered via
         * their `onNewToken` callback. The public manual entry point for hosts that own their
         * messaging service is [setPushToken].
         */
        internal fun onPushTokenReceived(token: String, provider: PushProvider) {
            SdkRegistry.all().forEach { sdk ->
                if (sdk::pushTokenManager.isInitialized) {
                    sdk.pushTokenManager.onTokenReceived(token, provider)
                }
            }
        }
    }
}

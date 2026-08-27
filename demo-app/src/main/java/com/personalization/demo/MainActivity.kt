package com.personalization.demo

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.personalization.Params
import com.personalization.Params.TrackEvent
import com.personalization.PushProvider
import com.personalization.Rees46
import com.personalization.SDK
import com.personalization.OnClickListener
import com.personalization.Product
import com.personalization.stories.views.StoriesView
import com.personalization.api.OnApiCallbackListener
import com.personalization.api.models.tracking.TrackingSourceType
import com.personalization.api.models.tracking.TrackingSource
import com.personalization.api.models.tracking.TrackingItem
import com.personalization.api.managers.TrackingApi
import com.personalization.api.models.purchase.PurchaseItemRequest
import com.personalization.api.models.purchase.PurchaseTrackingRequest
import com.personalization.api.params.ProductItemParams
import com.personalization.api.params.PurchasePredictParams
import com.personalization.demo.BuildConfig
import com.personalization.sdk.data.models.dto.popUp.Components
import org.json.JSONObject
import com.personalization.sdk.data.models.dto.popUp.PopupActions
import com.personalization.sdk.data.models.dto.popUp.PopupDto
import com.personalization.sdk.data.models.dto.popUp.Position

class MainActivity : AppCompatActivity() {

    private lateinit var sdk: SDK

    /** Most recent OnClickListener callbacks from the "Legacy UI" tab, newest first. */
    private val legacyStoriesEvents = mutableListOf<String>()

    private companion object {
        const val MAX_LOGGED_STORIES_EVENTS = 20
    }

    private object DemoTrackingNamespaceConstants {
        const val ITEM_ID = "android-demo-sku-001"
        const val SECOND_ITEM_ID = "android-demo-sku-002"
        const val CATEGORY_ID = "android-demo-category"
        const val SEARCH_QUERY = "demo boots"
        const val QUANTITY = 2
        const val PRICE = 49.9
        const val SOURCE_CODE = "android-demo-block"
    }

    private object DemoTrackEventConstants {
        /** Same value as SDK client-side validation errors for custom field key collisions. */
        const val CLIENT_VALIDATION_ERROR_CODE = -1
        // Must be an event registered for the shop, otherwise the backend returns
        // 400 "Event <name> not found". Reuses the same registered event as the Flutter demo.
        const val EVENT_NAME = "flutter_example"
        const val SAMPLE_UNIX_TIME = 123_456
        const val CATEGORY = "demo_category"
        const val LABEL = "demo_label"
        const val SAMPLE_VALUE = 100
        const val SAFE_CUSTOM_KEY = "demo_custom_key"
        const val SAFE_CUSTOM_VALUE = "android_demo_app"
        const val COLLISION_RESERVED_KEY = "shop_id"
        const val COLLISION_PLACEHOLDER_VALUE = "collision_demo"
    }

    private object DemoOrdersConstants {
        /**
         * Server-side shop secret for `orders/by_user`. Sourced from `shop.secret` in local.properties
         * (gitignored) via BuildConfig — never hardcode a real secret here. Falls back to a placeholder.
         */
        val SHOP_SECRET = BuildConfig.SHOP_SECRET
    }

    private object DemoLoyaltyConstants {
        const val PHONE = "79991234567"
        const val EMAIL = "demo@rees46.ru"
        const val FIRST_NAME = "Demo"
        const val LAST_NAME = "User"
    }

    private object DemoCatalogConstants {
        const val ITEM_ID = "300275"
        const val CATEGORY_SLUG = "smartfony-i-gadzhety"
        const val COLLECTION_ID = "1"
    }

    private object DemoProductViewConstants {
        const val PRODUCT_ID = "demo-product-view-001"
        const val DEMO_PRICE = 2499.99
        const val DEMO_AMOUNT = 1
    }

    private object DemoPurchaseTrackingConstants {
        const val ORDER_ID_MINIMAL = "android-demo-order-minimal"
        const val ORDER_ID_FULL = "android-demo-order-full"
        const val ORDER_PRICE_MINIMAL = 199.0
        const val ORDER_PRICE_FULL = 999.0
        const val ITEM_ID = "android-demo-sku-001"
        const val ITEM_AMOUNT = 1
        const val ITEM_PRICE = 99.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase if not already initialized
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // The shop is initialized once in DemoApplication.onCreate (Application) so it is ready in
        // every process — including the cold process FCM/HMS starts to deliver a push. Reach that same
        // registered instance by its shopId (do NOT create a new SDK()).
        sdk = Rees46.getInstance(BuildConfig.SHOP_ID)
        // Show the registered push provider(s) + token (FCM and/or HMS) in the header.
        observePushTokens()

        // Initialize fragment manager for popups
        sdk.inAppNotificationManager.initFragmentManager(supportFragmentManager)

        setupStoriesTabs()

        findViewById<Button>(R.id.btnHttpLog).setOnClickListener {
            startActivity(android.content.Intent(this, HttpLogActivity::class.java))
        }

        findViewById<Button>(R.id.btnShowTestPopup).setOnClickListener {
            showTestPopup()
        }

        findViewById<Button>(R.id.btnTrackEventCustomFields).setOnClickListener {
            trackEventWithCustomFieldsSuccess()
        }

        setupTrackingNamespaceButtons()

        findViewById<Button>(R.id.btnTrackEventCollision).setOnClickListener {
            trackEventWithReservedKeyCollision()
        }

        findViewById<Button>(R.id.btnTrackViewNoParams).setOnClickListener {
            trackProductViewIdOnly()
        }

        findViewById<Button>(R.id.btnTrackViewWithParams).setOnClickListener {
            trackProductViewWithItemParams()
        }

        findViewById<Button>(R.id.btnPredictDidOnly).setOnClickListener {
            predictPurchase(PurchasePredictParams())
        }

        findViewById<Button>(R.id.btnPredictWithEmail).setOnClickListener {
            predictPurchase(
                PurchasePredictParams(email = getString(R.string.predict_demo_email))
            )
        }

        findViewById<Button>(R.id.btnTrackPurchaseMinimal).setOnClickListener {
            trackPurchaseMinimal()
        }

        findViewById<Button>(R.id.btnTrackPurchaseFull).setOnClickListener {
            trackPurchaseFull()
        }

        findViewById<Button>(R.id.btnGetLastOrderProducts).setOnClickListener {
            getLastOrderProducts()
        }

        findViewById<Button>(R.id.btnGetUserOrders).setOnClickListener {
            getUserOrders()
        }

        findViewById<Button>(R.id.btnLoyaltyJoin).setOnClickListener {
            loyaltyJoin()
        }

        findViewById<Button>(R.id.btnLoyaltyStatus).setOnClickListener {
            loyaltyStatus()
        }

        findViewById<Button>(R.id.btnGetProfile).setOnClickListener {
            getProfile()
        }

        findViewById<Button>(R.id.btnGetProductCounters).setOnClickListener {
            getProductCounters()
        }

        findViewById<Button>(R.id.btnGetCategory).setOnClickListener {
            getCategory()
        }

        findViewById<Button>(R.id.btnGetCollection).setOnClickListener {
            getCollection()
        }

        findViewById<Button>(R.id.btnCopyToken).setOnClickListener {
            copyTokenToClipboard()
        }

        ensureNotificationPermission()
        showLastCrashIfAny()
        // App may have been opened by tapping a push — report the click to the SDK.
        handleNotificationClick(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
    }

    /**
     * Safety net for a known Jetpack Compose bug: on some OEM devices (notably Huawei/EMUI) a stray
     * hover event makes AndroidComposeView throw `IllegalStateException: The ACTION_HOVER_EXIT event
     * was not cleared`. The exception unwinds through `Activity.dispatchGenericMotionEvent`, so
     * catching it here stops it from crashing the app. Hover events are non-essential for touch, so
     * dropping the offending one is harmless. Root fix is the Compose BOM bump to 1.7.x (see
     * demo-app/build.gradle) — this catch stays as belt-and-suspenders for older/other OEM quirks.
     * Ref: Google issue 329330869.
     */
    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean =
        try {
            super.dispatchGenericMotionEvent(ev)
        } catch (e: IllegalStateException) {
            Log.w("MainActivity", "Swallowed a Compose hover-dispatch crash", e)
            false
        }

    /**
     * When the app is opened by tapping an SDK push, the launch intent carries the notification's
     * type/id. Forward them to the SDK so it can send `track/clicked`. Guarded so normal launches
     * (no push extras) don't trigger spurious tracking.
     */
    private fun handleNotificationClick(intent: Intent?) {
        val extras = intent?.extras ?: return
        val hasPushExtras = extras.getString("NOTIFICATION_TYPE") != null ||
            extras.getString("NOTIFICATION_ID") != null
        if (!hasPushExtras) return
        sdk.notificationClicked(extras)
    }

    /** If the previous run crashed (e.g. while handling a push), show the saved stack with Copy. */
    private fun showLastCrashIfAny() {
        val prefs = getSharedPreferences(DemoApplication.CRASH_PREFS, Context.MODE_PRIVATE)
        val crash = prefs.getString(DemoApplication.KEY_LAST_CRASH, null) ?: return
        AlertDialog.Builder(this)
            .setTitle("Last crash")
            .setMessage(crash)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("crash", crash))
                Toast.makeText(this, "Crash copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Clear") { _, _ ->
                prefs.edit().remove(DemoApplication.KEY_LAST_CRASH).apply()
            }
            .show()
    }

    /** Android 13+ requires the POST_NOTIFICATIONS runtime permission for any notification to show. */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                /* requestCode = */ 1001,
            )
        }
    }

    private fun getCollection() {
        sdk.collectionManager.getCollection(
            collectionId = DemoCatalogConstants.COLLECTION_ID,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.get_collection_ok, "products=${response.products.size}"),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_collection_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun getProfile() {
        sdk.profileManager.getProfile(
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.get_profile_ok,
                            "id=${response.id}, hasEmail=${response.hasEmail}, gender=${response.gender ?: "—"}",
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_profile_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun getProductCounters() {
        sdk.productsManager.getProductCounters(
            item = DemoCatalogConstants.ITEM_ID,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.get_product_counters_ok,
                            "now.view=${response.now?.view}, price_drop=${response.triggers?.priceDrop}",
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_product_counters_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun getCategory() {
        sdk.categoryManager.getCategory(
            category = DemoCatalogConstants.CATEGORY_SLUG,
            limit = 5,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.get_category_ok,
                            "total=${response.productsTotal}, products=${response.products.size}",
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_category_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun loyaltyJoin() {
        sdk.loyaltyManager.join(
            phone = DemoLoyaltyConstants.PHONE,
            email = DemoLoyaltyConstants.EMAIL,
            firstName = DemoLoyaltyConstants.FIRST_NAME,
            lastName = DemoLoyaltyConstants.LAST_NAME,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.loyalty_join_ok, response.status ?: "—"),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.loyalty_join_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun loyaltyStatus() {
        sdk.loyaltyManager.getStatus(
            identifier = DemoLoyaltyConstants.PHONE,
            onSuccess = { response ->
                runOnUiThread {
                    val member = response.payload?.member
                    val level = response.payload?.level?.name
                    Toast.makeText(
                        this,
                        getString(
                            R.string.loyalty_status_ok,
                            "${response.status ?: "—"} (member=$member, level=$level)",
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.loyalty_status_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun getUserOrders() {
        sdk.ordersManager.getUserOrders(
            shopSecret = DemoOrdersConstants.SHOP_SECRET,
            onSuccess = { orders ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.get_user_orders_ok, orders.size),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_user_orders_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun getLastOrderProducts() {
        sdk.ordersManager.getLastOrderProducts(
            onGetLastOrderProducts = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.get_last_order_products_ok, response.products.size),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${getString(R.string.get_last_order_products_fail)}: $code $msg",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun trackPurchaseMinimal() {
        val request = PurchaseTrackingRequest(
            orderId = DemoPurchaseTrackingConstants.ORDER_ID_MINIMAL,
            orderPrice = DemoPurchaseTrackingConstants.ORDER_PRICE_MINIMAL,
            items = listOf(
                PurchaseItemRequest(
                    id = DemoPurchaseTrackingConstants.ITEM_ID,
                    amount = DemoPurchaseTrackingConstants.ITEM_AMOUNT,
                    price = DemoPurchaseTrackingConstants.ITEM_PRICE,
                ),
            ),
        )
        sdk.trackPurchase(
            request,
            object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.track_purchase_ok),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${getString(R.string.track_purchase_fail)}: $msg",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
        )
    }

    private fun trackPurchaseFull() {
        val request = PurchaseTrackingRequest(
            orderId = DemoPurchaseTrackingConstants.ORDER_ID_FULL,
            orderPrice = DemoPurchaseTrackingConstants.ORDER_PRICE_FULL,
            items = listOf(
                PurchaseItemRequest(
                    id = DemoPurchaseTrackingConstants.ITEM_ID,
                    amount = 2,
                    price = 49.99,
                    quantity = 2,
                    lineId = "demo-line-1",
                    fashionSize = "L",
                ),
            ),
            deliveryType = "courier",
            deliveryAddress = "Demo address",
            paymentType = "card",
            isTaxFree = true,
            promocode = "DEMO10",
            orderCash = 100.0,
            orderBonuses = 10.0,
            orderDelivery = 5.0,
            orderDiscount = 15.0,
            channel = "mobile",
            custom = mapOf("demo_custom" to "android_demo"),
            recommendedBy = Params.RecommendedBy(Params.RecommendedBy.TYPE.RECOMMENDATION, "demo-block"),
            recommendedSource = JSONObject().put("source_key", "source_value"),
            stream = "demo-stream",
            segment = "A",
        )
        sdk.trackPurchase(
            request,
            object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.track_purchase_ok),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${getString(R.string.track_purchase_fail)}: $msg",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
        )
    }

    private fun predictPurchase(params: PurchasePredictParams) {
        sdk.predictManager.getProbabilityToPurchase(
            params = params,
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.predict_ok, response.probability, response.clientId),
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onError = { code, msg ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.predict_fail, code, msg ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    /**
     * One button per standard event, all of them going through the `tracking` namespace.
     * Each writes its outcome into `tvTrackingNamespaceResult`, which is what
     * `TrackingNamespaceDemoE2ETest` reads.
     */
    private fun setupTrackingNamespaceButtons() {
        findViewById<Button>(R.id.btnTrackingProductView).setOnClickListener {
            runTrackingDemo("productView") { tracking, listener ->
                tracking.productView(
                    itemId = DemoTrackingNamespaceConstants.ITEM_ID,
                    source = TrackingSource(
                        type = TrackingSourceType.DYNAMIC,
                        code = DemoTrackingNamespaceConstants.SOURCE_CODE
                    ),
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingCategoryView).setOnClickListener {
            runTrackingDemo("categoryView") { tracking, listener ->
                tracking.categoryView(
                    categoryId = DemoTrackingNamespaceConstants.CATEGORY_ID,
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingSearch).setOnClickListener {
            runTrackingDemo("search") { tracking, listener ->
                tracking.search(
                    query = DemoTrackingNamespaceConstants.SEARCH_QUERY,
                    results = listOf(
                        DemoTrackingNamespaceConstants.ITEM_ID,
                        DemoTrackingNamespaceConstants.SECOND_ITEM_ID
                    ),
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingAddToCart).setOnClickListener {
            runTrackingDemo("addToCart") { tracking, listener ->
                tracking.addToCart(
                    item = TrackingItem(
                        id = DemoTrackingNamespaceConstants.ITEM_ID,
                        quantity = DemoTrackingNamespaceConstants.QUANTITY,
                        price = DemoTrackingNamespaceConstants.PRICE
                    ),
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingSyncCart).setOnClickListener {
            runTrackingDemo("syncCart") { tracking, listener ->
                tracking.syncCart(
                    items = listOf(
                        TrackingItem(
                            id = DemoTrackingNamespaceConstants.ITEM_ID,
                            quantity = DemoTrackingNamespaceConstants.QUANTITY,
                            price = DemoTrackingNamespaceConstants.PRICE
                        ),
                        TrackingItem(id = DemoTrackingNamespaceConstants.SECOND_ITEM_ID)
                    ),
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingRemoveFromCart).setOnClickListener {
            runTrackingDemo("removeFromCart") { tracking, listener ->
                tracking.removeFromCart(
                    itemId = DemoTrackingNamespaceConstants.ITEM_ID,
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingAddToFavorites).setOnClickListener {
            runTrackingDemo("addToFavorites") { tracking, listener ->
                tracking.addToFavorites(
                    itemId = DemoTrackingNamespaceConstants.ITEM_ID,
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingSyncFavorites).setOnClickListener {
            runTrackingDemo("syncFavorites") { tracking, listener ->
                tracking.syncFavorites(
                    itemIds = listOf(
                        DemoTrackingNamespaceConstants.ITEM_ID,
                        DemoTrackingNamespaceConstants.SECOND_ITEM_ID
                    ),
                    listener = listener
                )
            }
        }

        findViewById<Button>(R.id.btnTrackingRemoveFromFavorites).setOnClickListener {
            runTrackingDemo("removeFromFavorites") { tracking, listener ->
                tracking.removeFromFavorites(
                    itemId = DemoTrackingNamespaceConstants.ITEM_ID,
                    listener = listener
                )
            }
        }

        // setSource stores the attribution locally and answers nothing — report it as sent.
        findViewById<Button>(R.id.btnTrackingSetSource).setOnClickListener {
            sdk.tracking.setSource(
                TrackingSource(
                    type = TrackingSourceType.DYNAMIC,
                    code = DemoTrackingNamespaceConstants.SOURCE_CODE
                )
            )
            showTrackingResult("setSource OK")
        }
    }

    private fun runTrackingDemo(
        method: String,
        call: (TrackingApi, OnApiCallbackListener) -> Unit
    ) {
        call(
            sdk.tracking,
            object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    showTrackingResult("$method OK")
                }

                override fun onError(code: Int, msg: String?) {
                    showTrackingResult("$method failed: $code ${msg ?: ""}")
                }
            }
        )
    }

    private fun showTrackingResult(text: String) = runOnUiThread {
        findViewById<TextView>(R.id.tvTrackingNamespaceResult).text = text
    }

    private fun trackProductViewIdOnly() {
        sdk.trackEventManager.track(TrackEvent.VIEW, DemoProductViewConstants.PRODUCT_ID)
        Toast.makeText(this, getString(R.string.track_view_no_params_queued), Toast.LENGTH_SHORT).show()
    }

    private fun trackProductViewWithItemParams() {
        val item = ProductItemParams(DemoProductViewConstants.PRODUCT_ID)
            .set(ProductItemParams.PARAMETER.PRICE, DemoProductViewConstants.DEMO_PRICE)
            .set(ProductItemParams.PARAMETER.AMOUNT, DemoProductViewConstants.DEMO_AMOUNT)

        sdk.trackEventManager.track(
            TrackEvent.VIEW,
            Params().put(item),
            object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.track_view_ok),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${getString(R.string.track_view_fail)}: $msg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun trackEventWithCustomFieldsSuccess() {
        val customFields = mapOf(DemoTrackEventConstants.SAFE_CUSTOM_KEY to DemoTrackEventConstants.SAFE_CUSTOM_VALUE)
        sdk.trackEvent(
            event = DemoTrackEventConstants.EVENT_NAME,
            time = DemoTrackEventConstants.SAMPLE_UNIX_TIME,
            category = DemoTrackEventConstants.CATEGORY,
            label = DemoTrackEventConstants.LABEL,
            value = DemoTrackEventConstants.SAMPLE_VALUE,
            customFields = customFields,
            listener = object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.track_event_ok),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${getString(R.string.track_event_fail)}: $msg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun trackEventWithReservedKeyCollision() {
        val customFields = mapOf(
            DemoTrackEventConstants.COLLISION_RESERVED_KEY to DemoTrackEventConstants.COLLISION_PLACEHOLDER_VALUE
        )
        sdk.trackEvent(
            event = DemoTrackEventConstants.EVENT_NAME,
            time = DemoTrackEventConstants.SAMPLE_UNIX_TIME,
            category = DemoTrackEventConstants.CATEGORY,
            label = DemoTrackEventConstants.LABEL,
            value = DemoTrackEventConstants.SAMPLE_VALUE,
            customFields = customFields,
            listener = object : OnApiCallbackListener() {
                override fun onSuccess(response: JSONObject?) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.track_event_unexpected_success),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    runOnUiThread {
                        val isClientValidation = code == DemoTrackEventConstants.CLIENT_VALIDATION_ERROR_CODE
                            && msg?.contains("customFields contains reserved keys") == true
                        val text = if (isClientValidation) {
                            "${getString(R.string.track_event_collision_ok)}\n$msg"
                        } else {
                            "${getString(R.string.track_event_fail)}: $msg"
                        }
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private val pushTokens = linkedMapOf<PushProvider, String>()

    /**
     * Shows the registered push provider(s) and token(s) in the header. Works the same for FCM and
     * HMS: the SDK captures and registers each token from its own messaging services (autoSendPushToken
     * on init) and reports it here. This is display only — registration is handled by the SDK.
     *
     * The SDK is initialized in DemoApplication, so the live listener may have already fired before
     * this Activity subscribed. To still show a token immediately we seed from the SDK's per-provider
     * cache and, for FCM, query Firebase directly; HMS (delivered asynchronously) arrives via the
     * live listener.
     */
    private fun observePushTokens() {
        val typeView = findViewById<TextView>(R.id.tvPushTokenType)
        val tokenView = findViewById<TextView>(R.id.tvPushToken)
        typeView.text = getString(R.string.push_token_type_placeholder)
        tokenView.text = getString(R.string.push_token_placeholder)

        // Tap the token to copy the most recent one to the clipboard.
        tokenView.setOnClickListener { copyTokenToClipboard() }

        // Seed from the SDK's per-provider cache (tokens already fetched + registered at init).
        PushProvider.entries.forEach { provider ->
            sdk.getPushToken(provider)?.let { pushTokens[provider] = it }
        }
        renderPushTokens(typeView, tokenView)

        // Guarantee the FCM token shows even before the SDK's async registration persists it.
        // Only when Firebase is actually configured: on a Huawei-only build there is no
        // google-services.json, so no default FirebaseApp, and FirebaseMessaging.getInstance()
        // would throw IllegalStateException and crash the Activity. The try/catch is defence in
        // depth. HMS tokens still arrive through the SDK cache/listener below.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        runOnUiThread {
                            pushTokens[PushProvider.FCM] = task.result
                            renderPushTokens(typeView, tokenView)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Live updates (HMS arrives asynchronously; FCM refresh). Display only.
        sdk.setOnPushTokenListener { token, provider ->
            runOnUiThread {
                pushTokens[provider] = token
                renderPushTokens(typeView, tokenView)
            }
        }
    }

    /** Copies the most recently received push token to the clipboard so a tester can share it. */
    private fun copyTokenToClipboard() {
        val entry = pushTokens.entries.lastOrNull()
        if (entry == null) {
            Toast.makeText(this, getString(R.string.push_token_copy_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("push_token", entry.value))
        Toast.makeText(
            this,
            getString(R.string.push_token_copied, entry.key.id),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderPushTokens(typeView: TextView, tokenView: TextView) {
        if (pushTokens.isEmpty()) {
            typeView.text = getString(R.string.push_token_type_placeholder)
            tokenView.text = getString(R.string.push_token_placeholder)
            return
        }
        typeView.text = getString(
            R.string.push_token_type_value,
            pushTokens.keys.joinToString(", ") { it.id }
        )
        tokenView.text = pushTokens.entries.joinToString("\n\n") { (provider, token) ->
            "${provider.id}: $token"
        }
    }

    private fun showTestPopup() {
        val testPopup = PopupDto(
            id = 999,
            channels = listOf("email"),
            position = Position.CENTERED,
            delay = 0,
            html = """
                <div class="popup-title">Test Popup</div>
                <p class="popup-999__intro">This is a test popup for Android SDK</p>
            """.trimIndent(),
            components = Components(
                header = "Test Popup",
                text = "This is a test popup for Android SDK",
                image = "",
                button = "",
                textEnabled = "",
                imageEnabled = "",
                headerEnabled = ""
            ),
            webPushSystem = false,
            popupActions = PopupActions(
                link = null,
                close = null,
                pushSubscribe = null
            )
        )

        sdk.inAppNotificationManager.shopPopUp(testPopup)
    }

    /**
     * Wires the bottom navigation and both stories tabs.
     *
     * "UI Kit" renders the block with the SDK's Compose wrapper, "Legacy UI" with the XML view, so
     * the two integration styles can be compared side by side. The API pane keeps the SDK method
     * demos and no longer carries a stories block of its own.
     */
    private fun setupStoriesTabs() {
        val apiContent = findViewById<View>(R.id.apiContent)
        val uiKitContent = findViewById<ComposeView>(R.id.uiKitContent)
        val legacyContent = findViewById<View>(R.id.legacyContent)
        val multiInstanceContent = findViewById<ComposeView>(R.id.multiInstanceContent)
        val storiesCode = getString(R.string.stories_code)

        // Legacy pane: the code comes from app:code in the layout and the view loads itself on
        // attach — it resolves its SDK instance through Rees46 and fetches the block, queuing the
        // request until the session is ready. Because the app is multi-shop (shop B lives in the
        // Multi tab), the view must name its shop, or the default resolution would be ambiguous —
        // so set shopId to shop A explicitly. The host only wires the click listener; there is no
        // initializeStoriesView call any more.
        val legacyLog = findViewById<TextView>(R.id.tvLegacyStoriesLog)
        val storiesView = findViewById<StoriesView>(R.id.storiesView)
        storiesView.shopId = BuildConfig.SHOP_ID
        storiesView.itemClickListener = object : OnClickListener {
            override fun onClick(url: String): Boolean {
                appendLegacyStoriesLog(legacyLog, "onClick(url): $url")
                return true
            }

            override fun onClick(product: Product): Boolean {
                appendLegacyStoriesLog(legacyLog, "onClick(product): ${product.name}")
                return true
            }
        }

        // UI Kit pane: the Compose widget resolves the instance itself from the shopId — shop A here,
        // named explicitly since the app is multi-shop. DemoTheme makes it follow light/dark.
        uiKitContent.setContent {
            DemoTheme { ComposeStoriesPane(code = storiesCode, shopId = BuildConfig.SHOP_ID) }
        }

        // Multi-instance pane: shop A (default) and shop B living side by side.
        multiInstanceContent.setContent {
            DemoTheme {
                MultiInstancePane(
                    shopIdA = BuildConfig.SHOP_ID,
                    shopIdB = BuildConfig.SHOP_ID_2,
                    storiesCodeA = storiesCode,
                    storiesCodeB = getString(R.string.stories2_code),
                )
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            apiContent.visibility = if (item.itemId == R.id.tabApi) View.VISIBLE else View.GONE
            uiKitContent.visibility = if (item.itemId == R.id.tabUiKit) View.VISIBLE else View.GONE
            legacyContent.visibility = if (item.itemId == R.id.tabLegacyUi) View.VISIBLE else View.GONE
            multiInstanceContent.visibility =
                if (item.itemId == R.id.tabMultiInstance) View.VISIBLE else View.GONE
            true
        }
        // Drive the initial pane through the same listener, so the checked item and the visible
        // pane cannot drift apart (including after the activity is recreated).
        bottomNav.selectedItemId = R.id.tabApi
    }

    private fun appendLegacyStoriesLog(target: TextView, message: String) {
        legacyStoriesEvents.add(0, message)
        if (legacyStoriesEvents.size > MAX_LOGGED_STORIES_EVENTS) {
            legacyStoriesEvents.removeAt(legacyStoriesEvents.lastIndex)
        }
        runOnUiThread { target.text = legacyStoriesEvents.joinToString("\n") }
    }
}


package com.personalizatio.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.personalizatio.SDK
import com.personalizatio.stories.views.StoriesView

abstract class AbstractMainActivity<out T : SDK> internal constructor(
    private val sdk: SDK
) : AppCompatActivity() {
    private lateinit var text: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //		Log.e("ID", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        val storiesView = findViewById<StoriesView>(R.id.stories_view)
        sdk.storiesManager.initialize(storiesView)

        if (intent.extras != null) {
            sdk.notificationClicked(intent.extras)
        }

        button = findViewById(R.id.button)
        text = findViewById(R.id.email)
        text.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                button.callOnClick()
            }
            false
        }

        button.setOnClickListener {
            if (text.getText().toString().isNotEmpty()) {
                val params = HashMap<String, String>()
                params["email"] = text.getText().toString()
                sdk.profile(params)
                Toast.makeText(applicationContext, "Email sent", Toast.LENGTH_LONG).show()
            }
        }

        sdk.notificationClicked(intent.extras)

//      //Запрашиваем поиск
//		val params = SearchParams()
//		params.put(SearchParams.Parameter.LOCATIONS, "location")
//		val filters = SearchParams.SearchFilters()
//		filters.put("voltage", arrayOf("11.1", "14.8"))
//		params.put(SearchParams.Parameter.FILTERS, filters);
//        sdk.search("coats", SearchParams.TYPE.FULL, params, object : OnApiCallbackListener() {
//            override fun onSuccess(response: JSONObject?) {
//                Log.i(sdk.tag, "Search response: $response")
//            }
//        })
//
//		//Запрашиваем поиск при клике на пустое поле
//        sdk.searchBlank(object : OnApiCallbackListener() {
//            override fun onSuccess(response: JSONObject?) {
//                Log.i(sdk.tag, "Search response: $response")
//            }
//        })
//
//		//Запрашиваем блок рекомендаций
//		val recommenderParams = Params()
//        recommenderParams.put(Params.Parameter.EXTENDED, true)
//        recommenderParams.put(Params.Parameter.ITEM, "37")
//        sdk.recommend("e9ddb9cdc66285fac40c7a897760582a", recommenderParams, object : OnApiCallbackListener() {
//            override fun onSuccess(response: JSONObject?) {
//                Log.i(sdk.tag, "Recommender response: $response")
//            }
//        })
//
//		//Просмотр товара (простой)
//        sdk.track(Params.TrackEvent.VIEW, "37")
//
//		//Добавление в корзину (простое)
//        sdk.track(Params.TrackEvent.CART, "37")
//
//		//Добавление в корзину (расширенный)
//		val cart = Params()
//		cart
//			.put(Params.Item("37")
//				.set(Params.Item.COLUMN.AMOUNT, 2)
//				.set(Params.Item.COLUMN.FASHION_SIZE, "M")
//			)
//			.put(Params.RecommendedBy(Params.RecommendedBy.TYPE.RECOMMENDATION, "e9ddb9cdc66285fac40c7a897760582a"))
//        sdk.track(Params.TrackEvent.CART, cart)
//
//		//Трекинг полной корзины
//		val fullCart = Params()
//        fullCart
//			.put(Params.Parameter.FULL_CART, true)
//			.put(Params.Item("37")
//				.set(Params.Item.COLUMN.AMOUNT, 2)
//				.set(Params.Item.COLUMN.FASHION_SIZE, "M")
//			)
//			.put(Params.Item("40")
//				.set(Params.Item.COLUMN.AMOUNT, 1)
//				.set(Params.Item.COLUMN.FASHION_SIZE, "M")
//			)
//        sdk.track(Params.TrackEvent.CART, fullCart)
//
//		//Покупка
//		val purchase = Params()
//		purchase
//				.put(Params.Item("37").set(Params.Item.COLUMN.AMOUNT, 2).set(Params.Item.COLUMN.PRICE, 10.5))
//				.put(Params.Item("38").set(Params.Item.COLUMN.AMOUNT, 2))
//				.put(Params.Parameter.ORDER_ID, "100234")
//				.put(Params.Parameter.ORDER_PRICE, 100500)
//				.put(Params.RecommendedBy(Params.RecommendedBy.TYPE.RECOMMENDATION, "e9ddb9cdc66285fac40c7a897760582a"))
//        sdk.track(Params.TrackEvent.PURCHASE, purchase)
//
//		//Просмотр категории
//        sdk.track(Params.TrackEvent.CATEGORY, Params().put(Params.Parameter.CATEGORY_ID, "100"))
//
//		//Трекинг поиска
//        sdk.track(Params.TrackEvent.SEARCH, Params().put(Params.Parameter.SEARCH_QUERY, "coats"))
    }
}

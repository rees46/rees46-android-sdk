package com.personalizatio

import android.util.Log
import com.personalizatio.api.params.ProductItemParams
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Params : AbstractParams<Params>() {
    /**
     * Основные параметры
     */
    enum class Parameter(override val value: String) : ParamInterface {
        LIMIT("limit"),
        ITEM("item"),
        PRICE("price"),
        LOCATIONS("locations"),

        /**
         * Available sizes: 120, 140, 160, 180, 200, 220, 310, 520
         */
        IMAGE_SIZE("resize_image"),
        BRANDS("brands"),
        EXCLUDE_BRANDS("exclude_brands"),
        CATEGORIES("categories"),
        DISCOUNT("discount"),
        FULL_CART("full_cart"),
        FULL_WISH("full_wish"),
        ORDER_ID("order_id"),
        ORDER_PRICE("order_price"),
        DELIVERY_ADDRESS("delivery_address"),
        DELIVERY_TYPE("delivery_type"),
        PROMOCODE("promocode"),
        PAYMENT_TYPE("payment_type"),
        TAX_FREE("tax_free"),
        CATEGORY_ID("category_id"),
        CATEGORY("category"),
        SEARCH_QUERY("search_query"),
        EXTENDED("extended"),
    }

    /**
     * Типы рекомендаций
     */
    class RecommendedBy {
        enum class TYPE(val value: String) {
            RECOMMENDATION("dynamic"),
            TRIGGER("chain"),
            BULK("bulk"),
            TRANSACTIONAL("transactional"),
            INSTANT_SEARCH("instant_search"),
            FULL_SEARCH("full_search"),
            STORIES("stories"),
        }

        val type: String
        var code: String? = null

        constructor(type: TYPE) {
            this.type = type.value
        }

        constructor(type: TYPE, code: String?) {
            this.type = type.value
            this.code = code
        }
    }

    /**
     * Product
     */
    @Deprecated(
        "This class will be removed in future versions.",
        level = DeprecationLevel.WARNING, replaceWith = ReplaceWith(
            "ProductItemParams",
            "com.personalizatio.api.params.ProductItemParams"
        )
    )
    class Item(id: String) {
        enum class COLUMN(val value: String) {
            ID("id"),
            AMOUNT("amount"),
            PRICE("price"),
            FASHION_SIZE("fashion_size"),
        }

        val columns: HashMap<String, String> = HashMap()

        init {
            columns[COLUMN.ID.value] = id
        }

        fun set(column: COLUMN, value: String): Item {
            columns[column.value] = value
            return this
        }

        fun set(column: COLUMN, value: Int): Item {
            return set(column, value.toString())
        }

        fun set(column: COLUMN, value: Double): Item {
            return set(column, value.toString())
        }

        fun set(column: COLUMN, value: Boolean): Item {
            return set(column, if (value) "1" else "0")
        }
    }

    class CustomOrderParameters {
        val parameters: HashMap<String, String> = HashMap()

        fun set(name: String, value: String): CustomOrderParameters {
            parameters[name] = value
            return this
        }

        fun set(name: String, value: Int): CustomOrderParameters {
            return set(name, value.toString())
        }

        fun set(name: String, value: Double): CustomOrderParameters {
            return set(name, value.toString())
        }

        fun set(name: String, value: Boolean): CustomOrderParameters {
            return set(name, if (value) "1" else "0")
        }
    }

    enum class TrackEvent(@JvmField var value: String) {
        VIEW("view"),
        CATEGORY("category"),
        CART("cart"),
        REMOVE_FROM_CART("remove_from_cart"),
        PURCHASE("purchase"),
        SEARCH("search"),
        WISH("wish"),
        REMOVE_FROM_WISH("remove_wish"),
    }

    /**
     * Вставка параметров рекомендаций
     */
    fun put(recommendedBy: RecommendedBy): Params {
        try {
            params.put(InternalParameter.RECOMMENDED_BY.value, recommendedBy.type)
            if (recommendedBy.code != null) {
                params.put(InternalParameter.RECOMMENDED_CODE.value, recommendedBy.code)
            }
        } catch (e: JSONException) {
            Log.e(SDK.TAG, e.message, e)
        }
        return this
    }

    /**
     * Add item
     */
    @Deprecated(
        "This method will be removed in future versions.",
        level = DeprecationLevel.WARNING, replaceWith = ReplaceWith(
            "put(productItemParams)",
            "com.personalizatio.api.params.ProductItemParams"
        )
    )
    fun put(item: Item): Params {
        return putProductParameters(item.columns)
    }

    /**
     * Add product
     */
    fun put(productItemParams: ProductItemParams): Params {
        return putProductParameters(productItemParams.parameters)
    }

    private fun putProductParameters(parameters: HashMap<String, String>): Params {
        try {
            val array: JSONArray
            if (params.has("items")) {
                array = params.getJSONArray("items")
            } else {
                array = JSONArray()
                params.put("items", array)
            }
            val jsonObject = JSONObject()
            for ((key, value) in parameters) {
                jsonObject.put(key, value)
            }
            array.put(jsonObject)
        } catch (e: JSONException) {
            Log.e(SDK.TAG, e.message, e)
        }

        return this
    }

    /**
     * Вставка свойст заказа
     */
    fun put(customOrderParameter: CustomOrderParameters): Params {
        try {
            val jsonObject = JSONObject()
            for ((key, value) in customOrderParameter.parameters) {
                jsonObject.put(key, value)
            }

            params.put("custom", jsonObject)
        } catch (e: JSONException) {
            Log.e(SDK.TAG, e.message, e)
        }

        return this
    }

    //---------------Private---------->
    internal enum class InternalParameter(override val value: String) : ParamInterface {
        SEARCH_TYPE("type"),
        SEARCH_QUERY("search_query"),
        RECOMMENDER_TYPE("recommender_type"),
        RECOMMENDER_CODE("recommender_code"),
        EVENT("event"),
        CATEGORY("category"),
        LABEL("label"),
        VALUE("value"),
        RECOMMENDED_BY("recommended_by"),
        RECOMMENDED_CODE("recommended_code"),
        EMAIL("email"),
        PHONE("phone"),
        EXTERNAL_ID("external_id"),
        LOYALTY_ID("loyalty_id"),
        TELEGRAM_ID("telegram_id"),
        PROPERTIES("properties"),
    }
}

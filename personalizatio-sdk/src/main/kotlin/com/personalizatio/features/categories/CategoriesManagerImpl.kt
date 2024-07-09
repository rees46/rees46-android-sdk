package com.personalizatio.features.categories

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.CategoriesManager
import com.personalizatio.api.managers.NetworkManager
import com.personalizatio.api.responses.categories.categories.GetCategoriesResponse
import com.personalizatio.api.responses.categories.category.GetCategoryResponse
import org.json.JSONArray
import org.json.JSONObject

internal class CategoriesManagerImpl(private val networkManager: NetworkManager) : CategoriesManager {

    override fun getCategories(
        onGetCategories: (GetCategoriesResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getCategories(Params(), onGetCategories, onError)
    }

    override fun getCategories(
        params: Params,
        onGetCategories: (GetCategoriesResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getCategories(params, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONArray) {
                val getCategoriesResponse = Gson().fromJson(response.toString(), GetCategoriesResponse::class.java)
                onGetCategories(getCategoriesResponse)
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getCategories(
        listener: OnApiCallbackListener
    ) {
        getCategories(Params(), listener)
    }

    override fun getCategories(
        params: Params,
        listener: OnApiCallbackListener
    ) {
        networkManager.getSecretAsync(GET_CATEGORIES_INFO_REQUEST, params.build(), listener)
    }

    override fun getCategory(
        categoryId: String,
        onGetCategory: (GetCategoryResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getCategory(categoryId, Params(), onGetCategory, onError)
    }

    override fun getCategory(
        categoryId: String,
        params: Params,
        onGetCategory: (GetCategoryResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getCategory(categoryId, params, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                val getCategoryResponse = Gson().fromJson(response.toString(), GetCategoryResponse::class.java)
                onGetCategory(getCategoryResponse)
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getCategory(
        categoryId: String,
        listener: OnApiCallbackListener
    ) {
        getCategory(categoryId, Params(), listener)
    }

    override fun getCategory(
        categoryId: String,
        params: Params,
        listener: OnApiCallbackListener
    ) {
        val request = "$GET_CATEGORY_REQUEST/$categoryId"
        networkManager.getAsync(request, params.build(), listener)
    }

    companion object {
        const val GET_CATEGORIES_INFO_REQUEST = "products/categories"
        const val GET_CATEGORY_REQUEST = "category"
    }
}

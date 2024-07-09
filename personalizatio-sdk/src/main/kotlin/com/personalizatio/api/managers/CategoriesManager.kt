package com.personalizatio.api.managers

import com.personalizatio.Params
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.responses.categories.categories.GetCategoriesResponse
import com.personalizatio.api.responses.categories.category.GetCategoryResponse

interface CategoriesManager {

    /**
     * Get categories
     *
     * @param listener Callback
     */
    fun getCategories(
        listener: OnApiCallbackListener
    )

    /**
     * Get categories
     *
     * @param params Params
     * @param listener Callback
     */
    fun getCategories(
        params: Params,
        listener: OnApiCallbackListener
    )

    /**
     * Get categories
     *
     * @param onGetCategories Callback for get categories
     * @param onError Callback for error
     */
    fun getCategories(
        onGetCategories: (GetCategoriesResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Get categories
     *
     * @param params Params
     * @param onGetCategories Callback for get categories
     * @param onError Callback for error
     */
    fun getCategories(
        params: Params,
        onGetCategories: (GetCategoriesResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Get category by id
     *
     * @param categoryId External ID
     * @param listener Callback
     */
    fun getCategory(
        categoryId: String,
        listener: OnApiCallbackListener
    )

    /**
     * Get category by id
     *
     * @param categoryId External ID
     * @param params Params
     * @param listener Callback
     */
    fun getCategory(
        categoryId: String,
        params: Params,
        listener: OnApiCallbackListener
    )

    /**
     * Get category by id
     *
     * @param categoryId External ID
     * @param onGetCategory Callback for get category
     * @param onError Callback for error
     */
    fun getCategory(
        categoryId: String,
        onGetCategory: (GetCategoryResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Get category by id
     *
     * @param categoryId External ID
     * @param params Params
     * @param onGetCategory Callback for get category
     * @param onError Callback for error
     */
    fun getCategory(
        categoryId: String,
        params: Params,
        onGetCategory: (GetCategoryResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )
}

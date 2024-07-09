package com.personalizatio.api.managers

import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.responses.search.SearchBlankResponse
import com.personalizatio.api.responses.search.SearchFullResponse
import com.personalizatio.api.responses.search.SearchInstantResponse
import com.personalizatio.api.params.SearchParams

interface SearchManager {

    /**
     * Search full
     *
     * @param query Search phrase
     * @param onGetSearchFull Callback for search full
     * @param onError Callback for error
     */
    fun searchFull(
        query: String,
        onGetSearchFull: (SearchFullResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Search full
     *
     * @param query Search phrase
     * @param listener Callback
     */
    fun searchFull(
        query: String,
        listener: OnApiCallbackListener
    )

    /**
     * Search full
     *
     * @param query Search phrase
     * @param searchParams SearchParams
     * @param onSearchFull Callback for search full
     * @param onError Callback for error
     */
    fun searchFull(
        query: String,
        searchParams: SearchParams,
        onSearchFull: (SearchFullResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Search full
     *
     * @param query Search phrase
     * @param searchParams SearchParams
     * @param listener Callback
     */
    fun searchFull(
        query: String,
        searchParams: SearchParams,
        listener: OnApiCallbackListener
    )

    /**
     * Search instant
     *
     * @param query Search phrase
     * @param onSearchInstant Callback for search instant
     * @param onError Callback for error
     */
    fun searchInstant(
        query: String,
        onSearchInstant: (SearchInstantResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Search instant
     *
     * @param query Search phrase
     * @param listener Callback
     */
    fun searchInstant(
        query: String,
        listener: OnApiCallbackListener
    )

    /**
     * Search instant
     *
     * @param query Search phrase
     * @param searchParams SearchParams
     * @param onSearchInstant Callback for search instant
     * @param onError Callback for error
     */
    fun searchInstant(
        query: String,
        searchParams: SearchParams,
        onSearchInstant: (SearchInstantResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )

    /**
     * Search instant
     *
     * @param query Search phrase
     * @param searchParams SearchParams
     * @param listener Callback
     */
    fun searchInstant(
        query: String,
        searchParams: SearchParams,
        listener: OnApiCallbackListener
    )

    /**
     * Search blank
     *
     * @param listener Callback
     */
    fun searchBlank(
        listener: OnApiCallbackListener
    )

    /**
     * Search blank
     *
     * @param onSearchBlank Callback for search blank
     * @param onError Callback for error
     */
    fun searchBlank(
        onSearchBlank: (SearchBlankResponse) -> Unit,
        onError: (Int, String?) -> Unit = { _: Int, _: String? -> }
    )
}

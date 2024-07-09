package com.personalizatio.features.search

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.params.SearchParams
import com.personalizatio.api.managers.SearchManager
import com.personalizatio.api.responses.search.SearchBlankResponse
import com.personalizatio.api.responses.search.SearchFullResponse
import com.personalizatio.api.responses.search.SearchInstantResponse
import org.json.JSONObject

internal class SearchManagerImpl(private val sdk: SDK) : SearchManager {

    override fun searchFull(
        query: String,
        onGetSearchFull: (SearchFullResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        searchFull(query, SearchParams(), onGetSearchFull, onError)
    }

    override fun searchFull(
        query: String,
        searchParams: SearchParams,
        onSearchFull: (SearchFullResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        searchFull(query, searchParams, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val searchFullResponse = Gson().fromJson(it.toString(), SearchFullResponse::class.java)
                    onSearchFull(searchFullResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun searchFull(query: String, listener: OnApiCallbackListener) {
        searchFull(query, SearchParams(), listener)
    }

    override fun searchFull(
        query: String,
        searchParams: SearchParams,
        listener: OnApiCallbackListener
    ) {
        search(query, SearchParams.TYPE.FULL, searchParams, listener)
    }

    override fun searchInstant(
        query: String,
        onSearchInstant: (SearchInstantResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        searchInstant(query, SearchParams(), onSearchInstant, onError)
    }

    override fun searchInstant(
        query: String,
        searchParams: SearchParams,
        onSearchInstant: (SearchInstantResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        searchInstant(query, searchParams, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val searchInstantResponse = Gson().fromJson(it.toString(), SearchInstantResponse::class.java)
                    onSearchInstant(searchInstantResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun searchInstant(
        query: String,
        listener: OnApiCallbackListener
    ) {
        searchInstant(query, SearchParams(), listener)
    }


    override fun searchInstant(
        query: String,
        searchParams: SearchParams,
        listener: OnApiCallbackListener
    ) {
        search(query, SearchParams.TYPE.INSTANT, searchParams, listener)
    }

    override fun searchBlank(
        onSearchBlank: (SearchBlankResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        searchBlank(object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val searchBlankResponse = Gson().fromJson(it.toString(), SearchBlankResponse::class.java)
                    onSearchBlank(searchBlankResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun searchBlank(listener: OnApiCallbackListener) {
        sdk.getAsync(BLANK_SEARCH_REQUEST, Params().build(), listener)
    }

    private fun search(
        query: String,
        type: SearchParams.TYPE,
        params: SearchParams,
        listener: OnApiCallbackListener
    ) {
        params
            .put(SearchParameter.SEARCH_TYPE, type.value)
            .put(SearchParameter.SEARCH_QUERY, query)
        sdk.getAsync(SEARCH_REQUEST, params.build(), listener)
    }

    companion object {
        private const val SEARCH_REQUEST = "search"
        private const val BLANK_SEARCH_REQUEST = "search/blank"
    }
}

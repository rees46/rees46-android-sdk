package com.personalizatio.features.recommendation

import com.google.gson.Gson
import com.personalizatio.Params
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.RecommendationManager
import com.personalizatio.api.responses.recommendation.GetRecommendationResponse
import com.personalizatio.api.responses.recommendation.GetExtendedRecommendationResponse
import org.json.JSONObject

internal class RecommendationManagerImpl(private val sdk: SDK) : RecommendationManager {

    override fun getRecommendation(
        recommenderCode: String,
        onGetRecommendation: (GetRecommendationResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        val params = getRecommendationParams()
        getRecommendation(recommenderCode, params, onGetRecommendation, onError)
    }

    override fun getRecommendation(recommenderCode: String, listener: OnApiCallbackListener) {
        val params = getRecommendationParams()
        getRecommendation(recommenderCode, params, listener)
    }

    override fun getRecommendation(
        recommenderCode: String,
        params: Params,
        onGetRecommendation: (GetRecommendationResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getRecommendation(recommenderCode, params, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val getRecommendationResponse = Gson().fromJson(it.toString(), GetRecommendationResponse::class.java)
                    onGetRecommendation(getRecommendationResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getExtendedRecommendation(
        recommenderCode: String,
        onGetExtendedRecommendation: (GetExtendedRecommendationResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        val params = getRecommendationParams(extended = true)
        getExtendedRecommendation(recommenderCode, params, onGetExtendedRecommendation, onError)
    }

    override fun getExtendedRecommendation(
        recommenderCode: String,
        params: Params,
        onGetExtendedRecommendation: (GetExtendedRecommendationResponse) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        getExtendedRecommendation(recommenderCode, params, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    val getExtendedRecommendationResponse = Gson().fromJson(it.toString(), GetExtendedRecommendationResponse::class.java)
                    onGetExtendedRecommendation.invoke(getExtendedRecommendationResponse)
                }
            }

            override fun onError(code: Int, msg: String?) {
                onError(code, msg)
            }
        })
    }

    override fun getExtendedRecommendation(recommenderCode: String, listener: OnApiCallbackListener) {
        val params = getRecommendationParams(extended = true)
        getRecommendation(recommenderCode, params, listener)
    }

    override fun getExtendedRecommendation(recommenderCode: String, params: Params, listener: OnApiCallbackListener) {
        params.put(GetRecommendationParameter.EXTENDED, true)
        getRecommendation(recommenderCode, params, listener)
    }

    override fun getRecommendation(recommenderCode: String, params: Params, listener: OnApiCallbackListener) {
        sdk.getAsync("$GET_RECOMMENDATION_REQUEST/$recommenderCode", params.build(), listener)
    }

    private fun getRecommendationParams(extended: Boolean? = null) : Params {
        val params = Params()

        if(extended != null) params.put(GetRecommendationParameter.EXTENDED, extended)

        return params
    }

    companion object {
        const val GET_RECOMMENDATION_REQUEST = "recommend"
    }
}

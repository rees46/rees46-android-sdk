package com.personalizatio.recommendation

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.personalizatio.Params
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.listeners.OnRecommendationListener
import com.personalizatio.api.managers.RecommendationManager
import com.personalizatio.entities.recommended.RecommendedEntity
import com.personalizatio.entities.recommended.RecommendedFullEntity
import org.json.JSONObject

internal class RecommendationManagerImpl(val sdk: SDK) : RecommendationManager {

    override fun recommend(recommenderCode: String, listener: OnApiCallbackListener) {
        recommend(recommenderCode, Params(), listener)
    }

    override fun recommend(recommenderCode: String, params: Params, listener: OnApiCallbackListener) {
        sdk.getAsync("recommend/$recommenderCode", params.build(), listener)
    }

    override fun recommend(recommenderCode: String, listener: OnRecommendationListener) {
        recommend(recommenderCode, Params(), listener)
    }

    override fun recommend(recommenderCode: String, params: Params, listener: OnRecommendationListener) {
        recommend(recommenderCode, params, object : OnApiCallbackListener() {
            override fun onSuccess(response: JSONObject?) {
                response?.let {
                    try {
                        val recommendedEntity = Gson().fromJson(it.toString(), RecommendedEntity::class.java)
                        listener.onGetRecommended(recommendedEntity)
                    }
                    catch (e: JsonSyntaxException) {
                        val recommendedFullEntity = Gson().fromJson(it.toString(), RecommendedFullEntity::class.java)
                        listener.onGetRecommended(recommendedFullEntity)
                    }
                }
            }
        })
    }
}

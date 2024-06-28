package com.personalizatio.api.managers

import com.personalizatio.Params
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.listeners.OnRecommendationListener

interface RecommendationManager {

    /**
     * Request a dynamic block of recommendations
     *
     * @param recommenderCode Recommendation block code
     * @param listener Callback
     */
    fun recommend(recommenderCode: String, listener: OnApiCallbackListener)

    /**
     * Request a dynamic block of recommendations
     *
     * @param recommenderCode Code of the dynamic block of recommendations
     * @param params Parameters for the request
     * @param listener Callback
     */
    fun recommend(recommenderCode: String, params: Params, listener: OnApiCallbackListener)

    /**
     * Request a dynamic block of recommendations
     *
     * @param recommenderCode Recommendation block code
     * @param listener Callback
     */
    fun recommend(recommenderCode: String, listener: OnRecommendationListener)

    /**
     * Request a dynamic block of recommendations
     *
     * @param recommenderCode Recommendation block code
     * @param listener Callback
     */
    fun recommend(recommenderCode: String, params: Params, listener: OnRecommendationListener)
}

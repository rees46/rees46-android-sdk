package com.personalization.api.managers

import com.personalization.api.OnApiCallbackListener
import org.json.JSONObject

interface NetworkManager {

    fun initialize(
        baseUrl: String,
        shopId: String,
        seance: String?,
        segment: String,
        stream: String
    )

    /**
     * Direct query execution
     */
    fun post(method: String, params: JSONObject, listener: OnApiCallbackListener?)

    /**
     * Asynchronous execution of a request if did is not specified and initialization has not been completed
     */
    fun postAsync(method: String, params: JSONObject, listener: OnApiCallbackListener? = null)

    /**
     * Direct query execution
     */
    fun get(method: String, params: JSONObject, listener: OnApiCallbackListener?)

    /**
     * Asynchronous execution of a request if did is not specified and initialization has not been completed
     */
    fun getAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?)

    fun executeQueueTasks() {}

    fun addTaskToQueue(thread: Thread)
}

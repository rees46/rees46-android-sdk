package com.personalizatio.network

import android.net.Uri
import com.personalizatio.SDK
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.api.managers.NetworkManager
import com.personalizatio.notifications.Source
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Collections

internal class NetworkManagerImpl(
    private val sdk: SDK,
    private val baseUrl: String,
    private val shopId: String,
    private val shopSecretKey: String,
    private val seance: String?,
    private val segment: String,
    private val stream: String,
    private val source: Source
): NetworkManager {

    private val queue: MutableList<Thread> = Collections.synchronizedList(ArrayList())

    override fun post(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        send(NetworkMethod.POST(method), params, listener)
    }

    override fun postAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendAsync { send(NetworkMethod.POST(method), params, listener) }
    }

    override fun postSecretAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendAsync { sendSecret(NetworkMethod.POST(method), params, listener) }
    }

    override fun get(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        send(NetworkMethod.GET(method), params, listener)
    }

    override fun getAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendAsync { send(NetworkMethod.GET(method), params, listener) }
    }

    override fun getSecretAsync(method: String, params: JSONObject, listener: OnApiCallbackListener?) {
        sendAsync { sendSecret(NetworkMethod.GET(method), params, listener) }
    }

    private fun send(networkMethod: NetworkMethod, params: JSONObject, listener: OnApiCallbackListener?) {
        sdk.updateSidActivity()

        sendMethod(networkMethod, params, listener)
    }

    private fun sendAsync(sendFunction: () -> Unit) {
        val thread = Thread(sendFunction)
        if (sdk.registerManager.did != null && sdk.initialized) {
            thread.start()
        } else {
            queue.add(thread)
        }
    }

    private fun sendSecret(networkMethod: NetworkMethod, params: JSONObject, listener: OnApiCallbackListener?) {
        sdk.updateSidActivity()

        sendSecretMethod(networkMethod, params, listener)
    }

    override fun executeQueueTasks() {
        for (thread in queue) {
            thread.start()
        }
        queue.clear()
    }

    override fun addTaskToQueue(thread: Thread) {
        queue.add(thread)
    }

    private fun sendSecretMethod(networkMethod: NetworkMethod, params: JSONObject, listener: OnApiCallbackListener?) {
        params.put(SHOP_SECRET_PARAMS_FIELD, shopSecretKey)
        sendMethod(networkMethod, params, listener)
    }

    private fun sendMethod(networkMethod: NetworkMethod, params: JSONObject, listener: OnApiCallbackListener?) {
        try {
            params.put(SHOP_ID_PARAMS_FIELD, shopId)
            if (sdk.registerManager.did != null) {
                params.put(DID_PARAMS_FIELD, sdk.registerManager.did)
            }
            if (seance != null) {
                params.put(SEANCE_PARAMS_FIELD, seance)
                params.put(SID_PARAMS_FIELD, seance)
            }
            params.put(SEGMENT_PARAMS_FIELD, segment)
            params.put(STREAM_PARAMS_FIELD, stream)

            val sourceJson = source.getJsonObject(sourceTimeDuration)
            if (sourceJson != null) {
                params.put(SOURCE_PARAMS_FIELD, sourceJson)
            }

            executeMethod(networkMethod, params, listener)
        } catch (e: JSONException) {
            SDK.error(e.message, e)
        }
    }

    private fun executeMethod(networkMethod: NetworkMethod, params: JSONObject, listener: OnApiCallbackListener?) {
        val thread = Thread {
            try {
                val buildUri = build(networkMethod, params)
                val url = getUrl(networkMethod, buildUri)
                val connection = getConnection(networkMethod, url, params)

                connection.connect()

                if (networkMethod is NetworkMethod.POST) {
                    SDK.debug(connection.responseCode.toString() + ": " + networkMethod.type + " " + url + " with body: " + params)
                } else {
                    SDK.debug(connection.responseCode.toString() + ": " + networkMethod.type + " " + buildUri.toString())
                }

                if (listener != null && connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = JSONTokener(readStream(connection.inputStream)).nextValue()
                    if (json is JSONObject) {
                        listener.onSuccess(json)
                    } else if (json is JSONArray) {
                        listener.onSuccess(json)
                    }
                }

                if (connection.responseCode >= 400) {
                    val error = readStream(connection.errorStream)
                    SDK.error(error)
                    listener?.onError(connection.responseCode, error)
                }

                connection.disconnect()
            } catch (e: ConnectException) {
                SDK.error(e.message)
                listener?.onError(504, e.message)
            } catch (e: Exception) {
                SDK.error(e.message, e)
                listener?.onError(-1, e.message)
            }
        }

        thread.start()
    }

    private fun build(networkMethod: NetworkMethod, params: JSONObject): Uri {
        val builder = Uri.parse(baseUrl + networkMethod.method).buildUpon()

        val it = params.keys()
        while (it.hasNext()) {
            val key = it.next()
            builder.appendQueryParameter(key, params.getString(key))
        }

        return builder.build()
    }

    private fun getUrl(networkMethod: NetworkMethod, buildUri: Uri) : URL {
        return if (networkMethod is NetworkMethod.POST) {
            URL(baseUrl + networkMethod.method)
        } else {
            URL(buildUri.toString())
        }
    }

    private fun getConnection(networkMethod: NetworkMethod, url: URL, params: JSONObject) : HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", SDK.userAgent())
        connection.requestMethod = networkMethod.type
        connection.connectTimeout = 5000

        if (networkMethod is NetworkMethod.POST) {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            val os = BufferedWriter(OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8))
            os.write(params.toString())
            os.flush()
            os.close()
        }

        return connection
    }

    private fun readStream(inputStream: InputStream): String {
        var reader: BufferedReader? = null
        val response = StringBuffer()
        try {
            reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                response.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return response.toString()
    }

    companion object {
        private var sourceTimeDuration = 172800 * 1000 // 2 days

        private const val SHOP_ID_PARAMS_FIELD = "shop_id"
        private const val SHOP_SECRET_PARAMS_FIELD = "shop_secret"
        private const val DID_PARAMS_FIELD = "did"
        private const val SEANCE_PARAMS_FIELD = "seance"
        private const val SID_PARAMS_FIELD = "sid"
        private const val SEGMENT_PARAMS_FIELD = "segment"
        private const val STREAM_PARAMS_FIELD = "stream"
        private const val SOURCE_PARAMS_FIELD = "source"
    }
}

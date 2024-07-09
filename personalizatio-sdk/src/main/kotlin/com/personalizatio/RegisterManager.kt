package com.personalizatio

import android.annotation.SuppressLint
import android.provider.Settings
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.personalizatio.SDK.Companion.TAG
import com.personalizatio.SDK.Companion.debug
import com.personalizatio.api.OnApiCallbackListener
import com.personalizatio.utils.PreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Date
import java.util.TimeZone

class RegisterManager(val sdk: SDK) {
    private var autoSendPushToken: Boolean = false

    internal var did: String? = null
        private set

    /**
     * Get did from properties or generate a new did
     */
    @SuppressLint("HardwareIds")
    internal fun initialize(autoSendPushToken: Boolean) {
        this.autoSendPushToken = autoSendPushToken

        if(did != null) return

        did = getDid()

        if (did == null) {
            did = Settings.Secure.getString(sdk.context.contentResolver, Settings.Secure.ANDROID_ID)

            init()
        }
        else {
            initializeSdk(null)
        }
    }

    /**
     * Init device token
     */
    private fun initToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String> ->
            if (!task.isSuccessful) {
                SDK.error("getInstanceId failed", task.exception)
                return@addOnCompleteListener
            }
            if (task.result == null) {
                SDK.error("Firebase result is null")
                return@addOnCompleteListener
            }

            val token = task.result
            debug("token: $token")

            val tokenField = getToken()

            val currentDate = Date()

            if (tokenField == null
                || tokenField != token
                || (currentDate.time - getLastPushTokenMilliseconds()) >= ONE_WEEK_MILLISECONDS) {

                sdk.setPushTokenNotification(token, object : OnApiCallbackListener() {
                    override fun onSuccess(response: JSONObject?) {
                        saveLastPushTokenDate(currentDate)
                        saveToken(token)
                    }
                })
            }
        }
    }

    private fun init() {
        if (isTestDevice) {
            Log.w(TAG, "Disable working Google Play Pre-Launch report devices")
            return
        }

        try {
            val params = JSONObject()
            params.put("tz", (TimeZone.getDefault().rawOffset / 3600000.0).toInt().toString())
            sdk.networkManager.get("init", params, object : OnApiCallbackListener() {
                @Volatile
                private var attempt = 0

                override fun onSuccess(response: JSONObject?) {
                    if(response == null) {
                        SDK.error("Init response is not correct.")
                        return
                    }

                    did = response.optString("did")
                    if(did.isNullOrEmpty()) {
                        SDK.error("Init response does not contain the correct did field.")
                        return
                    }

                    saveDid()

                    val seance = response.optString("seance")
                    if(seance.isNullOrEmpty()) {
                        SDK.error("Init response does not contain the correct seance field.")
                        return
                    }

                    initializeSdk(seance)
                }

                override fun onError(code: Int, msg: String?) {
                    if (code >= 500 || code <= 0) {
                        Log.e(TAG, "code: $code, $msg")
                        if (attempt < 5) {
                            attempt++
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000L * attempt)
                            init()
                        }
                    }
                    else {
                        SDK.error("Init error: code: $code, $msg")
                    }
                }
            })
        } catch (e: Exception) {
            SDK.error(e.message, e)
        }
    }

    private fun initializeSdk(sid: String?) {
        sdk.initialized(sid)

        initToken()
    }

    private val isTestDevice: Boolean
        get() = IS_TEST_DEVICE_FIELD == Settings.System.getString(sdk.context.contentResolver, FIREBASE_TEST_LAB)

    private fun getDid() : String? {
        return sdk.prefs().getString(DID_PREFS_KEY, null)
    }

    private fun saveDid() {
        did?.let { did ->
            PreferencesUtils.saveField(sdk.prefs(), DID_PREFS_KEY, did)
        }
    }

    private fun getToken() : String? {
        return sdk.prefs().getString(TOKEN_PREFS_KEY, null)
    }

    private fun saveToken(token: String) {
        PreferencesUtils.saveField(sdk.prefs(), TOKEN_PREFS_KEY, token)
    }

    private fun getLastPushTokenMilliseconds() : Long {
        return sdk.prefs().getLong(LAST_PUSH_TOKEN_DATE_PREFS_KEY, 0)
    }

    private fun saveLastPushTokenDate(date: Date) {
        PreferencesUtils.saveField(sdk.prefs(), LAST_PUSH_TOKEN_DATE_PREFS_KEY, date.time)
    }

    companion object {
        private const val DID_PREFS_KEY = "did"
        private const val TOKEN_PREFS_KEY = "token"
        private const val LAST_PUSH_TOKEN_DATE_PREFS_KEY = "last_push_token_date"
        private const val IS_TEST_DEVICE_FIELD = "true"
        private const val FIREBASE_TEST_LAB = "firebase.test.lab"

        private const val ONE_WEEK_MILLISECONDS = 7 * 24 * 60 * 60
    }
}
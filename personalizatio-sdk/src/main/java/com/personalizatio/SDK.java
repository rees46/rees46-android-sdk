package com.personalizatio;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.RemoteMessage;
import com.personalizatio.Params.InternalParameter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Created by Sergey Odintsov
 *
 * @author nixx.dj@gmail.com
 */
public class SDK {

	public static String TAG;
	public static String NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
	public static String NOTIFICATION_ID = "NOTIFICATION_ID";
	private final String PREFERENCES_KEY;
	private static final String DID_FIELD = "did";
	private static final String TOKEN_FIELD = "token";

	private Context context;
	private String shop_id;
	private String did;
	private String seance;
	private OnMessageListener onMessageListener;
	@SuppressLint("StaticFieldLeak")
	protected static SDK instance;

	private volatile boolean initialized = false;
	private volatile int attempt = 0;
	private ArrayList<Thread> queue = new ArrayList<>();
	private Search search;
	private final String segment;
	private final String stream;

	public static void initialize(Context context, String shop_id) {
		throw new IllegalStateException("You need make static initialize method!");
	}
	public static void initialize(Context context, String shop_id, String stream) {
		throw new IllegalStateException("You need make static initialize method!");
	}

	/**
	 * Update profile data
	 * https://reference.api.rees46.com/#save-profile-settings
	 * @param data profile data
	 */
	public static void profile(HashMap<String, String> data) {
		instance.sendAsync("profile/set", data);
	}

	/**
	 * @param data from data notification
	 */
	public static void notificationReceived(Map<String, String> data) {
		HashMap<String, String> params = new HashMap<>();
		if( data.get("type") != null ) {
			params.put("type", data.get("type"));
		}
		if( data.get("id") != null ) {
			params.put("code", data.get("id"));
		}
		if( params.size() > 0 ) {
			instance.sendAsync("web_push_subscriptions/received", params);
		}
	}

	/**
	 * @param extras from data notification
	 */
	public static void notificationClicked(Bundle extras) {
		if( extras.getString(NOTIFICATION_TYPE, null) != null && extras.getString(NOTIFICATION_ID, null) != null ) {
			HashMap<String, String> params = new HashMap<>();
			params.put("type", extras.getString(NOTIFICATION_TYPE));
			params.put("code", extras.getString(NOTIFICATION_ID));
			instance.sendAsync("web_push_subscriptions/clicked", params);
		}
	}

	/**
	 * @param listener Event on message receive
	 */
	public static void setOnMessageListener(OnMessageListener listener) {
		instance.onMessageListener = listener;
	}

	/**
	 * Быстрый поиск
	 *
	 * @param query    Поисковая фраза
	 * @param type     Тип поиска
	 * @param listener Колбек
	 */
	public static void search(String query, SearchParams.TYPE type, Api.OnApiCallbackListener listener) {
		search(query, type, new SearchParams(), listener);
	}

	/**
	 * Быстрый поиск
	 *
	 * @param query    Поисковая фраза
	 * @param type     Тип поиска
	 * @param params   Дополнительные параметры к запросу
	 * @param listener v
	 */
	public static void search(String query, SearchParams.TYPE type, SearchParams params, Api.OnApiCallbackListener listener) {
		if( instance.search != null ) {
			params
					.put(InternalParameter.SEARCH_TYPE, type.getValue())
					.put(InternalParameter.SEARCH_QUERY, query);
			instance.getAsync("search", params.build(), listener);
		} else {
			SDK.warn("Search not initialized");
		}
	}

	/**
	 * Пустой поиск
	 *
	 * @param listener v
	 */
	public static void search_blank(Api.OnApiCallbackListener listener) {
		if( instance.search != null ) {
			if( instance.search.blank == null ) {
				instance.getAsync("search/blank", (new Params()).build(), new Api.OnApiCallbackListener() {
					@Override
					public void onSuccess(JSONObject response) {
						instance.search.blank = response;
						listener.onSuccess(response);
					}

					@Override
					public void onError(int code, String msg) {
						listener.onError(code, msg);
					}
				});
			} else {
				listener.onSuccess(instance.search.blank);
			}
		} else {
			SDK.warn("Search not initialized");
		}
	}

	/**
	 * Запрос динамического блока рекомендаций
	 *
	 * @param recommender_code Код блока рекомендаций
	 * @param listener         Колбек
	 */
	public static void recommend(String recommender_code, Api.OnApiCallbackListener listener) {
		recommend(recommender_code, new Params(), listener);
	}

	/**
	 * Запрос динамического блока рекомендаций
	 *
	 * @param code     Код динамического блока рекомендаций
	 * @param params   Параметры для запроса
	 * @param listener Колбек
	 */
	public static void recommend(String code, Params params, Api.OnApiCallbackListener listener) {
		instance.getAsync("recommend/" + code, params.build(), listener);
	}

	/**
	 * Трекинг события
	 *
	 * @param event   Тип события
	 * @param item_id ID товара
	 */
	public static void track(Params.TrackEvent event, String item_id) {
		track(event, (new Params()).put(new Params.Item(item_id)), null);
	}

	/**
	 * Трекинг события
	 *
	 * @param event  Тип события
	 * @param params Параметры
	 */
	public static void track(Params.TrackEvent event, @NonNull Params params) {
		track(event, params, null);
	}

	/**
	 * Трекинг события
	 *
	 * @param event    Тип события
	 * @param params   Параметры для запроса
	 * @param listener Колбек
	 */
	public static void track(Params.TrackEvent event, @NonNull Params params, @Nullable Api.OnApiCallbackListener listener) {
		params.put(InternalParameter.EVENT, event.value);
		instance.sendAsync("push", params.build(), listener);
	}

	/**
	 * Трекинг кастомных событий
	 * @param event Ключ события
	 */
	public static void track(String event) {
		track(event, null, null, null, null);
	}

	/**
	 * Трекинг кастомных событий
	 * @param event Ключ события
	 * @param category Event category
	 * @param label    Event label
	 * @param value    Event value
	 */
	public static void track(String event, @Nullable String category, @Nullable String label, @Nullable Integer value) {
		track(event, category, label, value, null);
	}

	/**
	 * Трекинг кастомных событий
	 * @param event    Ключ события
	 * @param category Event category
	 * @param label    Event label
	 * @param value    Event value
	 * @param listener Колбек
	 */
	public static void track(String event, @Nullable String category, @Nullable String label, @Nullable Integer value, @Nullable Api.OnApiCallbackListener listener) {
		Params params = new Params();
		params.put(InternalParameter.EVENT, event);
		if( category != null ) {
			params.put(InternalParameter.CATEGORY, category);
		}
		if( label != null ) {
			params.put(InternalParameter.LABEL, label);
		}
		if( value != null ) {
			params.put(InternalParameter.VALUE, value);
		}
		instance.sendAsync("push/custom", params.build(), listener);
	}

	/**
	 * Возвращает идентификатор устройства
	 * @return String
	 */
	public static String getDid() {
		return instance.did;
	}

	/**
	 * Возвращает идентификатор сессии
	 */
	public static void getSid(Consumer<String> listener) {
		Thread thread = new Thread(() -> listener.accept(instance.seance));
		if( instance.initialized ) {
			thread.start();
		} else {
			instance.queue.add(thread);
		}
	}

	//----------Private--------------->

	/**
	 * @param message Сообщение
	 */
	static void debug(String message) {
		Log.d(instance.TAG, message);
	}

	/**
	 * @param message Сообщение
	 */
	static void warn(String message) {
		Log.w(instance.TAG, message);
	}

	/**
	 * @param message Сообщение об ошибке
	 */
	static void error(String message) {
		Log.e(instance.TAG, message);
	}

	/**
	 * @param message Сообщение об ошибке
	 */
	static void error(String message, Throwable e) {
		Log.e(instance.TAG, message, e);
	}

	/**
	 * @param shop_id Shop key
	 */
	protected SDK(Context context, String shop_id, String api_url, String tag, String prefs_key, String stream) {
		this.shop_id = shop_id;
		this.context = context;
		this.stream = stream;
		TAG = tag;
		PREFERENCES_KEY = prefs_key;
		Api.initialize(api_url);

		//Инициализируем сегмент
		segment = prefs().getString(PREFERENCES_KEY + ".segment", new String[]{"A", "B"}[(int) Math.round(Math.random())]);

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
			// Create channel to show notifications.
			String channelId = context.getString(R.string.notification_channel_id);
			String channelName = context.getString(R.string.notification_channel_name);
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			if( notificationManager != null ) {
				notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW));
			} else {
				SDK.error("NotificationManager not allowed");
			}
		}
		did();
	}

	/**
	 * @return preferences
	 */
	private SharedPreferences prefs() {
		return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
	}

	/**
	 * Get did from properties or generate a new did
	 */
	@SuppressLint("HardwareIds")
	private void did() {
		if( did == null ) {
			SharedPreferences preferences = prefs();
			did = preferences.getString(DID_FIELD, null);
			if( did == null ) {
				//get unique device id
				did = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
			}
			init();
			queue.add(new Thread(this::getToken));
		}
	}

	private boolean isTestDevice() {
		return "true".equals(Settings.System.getString(context.getContentResolver(), "firebase.test.lab"));
	}

	/**
	 * Connect to init script
	 */
	private void init() {

		//Disable working Google Play Pre-Launch report devices
		if( isTestDevice() ) {
			Log.w(TAG, "Disable working Google Play Pre-Launch report devices");
			return;
		}

		HashMap<String, String> params = new HashMap<>();
		params.put("tz", String.valueOf((int) (TimeZone.getDefault().getRawOffset() / 3600000.0)));
		send("get", "init", params, new Api.OnApiCallbackListener() {
			@Override
			public void onSuccess(JSONObject response) {
				try {
					initialized = true;
					seance = response.getString("seance");
					SDK.debug("Device ID: " + did + ", seance: " + seance);

					//Seach
					try {
						JSONObject s = response.getJSONObject("search");
						if( s.getBoolean("enabled") ) {
							search = new Search(s);
						} else {
							SDK.debug("Search disabled");
						}
					} catch(JSONException e) {
						SDK.debug(e.getMessage());
					}

					// Сохраняем данные в память
					SharedPreferences.Editor edit = prefs().edit();
					if( !response.getString("did").contains(did) ) {
						edit.putString(DID_FIELD, did);
					}
					edit.apply();

					// Выполняем таски из очереди
					for( Thread thread : queue ) {
						thread.start();
					}
					queue.clear();
				} catch(JSONException e) {
					SDK.error(e.getMessage(), e);
				}
			}

			@Override
			public void onError(int code, String msg) {
				if( code >= 500 ) {
					if( attempt < 5 ) {
						attempt++;
						Handler handler = new Handler(Looper.getMainLooper());
						handler.postDelayed(() -> init(), 1000 * attempt);
					}
				} else {
					SDK.error("Init error: " + msg);
				}
			}
		});
	}

	/**
	 * Get device token
	 */
	private void getToken() {
		FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
			if( !task.isSuccessful() ) {
				SDK.error("getInstanceId failed", task.getException());
				return;
			}
			if( task.getResult() == null ) {
				SDK.error("Firebase result is null");
				return;
			}

			// Get new Instance ID token
			final String token = task.getResult().getToken();
			SDK.debug("token: " + token);

			//Check send token
			if( prefs().getString(TOKEN_FIELD, null) == null || !Objects.equals(prefs().getString(TOKEN_FIELD, null), token) ) {

				//Send token
				HashMap<String, String> params = new HashMap<>();
				params.put("platform", "android");
				params.put("token", token);
				send("post", "mobile_push_tokens", params, new Api.OnApiCallbackListener() {
					@Override
					public void onSuccess(JSONObject msg) {
						SharedPreferences.Editor edit = prefs().edit();
						edit.putString(TOKEN_FIELD, token);
						edit.apply();
					}
				});
			}
		});
	}

	/**
	 * Прямое выполенение запроса
	 */
	private void send(String request_type, String method, Map<String, String> params, @Nullable Api.OnApiCallbackListener listener) {
		params.put("shop_id", shop_id);
		if( did != null ) {
			params.put("did", did);
		}
		if( seance != null ) {
			params.put("seance", seance);
			params.put("sid", seance);
		}
		params.put("segment", segment);
		params.put("stream", stream);
		Api.send(request_type, method, params, listener);
	}

	private void sendAsync(final String method, final HashMap<String, String> params) {
		sendAsync(method, params, null);
	}

	/**
	 * Асинхронное выполенение запросе, если did не указан и не выполнена инициализация
	 */
	void sendAsync(final String method, final HashMap<String, String> params, final @Nullable Api.OnApiCallbackListener listener) {
		Thread thread = new Thread(() -> send("post", method, params, listener));
		if( did != null && initialized ) {
			thread.start();
		} else {
			queue.add(thread);
		}
	}

	/**
	 * Асинхронное выполенение запросе, если did не указан и не выполнена инициализация
	 */
	void getAsync(final String method, final HashMap<String, String> params, final @Nullable Api.OnApiCallbackListener listener) {
		Thread thread = new Thread(() -> send("get", method, params, listener));
		if( did != null && initialized ) {
			thread.start();
		} else {
			queue.add(thread);
		}
	}

	//-------------Методы------------>

	public static void onMessage(RemoteMessage remoteMessage) {
		notificationReceived(remoteMessage.getData());
		if( instance.onMessageListener != null ) {
			instance.onMessageListener.onMessage(remoteMessage.getData());
		}
	}
}

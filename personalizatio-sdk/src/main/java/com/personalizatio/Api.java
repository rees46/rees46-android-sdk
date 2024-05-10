package com.personalizatio;

import android.net.Uri;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Created by Sergey Odintsov
 *
 * @author nixx.dj@gmail.com
 */
public final class Api {
	private static Api instance;
	private final String url;

	public abstract static class OnApiCallbackListener {
		public void onSuccess(JSONObject response) {}
		public void onSuccess(JSONArray response) {}
		public void onError(int code, String msg) {}
	}

	private Api(String url) {
		this.url = url;
	}

	static void initialize(String url) {
		instance = new Api(url);
	}

	/**
	 * @param method   api
	 * @param params   request
	 * @param listener callback
	 */
	public static void send(final String request_type, final String method, final JSONObject params, @Nullable final OnApiCallbackListener listener) {
		Thread thread = new Thread(() -> {
			try {
				Uri.Builder builder = Uri.parse(instance.url + method).buildUpon();
				for( Iterator<String> it = params.keys(); it.hasNext(); ) {
					String key = it.next();
					builder.appendQueryParameter(key, params.getString(key));
				}

				URL url;
				if (request_type.equalsIgnoreCase("POST")) {
					url = new URL(instance.url + method);
				} else {
					url = new URL(builder.build().toString());
				}

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", SDK.userAgent());
				conn.setRequestMethod(request_type.toUpperCase());
				conn.setConnectTimeout(5000);

				if (request_type.equalsIgnoreCase("POST")) {
					conn.setRequestProperty("Content-Type", "application/json");
					conn.setDoOutput(true);
					conn.setDoInput(true);
					BufferedWriter os = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
					os.write(params.toString());
					os.flush();
					os.close();
				}

				conn.connect();

				if( request_type.equalsIgnoreCase("POST") ) {
					SDK.debug(conn.getResponseCode() + ": " + request_type.toUpperCase() + " " + url + " with body: " + params);
				} else {
					SDK.debug(conn.getResponseCode() + ": " + request_type.toUpperCase() + " " + builder.build().toString());
				}

				if( listener != null && conn.getResponseCode() == HttpURLConnection.HTTP_OK ) {
					Object json = new JSONTokener(readStream(conn.getInputStream())).nextValue();
					if( json instanceof JSONObject ) {
						listener.onSuccess((JSONObject) json);
					} else if( json instanceof JSONArray ) {
						listener.onSuccess((JSONArray) json);
					}
				}

				if( conn.getResponseCode() >= 400 ) {
					String error = readStream(conn.getErrorStream());
					SDK.error(error);
					if( listener != null ) {
						listener.onError(conn.getResponseCode(), error);
					}
				}

				conn.disconnect();
			} catch(ConnectException e) {
				SDK.error(e.getMessage());
				if( listener != null ) {
					listener.onError(504, e.getMessage());
				}
			} catch(Exception e) {
				SDK.error(e.getMessage(), e);
				if( listener != null ) {
					listener.onError(-1, e.getMessage());
				}
			}
		});

		thread.start();
	}

	private static String readStream(InputStream in) {
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while((line = reader.readLine()) != null) {
				response.append(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return response.toString();
	}
}

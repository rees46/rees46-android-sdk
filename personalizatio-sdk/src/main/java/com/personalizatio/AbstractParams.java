package com.personalizatio;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

abstract class AbstractParams {
	protected final JSONObject params = new JSONObject();

	public interface ParamInterface {
		String getValue();
	}

	public AbstractParams put(ParamInterface param, int value) {
		return put(param, String.valueOf(value));
	}

	/**
	 * Вставка строковых параметров
	 */
	public AbstractParams put(ParamInterface param, String value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return this;
	}

	public AbstractParams put(ParamInterface param, boolean value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return this;
	}

	public AbstractParams put(ParamInterface param, JSONObject value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return this;
	}

	/**
	 * Вставка параметров с массивом
	 */
	public AbstractParams put(ParamInterface param, String[] value) {
		return put(param, TextUtils.join(",", value));
	}

	JSONObject build() {
		return params;
	}
}

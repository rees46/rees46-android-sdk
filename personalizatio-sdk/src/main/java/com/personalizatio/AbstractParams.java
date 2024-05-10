package com.personalizatio;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

abstract class AbstractParams<P extends AbstractParams<P>> {
	protected final JSONObject params = new JSONObject();

	public interface ParamInterface {
		String getValue();
	}

	/**
	 * Вставка строковых параметров
	 */
	public P put(P.ParamInterface param, String value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return (P) this;
	}

	public P put(P.ParamInterface param, int value) {
		return put(param, String.valueOf(value));
	}

	public P put(P.ParamInterface param, boolean value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return (P) this;
	}

	public P put(P.ParamInterface param, JSONObject value) {
		try {
			params.put(param.getValue(), value);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return (P) this;
	}

	/**
	 * Вставка параметров с массивом
	 */
	public P put(P.ParamInterface param, String[] value) {
		try {
			params.put(param.getValue(), TextUtils.join(",", value));
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return (P) this;
	}

	JSONObject build() {
		return params;
	}
}

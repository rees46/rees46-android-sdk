package com.personalizatio;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class Params extends AbstractParams<Params> {

	/**
	 * Основные параметры
	 */
	public enum Parameter implements ParamInterface {
		LIMIT("limit"),
		ITEM("item"),
		PRICE("price"),
		LOCATIONS("locations"),
		/**
		 * Available sizes: 120, 140, 160, 180, 200, 220, 310, 520
		 */
		IMAGE_SIZE("resize_image"),
		BRANDS("brands"),
		EXCLUDE_BRANDS("exclude_brands"),
		CATEGORIES("categories"),
		DISCOUNT("discount"),
		FULL_CART("full_cart"),
		ORDER_ID("order_id"),
		ORDER_PRICE("order_price"),
		DELIVERY_ADDRESS("delivery_address"),
		DELIVERY_TYPE("delivery_type"),
		PROMOCODE("promocode"),
		PAYMENT_TYPE("payment_type"),
		TAX_FREE("tax_free"),
		CATEGORY_ID("category_id"),
		CATEGORY("category"),
		SEARCH_QUERY("search_query"),
		EXTENDED("extended"),
		;

		private String value;

		Parameter(String v) {
			value = v;
		}

		@Override
		public String getValue() {
			return value;
		}
	}

	/**
	 * Типы рекомендаций
	 */
	 public static final class RecommendedBy {
		public enum TYPE {
			RECOMMENDATION("dynamic"),
			TRIGGER("chain"),
			BULK("bulk"),
			TRANSACTIONAL("transactional"),
			INSTANT_SEARCH("instant_search"),
			FULL_SEARCH("full_search"),
			STORIES("stories"),
			;

			private String value;

			TYPE(String v) {
				value = v;
			}

			public String getValue() {
				return value;
			}
		}

		String type;
		String code;

		public RecommendedBy(TYPE type) {
			this.type = type.getValue();
		}

		public RecommendedBy(TYPE type, String code) {
			this(type);
			this.code = code;
		}
	}

	/**
	 * Товар
	 */
	public static final class Item {
		public enum COLUMN {
			ID("id"),
			AMOUNT("amount"),
			PRICE("price"),
			FASHION_SIZE("fashion_size"),
			;

			String value;

			COLUMN(String v) {
				value = v;
			}
		}

		final HashMap<String, String> columns = new HashMap<>();

		public Item(@NonNull String id) {
			columns.put(COLUMN.ID.value, id);
		}

		public Item set(COLUMN column, @NonNull String value) {
			columns.put(column.value, value);
			return this;
		}

		public Item set(COLUMN column, int value) {
			return set(column, String.valueOf(value));
		}
		public Item set(COLUMN column, double value) {
			return set(column, String.valueOf(value));
		}
		public Item set(COLUMN column, boolean value) {
			return set(column, value ? "1" : "0");
		}
	}

	public enum TrackEvent {
		VIEW("view"),
		CATEGORY("category"),
		CART("cart"),
		REMOVE_FROM_CART("remove_from_cart"),
		PURCHASE("purchase"),
		SEARCH("search"),
		WISH("wish"),
		REMOVE_FROM_WISH("remove_wish"),
		;
		String value;
		TrackEvent(String v) {
			value = v;
		}
	}

	/**
	 * Вставка параметров рекомендаций
	 */
	public Params put(RecommendedBy recommended_by) {
		try {
			params.put(InternalParameter.RECOMMENDED_BY.getValue(), recommended_by.type);
			if( recommended_by.code != null ) {
				params.put(InternalParameter.RECOMMENDED_CODE.getValue(), recommended_by.code);
			}
		} catch( JSONException e ) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}
		return this;
	}

	/**
	 * Вставка товара
	 */
	public Params put(Item item) {
		try {
			JSONArray array;
			if(params.has("items")) {
				array = params.getJSONArray("items");
			} else {
				array = new JSONArray();
				params.put("items", array);
			}
			JSONObject object = new JSONObject();
			for( Map.Entry<String, String> entry : item.columns.entrySet() ) {
				object.put(entry.getKey(), entry.getValue());
			}
			array.put(object);
		} catch(JSONException e) {
			Log.e(SDK.TAG, e.getMessage(), e);
		}

		return this;
	}

	//---------------Private---------->

	enum InternalParameter implements ParamInterface {
		SEARCH_TYPE("type"),
		SEARCH_QUERY("search_query"),
		RECOMMENDER_TYPE("recommender_type"),
		RECOMMENDER_CODE("recommender_code"),
		EVENT("event"),
		CATEGORY("category"),
		LABEL("label"),
		VALUE("value"),
		RECOMMENDED_BY("recommended_by"),
		RECOMMENDED_CODE("recommended_code"),
		EMAIL("email"),
		PHONE("phone"),
		PROPERTIES("properties"),
		;

		String value;

		InternalParameter(String v) {
			value = v;
		}

		@Override
		public String getValue() {
			return value;
		}
	}

}

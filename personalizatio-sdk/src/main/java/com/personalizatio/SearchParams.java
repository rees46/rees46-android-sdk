package com.personalizatio;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class SearchParams extends AbstractParams {

	/**
   * <a href="https://reference.api.rees46.com/#full-search">...</a>
   */
	public enum Parameter implements AbstractParams.ParamInterface {
		PAGE("page"),
		LIMIT("limit"),
		CATEGORY_LIMIT("category_limit"),
		CATEGORIES("categories"),
		EXTENDED("extended"),
		SORT_BY("sort_by"),
		SORT_DIR("sort_dir"),
		LOCATIONS("locations"),
		BRANDS("brands"),
		FILTERS("filters"),
		PRICE_MIN("price_min"),
		PRICE_MAX("price_max"),
		COLORS("colors"),
		FASHION_SIZES("fashion_sizes"),
		EXCLUDE("exclude"),
		//params.put(SearchParams.Parameter.NO_CLARIFICATION, true);
		NO_CLARIFICATION("no_clarification"),
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
	 * Типы поиска
	 */
	public enum TYPE {
		INSTANT("instant_search"),
		FULL("full_search");
		private String value;

		TYPE(String v) {
			value = v;
		}

		public String getValue() {
			return value;
		}
	}


	/**
	 * Структура для фильтров
	 */
	public final static class SearchFilters {
		private final HashMap<String, String[]> filters = new HashMap<>();

		public void put(String key, String[] values) {
			filters.put(key, values);
		}

		public @NotNull String toString() {
			JSONObject json = new JSONObject();
			for( Map.Entry<String, String[]> entry : filters.entrySet() ) {
				String key = entry.getKey();
				try {
					json.put(key, new JSONArray(entry.getValue()));
				} catch(JSONException e) {
					SDK.warn(e.getMessage());
				}
			}
			return json.toString();
		}
	}

	public SearchParams put(Parameter param, SearchFilters value) {
		super.put(param, value.toString());
		return this;
	}
}

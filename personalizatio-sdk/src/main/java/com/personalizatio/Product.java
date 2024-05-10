package com.personalizatio;

import org.json.JSONException;
import org.json.JSONObject;

public final class Product {
	public final String id;
	public final String name;
	public final String brand;
	public final String image;
	public final String oldprice;
	public final String price;
	public final String discount;
	public final String url;
	public final String deeplink;
	public final String promocode;
	public final String price_with_promocode;
	public final String discount_percent;

	public Product(JSONObject product) throws JSONException {
		id = product.getString("id");
		name = product.getString("name");
		image = product.getString("image_url");
		if( product.has("oldprice") && product.getDouble("oldprice") > 0 ) {
			oldprice = product.getString("oldprice_formatted");
		} else {
			oldprice = null;
		}
		if( product.has("brand") ) {
			brand = product.getString("brand");
		} else {
			brand = null;
		}
		if( product.has("promocode") ) {
			promocode = product.getString("promocode");
		} else {
			promocode = null;
		}
		if( product.has("price_with_promocode") ) {
			price_with_promocode = product.getString("price_with_promocode_formatted");
		} else {
			price_with_promocode = null;
		}
		if( product.has("discount_percent") ) {
			discount_percent = product.getString("discount_percent");
		} else {
			discount_percent = null;
		}
		price = product.getString("price_formatted");
		url = product.getString("url");
		if( product.has("deeplink_android") ) {
			deeplink = product.getString("deeplink_android");
		} else {
			deeplink = null;
		}
		if( product.has("discount") && product.getDouble("oldprice") > 0 && product.getDouble("oldprice") > product.getDouble("price") ) {
			discount = product.getString("discount");
		} else {
			discount = null;
		}
	}
}

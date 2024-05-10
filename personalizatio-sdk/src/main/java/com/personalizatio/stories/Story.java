package com.personalizatio.stories;

import androidx.annotation.NonNull;

import com.personalizatio.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

final class Story {
	public int id;
	public String avatar;
	public String name;
	public boolean viewed;
	public boolean pinned;
	public int start_position;
	public ArrayList<Story.Slide> slides;

	public Story(@NonNull JSONObject json) throws JSONException {
		id = json.getInt("id");
		avatar = json.getString("avatar");
		name = json.getString("name");
		viewed = json.getBoolean("viewed");
		pinned = json.getBoolean("pinned");
		start_position = json.getInt("start_position");
		slides = new ArrayList<>();
		JSONArray json_slides = json.getJSONArray("slides");
		for( int i = 0; i < json_slides.length(); i++ ) {
			slides.add(new Slide(json_slides.getJSONObject(i)));
		}
	}

	static class Slide implements Serializable {
		public String id;
		public String background;
		public String background_color;
		public String preview;
		public String type;
		public ArrayList<Story.Slide.Element> elements;
		public long duration;
		public boolean prepared = false;

		public Slide(@NonNull JSONObject json) throws JSONException {
			id = json.getString("id");
			background = json.getString("background");
			if( json.has("background_color") ) {
				background_color = json.getString("background_color");
			}
			if( json.has("preview") ) {
				preview = json.getString("preview");
			}
			type = json.getString("type");
			duration = json.optLong("duration", 5) * 1000L;
			elements = new ArrayList<>();
			JSONArray json_elements = json.getJSONArray("elements");
			for( int i = 0; i < json_elements.length(); i++ ) {
				elements.add(new Element(json_elements.getJSONObject(i)));
			}
		}

		static class Element {
			public String type;
			public String link;
			public String title;
			public String subtitle;
			public String icon;
			public String background;
			public String color;
			public Boolean text_bold = false;
			public String label_hide;
			public String label_show;
			public ArrayList<Product> products = new ArrayList<>();
			public Product item;

			public Element(@NonNull JSONObject json) throws JSONException {
				type = json.getString("type");
				if( json.has("link_android") ) {
					link = json.getString("link_android");
				}
				if( (link == null || link.isEmpty()) && json.has("link") ) {
					link = json.getString("link");
				}
				if( json.has("icon") ) {
					icon = json.getString("icon");
				}
				if( json.has("title") ) {
					title = json.getString("title");
				}
				if( json.has("subtitle") ) {
					subtitle = json.getString("subtitle");
				}
				if( json.has("background") ) {
					background = json.getString("background");
				}
				if( json.has("color") ) {
					color = json.getString("color");
				}
				if( json.has("text_bold") ) {
					text_bold = json.getBoolean("text_bold");
				}
				if( json.has("labels") ) {
					label_hide = json.getJSONObject("labels").getString("hide_carousel");
					label_show = json.getJSONObject("labels").getString("show_carousel");
				}
				if( json.has("products") ) {
					JSONArray products = json.getJSONArray("products");
					for( int i = 0; i < products.length(); i++ ) {
						this.products.add(new Product(products.getJSONObject(i)));
					}
				}
				if( json.has("item") && type.equals("product") ) {
					item = new Product(json.getJSONObject("item"));
				}
			}
		}
	}
}

package com.personalizatio.stories.models.elements;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class HeaderElement implements LinkElement {

    private String title;
    private String subtitle;
    private final String link;
    private String icon;

    public HeaderElement(@NonNull JSONObject json) throws JSONException {
        if (json.has("icon")) {
            icon = json.getString("icon");
        }
        if (json.has("title")) {
            title = json.getString("title");
        }
        if (json.has("subtitle")) {
            subtitle = json.getString("subtitle");
        }
        link = getLink(json);
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    @Override
    public String getLink() {
        return link;
    }
}

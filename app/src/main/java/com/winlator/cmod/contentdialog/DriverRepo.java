package com.winlator.cmod.contentdialog;

import org.json.JSONException;
import org.json.JSONObject;

public class DriverRepo {
    public String name;
    public String apiUrl;

    public DriverRepo(String name, String apiUrl) {
        this.name = name;
        this.apiUrl = apiUrl;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("url", apiUrl);
        return obj;
    }

    public static DriverRepo fromJson(JSONObject obj) {
        return new DriverRepo(
            obj.optString("name", "Unknown Repo"),
            obj.optString("url", "")
        );
    }
}
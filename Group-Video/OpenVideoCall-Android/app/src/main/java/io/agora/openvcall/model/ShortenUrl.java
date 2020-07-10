package io.agora.openvcall.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ShortenUrl {

    @SerializedName("shorturl")
    @Expose
    private String shortUrl;

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }
}

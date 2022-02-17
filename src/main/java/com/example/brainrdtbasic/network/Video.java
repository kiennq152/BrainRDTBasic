package com.example.brainrdtbasic.network;

import com.google.gson.annotations.SerializedName;

public class Video {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("thumbnail")
    private String thumbnail;

    public String getId(){
      return id;
    }
    public String getUrl(){
        return url;
    }

    public String getName(){
        return name;
    }

    public String getThumbnail(){
        return thumbnail;
    }
}

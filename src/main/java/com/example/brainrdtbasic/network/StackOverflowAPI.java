package com.example.brainrdtbasic.network;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface StackOverflowAPI {
//    @GET("/api/v1/login")
////    Call<StackOverflowQuestions> loadQuestion(@Query("tagged") String tags);
//    Call<StackOverflowQuestions> isValidUser(
//            @Query("ssid") String ssid,
//            @Query("status") String status,
//            @Query("err") String err);

    //load video API
    @POST("/api/v1/videoList")
    @FormUrlEncoded
    Call<Video> loadVideo(@Field("start_index") String stid, @Field("count") String cnt, @Field("access_token") String ssid);
    //load video API
    @POST("/api/v1/watchedVideos")
    @FormUrlEncoded
    Call<ResponseBody> getVideolist(@Field("user") String user, @Field("start_index") String stid, @Field("count") String cnt);
}

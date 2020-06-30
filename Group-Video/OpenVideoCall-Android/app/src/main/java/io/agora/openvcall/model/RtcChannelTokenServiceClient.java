package io.agora.openvcall.model;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RtcChannelTokenServiceClient {

    private static final String BASE_URL = "https://face-chat-overlay.herokuapp.com";
    private static Retrofit retrofit = null;

    public static RtcChannelTokenService getClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return (RtcChannelTokenService) retrofit.create(RtcChannelTokenService.class);
    }
}

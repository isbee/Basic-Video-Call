package io.agora.openvcall.model;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RtcChannelTokenService {
    @GET("/access_token?")
    Call<RtcChannelToken> getRtcChannelToken(@Query("channel") String channel, @Query("uid") String userId);
}

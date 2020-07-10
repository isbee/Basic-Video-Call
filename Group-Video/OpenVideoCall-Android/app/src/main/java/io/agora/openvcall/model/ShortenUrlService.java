package io.agora.openvcall.model;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ShortenUrlService {
    @GET("/create.php?")
    Call<ShortenUrl> getShortenUrl(@Query("format") String format, @Query("url") String targetUrl);
}

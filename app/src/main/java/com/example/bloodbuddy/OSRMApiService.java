package com.example.bloodbuddy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OSRMApiService<OSRMRouteResponse> {
    @GET("route/v1/driving/")
    Call<OSRMRouteResponse> getRoute(
            @Query("coordinates") String coordinates,
            @Query("overview") String overview,
            @Query("geometries") String geometries
    );
}

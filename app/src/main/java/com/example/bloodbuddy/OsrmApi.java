package com.example.bloodbuddy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface OsrmApi {
    @GET("route/v1/driving/{coordinates}?overview=full&geometries=geojson")
    Call<RouteResponse> getRoute(@Path("coordinates") String coordinates);
}
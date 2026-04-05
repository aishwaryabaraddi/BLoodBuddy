package com.example.bloodbuddy;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NearbyAssetService {
    @GET("NearbyAssets/ws/v2/getNearbyAssetData")
    Call<List<NearbyAssetResponse>> getNearbyAssets(
            @Query("code") int code,
            @Query("Lon") double lon,
            @Query("Lat") double lat
    );
}

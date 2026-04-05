package com.example.bloodbuddy;

import com.google.gson.annotations.SerializedName;

public class NearbyAssetResponse {

    @SerializedName("assetName")
    private String assetName;

    @SerializedName("y")
    private int y;

    @SerializedName("x")
    private int x;

    @SerializedName("distance")
    private double distance;

    @SerializedName("address")
    private String address;

    @SerializedName("OtherInfo")
    private String otherInfo;

    @SerializedName("AssetType")
    private String assetType;

    @SerializedName("Lon")
    private String lon;

    @SerializedName("Lat")
    private String lat;

    // Getters
    public String getAssetName() {
        return assetName;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public double getDistance() {
        return distance;
    }

    public String getAddress() {
        return address;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getLon() {
        return lon;
    }

    public String getLat() {
        return lat;
    }
}

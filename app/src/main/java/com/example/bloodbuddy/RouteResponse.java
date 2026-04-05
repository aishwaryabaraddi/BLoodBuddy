package com.example.bloodbuddy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RouteResponse {
    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("geometry")
        public Geometry geometry;
        public double duration;
        public double distance;
    }

    public static class Geometry {
        @SerializedName("coordinates")
        public List<List<Double>> coordinates;
    }
}
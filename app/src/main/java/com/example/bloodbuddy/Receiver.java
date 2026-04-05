package com.example.bloodbuddy;

import java.io.Serializable;

public class Receiver implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String district;
    private String taluk;
    private String bloodGroup;
    private String toWhomFor;
    private String location;
    private double latitude;
    private double longitude;

    // Default constructor required for calls to DataSnapshot.getValue(Receiver.class)
    public Receiver() {
    }

    public Receiver(String id, String name, String phoneNumber, String district, String taluk, String bloodGroup, String toWhomFor, String location, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.taluk = taluk;
        this.bloodGroup = bloodGroup;
        this.toWhomFor = toWhomFor;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDistrict() {
        return district;
    }

    public String getTaluk() {
        return taluk;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getToWhomFor() {
        return toWhomFor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

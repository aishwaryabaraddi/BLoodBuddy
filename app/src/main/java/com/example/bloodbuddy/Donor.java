package com.example.bloodbuddy;

import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.UUID;

public class Donor implements Serializable {
    private String id; // Unique ID for each donor
    private String name;
    private String phoneNumber;
    private String district;
    private String taluk;
    private String lastDonated;
    private String bloodGroup;
    private String location;
    private double latitude;
    private double longitude;

    // Default constructor required for calls to DataSnapshot.getValue(Donor.class)
    public Donor() {
    }

    // Constructor for creating donor with all details including ID
    public Donor(String id, String name, String phoneNumber, String district, String taluk, String lastDonated, String bloodGroup, String location, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.taluk = taluk;
        this.lastDonated = lastDonated;
        this.bloodGroup = bloodGroup;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Constructor for creating donor without ID (generates ID automatically)
    public Donor(String name, String phoneNumber, String district, String taluk, String lastDonated, String bloodGroup, String location, double latitude, double longitude) {
        this.id = generateUniqueId(); // Generate unique ID
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.taluk = taluk;
        this.lastDonated = lastDonated;
        this.bloodGroup = bloodGroup;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Donor(String name, String bloodGroup, String phoneNumber) {
        this.name = name;
        this.bloodGroup = bloodGroup;
        this.phoneNumber = phoneNumber;

    }

    // Getter methods


    @PropertyName("id")
    public void setid(String id) {
        this.id = id;
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

    public String getLastDonated() {
        return lastDonated;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getLocation() {
        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // Setter methods
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Method to generate a unique ID (for example purposes, can be adjusted as needed)
    private String generateUniqueId() {
        // Implement your unique ID generation logic here (e.g., UUID.randomUUID().toString())
        return UUID.randomUUID().toString();
    }

    public String getid() {
        return id;
    }

    public boolean getRequestUserId() {
        return false;
    }
}

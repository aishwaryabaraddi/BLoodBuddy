package com.example.bloodbuddy;

import java.io.Serializable;

public class Donor implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String district;
    private String taluk;
    private String lastDonated;
    private String bloodGroup;
    private String location;
    private String gender;
    private String age;
    private String weight;
    private String donationCount;
    private String disease;
    private double latitude;
    private double longitude;

    // Required empty constructor for Firestore
    public Donor() {}

    public Donor(String name, String bloodGroup, String phoneNumber) {
        this.name = name;
        this.bloodGroup = bloodGroup;
        this.phoneNumber = phoneNumber;
    }

    public Donor(String id, String name, String phoneNumber, String district, String taluk, String lastDonated, String bloodGroup, String location, String gender, String age, String weight, String donationCount, String disease, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.taluk = taluk;
        this.lastDonated = lastDonated;
        this.bloodGroup = bloodGroup;
        this.location = location;
        this.gender = gender;
        this.age = age;
        this.weight = weight;
        this.donationCount = donationCount;
        this.disease = disease;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getTaluk() { return taluk; }
    public void setTaluk(String taluk) { this.taluk = taluk; }

    public String getLastDonated() { return lastDonated; }
    public void setLastDonated(String lastDonated) { this.lastDonated = lastDonated; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getDonationCount() { return donationCount; }
    public void setDonationCount(String donationCount) { this.donationCount = donationCount; }

    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = disease; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

package com.example.bloodbuddy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Receiver implements Serializable {
    private String id;
    private String userId; // Link to the user who made the request
    private String name;
    private String phoneNumber;
    private String district;
    private String taluk;
    private String bloodGroup;
    private String toWhomFor;
    private String location;
    private String patientAge;
    private String patientGender;
    private double latitude;
    private double longitude;
    private long timestamp;
    private boolean active;
    private List<String> responderIds;

    public Receiver() {
        this.responderIds = new ArrayList<>();
    }

    public Receiver(String id, String userId, String name, String phoneNumber, String district, String taluk, String bloodGroup, String toWhomFor, String location, double latitude, double longitude) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.taluk = taluk;
        this.bloodGroup = bloodGroup;
        this.toWhomFor = toWhomFor;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.active = true;
        this.responderIds = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getTaluk() { return taluk; }
    public void setTaluk(String taluk) { this.taluk = taluk; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getToWhomFor() { return toWhomFor; }
    public void setToWhomFor(String toWhomFor) { this.toWhomFor = toWhomFor; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPatientAge() { return patientAge; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<String> getResponderIds() { return responderIds; }
    public void setResponderIds(List<String> responderIds) { this.responderIds = responderIds; }
}

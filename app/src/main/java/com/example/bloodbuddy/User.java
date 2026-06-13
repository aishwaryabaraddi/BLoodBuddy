package com.example.bloodbuddy;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class User implements Serializable {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String state;
    private String district;
    private String taluk;
    private String gender;
    private String bloodGroup;
    private String imageUrl;
    private String dob; 
    
    private boolean donor; // Renamed to be Firebase friendly (field name 'donor')
    private String lastDonationDate;
    private boolean verified; 
    private int totalDonations;
    private boolean admin; // Renamed to be Firebase friendly (field name 'admin')

    public User() {
    }

    public User(String id, String name, String email, String phone, String state, String district, String taluk, String gender, String bloodGroup) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.state = state;
        this.district = district;
        this.taluk = taluk;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.donor = false;
        this.verified = false;
        this.totalDonations = 0;
        this.admin = false;
    }

    public User(String id, String name, String email, String phone, String state, String district, String taluk, String gender, String bloodGroup, String dob) {
        this(id, name, email, phone, state, district, taluk, gender, bloodGroup);
        this.dob = dob;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getTaluk() { return taluk; }
    public void setTaluk(String taluk) { this.taluk = taluk; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public boolean isDonor() { return donor; }
    public void setDonor(boolean donor) { this.donor = donor; }

    public String getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}

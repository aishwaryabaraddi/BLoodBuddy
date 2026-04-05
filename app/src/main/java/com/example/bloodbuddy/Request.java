package com.example.bloodbuddy;

public class Request {
    private String requesterName;
    private String requesterPhone;
    private String requesterTaluk;
    private String message;

    // Empty constructor required for Firebase
    public Request() {
    }

    public Request(String requesterName, String requesterPhone, String requesterTaluk, String message) {
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.requesterTaluk = requesterTaluk;
        this.message = message;
    }

    // Getters and Setters
    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public String getRequesterTaluk() {
        return requesterTaluk;
    }

    public String getMessage() {
        return message;
    }

    // Setters
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    public void setRequesterTaluk(String requesterTaluk) {
        this.requesterTaluk = requesterTaluk;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


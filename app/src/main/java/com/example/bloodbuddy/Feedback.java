package com.example.bloodbuddy;

public class Feedback {

    public String feedbackId;
    public String name;
    public String contact;
    public String email;
    public String feedback;

    public Feedback() {
        // Default constructor required for calls to DataSnapshot.getValue(Feedback.class)
    }

    public Feedback(String feedbackId, String name, String contact, String email, String feedback) {
        this.feedbackId = feedbackId;
        this.name = name;
        this.contact = contact;
        this.email = email;
        this.feedback = feedback;
    }
}

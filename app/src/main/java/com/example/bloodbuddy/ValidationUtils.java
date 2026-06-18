package com.example.bloodbuddy;

import android.util.Patterns;

public class ValidationUtils {

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 3
                && name.matches("^[\\p{L}\\s.'-]+$");
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidMobile(String phone) {
        if (phone == null || phone.length() != 10) return false;
        if (!phone.matches("^[6-9]\\d{9}$")) return false;
        return !phone.matches("(\\d)\\1{9}");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static int parseAge(String ageStr) {
        if (ageStr == null || ageStr.trim().isEmpty()) return -1;
        try {
            return Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static double parseWeight(String weightStr) {
        if (weightStr == null || weightStr.trim().isEmpty()) return -1;
        try {
            return Double.parseDouble(weightStr.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

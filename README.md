# 🩸 Blood Buddy

**Connecting Life-Savers with Those in Need**

Blood Buddy is a comprehensive Android application designed to bridge the gap between blood donors and recipients. It simplifies finding verified donors by location and blood group, while providing an AI-powered assistant for health and eligibility queries.

---

## 🚀 Getting Started

### For Users
1. **Download the APK**: You can download the latest build from the "Actions" tab in this GitHub repository (see GitHub Actions section).
2. **Install**: Open the `.apk` file on your Android device and allow installation from unknown sources.
3. **Sign Up**: Register as a donor or a regular user to start saving lives.

### For Developers
1. **Clone the repo**: `git clone https://github.com/your-username/BloodBuddy.git`
2. **Open in Android Studio**: Use Android Studio Koala or newer.
3. **JDK Version**: Ensure your project is set to use **JDK 17**.
4. **Firebase**: Add your `google-services.json` to the `app/` directory.

---

## 📱 Features & Screens

### 1. Home Dashboard
* **Dynamic Carousel**: View important announcements and upcoming blood camps.
* **Navigation**: Quick access to "Find Donors", "Request Blood", and "AI Chat".

### 2. Donor Discovery
* **Search Filters**: Filter donors by Blood Group, State, District, and Taluk.
* **Donor Profiles**: View donor contact details, verification status, and donation history.
* **Maps**: See donor locations on an interactive map (powered by OSMDroid).

### 3. Blood Buddy AI Assistant
* **AI Chatbot**: Powered by **Gemini 1.5 Flash**.
* **Instant Answers**: Ask questions about donation eligibility, health tips, and recovery.
* **Safety First**: Integrated safety filters to ensure reliable medical information.

### 4. Admin Portal
* **Banner Management**: Admins can upload and update home screen carousel images.
* **User Verification**: Tools to verify donor credentials and maintain platform trust.

---

## 🛠 Tech Stack
* **Language**: Java
* **UI**: XML / Material Design
* **Backend**: Firebase (Auth, Firestore, Realtime DB)
* **AI**: Google Generative AI SDK (Gemini)
* **Maps**: OSMDroid
* **Libraries**: Volley, Retrofit, Picasso, Glide

---

## 🤖 CI/CD with GitHub Actions
This project is configured to automatically build a debug APK on every push to the `main` or `master` branches.
* **Location**: `.github/workflows/build_apk.yml`
* **How to use**: 
    1. Go to the **Actions** tab in GitHub.
    2. Select the **Android Build APK** workflow.
    3. Download the generated **Artifact** (BloodBuddy-Debug-APK).

---

## 📄 Project Documentation
For a detailed presentation of this project, refer to the [Project Overview Document](PROJECT_OVERVIEW.md) (or see the detailed sections above).

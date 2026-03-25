# Product Requirements Document (PRD)
## Fashion Item Recommender – Android App (Registration & Onboarding Phase)

---

## 1. Overview

The Fashion Item Recommender App is an Android application designed to provide personalized fashion recommendations to users based on their physical attributes, preferences, and lifestyle.  
This PRD covers **Phase 1**, which focuses exclusively on **user registration and onboarding** to collect structured data required for future recommendation systems.

---

## 2. Goals & Objectives

### Primary Goals
- Enable fast and secure user registration using Google Sign-In
- Collect essential user profile data for personalization
- Store user data in a scalable, ML-ready format

### Secondary Goals
- Ensure smooth onboarding experience
- Allow optional and skip-friendly data collection
- Prepare foundation for future fashion recommendation logic

---

## 3. Scope

### In Scope (Phase 1)
- Google Sign-In authentication
- User onboarding questionnaire
- User profile data storage
- Onboarding completion validation

### Out of Scope (Phase 1)
- Fashion recommendations
- Outfit generation
- Admin panel
- Product catalog
- Payments or subscriptions

---

## 4. Target Users

- Android smartphone users
- Age group: 16–40
- Users interested in fashion, styling, and outfit suggestions
- College students, working professionals, casual fashion enthusiasts

---

## 5. User Flow

1. User opens the app
2. User signs in using Google
3. App checks onboarding completion status
4. If onboarding not completed:
   - User is guided through onboarding questions
5. User profile is saved
6. User is redirected to the home screen (placeholder)

---

## 6. Functional Requirements

### 6.1 Authentication
- Users must sign in using Google
- Authentication handled via Firebase Authentication
- Each user is assigned a unique user ID (UID)

---

### 6.2 Onboarding Questionnaire

#### Mandatory Fields
- Gender  
  - Male  
  - Female  
  - Non-binary  
  - Prefer not to say  

- Body Type  
  - Slim  
  - Average  
  - Athletic  
  - Curvy  
  - Plus-size  

- Preferred Clothing Style (Multi-select)  
  - Casual  
  - Streetwear  
  - Formal  
  - Traditional / Ethnic  
  - Sportswear  
  - Minimal  

- Fit Preference  
  - Slim fit  
  - Regular fit  
  - Oversized  

- Budget Range  
  - Low  
  - Medium  
  - Premium  

---

#### Optional Fields
- Height (cm)
- Climate preference (Hot / Cold / Moderate)
- Occasion-based dressing
- Favorite colors
- Brand preferences

---

### 6.3 Onboarding Experience
- Step-by-step question flow
- Progress indicator (e.g., 3 of 6 completed)
- Ability to skip optional questions
- Validation for mandatory questions

---

## 7. Data Requirements

### 7.1 Database
- Firebase Firestore

### 7.2 User Data Schema

```json
{
  "uid": "string",
  "name": "string",
  "email": "string",
  "gender": "string",
  "bodyType": "string",
  "height": "number",
  "styles": ["string"],
  "fitPreference": "string",
  "budget": "string",
  "climate": "string",
  "onboardingCompleted": true,
  "createdAt": "timestamp"
}
```

## 8. Non-Functional Requirements

Fast onboarding (< 2 minutes)

Secure authentication and data storage

Scalable database structure

Clean and intuitive UI

Compliance with basic data privacy standards

## 9. Tech Stack

Platform: Android

Language: Kotlin

UI: Jetpack Compose

Authentication: Firebase Authentication (Google Sign-In)

Database: Firebase Firestore
# Phase 1: Foundation & Authentication Setup (Android)

> **Milestone**: Android & Python Recommender MVP
> **Objective**: Configure Firebase, implement Google Sign-In (Credential Manager), establish core NavGraph, and build the Splash and SignIn screens.

## Requirements

The codebase must have a baseline Android application module named `app` using Jetpack Compose.
- **`app/build.gradle.kts`** must include `firebase-auth`, `firebase-firestore`, and `credentials-play-services-auth`.
- **`google-services.json`** must be present in the `app/` directory for Firebase to initialize.
- A basic `NavGraph.kt` must exist to route between `SplashScreen`, `SignInScreen`, and `HomeScreen`.

## Execution Plan

### 1. Project Cleanup & Validation
- Start by checking if `MainActivity.kt` and `NavGraph.kt` correctly reference the screens mapped out in the Roadmap.
- *Wait, I mapped the codebase and saw the Android app already has dependencies for Firebase, Google ID, Navigation Compose, etc., and `NavGraph.kt` exists with all these routes.*
- **Action**: Verify the existing `google-services.json` and gradle files to ensure Firebase is indeed ready. Check `MainActivity.kt` to ensure Firebase is initialized or the Compose entry point is intact.

### 2. Implement Splash Screen
- **Target File**: `app/src/main/java/com/closetai/app/ui/screens/SplashScreen.kt`
- **Details**: 
  - Should display a Lottie animation.
  - Check Firebase Auth state (`FirebaseAuth.getInstance().currentUser`).
  - Delay for 2 seconds (or until animation finishes).
  - If user exists -> Navigate to `HomeScreen` (or `Onboarding` if profile incomplete).
  - If no user -> Navigate to `SignInScreen`.

### 3. Implement Google Sign-In Screen
- **Target File**: `app/src/main/java/com/closetai/app/ui/screens/SignInScreen.kt`
- **Details**:
  - UI matching `theme.md` (Neutral background, fashion-first, glassmorphism hints on the button).
  - Use modern Android `CredentialManager` for Google Sign-In.
  - On success, authenticate with `FirebaseAuth.getInstance().signInWithCredential`.
  - Check if the user document exists in Firestore (`users/{uid}`).
  - If exists -> Navigate to `HomeScreen`.
  - If not exists -> Navigate to `GenderScreen` (Onboarding Part 1).

### 4. Create UserRepository (Firestore Abstraction)
- **Target File**: `app/src/main/java/com/closetai/app/data/repository/UserRepository.kt`
- **Details**:
  - Implement a check `suspend fun checkUserExists(uid: String): Boolean` traversing Firestore.
  - *(Optional)* Create the data model `UserProfile` to mirror the JSON schema requested in the MVP.

## Context Needed for Execution
- **`theme.md`**: For UI styling and glassmorphism rules.
- **`/screens/...`**: To understand the exact visual layout expected for Splash and SignIn screens.

## Technical Debt / Risks
- Firebase setup needs a valid SHA-1 footprint in the Firebase Console for Google Sign-In to work. We must ensure the `debug.keystore` or `release-keystore.jks` matches what is on Firebase. (This requires the user's explicit setup externally).

## State Update
Upon completing these steps, update `STATE.md` to reflect Phase 1 completion and progress to Phase 2.

# Project Specification: ClosetAI

> **Status**: Active
> **Created**: 2026-02-21

## 1. Overview
ClosetAI is a native Android application designed to provide personalized fashion recommendations and wardrobe management. The core experience revolves around gathering detailed user tailoring preferences (budget, body type, occasions, style, and fit) and utilizing Firebase backend services for authentication and data storage. It is supported by a Python-based backend service for an "AI Closet" fashion recommender that searches multiple e-commerce platforms to return personalized clothing recommendations.

## 2. Core Features (Android MVP)
- **User Authentication**: Secure login and registration using Google Sign-In (Credential Manager) and Firebase Auth.
- **Comprehensive Onboarding Flow**: A multi-step flow to collect detailed user preferences (Gender, Body Type, Size Measurements, Skin Tone, Style, Fit, Occasion, Budget, Special Requirements, Clothing Categories).
- **Persistent User Profiles**: Storing user preferences in Firebase Firestore.
- **Home/Dashboard**: A landing screen post-onboarding (currently minimal, serves as a foundation for future features).

## 3. Core Features (Python Backend Recommender MVP)
- **Multi-Source Scraping**: Async scraping for Myntra, Amazon India, Ajio, and Nykaa Fashion.
  - Handles anti-bot measures (user-agent rotation, delays, headless browser configs).
  - Fallbacks to official APIs if available (Amazon Product Advertising API, Myntra affiliate).
  - Caches results in Redis to avoid repeated scraping.
- **Recommendation Engine Logic**:
  - Filters by: size availability, price range, gender category.
  - Scores items by: style tag matching (80% weight), color palette match (15%), brand preference (5%).
  - Returns top 10-15 items across different platforms.
  - Diversifies results (2-3 items per platform max to avoid bias).
- **API Endpoint**: `POST /api/v1/recommendations`
  - Accepts a JSON payload containing `user_profile` and `context`.
  - Returns a JSON response containing `recommendations` and a `styling_tip`.

## 4. Technical Stack
### Frontend (Android)
- **Platform**: Native Android (Kotlin 2.0.21)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **UI Framework**: Jetpack Compose
- **Navigation**: Jetpack Navigation Compose
- **Animations**: Lottie

### Backend (Python Recommender)
- **Language**: Python
- **Framework**: FastAPI
- **Web Scraping/Requests**: `aiohttp` or `playwright`
- **Caching**: Redis
- **Architecture**: Class-based (`FashionRecommender`, `ScraperManager`, `MatchScorer`)
- **Environment Management**: Environment variables for API keys

### Shared Services
- **Backend/BaaS**: Firebase (Authentication, Firestore)

## 5. Key Constraints & Requirements
- **Design (Android)**: Must utilize Jetpack Compose exclusively for UI.
- **State Management (Android)**: Complex onboarding state must be retained across multiple screens using a shared `OnboardingViewModel`.
- **Security**: User data and preferences must be securely stored in Firestore, accessible only to the authenticated user.
- **Scraping Policies (Python)**: Respect `robots.txt` and rate limits (max 1 request/sec per site). Implement retry logic with exponential backoff.
- **Error Handling (Python)**: Include robust error handling for site blocks/outages.

## 6. Future Scope (Beyond MVP)
- Virtual wardrobe management (uploading photos of existing clothes).
- Integration with weather APIs (e.g., OpenWeatherMap) and occasion/festival detection for context-aware recommendations.

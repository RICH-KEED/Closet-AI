# MVP Definition – Fashion Item Recommender App & Backend

---

## 1. Purpose of MVP

The goal of this MVP is to build a **functional foundation** of the Fashion Item Recommender ecosystem. This includes:
1. An **Android App** focused on implementing user registration and collecting structured user tailoring preferences (onboarding).
2. A **Python-based Backend Service** that acts as the "AI Closet" engine to scrape multiple e-commerce platforms and return personalized clothing recommendations based on those preferences.

---

## 2. MVP Scope (What MUST Be Built)

### ✅ Included in Android App MVP
- Google Sign-In authentication
- User onboarding flow (detailed preferences)
- User profile creation & storage
- Minimal home screen (placeholder)
- Theme and UI consistency

### ✅ Included in Python Backend MVP
- FastAPI service with `POST /api/v1/recommendations` endpoint
- Async web scraping strategy (Myntra, Amazon India, Ajio, Nykaa Fashion) using `aiohttp` or `playwright`.
- Anti-bot measures, retry logic, and exponential backoff
- Redis caching for product data
- Recommendation engine logic filtering by size, price, and gender, and scoring by style, color, and brand.
- Structured JSON output format with recommendations and styling tips.

### ❌ Excluded from MVP
- Virtual wardrobe analysis (uploading photos)
- Payments or subscriptions
- Admin features
- Direct outfit generation UI on Android (Backend will provide raw item recommendations first)
- Weather/Occasion dynamic detection API integration (Placeholders only)

---

## 3. Reference Documents (IMPORTANT)

All development decisions **must follow** these documents:

- **`PRD.md`** → Product requirements & user flow  
- **`theme.md`** → UI theme, colors, typography, glassmorphism rules  
- **`SPEC.md`** → Core architectural specifications  

---

## 4. Screens to Build (Android MVP)

### Required Screens
1. Splash Screen  
2. Google Sign-In Screen  
3. Onboarding (Gender, Body Type, Size, Skin Tone, Style, Fit, Budget, Occasion, Special Req., Categories)  
4. Onboarding Completion Screen  
5. Empty Home Screen  

---

## 5. Functional Requirements (MVP)

### Authentication & Storage (Android)
- Google Sign-In using Firebase Authentication
- Firebase Firestore to store profiles (One document per user)

### Python Recommender Backend
- **Input:** JSON payload with `user_profile` and `context`.
- **Logic:** 
  - Filter: Size, price range, gender category.
  - Score: Style tags (80%), color match (15%), brand matching (5%).
  - Diversify: 2-3 items max per platform (top 10-15 total).
- **Scraping Guidelines:** Respect `robots.txt` and rate limits (1 req/sec). Fallback to official APIs when viable. Handle site blocks gracefully.

---

## 6. Theme & UI Rules (Android MVP)

- Minimal, modern, fashion-first UI (Light theme default)
- Image-first layouts with rounded components
- Subtle glassmorphism limits as defined in `theme.md`

---

## 7. AI Instructions (MANDATORY)

When using AI to generate code, UI, or logic:

- Always load and follow: `PRD.md`, `theme.md`, `SPEC.md`.
- Keep the Python architecture class-based (`FashionRecommender`, `ScraperManager`, `MatchScorer`).
- Do NOT introduce new UI features outside the Android scope.

---

## 8. Tech Stack (MVP)

### Android App
- Platform: Android (Kotlin)
- UI: Jetpack Compose
- Auth/DB: Firebase (Google Sign-In, Firestore)

### Python Backend
- Framework: FastAPI (Python)
- Scraping: `aiohttp` / `playwright`
- Caching: Redis
- Architecture: Class-based, environment-driven (`.env`)

---

## 9. Success Criteria

The MVP is considered complete when:
- A user can sign in and complete the onboarding flow on Android.
- The Python backend can receive a mocked JSON payload of those preferences and successfully return 10-15 matching, diversified real-world clothing items from scraped platforms.
- Error handling and caching are proven functional on the backend.

---

## 10. Post-MVP (Next Phase)

- Integrating the Android App with the Python Backend (live recommendations UI).
- Real-time OpenWeatherMap API integration.
- Virtual wardrobe integration.

---

**MVP Status:** Ready for Implementation  
**Last Updated:** 2026-02-21  
**Owner:** Product / Engineering Team

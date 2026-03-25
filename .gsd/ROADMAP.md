# ROADMAP.md

> **Current Milestone**: Dynamic Context & Virtual Wardrobe
> **Goal**: Expand ClosetAI beyond static preferences by introducing real-time environmental context (weather) and the foundation for users to digitize their real-world clothing.

## Must-Haves
- [ ] Google Sign-In authentication via Firebase
- [ ] Comprehensive Android Onboarding flow
- [ ] User profile creation and persistent storage in Firebase Firestore
- [ ] Asynchronous Python Scraper (Myntra, Amazon India, Ajio, Nykaa)
- [ ] Recommendation Engine Logic (Filter by size/price/gender, Score by style/color/brand)
- [ ] FastAPI Endpoint to return matching products in a structured JSON
- [ ] Adherence to UI theme (Glassmorphism, rounded components, light theme as default) on Android

## Phases

### Phase 1: Foundation & Authentication Setup (Android)
**Status**: ⬜ Not Started
**Objective**: Configure Firebase, implement Google Sign-In (Credential Manager), establish core NavGraph, and build the Splash and SignIn screens.

### Phase 2: Core Onboarding Flow Part 1 (Android)
**Status**: ⬜ Not Started
**Objective**: Build ViewModel for state management. Implement Gender, Body Type, Size Measurements, and Skin Tone screens.

### Phase 3: Core Onboarding Flow Part 2 (Android)
**Status**: ⬜ Not Started
**Objective**: Implement Style Preference, Fit Preference, Typical Occasions, and Budget Constraints screens.

### Phase 4: Core Onboarding Flow Part 3 (Android)
**Status**: ⬜ Not Started
**Objective**: Implement Special Requirements and Clothing Categories screens. Build Onboarding Completion screen to save all data to Firestore.

### Phase 5: Python Backend Setup & Scraping Hooks
**Status**: ⬜ Not Started
**Objective**: Initialize Python FastAPI project. Implement `aiohttp`/`playwright` scraper hooks for Myntra, Amazon, Ajio, and Nykaa with basic anti-bot handling.

### Phase 6: Recommendation Engine & Caching (Python)
**Status**: ✅ Completed
**Objective**: Implement Redis caching. Build the scoring logic (matching by style 80%, color 15%, brand 5%) and the `POST /api/v1/recommendations` endpoint.

### Phase 7: Verification & Polish
**Status**: ✅ Completed
**Objective**: Ensure the Android UI fully aligns with `theme.md`. Mock payload generation from Android to test against the Python backend. Ensure successful End-to-End JSON response.

---

> **Current Milestone**: Dynamic Context & Virtual Wardrobe
> **Goal**: Expand ClosetAI beyond static preferences by introducing real-time environmental context (weather) and the foundation for users to digitize their real-world clothing.

## Milestone 2 Phases

### Phase 8: Real-Time Context Integration (Weather API)
**Status**: ✅ Completed
**Objective**: Integrate OpenWeatherMap API in Backend. Android fetches location and sends coordinates in context.

### Phase 9: Virtual Wardrobe Foundation (Storage & UI)
**Status**: ✅ Completed
**Objective**: Build "My Wardrobe" Screen. Setup Camera/Gallery intent and Firebase Cloud Storage for clothing uploads.

### Phase 10: Outfit Generation Logic (V1)
**Status**: ⬜ Not Started
**Objective**: Combine Virtual Wardrobe items + scraped items to generate context-aware outfits considering weather.

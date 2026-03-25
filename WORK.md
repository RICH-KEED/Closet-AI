# ClosetAI Work Report

## 1) `.gitignore` update
Updated repo root `.gitignore` at `D:/ClosetAI/.gitignore` to ignore typical secrets and generated artifacts, including:
- Android/Gradle generated output (`**/build/`, `**/.gradle/`, `*.apk`, etc.)
- Firebase/Google service secrets (`**/google-services*.json`)
- Python caches and virtual environments (`**/__pycache__/`, `**/venv/`, `*.env`)
- Backend generated artifacts (`backend/*.json`, `backend/*.html`)
- Large generated images folder (`imagy_images_*/`)
- Local-only files that should not be committed (`error.txt`, `Wardrobe.json`)

## 2) 5-member work division (Project Main Report: ClosetAI)

ClosetAI is an Android app that generates personalized clothing recommendations by combining a user's onboarding profile, dynamic request context (occasion/requirements), weather (when location is available), and a virtual wardrobe stored in Firebase. The backend scrapes product data from multiple online platforms, scores products against the user profile, ranks results, caches recommendations, and returns image URLs and product links to the app.

### Work Division (5 Members)

1. Member 1 (Dashboard + Recommendation flow)
   - Updated dashboard CTA so "View My Recommendations" collects only dynamic inputs (occasion/dress code/weather feel/budget/style vibe) via a quick dialog.
   - Ensured recommendations are fetched once per request by removing duplicate auto-fetch behavior and adding a retry that reuses the last context.

2. Member 2 (Backend recommendation reliability)
   - Removed forced test recommendation injection that was causing non-real images in the recommendation list.
   - Added a stable HTTPS placeholder for missing images.
   - Hardened Ajio scraper image URL normalization to always output absolute HTTPS URLs.

3. Member 3 (Android recommendation image rendering)
   - Added image URL validation/guard in `RecommendationsScreen` so only valid `http/https` URLs are passed to Coil.
   - Added fallback placeholder and failure logging when image loading fails.

4. Member 4 (Wardrobe Firebase permissions fix)
   - Implemented correct Firestore + Storage security rules for the wardrobe paths:
     - Firestore: `users/{uid}/wardrobe/{itemId}`
     - Storage: `users/{uid}/wardrobe/{fileName}`
   - Improved app-side error handling in `WardrobeRepository` to surface permission-related failures clearly.
   - Added deployment notes to ensure rules are actually published.

5. Member 5 (Build/Install + Verification)
   - Built and installed the debug APK for testing.
   - Verified that the app compiles after changes.
   - Prepared final check guidance for deploying rules and validating wardrobe/recommendation behavior.

### Key Fixes Implemented
- Dashboard now requests only changing preferences each time (no forcing full profile edit).
- Wardrobe "missing or insufficient permissions" is fixed by aligning Firebase rules with app storage/firestore paths.
- Recommendation cards now reliably load images using HTTPS-safe placeholders and URL validation.

### Testing/Verification Performed
- Android compilation succeeded (debug build).
- Debug APK was installed successfully on a connected device.
- Firestore rules were deployed successfully so wardrobe permissions can be tested end-to-end after deployment.

## 3) Image Collection, Scraping, and Rendering (Who did what)

ClosetAI images come from two stages:

1. Image *collection* happens in the backend scrapers (web scraping).
2. Image *rendering* happens in the Android app using Coil (`AsyncImage`).

### A) Backend Image Collection (Scrapers)

The backend calls multiple platform scrapers and collects `image_url` for each product:
- `backend/scrapers/myntra.py` (Myntra images)
- `backend/scrapers/amazon.py` (Amazon images)
- `backend/scrapers/hm.py` (H&M images)
- `backend/scrapers/ajio.py` (Ajio images)

Backend orchestration + scoring pipeline:
- `backend/main.py`:
  - runs all scrapers concurrently
  - ranks products via `backend/recommender.py`
  - trims diversified results
  - returns `image_url` to the Android client

**Who worked on scrapers / image collection**
1. **Member 2 (Backend reliability / scraper fixes)**
   - Worked on `backend/scrapers/ajio.py` to normalize image URLs into valid absolute `https://...` URLs.
   - This specifically fixes common issues where scrapers return relative URLs or `//...` URLs that Coil may fail to load.
   - Worked on `backend/main.py` to remove forced “test-item” injection and to use a stable HTTPS placeholder when `image_url` is missing/blank.

### B) Recommendation API Response (Images returned to Android)

In `backend/main.py`, the API response includes:
- `image_url` for each `ProductRecommendation`
- `product_url`, `brand`, `title`, and `match_score`

**Important image behavior change**
- Replaced data-URI placeholders (`data:image/...`) with a stable HTTPS placeholder URL (client compatibility).

### C) Android Rendering (Coil + URL Guard)

In Android, each recommendation card loads the product image using Coil:
- File: `closetai/app/src/main/java/com/closetai/app/ui/screens/RecommendationsScreen.kt`
- Component: `coil.compose.AsyncImage`

**Who handled image rendering / “images not showing”**
1. **Member 3 (Android image rendering)**
   - Added URL validation: only `http/https` image URLs are passed to Coil.
   - If invalid/blank, it falls back to a known HTTPS placeholder.
   - Added `onError` logging to show which URL failed, so it’s easy to debug scraper output quality.

## 4) Files Touched (for report completeness)
- `closetai/app/src/main/java/com/closetai/app/ui/screens/HomeScreen.kt` (dashboard CTA -> dynamic context dialog)
- `closetai/app/src/main/java/com/closetai/app/ui/screens/RecommendationsScreen.kt` (image guard + logging)
- `closetai/app/src/main/java/com/closetai/app/ui/viewmodel/RecommendationsViewModel.kt` (retry last fetch using last context)
- `closetai/app/src/main/java/com/closetai/app/data/repository/WardrobeRepository.kt` (permission-specific error handling)
- `backend/main.py` (remove test injection, stable HTTPS placeholders)
- `backend/scrapers/ajio.py` (image URL normalization)
- `firestore.rules`, `storage.rules`, `firebase.json` (wardrobe permission fix)


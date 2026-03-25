# Phase 9: Virtual Wardrobe Foundation (Storage & UI)

## Context
Phase 8 integrated real-time location. Now in Phase 9, we must build the foundation for the Virtual Wardrobe. This involves capturing clothing items with the camera (or gallery) and uploading them to Firebase Cloud Storage.

## Objectives
1. Add Firebase Cloud Storage for storing user clothing images.
2. Build the "My Wardrobe" Android screen with a grid layout.
3. Add a floating action button (FAB) to launch a bottom sheet / dialog to Add Item (Camera vs Gallery).
4. Implement Image compression and upload logic to Firebase Storage (`users/{uid}/wardrobe/`).
5. Save the image URL & basic metadata (Category, Color) into a new Firestore subcollection.

## Step-by-Step Execution Plan

### Step 1: Dependencies & Firebase Setup
- Add `com.google.firebase:firebase-storage` to `libs.versions.toml` and `build.gradle.kts`.
- Add `io.coil-kt:coil-compose` for asynchronous image loading from URLs.
- *Wait, actually, I also need to ensure CameraX or basic intent is available.* Since it's MVP, we will use `ActivityResultContracts.TakePicturePreview` (Bitmap) or `GetContent` (Uri) to keep it simple and native.

### Step 2: Data Models (Android)
- Create `WardrobeItem` data class:
  ```kotlin
  data class WardrobeItem(
      val id: String = "",
      val imageUrl: String = "",
      val category: String = "",
      val color: String = "",
      val addedAt: Long = System.currentTimeMillis()
  )
  ```
- Create `WardrobeRepository` to handle Storage uploads and Firestore writes.

### Step 3: ViewModels
- Create `WardrobeViewModel` to manage state (Loading, Success, Uploading, Error).
- Expose methods: `fetchWardrobeItems()`, `uploadItem(bitmap/uri, category, color)`.

### Step 4: UI Implementation
- Update `Screen.kt` to include `Screen.Wardrobe`.
- Update `HomeScreen.kt` to add a bottom navigation bar, moving from an empty placeholder to a real app flow (Home, Wardrobe, Profile).
- Build `WardrobeScreen.kt` featuring a `LazyVerticalGrid` to display uploaded items using `AsyncImage` (Coil).
- Implement an "Add Item" bottom sheet asking for Category and Color before saving.

### Step 5: Backend Preparation
- Python backend won't need immediate changes in Phase 9, as Firebase handles storage. However, we'll keep it in mind for Phase 10 (Outfit Generation).

## Definition of Done
- User can navigate to the Wardrobe tab.
- User can capture/select a photo, add a category/color, and click "Save".
- The image uploads to Firebase Storage, the metadata saves to Firestore, and the Grid refreshes to show the new item.

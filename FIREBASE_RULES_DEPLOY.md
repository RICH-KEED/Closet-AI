# Firebase Rules Deployment

ClosetAI wardrobe paths used by the app:

- Firestore: `users/{uid}/wardrobe/{itemId}`
- Storage: `users/{uid}/wardrobe/{fileName}`

These rules are now versioned in:

- `firestore.rules`
- `storage.rules`
- `firebase.json`

## Deploy

From project root:

```bash
firebase login
firebase use <your-firebase-project-id>
firebase deploy --only firestore:rules,storage
```

## Verify

1. Sign in on Android app.
2. Upload one wardrobe item.
3. Open wardrobe list and confirm item appears.
4. Confirm no `PERMISSION_DENIED` errors.

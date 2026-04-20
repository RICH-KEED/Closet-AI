# ClosetAI Expo Deck Source

## 1. Project Title
ClosetAI: Your AI Stylist for Smart Fashion Discovery

Tagline: Personalized fashion recommendations from live e-commerce catalogs, tailored to your body profile, style, budget, and occasion.

---

## 2. Problem Statement
Fashion discovery today is broken for everyday users:

- Too many options across different shopping apps and websites
- Generic recommendations that ignore body type, fit, and personal style
- Time-consuming browsing with low confidence in final choices
- No unified place to combine wardrobe context with shopping discovery

What this causes:

- Decision fatigue
- Poor purchase satisfaction
- Higher return likelihood
- Wasted time for students and working professionals

---

## 3. Our Solution
ClosetAI is a mobile-first AI fashion recommendation system that:

- Understands user preferences through structured onboarding
- Uses context like occasion, budget, and weather
- Pulls fresh product data from fashion platforms
- Ranks products using a transparent match-scoring engine
- Learns from user feedback (like/dislike)

Result: fewer but better recommendations with clear reasoning.

---

## 4. Target Users
Primary audience:

- Age 16 to 40
- Android-first users
- College students and young professionals
- Users who want quick, confident outfit choices

Initial market focus:

- India-focused fashion ecosystem
- Platform coverage includes Myntra, Amazon, Ajio, and Nykaa in project scope

---

## 5. Methodology
Our implementation methodology combines product-first and engineering-first thinking:

1. Define structured user signals
- Gender, body type, size, colors, style preferences, budget, occasions, climate

2. Build robust data ingestion
- Scraper connectors, retries, delay strategy, parser fallback behavior

3. Apply recommendation scoring
- Weighted match logic for style, color, and brand
- Optional weather-aware adjustments
- Feedback-based reranking

4. Optimize response speed and reliability
- Cache recommendations with profile-context hashing
- Use pagination and platform diversification
- Graceful fallback for empty or missing fields

5. Close the loop with interaction feedback
- Like/dislike actions update user preference signal quality

---

## 6. System Architecture
High-level architecture:

- Android App (Jetpack Compose)
- Firebase Authentication (Google Sign-In)
- Firestore (user profile and wardrobe metadata)
- FastAPI backend for recommendation orchestration
- Scraper layer for live product extraction
- Redis cache for faster repeated recommendations
- Optional weather and try-on services

Data flow:

1. User signs in and completes onboarding
2. App sends profile + request context to backend
3. Backend fetches candidate items
4. MatchScorer ranks and diversifies results
5. Results are returned with reasons and scores
6. User actions feed back into reranking logic

---

## 7. AI and Recommendation Logic
Core ranking design in current implementation:

- Style match has highest weight
- Color match has medium weight
- Brand signal has lower weight
- Weather contributes bonuses or penalties when context is available
- User likes and dislikes apply lightweight personalization boosts

Important behavior:

- Scores are normalized for UI consistency
- If confidence is low, system still returns useful fallback recommendations
- Match reasons are attached to every recommendation to keep output explainable

---

## 8. What Is Implemented Now
Mobile side:

- Authentication and onboarding journey
- Home, recommendations, saved items, wardrobe, settings, try-ons screens
- Recommendation API integration with retry and load-more patterns
- Wardrobe upload pipeline to backend + Firestore metadata

Backend side:

- Recommendation endpoint
- Feedback endpoint
- Saved recommendations endpoint
- Wardrobe image upload endpoint
- Try-on endpoint integration path
- Caching and weather integration support

Infrastructure side:

- Firebase rules for user-owned wardrobe data
- Android permissions and networking baseline

---

## 9. Innovation Highlights
Why ClosetAI is compelling for judges:

- Practical personalization using structured profile + real-time context
- Explainable recommendations, not black-box output
- Multi-source ingestion architecture instead of single-catalog dependency
- End-to-end product: app UX + backend intelligence + cloud storage security
- Clear path from MVP to advanced features like virtual try-on and deeper personalization

---

## 10. Technical Stack
Frontend:

- Kotlin
- Jetpack Compose
- Retrofit
- Coil
- Firebase Android SDK

Backend:

- Python
- FastAPI
- Async scraping and parsing libraries
- Redis caching

Cloud and Data:

- Firebase Authentication
- Firestore
- Firebase Storage rules model

---

## 11. Challenges and Mitigation
Challenge: Scraper instability due to source website changes
Mitigation: retries, fallback parsing, modular scraper design

Challenge: Image quality and broken links
Mitigation: URL normalization and placeholder fallback strategy

Challenge: Cold-start users with limited preference history
Mitigation: onboarding depth + broad but constrained ranking fallback

Challenge: Performance for repeated requests
Mitigation: profile-context cache keying + paginated delivery

---

## 12. Evaluation and Success Criteria
User-level outcomes:

- Time to first useful recommendation
- Relevance of recommendations (qualitative and feedback-driven)
- Reduction in browsing friction

System-level outcomes:

- Recommendation endpoint latency
- Cache hit rate
- Error rate from scraping pipeline
- Feedback action volume per user

MVP success definition:

- User can onboard, request recommendations, view explainable results, save preferences, and manage wardrobe context in one connected flow.

---

## 13. Roadmap
Near-term:

- Harden multi-platform scraping reliability
- Improve feedback loop weighting using behavior trends
- Better wardrobe-to-recommendation influence

Mid-term:

- Expand virtual try-on quality and reliability
- Add stronger occasion and weather intelligence
- Improve recommendation diversity control

Long-term:

- Personal stylist assistant flows
- Marketplace partnerships and affiliate model options
- Multi-region scaling with localized catalogs

---

## 14. Demo Script for Judges (3 to 5 minutes)
1. Sign in with Google
2. Show onboarding profile inputs and why they matter
3. Trigger recommendations using dynamic context (occasion and budget)
4. Open recommendation cards and explain match reasons
5. Like and dislike a few items
6. Refresh recommendations to show personalization behavior
7. Add a wardrobe item and show how ecosystem is connected
8. Briefly show try-on flow as future-forward capability

---

## 15. Slide Conversion Plan
Use this section-to-slide mapping when converting to PPT:

- Slide 1: Title and tagline
- Slide 2: Problem statement
- Slide 3: Solution overview
- Slide 4: Target users
- Slide 5: Methodology
- Slide 6: Architecture diagram
- Slide 7: AI ranking logic
- Slide 8: Implemented features
- Slide 9: Innovation highlights
- Slide 10: Tech stack
- Slide 11: Challenges and mitigation
- Slide 12: Metrics and success criteria
- Slide 13: Roadmap
- Slide 14: Live demo script
- Slide 15: Closing and Q and A

---

## 16. Evidence Base Used for This Expo Narrative
Primary project understanding was synthesized from product docs, backend code, Android navigation and repositories, and Firebase rules.

Suggested evidence files to cite verbally during judging:

- prd.md
- MVP.md
- WORK.md
- backend/main.py
- backend/recommender.py
- backend/models.py
- backend/caching.py
- backend/scrapers/myntra.py
- backend/scrapers/amazon.py
- backend/scrapers/ajio.py
- closetai/app/src/main/java/com/closetai/app/navigation/NavGraph.kt
- closetai/app/src/main/java/com/closetai/app/data/api/ClosetAiApi.kt
- closetai/app/src/main/java/com/closetai/app/data/repository/WardrobeRepository.kt
- firestore.rules
- storage.rules

---

## 17. Closing One-Liner
ClosetAI turns scattered fashion browsing into a guided, intelligent, and explainable styling experience built for real users and real shopping behavior.

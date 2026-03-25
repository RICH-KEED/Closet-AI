# Phase 6: Recommendation Engine & Caching (Python)

> **Milestone**: Android & Python Recommender MVP
> **Objective**: Implement Redis caching. Build the scoring logic (matching by style 80%, color 15%, brand 5%) and update the `POST /api/v1/recommendations` endpoint.

## Requirements

The codebase must implement the recommendation logic matching the requirements from `MVP.MD`.
- User profiles must be passed to the scraping hooks, filtered, and scored.
- Results should be cached in Redis with a TTL (e.g., 1 hour) based on user profile hash to avoid re-scraping the same preferences immediately.
- The scoring algorithm: Match style (80%), color (15%), brand (5%).

## Execution Plan

### 1. Redis Configuration & Dependency
- **Target File**: `backend/caching.py`
- **Details**:
  - Implement a `RedisCache` utility class wrapper using `redis.asyncio` or standard `redis`.
  - Connect via `REDIS_URL` in `.env`.
  - Create functions `get_cached_recommendations(profile_hash)` and `set_cached_recommendations(profile_hash, results)`.

### 2. Implement Matching & Scoring Logic
- **Target File**: `backend/recommender.py`
- **Details**:
  - Create `MatchScorer` class.
  - Implement `score_product(product, user_profile)`.
  - Normalize text (e.g., `product.title.lower()` vs `user_profile.styles`).
  - Calculate logic: Style hit (+80), Color hit (+15), Brand hit (+5).
  - Add logic to filter out entirely unmatched products or those not matching size/gender.

### 3. Update FastAPI Endpoint
- **Target File**: `backend/main.py`
- **Details**:
  - Generate a unique hash of the `user_profile` + `context`.
  - Check Redis cache first. If hit, return JSON.
  - If miss: Await `ScraperManager.fetch_recommendations`.
  - Pass the scraped array to `MatchScorer.rank_products()`.
  - Trim the results to top 15 diversified items (limit 3 per platform max).
  - Cache results in Redis.
  - Return the final JSON.

## Context Needed for Execution
- **`MVP.MD`**: Ensures we're adhering strictly to the required scoring weights and limits.

## Technical Debt / Risks
- Redis must be running locally (`localhost:6379`) or via Docker for this to work. We must instruct the user to ensure Redis is active.
- Exact styling terms might differ across e-commerce sites. Basic string matching (`in`) will be used for Phase 6 MVP to keep it simple but functional.

## State Update
Upon completing these steps, update `STATE.md` to reflect Phase 6 completion and progress to Phase 7.

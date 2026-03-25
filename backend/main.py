from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from models import RecommendationRequest, ProductRecommendation, UserProfile
from scrapers.ajio import AjioScraper
from scrapers.amazon import AmazonScraper
from scrapers.hm import HMScraper
from scrapers.myntra import MyntraScraper
import hashlib
from caching import redis_cache
from recommender import MatchScorer
from weather import weather_service
from typing import List
import os
import asyncio
import logging
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="ClosetAI Recommender API",
    description="Backend service for fetching personalized fashion recommendations",
    version="1.0.0"
)

# Increment when recommendation payload shape/logic changes.
# Prevents stale Redis cache entries from breaking the client after scraper/API fixes.
CACHE_VERSION = os.getenv("RECOMMENDER_CACHE_VERSION", "v3")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health_check():
    return {"status": "ok", "environment": os.getenv("ENVIRONMENT", "development")}

@app.post("/api/v1/recommendations", response_model=List[ProductRecommendation])
async def get_recommendations(request: RecommendationRequest):
    """
    Fetches real-world clothing recommendations based on the user's profile
    and contextual data (weather, occasion, etc).
    """
    profile = request.user_profile
    context = request.context or {}
    
    # 1. Generate a unique hash of the user_profile + context
    hash_input = f"{CACHE_VERSION}|{profile.model_dump_json()}|{context}"
    profile_hash = "rec:" + hashlib.md5(hash_input.encode()).hexdigest()
    
    # 2. Check Redis cache first
    cached = await redis_cache.get_cached_recommendations(profile_hash)
    if cached:
        return cached
    
    scrapers = [HMScraper(), MyntraScraper(), AmazonScraper(), AjioScraper()]
    
    # Run all scrapers concurrently
    # Pass user context through to scraper logic
    tasks = [scraper.scrape(profile, context) for scraper in scrapers]
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    all_recommendations = []
    logger = logging.getLogger(__name__)
    
    for i, res in enumerate(results):
        if isinstance(res, Exception):
            logger.error(f"Scraper error: {res}")
        else:
            all_recommendations.extend(res)
    
    # 3. Fetch weather if location context exists
    current_weather = None
    if "lat" in context and "lon" in context:
        try:
            lat = float(context["lat"])
            lon = float(context["lon"])
            current_weather = await weather_service.get_weather(lat, lon)
        except ValueError:
            logger.warning("Invalid lat/lon format in context.")
            
    # 4. Pass the scraped array to MatchScorer.rank_products()
    ranked = MatchScorer.rank_products(all_recommendations, profile, current_weather)
    
    # 5. Trim the results to top 40 diversified items (limit 10 per platform max)
    final_results = []
    platform_counts = {}
    for item in ranked:
        p = item.platform
        count = platform_counts.get(p, 0)
        if count < 10:
            final_results.append(item)
            platform_counts[p] = count + 1
        if len(final_results) >= 40:
            break
            
    # 6. Cache results in Redis
    # Safety: ensure UI never receives blank `image_url`.
    # Use a stable HTTPS placeholder for client compatibility.
    placeholder_image_url = (
        "https://via.placeholder.com/600x800.png?text=ClosetAI"
    )
    for item in final_results:
        if not getattr(item, "image_url", None) or not str(item.image_url).strip():
            item.image_url = placeholder_image_url

    await redis_cache.set_cached_recommendations(profile_hash, final_results)
    
    return final_results

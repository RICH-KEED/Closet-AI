from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from models import RecommendationRequest, ProductRecommendation, UserProfile, RecommendationFeedbackRequest, TryOnResponse
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
from curl_cffi.requests import AsyncSession
import tempfile
import base64
import httpx

try:
    from gradio_client import Client as GradioClient, file as gradio_file
except Exception:
    GradioClient = None  # type: ignore[assignment]
    gradio_file = None  # type: ignore[assignment]

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

@app.get("/api/v1/scrapers/connectivity")
async def scrapers_connectivity():
    """
    Quick connectivity check to external fashion websites used by scrapers.
    Useful when scrapers return 0 results due to network blocking/changes.
    """
    targets = {
        "myntra": "https://www.myntra.com",
    }

    results = {}
    async with AsyncSession(impersonate="chrome110") as session:
        for name, url in targets.items():
            try:
                resp = await session.get(url, timeout=10)
                results[name] = {"ok": resp.status_code == 200, "status_code": resp.status_code}
            except Exception as e:
                results[name] = {"ok": False, "error": str(e)}

    return results

@app.post("/api/v1/recommendations/feedback")
async def set_recommendation_feedback(request: RecommendationFeedbackRequest):
    """
    Stores like/dislike feedback for later re-ranking.
    """
    await redis_cache.set_user_feedback(
        user_uid=request.user_uid,
        product_id=request.product_id,
        action=request.action
    )
    snapshot = None
    if (request.action or "").strip().lower() == "like":
        snapshot = {
            "id": request.product_id,
            "title": request.title or "Saved item",
            "brand": request.brand or "Unknown",
            "price": float(request.price or 0.0),
            "image_url": request.image_url or "https://via.placeholder.com/600x800.png?text=ClosetAI",
            "product_url": request.product_url or "",
            "platform": request.platform or "unknown",
            "match_score": float(request.match_score or 0.0),
            "match_reasons": request.match_reasons or [],
        }
    await redis_cache.set_saved_item_snapshot(
        user_uid=request.user_uid,
        product_id=request.product_id,
        action=request.action,
        snapshot=snapshot,
    )
    version = await redis_cache.get_feedback_version(request.user_uid)
    return {"status": "ok", "feedback_version": version}

@app.get("/api/v1/recommendations/saved/{user_uid}", response_model=List[ProductRecommendation])
async def get_saved_recommendations(user_uid: str):
    return await redis_cache.get_saved_items(user_uid)

@app.post("/api/v1/recommendations", response_model=List[ProductRecommendation])
async def get_recommendations(request: RecommendationRequest):
    """
    Fetches real-world clothing recommendations based on the user's profile
    and contextual data (weather, occasion, etc).
    """
    profile = request.user_profile
    context = request.context or {}
    offset = max(0, request.offset)
    limit = min(max(1, request.limit), 50)
    
    # 1. Generate a unique hash of the user_profile + context
    hash_input = f"{CACHE_VERSION}|{profile.model_dump_json()}|{context}"
    profile_hash = "rec:" + hashlib.md5(hash_input.encode()).hexdigest()
    
    # 2. Check Redis cache first (cache the scraped candidates, not the paginated slice)
    cached_candidates = await redis_cache.get_cached_recommendations(profile_hash)
    if cached_candidates:
        all_recommendations = cached_candidates
    else:
        # Myntra-only mode (temporary) because other sources are unstable on current network.
        scrapers = [MyntraScraper()]

        # Run all scrapers concurrently
        # Pass user context through to scraper logic
        tasks = [scraper.scrape(profile, context) for scraper in scrapers]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        all_recommendations = []
        logger = logging.getLogger(__name__)

        for res in results:
            if isinstance(res, Exception):
                logger.error(f"Scraper error: {res}", exc_info=True)
            else:
                all_recommendations.extend(res)

        # Cache scraped candidates for fast "Load more" re-ranking
        await redis_cache.set_cached_recommendations(profile_hash, all_recommendations)
    
    # 3. Fetch weather if location context exists
    current_weather = None
    if "lat" in context and "lon" in context:
        try:
            lat = float(context["lat"])
            lon = float(context["lon"])
            current_weather = await weather_service.get_weather(lat, lon)
        except ValueError:
            logger.warning("Invalid lat/lon format in context.")
            
    # 4. Load user feedback (liked/disliked product ids) for re-ranking.
    liked_ids, disliked_ids = await redis_cache.get_user_feedback_sets(profile.uid)
    feedback_version = await redis_cache.get_feedback_version(profile.uid)

    # Cache re-ranked + diversified results per feedback version.
    ranked_cache_key = f"{profile_hash}:rank:v{feedback_version}"
    cached_ranked = await redis_cache.get_cached_recommendations(ranked_cache_key)

    if cached_ranked:
        final_results = cached_ranked
    else:
        ranked = MatchScorer.rank_products(
            all_recommendations,
            profile,
            current_weather,
            liked_product_ids=liked_ids,
            disliked_product_ids=disliked_ids,
        )

        # 5. Diversify + cap total so the UI never gets overwhelmed.
        diversified = []
        platform_counts = {}
        MAX_TOTAL = 200
        MAX_PER_PLATFORM = 50
        for item in ranked:
            p = item.platform
            count = platform_counts.get(p, 0)
            if count < MAX_PER_PLATFORM:
                diversified.append(item)
                platform_counts[p] = count + 1
            if len(diversified) >= MAX_TOTAL:
                break

        # Safety: if ranking/diversification resulted in empty but scrapers produced candidates,
        # fall back to the raw scraped list so the UI never shows an empty state.
        if not diversified and all_recommendations:
            fallback = []
            platform_counts = {}
            for item in all_recommendations:
                p = item.platform
                count = platform_counts.get(p, 0)
                if count < MAX_PER_PLATFORM:
                    item.match_score = 0.0
                    if not getattr(item, "match_reasons", None):
                        item.match_reasons = ["Setting up your style profile"]
                    fallback.append(item)
                    platform_counts[p] = count + 1
                if len(fallback) >= MAX_TOTAL:
                    break
            diversified = fallback

        final_results = diversified

        # Cache the diversified ranked list so "Load more" is fast.
        await redis_cache.set_cached_recommendations(ranked_cache_key, final_results)

    # 6. Return only the requested page slice
    placeholder_image_url = (
        "https://via.placeholder.com/600x800.png?text=ClosetAI"
    )
    page = final_results[offset: min(offset + limit, len(final_results))]

    # Safety: ensure UI never receives blank `image_url` (fix only the returned slice)
    for item in page:
        if not getattr(item, "image_url", None) or not str(item.image_url).strip():
            item.image_url = placeholder_image_url

    return page


@app.post("/api/v1/tryon", response_model=TryOnResponse)
async def try_on(
    user_image: UploadFile = File(...),
    garment_image_url: str = Form(...),
    garment_des: str = Form(""),
    denoise_steps: int = Form(30),
    seed: int = Form(42),
):
    """
    Virtual try-on via a Hugging Face Space cloned by the user.
    - user_image: selfie uploaded once on device
    - garment_image_url: image URL from scraped recommendation
    - garment_des: short description hint (optional)
    Returns base64 PNG/JPEG for direct display in the app.
    """
    space_id = os.getenv("TRYON_SPACE_ID", "").strip()
    hf_token = os.getenv("HF_TOKEN", "").strip()
    if not space_id:
        raise HTTPException(status_code=500, detail="TRYON_SPACE_ID is not configured")
    if GradioClient is None or gradio_file is None:
        raise HTTPException(status_code=500, detail="gradio_client is not installed on the backend")

    # Download garment image and persist both inputs to temp files for gradio_client.
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            garment_resp = await client.get(garment_image_url)
            garment_resp.raise_for_status()
            garment_bytes = garment_resp.content
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to fetch garment image: {e}")

    try:
        user_bytes = await user_image.read()
        if not user_bytes:
            raise HTTPException(status_code=400, detail="Empty user_image")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to read user_image: {e}")

    logger = logging.getLogger(__name__)

    with tempfile.TemporaryDirectory() as td:
        user_path = os.path.join(td, "user.jpg")
        garment_path = os.path.join(td, "garment.jpg")
        with open(user_path, "wb") as f:
            f.write(user_bytes)
        with open(garment_path, "wb") as f:
            f.write(garment_bytes)

        try:
            gr_client = GradioClient(space_id, hf_token=hf_token or None)
            result = gr_client.predict(
                dict={"background": gradio_file(user_path), "layers": [], "composite": None},
                garm_img=gradio_file(garment_path),
                garment_des=garment_des or "",
                is_checked=True,
                # Myntra gives "model-wearing" images; enabling crop usually helps
                # the Space isolate the garment area better.
                is_checked_crop=True,
                denoise_steps=float(denoise_steps),
                seed=float(seed),
                api_name="/tryon",
            )
        except Exception as e:
            logger.error(f"Try-on generation failed: {e}", exc_info=True)
            raise HTTPException(status_code=502, detail=f"Try-on generation failed: {e}")

        # result is typically a tuple/list of file paths returned by Gradio.
        try:
            output_path = result[0] if isinstance(result, (list, tuple)) else result
            if not output_path:
                raise ValueError("Empty output path from Space")
            with open(output_path, "rb") as f:
                out_bytes = f.read()
        except Exception as e:
            raise HTTPException(status_code=502, detail=f"Failed to read generated output: {e}")

        encoded = base64.b64encode(out_bytes).decode("utf-8")
        return TryOnResponse(status="ok", image_base64=encoded)

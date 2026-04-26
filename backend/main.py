from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
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
import json
from io import BytesIO

try:
    from PIL import Image
except Exception:
    Image = None  # type: ignore[assignment]

try:
    from gradio_client import Client as GradioClient, handle_file as gradio_file
    from gradio_client.exceptions import AppError as GradioAppError
except Exception:
    GradioClient = None  # type: ignore[assignment]
    gradio_file = None  # type: ignore[assignment]
    GradioAppError = None  # type: ignore[assignment]

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

# Static file serving for uploaded wardrobe images (avoids Firebase Storage).
_BACKEND_DIR = os.path.dirname(__file__)
UPLOADS_DIR = os.getenv("UPLOADS_DIR", os.path.join(_BACKEND_DIR, "uploads"))
WARDROBE_UPLOAD_SUBDIR = "wardrobe"
WARDROBE_UPLOAD_DIR = os.path.join(UPLOADS_DIR, WARDROBE_UPLOAD_SUBDIR)
os.makedirs(WARDROBE_UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOADS_DIR), name="uploads")

@app.get("/health")
async def health_check():
    return {"status": "ok", "environment": os.getenv("ENVIRONMENT", "development")}


def _bytes_look_like_image(data: bytes) -> bool:
    """Accept octet-stream uploads when bytes are clearly JPEG/PNG/WebP."""
    if len(data) < 12:
        return False
    if data[:3] == b"\xff\xd8\xff":
        return True
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return True
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return True
    return False


@app.post("/api/v1/wardrobe/upload")
async def upload_wardrobe_image(request: Request, image: UploadFile = File(...)):
    """
    Upload wardrobe image to backend storage and return a public URL.
    Android stores this URL inside Firestore as WardrobeItem.imageUrl.
    """
    ext = os.path.splitext(image.filename or "")[1].lower()
    if ext not in (".jpg", ".jpeg", ".png", ".webp"):
        ext = ".jpg"

    filename = f"{hashlib.sha256(os.urandom(32)).hexdigest()}{ext}"
    dst_path = os.path.join(WARDROBE_UPLOAD_DIR, filename)

    try:
        data = await image.read()
        if not data:
            raise HTTPException(status_code=400, detail="Empty image upload")
        content_type = (image.content_type or "").lower().strip()
        looks_image = content_type.startswith("image/")
        if not looks_image and content_type in ("application/octet-stream", "binary/octet-stream", ""):
            looks_image = _bytes_look_like_image(data)
        if not looks_image:
            raise HTTPException(
                status_code=400,
                detail=f"Expected image upload, got content_type={image.content_type!r}",
            )
        with open(dst_path, "wb") as f:
            f.write(data)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to save image: {e}")

    public_base = (os.getenv("PUBLIC_BASE_URL") or "").strip().rstrip("/")
    if public_base:
        base = public_base
    else:
        base = str(request.base_url).rstrip("/")
    url = f"{base}/uploads/{WARDROBE_UPLOAD_SUBDIR}/{filename}"
    return {"status": "ok", "image_url": url}

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
    
    # 1. Generate a unique hash of the user_profile + context.
    # IMPORTANT: make this stable across requests so Redis caching actually hits.
    # - sort context keys
    # - sort wardrobe items (order may vary client-side)
    context_stable = json.dumps(context or {}, sort_keys=True, separators=(",", ":"))
    profile_dump = profile.model_dump()
    wardrobe = profile_dump.get("wardrobe") or []
    if isinstance(wardrobe, list):
        try:
            wardrobe_sorted = sorted(
                wardrobe,
                key=lambda w: str((w or {}).get("id", "")) if isinstance(w, dict) else "",
            )
            profile_dump["wardrobe"] = wardrobe_sorted
        except Exception:
            pass
    profile_stable = json.dumps(profile_dump, sort_keys=True, separators=(",", ":"))
    hash_input = f"{CACHE_VERSION}|{profile_stable}|{context_stable}"
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
    is_checked: bool = Form(True),
    is_checked_crop: bool = Form(False),
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
        logger = logging.getLogger(__name__)
        logger.info(f"Try-on: fetching garment image from URL: {garment_image_url}")
        async with httpx.AsyncClient(timeout=60, follow_redirects=True) as client:
            garment_resp = await client.get(garment_image_url)
            garment_resp.raise_for_status()
            garment_bytes = garment_resp.content
        logger.info(f"Try-on: fetched garment image bytes: {len(garment_bytes)} bytes")
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

    def _gradio_error_detail(err: Exception) -> str:
        """
        gradio_client.exceptions.AppError often stringifies to just `'RuntimeError'`.
        Extract richer fields (message/status/payload) when present.
        """
        parts: list[str] = []
        try:
            parts.append(repr(err))
        except Exception:
            parts.append(str(err))

        for attr in ("message", "status", "code", "type"):
            try:
                val = getattr(err, attr, None)
            except Exception:
                val = None
            if val not in (None, "", "None"):
                parts.append(f"{attr}={val}")

        # Some versions keep the original server payload in __dict__.
        try:
            payload = getattr(err, "__dict__", None)
            if isinstance(payload, dict) and payload:
                # Avoid huge blobs; keep only likely-useful keys.
                keep_keys = ("message", "status", "code", "type", "detail", "data")
                slim = {k: payload.get(k) for k in keep_keys if k in payload}
                if slim:
                    parts.append(f"payload={slim}")
        except Exception:
            pass

        # Prefer the best human-readable message if available.
        try:
            msg = getattr(err, "message", None)
            if isinstance(msg, str) and msg.strip():
                return msg.strip()
        except Exception:
            pass

        return " | ".join(p for p in parts if p)

    # If scraped garment images cause Space runtime errors (common when they are model-wearing photos),
    # fall back to an IDM-VTON example cloth image for demo reliability.
    # Set TRYON_ENABLE_CLOTH_FALLBACK=0 to disable.
    cloth_fallback_enabled = (os.getenv("TRYON_ENABLE_CLOTH_FALLBACK", "1").strip() != "0")
    example_cloth_url = os.getenv(
        "TRYON_EXAMPLE_CLOTH_URL",
        "https://huggingface.co/spaces/yisol/IDM-VTON/resolve/main/example/cloth/09133_00.jpg",
    ).strip()

    def _normalize_image_bytes(raw: bytes, max_side: int) -> bytes:
        """
        Best-effort: normalize to RGB JPEG and cap size to reduce Space runtime failures.
        If Pillow isn't available, return the raw bytes.
        """
        if Image is None:
            return raw
        try:
            img = Image.open(BytesIO(raw))
            img = img.convert("RGB")
            w, h = img.size
            scale = min(1.0, float(max_side) / float(max(w, h)))
            if scale < 1.0:
                img = img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
            out = BytesIO()
            img.save(out, format="JPEG", quality=90, optimize=True)
            return out.getvalue()
        except Exception:
            return raw

    # Normalize inputs to reduce Space 'RuntimeError' likelihood.
    user_bytes = _normalize_image_bytes(user_bytes, max_side=1024)
    garment_bytes = _normalize_image_bytes(garment_bytes, max_side=1024)

    with tempfile.TemporaryDirectory() as td:
        user_path = os.path.join(td, "user.jpg")
        garment_path = os.path.join(td, "garment.jpg")
        with open(user_path, "wb") as f:
            f.write(user_bytes)
        with open(garment_path, "wb") as f:
            f.write(garment_bytes)
        logger.info(f"Try-on: saved user image to {user_path}")
        logger.info(f"Try-on: saved garment image to {garment_path}")

        try:
            # gradio_client supports `token=` for private HF Spaces.
            logger.info(
                "Try-on: calling HF Space '%s' /tryon with is_checked=%s, is_checked_crop=%s, "
                "denoise_steps=%s, seed=%s, cloth_fallback_enabled=%s",
                space_id,
                is_checked,
                is_checked_crop,
                denoise_steps,
                seed,
                cloth_fallback_enabled,
            )
            gr_client = GradioClient(space_id, token=hf_token or None)
            last_err: Exception | None = None
            for attempt in range(1, 3):
                try:
                    result = gr_client.predict(
                        dict={"background": gradio_file(user_path), "layers": [], "composite": None},
                        garm_img=gradio_file(garment_path),
                        garment_des=garment_des or "",
                        is_checked=bool(is_checked),
                        is_checked_crop=bool(is_checked_crop),
                        denoise_steps=int(denoise_steps),
                        seed=int(seed),
                        api_name="/tryon",
                    )
                    last_err = None
                    break
                except Exception as e:
                    last_err = e
                    # transient errors are common on Spaces (cold start / zerogpu / runtime)
                    if attempt < 2:
                        await asyncio.sleep(1.0 * attempt)
                        continue
            if last_err is not None:
                raise last_err
        except Exception as e:
            logger.error(f"Try-on generation failed: {_gradio_error_detail(e)}", exc_info=True)
            detail = _gradio_error_detail(e)

            # Demo-safe fallback: if Space throws RuntimeError for the scraped garment image,
            # retry once with a known-good example cloth image.
            if cloth_fallback_enabled and ("RuntimeError" in detail or "IndexError" in detail):
                try:
                    logger.warning("Try-on failed for garment image; retrying with example cloth fallback.")
                    async with httpx.AsyncClient(timeout=60, follow_redirects=True) as client:
                        fb = await client.get(example_cloth_url)
                        fb.raise_for_status()
                        fb_bytes = _normalize_image_bytes(fb.content, max_side=1024)
                    with open(garment_path, "wb") as f:
                        f.write(fb_bytes)
                    result = gr_client.predict(
                        dict={"background": gradio_file(user_path), "layers": [], "composite": None},
                        garm_img=gradio_file(garment_path),
                        garment_des=(garment_des or "") + " (fallback cloth)",
                        is_checked=True,
                        is_checked_crop=True,
                        denoise_steps=float(denoise_steps),
                        seed=float(seed),
                        api_name="/tryon",
                    )
                except Exception as fb_err:
                    logger.error(f"Try-on fallback also failed: {_gradio_error_detail(fb_err)}", exc_info=True)
                    raise HTTPException(status_code=502, detail=f"Try-on generation failed: {detail}")
            else:
                raise HTTPException(status_code=502, detail=f"Try-on generation failed: {detail}")

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

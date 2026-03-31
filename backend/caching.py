import json
import os
import logging
from redis.asyncio import Redis, from_url
from typing import List, Optional, Set, Dict, Tuple
from models import ProductRecommendation

logger = logging.getLogger(__name__)

class RedisCache:
    def __init__(self):
        redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self.redis: Optional[Redis] = None
        self.ttl = 3600  # 1 hour
        try:
            self.redis = from_url(redis_url, decode_responses=True)
            logger.info(f"Initialized Redis cache at {redis_url}")
        except Exception as e:
            logger.error(f"Failed to initialize Redis cache: {e}")

    async def get_cached_recommendations(self, profile_hash: str) -> Optional[List[ProductRecommendation]]:
        if not self.redis:
            return None
        try:
            cached_data = await self.redis.get(profile_hash)
            if cached_data:
                logger.info(f"Cache hit for hash {profile_hash}")
                data_list = json.loads(cached_data)
                return [ProductRecommendation(**item) for item in data_list]
            return None
        except Exception as e:
            pass # Suppress logging if Redis is unavailable
            return None

    async def set_cached_recommendations(self, profile_hash: str, results: List[ProductRecommendation]):
        if not self.redis:
            return
        try:
            data_list = [item.model_dump() for item in results]
            await self.redis.setex(name=profile_hash, time=self.ttl, value=json.dumps(data_list))
            # logger.info(f"Successfully cached recommendations for hash {profile_hash}")
        except Exception as e:
            pass # Suppress logging if Redis is unavailable

    async def set_user_feedback(self, user_uid: str, product_id: str, action: str) -> None:
        liked_key = f"feedback:{user_uid}:liked"
        disliked_key = f"feedback:{user_uid}:disliked"
        version_key = f"feedback:{user_uid}:version"

        normalized = (action or "").strip().lower()
        if normalized not in {"like", "dislike"}:
            return

        try:
            if self.redis:
                if normalized == "like":
                    await self.redis.sadd(liked_key, product_id)
                    await self.redis.srem(disliked_key, product_id)
                else:
                    await self.redis.sadd(disliked_key, product_id)
                    await self.redis.srem(liked_key, product_id)
                await self.redis.incr(version_key)
                return
        except Exception:
            # Fall back to in-memory below.
            pass

        # In-memory fallback (only used if Redis isn't available).
        entry = _IN_MEMORY_FEEDBACK.setdefault(
            user_uid, {"liked": set(), "disliked": set(), "version": 0}
        )
        liked: Set[str] = entry["liked"]  # type: ignore[assignment]
        disliked: Set[str] = entry["disliked"]  # type: ignore[assignment]

        if normalized == "like":
            liked.add(product_id)
            disliked.discard(product_id)
        else:
            disliked.add(product_id)
            liked.discard(product_id)

        entry["version"] = int(entry.get("version", 0)) + 1

    async def set_saved_item_snapshot(
        self,
        user_uid: str,
        product_id: str,
        action: str,
        snapshot: Optional[dict],
    ) -> None:
        key = f"feedback:{user_uid}:saved_items"
        normalized = (action or "").strip().lower()
        if normalized not in {"like", "dislike"}:
            return

        try:
            if self.redis:
                if normalized == "like" and snapshot:
                    await self.redis.hset(key, product_id, json.dumps(snapshot))
                elif normalized == "dislike":
                    await self.redis.hdel(key, product_id)
                return
        except Exception:
            pass

        entry = _IN_MEMORY_FEEDBACK.setdefault(
            user_uid, {"liked": set(), "disliked": set(), "version": 0, "saved_items": {}}
        )
        saved_items: Dict[str, dict] = entry.setdefault("saved_items", {})  # type: ignore[assignment]
        if normalized == "like" and snapshot:
            saved_items[product_id] = snapshot
        elif normalized == "dislike":
            saved_items.pop(product_id, None)

    async def get_user_feedback_sets(self, user_uid: str) -> Tuple[Set[str], Set[str]]:
        liked_key = f"feedback:{user_uid}:liked"
        disliked_key = f"feedback:{user_uid}:disliked"

        if self.redis:
            try:
                liked = await self.redis.smembers(liked_key)
                disliked = await self.redis.smembers(disliked_key)
                return set(liked or []), set(disliked or [])
            except Exception:
                pass

        entry = _IN_MEMORY_FEEDBACK.get(user_uid)
        if not entry:
            return set(), set()
        liked: Set[str] = entry.get("liked", set())  # type: ignore[assignment]
        disliked: Set[str] = entry.get("disliked", set())  # type: ignore[assignment]
        return set(liked), set(disliked)

    async def get_feedback_version(self, user_uid: str) -> int:
        version_key = f"feedback:{user_uid}:version"
        if self.redis:
            try:
                v = await self.redis.get(version_key)
                if v is None:
                    return 0
                return int(v)
            except Exception:
                pass

        entry = _IN_MEMORY_FEEDBACK.get(user_uid)
        if not entry:
            return 0
        return int(entry.get("version", 0))

    async def get_saved_items(self, user_uid: str) -> List[ProductRecommendation]:
        key = f"feedback:{user_uid}:saved_items"
        if self.redis:
            try:
                raw_map = await self.redis.hgetall(key)
                items: List[ProductRecommendation] = []
                for raw in raw_map.values():
                    try:
                        items.append(ProductRecommendation(**json.loads(raw)))
                    except Exception:
                        continue
                return items
            except Exception:
                pass

        entry = _IN_MEMORY_FEEDBACK.get(user_uid, {})
        raw_saved = entry.get("saved_items", {})
        if not isinstance(raw_saved, dict):
            return []
        items: List[ProductRecommendation] = []
        for raw in raw_saved.values():
            if not isinstance(raw, dict):
                continue
            try:
                items.append(ProductRecommendation(**raw))
            except Exception:
                continue
        return items

redis_cache = RedisCache()

# Simple in-memory fallback so local development keeps working even if Redis
# isn't running. This is only used when Redis initialization fails.
_IN_MEMORY_FEEDBACK: Dict[str, Dict[str, object]] = {}


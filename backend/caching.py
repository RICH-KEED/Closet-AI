import json
import os
import logging
from redis.asyncio import Redis, from_url
from typing import List, Optional
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

redis_cache = RedisCache()

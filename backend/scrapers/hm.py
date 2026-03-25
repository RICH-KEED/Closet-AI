import logging
from typing import List
from models import ProductRecommendation, UserProfile

logger = logging.getLogger(__name__)

class HMScraper:
    def __init__(self):
        self.platform = "hm"

    async def scrape(self, user_profile: UserProfile, context: dict) -> List[ProductRecommendation]:
        logger.info("HMScraper: Returning mock recommendations")
        return [
            ProductRecommendation(
                id="hm_mock_1",
                title="H&M Mock Black Shirt",
                brand="H&M",
                price=1299.0,
                image_url="https://via.placeholder.com/300x400?text=HM+Shirt",
                product_url="https://www2.hm.com/en_in/",
                platform="hm",
                match_score=0.85,
                match_reasons=["Matches casual style (Mock)"]
            )
        ]

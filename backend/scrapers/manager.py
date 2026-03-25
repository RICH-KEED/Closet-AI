import asyncio
from typing import List
from models import ProductRecommendation, UserProfile

from .myntra import MyntraScraper
from .amazon import AmazonScraper
from .ajio import AjioScraper
from .nykaa import NykaaScraper

class ScraperManager:
    def __init__(self):
        self.scrapers = [
            MyntraScraper(),
            AmazonScraper(),
            AjioScraper(),
            NykaaScraper()
        ]

    async def fetch_recommendations(self, user_profile: UserProfile, context: dict) -> List[ProductRecommendation]:
        """
        Orchestrates scraping across multiple platforms concurrently.
        Respects basic rate-limiting inside individual scraper classes.
        """
        tasks = []
        for scraper in self.scrapers:
            tasks.append(scraper.scrape(user_profile, context))
            
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        all_recommendations = []
        for result in results:
            if isinstance(result, Exception):
                # Log the scraping failure but continue collecting others
                print(f"Scraper failed with: {result}")
            elif isinstance(result, list):
                all_recommendations.extend(result)
                
        return all_recommendations

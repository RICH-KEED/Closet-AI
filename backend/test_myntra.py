import asyncio
import logging
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))
logging.basicConfig(level=logging.INFO)

from models import UserProfile
from scrapers.myntra import MyntraScraper

async def main():
    profile = UserProfile(
        uid="test",
        gender="female",
        bodyType="hourglass",
        clothingSize="M",
        budget="under2000",
        styles=["casual", "ethnic"],
        favoriteColors=["blue", "black"],
        occasions=["party"]
    )

    scraper = MyntraScraper()
    results = await scraper.scrape(profile, {})
    print(f"\n✅ Got {len(results)} results from Myntra\n")
    for r in results[:5]:
        print(f"  [{r.match_score}] {r.brand} - {r.title[:50]} | ₹{r.price}")
        print(f"       {r.product_url[:70]}")
        print(f"       Reasons: {', '.join(r.match_reasons)}")
        print()

if __name__ == "__main__":
    asyncio.run(main())

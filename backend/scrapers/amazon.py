import logging
from typing import List, Optional, Tuple
from bs4 import BeautifulSoup
from curl_cffi.requests import AsyncSession
from models import ProductRecommendation, UserProfile

logger = logging.getLogger(__name__)

class AmazonScraper:
    def __init__(self):
        self.platform = "amazon"

    def _build_search_query(self, profile: UserProfile, context: dict) -> str:
        query_parts = []
        if profile.gender:
            query_parts.append(profile.gender)
        occasion = context.get("occasion")
        if occasion:
            query_parts.append(occasion)
        if profile.clothingSize:
            query_parts.append(f"size {profile.clothingSize}")
        if profile.favoriteColors:
            query_parts.append(profile.favoriteColors[0])
        return " ".join(query_parts)

    def _parse_price(self, price_str: str) -> Optional[float]:
        try:
            clean_price = price_str.replace("₹", "").replace(",", "").strip()
            return float(clean_price)
        except (ValueError, AttributeError):
            return None

    def _score_product(self, title: str, price: Optional[float], profile: UserProfile, context: dict) -> Tuple[float, List[str]]:
        score = 0.5
        reasons = []
        title_lower = title.lower()
        for color in (profile.favoriteColors or []):
            if color.lower() in title_lower:
                score += 0.2
                reasons.append(f"Available in your color '{color}'")
        if price:
            budget_str = profile.budget or ""
            max_budget = 2000
            if "under" in budget_str.lower():
                try:
                    max_budget = int(''.join(filter(str.isdigit, budget_str)))
                except ValueError:
                    pass
            if price <= max_budget:
                score += 0.2
                reasons.append(f"Within budget (₹{price})")
            else:
                score -= 0.3
                reasons.append(f"Over budget (₹{price})")
        for style in (profile.styles or []):
            if style.lower() in title_lower:
                score += 0.2
                reasons.append(f"Matches '{style}' style")
        return min(round(score, 2), 1.0), reasons

    async def scrape(self, user_profile: UserProfile, context: dict) -> List[ProductRecommendation]:
        raw_query = self._build_search_query(user_profile, context)
        url = f"https://www.amazon.in/s?k={raw_query.replace(' ', '+')}"
        recommendations = []
        
        try:
            logger.info(f"Amazon: fetching {url}")
            async with AsyncSession(impersonate="chrome110") as session:
                resp = await session.get(url, timeout=20)
                if resp.status_code != 200:
                    logger.warning(f"Amazon: HTTP {resp.status_code} for '{raw_query}'")
                    return recommendations

                soup = BeautifulSoup(resp.text, "html.parser")
                results = soup.find_all("div", attrs={"data-component-type": "s-search-result"})
                logger.info(f"Amazon: found {len(results)} raw products for '{raw_query}'")

                for item in results[:20]:
                    title_el = item.find("h2")
                    title = title_el.text.strip() if title_el else None
                    if not title:
                        continue

                    price_el = item.find("span", class_="a-price-whole")
                    price_str = price_el.text.strip() if price_el else None
                    price = self._parse_price(price_str) if price_str else None

                    img_el = item.find("img", class_="s-image")
                    img_url = img_el.get("src") if img_el else None

                    link_el = item.find("a", class_="a-link-normal")
                    product_url = "https://www.amazon.in" + link_el.get("href") if link_el else None
                    
                    asin = item.get("data-asin", "")
                    product_id = f"amazon-{asin}" if asin else f"amazon-{hash(product_url)}"

                    if title and img_url and product_url:
                        score, reasons = self._score_product(title, price, user_profile, context)
                        if score > 0.4:
                            recommendations.append(
                                ProductRecommendation(
                                    id=product_id,
                                    title=title,
                                    brand="Amazon Vendor",
                                    price=price or 0.0,
                                    image_url=img_url,
                                    product_url=product_url,
                                    platform=self.platform,
                                    match_score=score,
                                    match_reasons=reasons
                                )
                            )
        except Exception as e:
            logger.error(f"Amazon scraper error: {e}")
            
        logger.info(f"Amazon: returning {len(recommendations)} products")
        return recommendations

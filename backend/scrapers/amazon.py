import asyncio
import logging
import re
from typing import List, Optional, Tuple
from bs4 import BeautifulSoup
from curl_cffi.requests import AsyncSession
from models import ProductRecommendation, UserProfile

logger = logging.getLogger(__name__)

class AmazonScraper:
    def __init__(self):
        self.platform = "amazon"

    def _parse_budget_value(self, budget_raw: str) -> float:
        cleaned = (budget_raw or "").lower()
        digits = re.findall(r"\d+", cleaned)
        if digits:
            try:
                return float(digits[0])
            except ValueError:
                pass
        return 2000.0

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

    def _score_product(
        self,
        title: str,
        price: Optional[float],
        profile: UserProfile,
        context: dict,
        max_budget: float,
    ) -> Tuple[float, List[str]]:
        score = 0.5
        reasons = []
        title_lower = title.lower()
        for color in (profile.favoriteColors or []):
            if color.lower() in title_lower:
                score += 0.2
                reasons.append(f"Available in your color '{color}'")
        if price:
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
        max_budget = self._parse_budget_value(user_profile.budget or "")
        recommendations = []

        last_error: Optional[Exception] = None
        for attempt in range(1, 4):
            attempt_recommendations: List[ProductRecommendation] = []
            try:
                logger.info(f"Amazon: fetching {url} (attempt {attempt}/3)")
                async with AsyncSession(impersonate="chrome110") as session:
                    resp = await session.get(url, timeout=30)
                    if resp.status_code != 200:
                        logger.warning(
                            f"Amazon: HTTP {resp.status_code} for '{raw_query}' (attempt {attempt}/3)"
                        )
                        continue

                    soup = BeautifulSoup(resp.text, "html.parser")
                    results = soup.find_all(
                        "div", attrs={"data-component-type": "s-search-result"}
                    )
                    logger.info(
                        f"Amazon: found {len(results)} raw products for '{raw_query}' (attempt {attempt}/3)"
                    )

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
                        product_url = (
                            "https://www.amazon.in" + link_el.get("href")
                            if link_el and link_el.get("href")
                            else None
                        )

                        asin = item.get("data-asin", "")
                        product_id = (
                            f"amazon-{asin}" if asin else f"amazon-{hash(product_url)}"
                        )

                        if title and img_url and product_url:
                            if price is not None and price > max_budget:
                                continue
                            score, reasons = self._score_product(
                                title, price, user_profile, context, max_budget
                            )
                            if score > 0.4:
                                attempt_recommendations.append(
                                    ProductRecommendation(
                                        id=product_id,
                                        title=title,
                                        brand="Amazon Vendor",
                                        price=price or 0.0,
                                        image_url=img_url,
                                        product_url=product_url,
                                        platform=self.platform,
                                        match_score=score,
                                        match_reasons=reasons,
                                    )
                                )

                recommendations = attempt_recommendations
                break
            except Exception as e:
                last_error = e
                logger.error(
                    f"Amazon scraper error (attempt {attempt}/3): {e}",
                    exc_info=True,
                )
                if attempt < 3:
                    await asyncio.sleep(1.0 * attempt)
                continue

        if last_error and not recommendations:
            logger.warning(f"Amazon: finished with error: {last_error}", exc_info=True)
            
        logger.info(f"Amazon: returning {len(recommendations)} products")
        return recommendations

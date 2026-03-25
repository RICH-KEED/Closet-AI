import json
import logging
from typing import List, Optional, Tuple
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup
from models import ProductRecommendation, UserProfile

logger = logging.getLogger(__name__)

class AjioScraper:
    def __init__(self):
        self.platform = "ajio"
        self.base_url = "https://www.ajio.com"

    def _build_search_query(self, profile: UserProfile, context: dict) -> str:
        query_parts = []
        if profile.gender:
            query_parts.append(profile.gender)
        occasion = context.get("occasion")
        if occasion:
            query_parts.append(occasion)
        if profile.clothingSize:
            query_parts.append(profile.clothingSize)
        if profile.favoriteColors:
            query_parts.append(profile.favoriteColors[0])
        return " ".join(query_parts)

    def _extract_json(self, text: str) -> str:
        start_idx = text.find('{')
        if start_idx == -1: return ""
        
        depth = 0
        in_string = False
        escape = False
        
        for i in range(start_idx, len(text)):
            char = text[i]
            if escape:
                escape = False
                continue
            if char == '\\':
                escape = True
                continue
            if char == '"':
                in_string = not in_string
                continue
            if not in_string:
                if char == '{':
                    depth += 1
                elif char == '}':
                    depth -= 1
                    if depth == 0:
                        return text[start_idx:i+1]
        return ""

    def _normalize_url(self, url: str) -> str:
        if not url:
            return ""
        normalized = url.strip()
        if normalized.startswith("//"):
            return f"https:{normalized}"
        if normalized.startswith("/"):
            return f"{self.base_url}{normalized}"
        if normalized.startswith("http://"):
            return "https://" + normalized[len("http://"):]
        if normalized.startswith("https://"):
            return normalized
        return f"{self.base_url}/{normalized.lstrip('/')}"

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
        # Ajio search uses %20 for spaces
        url = f"https://www.ajio.com/search/{raw_query.replace(' ', '%20')}"
        
        recommendations = []
        try:
            logger.info(f"Ajio: fetching {url}")
            async with AsyncSession(impersonate="chrome110") as session:
                resp = await session.get(url, timeout=20)
                if resp.status_code != 200:
                    logger.warning(f"Ajio: HTTP {resp.status_code} for '{raw_query}'")
                    return recommendations

                soup = BeautifulSoup(resp.text, "html.parser")
                scripts = soup.find_all("script")
                raw_json_str = ""
                
                for script in scripts:
                    if script.string and "window.__PRELOADED_STATE__" in script.string:
                        raw_json_str = self._extract_json(script.string)
                        break
                        
                if not raw_json_str:
                    logger.warning("Ajio: Could not find __PRELOADED_STATE__")
                    return recommendations
                    
                data = json.loads(raw_json_str)
                
                # Recursive search for product nodes
                def find_products(obj):
                    found = []
                    if isinstance(obj, dict):
                        if "name" in obj and "price" in obj and "url" in obj:
                            found.append(obj)
                        for k, v in obj.items():
                            res = find_products(v)
                            if res: found.extend(res)
                    elif isinstance(obj, list):
                        for item in obj:
                            res = find_products(item)
                            if res: found.extend(res)
                    return found

                product_nodes = find_products(data)
                logger.info(f"Ajio: found {len(product_nodes)} raw products")

                # Parse top 20
                for node in product_nodes[:20]:
                    title = node.get("name")
                    product_url = self._normalize_url(node.get("url", ""))
                    brand = node.get("brandName", "Ajio Vendor")
                    
                    price_val = 0.0
                    price_obj = node.get("price")
                    if isinstance(price_obj, dict):
                        price_val = price_obj.get("value", 0.0)
                    elif isinstance(price_obj, (int, float)):
                        price_val = float(price_obj)

                    # Extract primary image
                    images = node.get("images", [])
                    img_url = ""
                    if images and len(images) > 0 and isinstance(images[0], dict):
                        img_url = images[0].get("url", "")
                        
                    img_url = self._normalize_url(img_url)
                        
                    product_id = f"ajio-{node.get('code', hash(product_url))}"

                    if title and img_url and product_url:
                        score, reasons = self._score_product(title, price_val, user_profile, context)
                        if score > 0.4:
                            recommendations.append(
                                ProductRecommendation(
                                    id=product_id,
                                    title=title,
                                    brand=brand,
                                    price=price_val,
                                    image_url=img_url,
                                    product_url=product_url,
                                    platform=self.platform,
                                    match_score=score,
                                    match_reasons=reasons
                                )
                            )

        except Exception as e:
            logger.error(f"Ajio scraper error: {e}")

        logger.info(f"Ajio: returning {len(recommendations)} products")
        return recommendations

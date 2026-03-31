import asyncio
import json
import logging
import random
import re
from typing import List, Optional
from models import ProductRecommendation, UserProfile
import aiohttp
from bs4 import BeautifulSoup

logger = logging.getLogger(__name__)

# ── Constants ──────────────────────────────────────────────────────────────────

MYNTRA_SEARCH_URL = "https://www.myntra.com/{query}?rawQuery={raw}"
MYNTRA_PRODUCT_BASE = "https://www.myntra.com/"

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
]

BUDGET_MAP = {
    "under500":  500,
    "under1000": 1000,
    "under2000": 2000,
    "under5000": 5000,
    "luxury":    99999,
}

STYLE_QUERY_MAP = {
    "casual":     "casual-shirts",
    "formal":     "formal-shirts",
    "ethnic":     "kurtas",
    "western":    "jeans",
    "streetwear": "streetwear-tshirts",
    "minimalist": "basics-tshirts",
    "bohemian":   "boho-tops",
    "sporty":     "activewear-tshirts",
    "party":      "party-dresses",
    "workwear":   "formal-trousers",
}

GENDER_MAP = {
    "male": "men", "man": "men", "men": "men",
    "female": "women", "woman": "women", "women": "women",
}

# ── Scraper ────────────────────────────────────────────────────────────────────

class MyntraScraper:
    def __init__(self):
        self.platform = "myntra"
        self.max_results_per_query = 15
        self.request_delay = (0.5, 1.5)

    def _build_headers(self) -> dict:
        return {
            "User-Agent": random.choice(USER_AGENTS),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.5",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "DNT": "1",
            "Upgrade-Insecure-Requests": "1",
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "none",
            "Sec-Fetch-User": "?1",
        }

    def _build_queries(self, user_profile: UserProfile) -> List[tuple[str, str]]:
        """
        Returns list of (slug, raw_query) tuples for Myntra URL construction.
        """
        gender = GENDER_MAP.get((user_profile.gender or "").lower(), "")
        queries = []

        for style in (user_profile.styles or []):
            slug = STYLE_QUERY_MAP.get(style.lower(), f"{style}-clothing")
            if gender:
                slug = f"{gender}-{slug}"
            raw = f"{gender} {style} wear".strip()
            queries.append((slug, raw))

        for occasion in (user_profile.occasions or []):
            slug = f"{gender}-{occasion}-wear" if gender else f"{occasion}-wear"
            queries.append((slug, f"{gender} {occasion} wear".strip()))

        if not queries:
            slug = f"{gender}-clothing" if gender else "clothing"
            queries.append((slug, f"{gender} clothing".strip()))

        # deduplicate
        seen, unique = set(), []
        for q in queries:
            if q[0] not in seen:
                seen.add(q[0])
                unique.append(q)

        return unique[:2]  # max 2 because Playwright is slower

    def _parse_price(self, val) -> float:
        if val is None:
            return 0.0
        try:
            return float(str(val).replace(",", "").strip())
        except (ValueError, TypeError):
            return 0.0

    def _parse_budget_value(self, budget_raw: str) -> float:
        cleaned = (budget_raw or "").lower().replace(" ", "")
        if not cleaned:
            return 5000
        mapped = BUDGET_MAP.get(cleaned)
        if mapped is not None:
            return float(mapped)
        digits = re.findall(r"\d+", cleaned)
        if digits:
            try:
                return float(digits[0])
            except ValueError:
                pass
        return 5000

    def _compute_match_score(
        self,
        product: dict,
        user_profile: UserProfile,
        max_budget: float,
    ) -> tuple[float, List[str]]:
        score = 0.0
        reasons: List[str] = []
        price = self._parse_price(product.get("discountedPrice") or product.get("price"))
        name_lower = (
            (product.get("productName") or "") + " " +
            (product.get("brand") or "")
        ).lower()

        # Budget (40 pts)
        if max_budget >= 99999:
            score += 0.40
            reasons.append("No budget limit")
        elif price > 0 and price <= max_budget:
            score += 0.40
            reasons.append(f"Within ₹{int(max_budget)} budget")

        # Color match (30 pts)
        user_colors = [c.lower() for c in (user_profile.favoriteColors or [])]
        for color in user_colors:
            if color in name_lower:
                score += 0.30
                reasons.append(f"Available in {color}")
                break
        else:
            if not user_colors:
                score += 0.15

        # Style/occasion match (30 pts)
        style_terms = [s.lower() for s in (user_profile.styles or [])]
        style_terms += [o.lower() for o in (user_profile.occasions or [])]
        for term in style_terms:
            mapped = STYLE_QUERY_MAP.get(term, term).replace("-", " ").lower()
            if any(word in name_lower for word in mapped.split()):
                score += 0.30
                reasons.append(f"Matches {term} style")
                break
        else:
            if style_terms:
                score += 0.10

        return min(round(score, 2), 1.0), reasons

    async def _fetch_query(
        self,
        session: aiohttp.ClientSession,
        slug: str,
        raw_query: str,
        user_profile: UserProfile,
        max_budget: float,
    ) -> List[ProductRecommendation]:
        """Fetches the search page HTML and extracts the embedded __myx JSON data."""
        url = f"https://www.myntra.com/{slug}?rawQuery={raw_query.replace(' ', '%20')}"

        try:
            logger.info(f"Myntra: fetching {url}")
            request_timeout = aiohttp.ClientTimeout(total=30)

            html = None
            last_error: Optional[Exception] = None
            for attempt in range(1, 4):
                try:
                    async with session.get(
                        url,
                        headers=self._build_headers(),
                        timeout=request_timeout,
                    ) as resp:
                        if resp.status != 200:
                            logger.warning(
                                f"Myntra: HTTP {resp.status} for '{raw_query}' (attempt {attempt}/3)"
                            )
                            continue
                        html = await resp.text()
                        break
                except Exception as e:
                    last_error = e
                    logger.warning(
                        f"Myntra: request error for '{raw_query}' (attempt {attempt}/3): {e}",
                        exc_info=True,
                    )
                    if attempt < 3:
                        await asyncio.sleep(1.0 * attempt)
                    continue

            if not html:
                if last_error:
                    logger.error(
                        f"Myntra: failed to fetch '{raw_query}' after retries: {last_error}",
                        exc_info=True,
                    )
                return []

            # Find the window.__myx = {...} block using BeautifulSoup
            soup = BeautifulSoup(html, "html.parser")
            scripts = soup.find_all("script")
            
            json_str = None
            for script in scripts:
                if script.string and "window.__myx = " in script.string:
                    text = script.string
                    # Extract everything after window.__myx = 
                    start_idx = text.find("window.__myx = ") + len("window.__myx = ")
                    json_start = text[start_idx:]
                    
                    # The script block usually ends or transitions. We'll use a regex to grab the outermost object
                    # Since we are isolated in this script tag, a greedy dotall matched against the last ';' works well.
                    match = re.match(r"\s*(\{.*?\});?\s*(?:window\.|var |let |const |$)", json_start, re.DOTALL)
                    if match:
                        json_str = match.group(1)
                    else:
                        # Fallback: just split by the next known variable assignment
                        parts = re.split(r";\s*window\.", json_start)
                        json_str = parts[0].strip()
                        if json_str.endswith(";"):
                            json_str = json_str[:-1]
                    break
            
            if not json_str:
                logger.warning(f"Myntra: could not find __myx marker in HTML script tags for '{raw_query}'")
                return []

            try:
                data = json.loads(json_str)
            except json.JSONDecodeError as e:
                logger.error(f"Myntra: JSON decode error for '{raw_query}': {e}")
                return []
            raw_products = (
                data.get("searchData", {}).get("results", {}).get("products")
                or data.get("results", {}).get("products")
                or data.get("products")
                or []
            )

            logger.info(f"Myntra: found {len(raw_products)} raw products for '{raw_query}'")

            results: List[ProductRecommendation] = []
            for p in raw_products:
                try:
                    product_id = str(p.get("productId") or p.get("id") or "")
                    if not product_id:
                        continue

                    price = self._parse_price(p.get("discountedPrice") or p.get("price"))

                    if max_budget < 99999 and price > max_budget:
                        continue

                    title = p.get("productName") or p.get("title") or "Unknown"
                    brand = p.get("brand") or "Unknown"

                    images = p.get("images") or []
                    image_url = ""

                    # Myntra's embedded product objects have changed structure over time.
                    # Make extraction resilient: handle images as list[str] or list[dict] with different key names.
                    if isinstance(images, list) and images:
                        candidate_urls: list[str] = []
                        for img in images[:5]:
                            if isinstance(img, str):
                                if img.strip():
                                    candidate_urls.append(img.strip())
                            elif isinstance(img, dict):
                                candidate_urls.extend(
                                    [
                                        img.get("securePath"),
                                        img.get("path"),
                                        img.get("secure_url"),
                                        img.get("secureUrl"),
                                        img.get("url"),
                                        img.get("imageUrl"),
                                        img.get("imageURL"),
                                        img.get("src"),
                                        img.get("thumbnail"),
                                    ]
                                )

                        # Pick the first non-empty string.
                        for u in candidate_urls:
                            if isinstance(u, str) and u.strip():
                                image_url = u.strip()
                                break

                    # Fallbacks from product root.
                    if not image_url:
                        image_url = (
                            p.get("image_url")
                            or p.get("imageURL")
                            or p.get("imageUrl")
                            or p.get("productImage")
                            or p.get("thumbnail")
                            or ""
                        )

                    if isinstance(image_url, str) and image_url and image_url.startswith("//"):
                        image_url = "https:" + image_url

                    # Best-effort normalization for relative/partial Myntra URLs.
                    if isinstance(image_url, str) and image_url:
                        if image_url.startswith("/"):
                            image_url = "https://www.myntra.com" + image_url
                        elif not image_url.startswith("http"):
                            image_url = MYNTRA_PRODUCT_BASE + image_url.lstrip("/")
                        elif image_url.startswith("http://"):
                            # Upgrade to HTTPS for better compatibility on mobile networks.
                            image_url = "https://" + image_url[len("http://"):]

                    # Ensure UI never receives an empty image_url.
                    if not image_url or (isinstance(image_url, str) and not image_url.strip()):
                        # Offline-safe 1x1 PNG (base64 data URI).
                        image_url = (
                            "data:image/png;base64,"
                            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8"
                            "/x8AAwMCAO+kv1kAAAAASUVORK5CYII="
                        )

                    slug_url = p.get("landingPageUrl") or p.get("slug") or product_id
                    product_url = (
                        slug_url if slug_url.startswith("http")
                        else MYNTRA_PRODUCT_BASE + slug_url.lstrip("/")
                    )

                    score, reasons = self._compute_match_score(p, user_profile, max_budget)

                    results.append(ProductRecommendation(
                        id=f"myntra_{product_id}",
                        title=title,
                        brand=brand,
                        price=price,
                        image_url=image_url,
                        product_url=product_url,
                        platform=self.platform,
                        match_score=score,
                        match_reasons=reasons,
                    ))

                    if len(results) >= self.max_results_per_query:
                        break
                except Exception as e:
                    logger.warning(f"Myntra: product parse error: {e}")
                    continue

            return results

        except Exception as e:
            logger.error(f"Myntra: request error for '{raw_query}': {e}")
            return []

    async def scrape(self, user_profile: UserProfile, context: dict) -> List[ProductRecommendation]:
        """
        Main entrypoint — makes concurrent aiohttp requests to Myntra.
        """
        queries = self._build_queries(user_profile)
        budget_str = user_profile.budget or ""
        max_budget = self._parse_budget_value(budget_str)

        logger.info(f"Myntra: {len(queries)} queries | budget ₹{max_budget}")

        all_results: List[ProductRecommendation] = []

        async with aiohttp.ClientSession() as session:
            for i, (slug, raw_query) in enumerate(queries):
                if i > 0:
                    await asyncio.sleep(random.uniform(*self.request_delay))
                
                results = await self._fetch_query(session, slug, raw_query, user_profile, max_budget)
                all_results.extend(results)

        # Deduplicate
        seen_ids: set = set()
        unique: List[ProductRecommendation] = []
        for r in all_results:
            if r.id not in seen_ids:
                seen_ids.add(r.id)
                unique.append(r)

        unique.sort(key=lambda x: x.match_score, reverse=True)
        logger.info(f"Myntra: returning {len(unique)} products")
        return unique

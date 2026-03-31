from typing import List, Dict, Any, Optional, Set
from models import ProductRecommendation, UserProfile

class MatchScorer:
    STYLE_WEIGHT = 80.0
    COLOR_WEIGHT = 15.0
    BRAND_WEIGHT = 5.0
    WEATHER_WEIGHT = 20.0  # Bonus/Penalty for weather match

    @classmethod
    def score_product(
        cls,
        product: ProductRecommendation,
        profile: UserProfile,
        weather: dict = None,
        liked_product_ids: Optional[Set[str]] = None,
        disliked_product_ids: Optional[Set[str]] = None,
    ) -> ProductRecommendation:
        score = 0.0
        reasons = []
        liked_product_ids = liked_product_ids or set()
        disliked_product_ids = disliked_product_ids or set()

        title_lower = product.title.lower() if product.title else ""
        brand_lower = product.brand.lower() if product.brand else ""

        # Style matching (80%)
        # Here we just check if any of the user's preferred styles appear in the title
        matched_style = False
        for style in profile.styles:
            if style.lower() in title_lower:
                score += cls.STYLE_WEIGHT
                reasons.append(f"Matches style '{style}'")
                matched_style = True
                break
        
        # Color matching (15%)
        # Check if any preferred color appears in the title
        for color in profile.favoriteColors:
            if color.lower() in title_lower:
                score += cls.COLOR_WEIGHT
                reasons.append(f"Matches color '{color}'")
                break

        # Brand matching (5%)
        for style in profile.styles:
            if style.lower() == brand_lower:
                score += cls.BRAND_WEIGHT
                reasons.append(f"Matches brand '{product.brand}'")
                break
                
        # Weather matching (±20%)
        if weather:
            temp = weather.get("temp", 25)
            condition = weather.get("condition", "clear")
            
            # Hot weather logic
            if temp > 25:
                if any(w in title_lower for w in ["short", "tank", "summer", "linen", "cotton", "sleeveless"]):
                    score += cls.WEATHER_WEIGHT
                    reasons.append(f"Great for warm weather ({temp}°C)")
                elif any(w in title_lower for w in ["jacket", "sweater", "hoodie", "winter", "wool"]):
                    score -= cls.WEATHER_WEIGHT
                    reasons.append(f"Might be too warm for current weather ({temp}°C)")
            
            # Cold weather logic
            elif temp < 15:
                if any(w in title_lower for w in ["jacket", "sweater", "hoodie", "winter", "wool", "coat"]):
                    score += cls.WEATHER_WEIGHT
                    reasons.append(f"Perfect for cold weather ({temp}°C)")
                elif any(w in title_lower for w in ["shorts", "tank", "sleeveless"]):
                    score -= cls.WEATHER_WEIGHT
                    reasons.append(f"Might be too cold for current weather ({temp}°C)")
                    
            # Rain logic
            if "rain" in condition or "drizzle" in condition:
                if any(w in title_lower for w in ["waterproof", "jacket", "rain", "windbreaker"]):
                    score += cls.WEATHER_WEIGHT
                    reasons.append("Good choice for rainy weather")

        # User feedback boost/penalty (small but noticeable)
        if product.id in liked_product_ids:
            score += 0.2
            reasons.append("You liked this item")
        if product.id in disliked_product_ids:
            score -= 0.2
            reasons.append("You disliked this item")

        # Clamp to [0, 1] because the UI expects normalized scores.
        score = max(0.0, min(score, 1.0))
        product.match_score = score
        product.match_reasons = reasons
        return product

    @classmethod
    def rank_products(
        cls,
        products: List[ProductRecommendation],
        profile: UserProfile,
        weather: dict = None,
        liked_product_ids: Optional[Set[str]] = None,
        disliked_product_ids: Optional[Set[str]] = None,
    ) -> List[ProductRecommendation]:
        scored_products = []
        for p in products:
            scored_p = cls.score_product(
                p,
                profile,
                weather,
                liked_product_ids=liked_product_ids,
                disliked_product_ids=disliked_product_ids,
            )
            scored_products.append(scored_p)
        
        # Sort by score descending
        scored_products.sort(key=lambda x: x.match_score, reverse=True)
        # Safety: if everything scored 0, keep items instead of returning empty.
        # This helps first-time profiles where we may not match any keyword yet.
        if scored_products and scored_products[0].match_score <= 0:
            for item in scored_products:
                if not item.match_reasons:
                    item.match_reasons = ["Setting up your style profile"]
        return scored_products

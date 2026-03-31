from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class WardrobeItem(BaseModel):
    id: str
    image_url: str
    category: str
    color: str

class UserProfile(BaseModel):
    uid: str
    gender: Optional[str] = None
    bodyType: Optional[str] = None
    clothingSize: Optional[str] = None
    budget: Optional[str] = None
    styles: List[str] = Field(default_factory=list)
    favoriteColors: List[str] = Field(default_factory=list)
    occasions: List[str] = Field(default_factory=list)
    climate: Optional[str] = None
    wardrobe: Optional[List[WardrobeItem]] = Field(default_factory=list)

class RecommendationRequest(BaseModel):
    user_profile: UserProfile
    context: Optional[dict] = Field(default_factory=dict)
    # Pagination for "Load more" in the Android app.
    # We default to the first page so older clients still work.
    offset: int = 0
    limit: int = 20

class RecommendationFeedbackRequest(BaseModel):
    """
    Stores user feedback for a recommendation product.
    `action` should be either "like" or "dislike".
    """
    user_uid: str
    product_id: str
    action: str
    # Optional, but can help with debugging/analytics.
    platform: Optional[str] = None
    # Optional snapshot so liked items can be shown in a Saved screen later.
    title: Optional[str] = None
    brand: Optional[str] = None
    price: Optional[float] = None
    image_url: Optional[str] = None
    product_url: Optional[str] = None
    match_score: Optional[float] = None
    match_reasons: Optional[List[str]] = None

class ProductRecommendation(BaseModel):
    id: str
    title: str
    brand: str
    price: float
    image_url: str
    product_url: str
    platform: str
    match_score: float
    match_reasons: List[str] = Field(default_factory=list)


class TryOnResponse(BaseModel):
    status: str = "ok"
    image_base64: str

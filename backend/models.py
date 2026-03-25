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

from pydantic import BaseModel
from typing import List, Optional


class RecommendRequest(BaseModel):
    user_id: int
    limit_campaigns: Optional[int] = 5
    limit_projects: Optional[int] = 5
    limit_posts: Optional[int] = 10


class RecommendResponse(BaseModel):
    user_id: int
    recommended_campaign_ids: List[int]
    recommended_project_ids: List[int]
    recommended_post_ids: List[int]
    model_version: str
    is_cold_start: bool


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_version: str
    total_interactions: int


class RetrainResponse(BaseModel):
    status: str
    message: str
    new_model_version: str
    training_samples: int

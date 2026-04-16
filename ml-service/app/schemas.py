from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional


class RecommendRequest(BaseModel):
    user_id: int = Field(..., ge=1, description="Platform user id")
    limit_campaigns: Optional[int] = Field(default=5, ge=0, le=50)
    limit_projects: Optional[int] = Field(default=5, ge=0, le=50)
    limit_posts: Optional[int] = Field(default=10, ge=0, le=50)
    limit_events: Optional[int] = Field(default=9, ge=0, le=50)


class RecommendResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    user_id: int
    recommended_campaign_ids: List[int]
    recommended_project_ids: List[int]
    recommended_post_ids: List[int]
    recommended_event_ids: List[int]
    model_version: str
    is_cold_start: bool


class HealthResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    status: str
    model_loaded: bool
    model_version: str
    total_interactions: int


class RetrainResponse(BaseModel):
    status: str
    message: str
    new_model_version: str
    training_samples: int

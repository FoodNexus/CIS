import pytest
from pydantic import ValidationError

from app.schemas import RecommendRequest


def test_recommend_request_accepts_valid_limits():
    r = RecommendRequest(user_id=1, limit_campaigns=10, limit_posts=50)
    assert r.limit_campaigns == 10
    assert r.limit_projects == 5
    assert r.limit_posts == 50


def test_reject_invalid_user_id():
    with pytest.raises(ValidationError):
        RecommendRequest(user_id=0)


def test_reject_limit_too_high():
    with pytest.raises(ValidationError):
        RecommendRequest(user_id=1, limit_posts=51)


def test_recommend_request_accepts_zero_limits():
    r = RecommendRequest(user_id=1, limit_campaigns=0, limit_projects=0, limit_posts=0, limit_events=9)
    assert r.limit_campaigns == 0
    assert r.limit_events == 9

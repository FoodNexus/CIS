"""HTTP API tests for the ML FastAPI service (no real database)."""
from __future__ import annotations

from datetime import datetime

import pandas as pd
import pytest
from fastapi.testclient import TestClient
from app.main import app

pytestmark = pytest.mark.usefixtures("_patch_data_and_model")


@pytest.fixture
def _patch_data_and_model(monkeypatch):
    """Stub DB-backed loaders and fix singleton model state."""
    import app.main as main

    empty_ix = pd.DataFrame(
        columns=["user_id", "entity_type", "entity_id", "action", "created_at", "weight"]
    )

    campaigns = pd.DataFrame(
        {
            "id": [10, 11],
            "name": ["C1", "C2"],
            "type": ["AWARENESS", "AWARENESS"],
            "vote_count": [5, 1],
            "created_at": [datetime.now(), datetime.now()],
            "needed_amount": [100.0, 100.0],
        }
    )
    projects = pd.DataFrame(
        {
            "id": [20],
            "title": ["P1"],
            "current_amount": [10.0],
            "goal_amount": [100.0],
            "vote_count": [2],
            "start_date": [datetime.now()],
        }
    )
    posts = pd.DataFrame(
        {
            "id": [30],
            "type": ["STATUS"],
            "likes_count": [3],
            "created_at": [datetime.now()],
        }
    )
    events = pd.DataFrame(
        {
            "id": [40],
            "title": ["E1"],
            "type": ["VISITE"],
            "date": [datetime.now()],
            "location": [""],
            "current_participants": [0],
            "max_capacity": [100],
            "created_at": [datetime.now()],
            "status": ["UPCOMING"],
        }
    )
    votes = pd.DataFrame(columns=["user_id", "campaign_id"])
    fundings = pd.DataFrame(columns=["user_id", "project_id"])
    event_regs = pd.DataFrame(columns=["user_id", "event_id"])

    monkeypatch.setattr(main, "get_interaction_count", lambda: 7)
    monkeypatch.setattr(main, "load_interactions", lambda: empty_ix)
    monkeypatch.setattr(main, "load_active_campaigns", lambda: campaigns)
    monkeypatch.setattr(main, "load_active_projects", lambda: projects)
    monkeypatch.setattr(main, "load_accepted_posts", lambda: posts)
    monkeypatch.setattr(main, "load_upcoming_events", lambda: events)
    monkeypatch.setattr(main, "load_user_votes", lambda: votes)
    monkeypatch.setattr(main, "load_user_fundings", lambda: fundings)
    monkeypatch.setattr(main, "load_user_event_registrations", lambda: event_regs)

    m = main.recommendation_model
    monkeypatch.setattr(m, "is_loaded", False, raising=False)
    monkeypatch.setattr(m, "model_version", "api_test", raising=False)


@pytest.fixture
def client():
    return TestClient(app)


def test_health_ok(client: TestClient):
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["total_interactions"] == 7
    assert "model_version" in body


def test_model_info_ok(client: TestClient):
    r = client.get("/model/info")
    assert r.status_code == 200
    body = r.json()
    assert body["version"] == "api_test"
    assert body["is_loaded"] is False
    assert "model_path" in body


def test_recommend_ok_cold_start(client: TestClient):
    r = client.post(
        "/recommend",
        json={"user_id": 1, "limit_campaigns": 2, "limit_projects": 1, "limit_posts": 1},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["user_id"] == 1
    assert body["is_cold_start"] is True
    assert body["model_version"] == "api_test"
    assert body["recommended_campaign_ids"] == [10, 11]
    assert body["recommended_project_ids"] == [20]
    assert body["recommended_post_ids"] == [30]
    assert body["recommended_event_ids"] == [40]


def test_recommend_validation_invalid_user(client: TestClient):
    r = client.post("/recommend", json={"user_id": 0})
    assert r.status_code == 422


def test_recommend_validation_limit_too_high(client: TestClient):
    r = client.post("/recommend", json={"user_id": 1, "limit_posts": 99})
    assert r.status_code == 422


def test_retrain_accepted(client: TestClient, monkeypatch):
    monkeypatch.setattr("app.main.train", lambda: {"status": "skipped"})
    r = client.post("/retrain")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "accepted"
    assert body["training_samples"] == 7


def test_openapi_docs_available(client: TestClient):
    r = client.get("/docs")
    assert r.status_code == 200
    r2 = client.get("/openapi.json")
    assert r2.status_code == 200
    assert "openapi" in r2.json()

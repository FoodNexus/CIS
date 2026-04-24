import pandas as pd
import numpy as np
import joblib
import os
import logging
from datetime import datetime
from surprise import SVD, Dataset, Reader
from typing import List

logger = logging.getLogger(__name__)
MIN_INTERACTIONS_FOR_SVD = 10  # below this → use popularity only

# SVD hyperparameters (regularization via SVD_REG_ALL; increase if unstable / overfitting)
SVD_N_FACTORS = int(os.getenv("SVD_N_FACTORS", "50"))
SVD_N_EPOCHS = int(os.getenv("SVD_N_EPOCHS", "20"))
SVD_LR_ALL = float(os.getenv("SVD_LR_ALL", "0.005"))
SVD_REG_ALL = float(os.getenv("SVD_REG_ALL", "0.02"))
# Clip Surprise raw estimates before blending (rating_scale is 1–5; estimates can drift)
SVD_EST_CLIP_LO = float(os.getenv("SVD_EST_CLIP_LO", "0.5"))
SVD_EST_CLIP_HI = float(os.getenv("SVD_EST_CLIP_HI", "5.5"))
MAX_RECOMMEND_ITEMS = int(os.getenv("MAX_RECOMMEND_ITEMS", "50"))


def get_model_path() -> str:
    return os.getenv("MODEL_PATH", "/app/models/svd_model.pkl")


def _item_key(entity_type: str, entity_id: int) -> str:
    return f"{entity_type}:{int(entity_id)}"


class RecommendationModel:

    def __init__(self):
        self.svd_model = None
        self.model_version = "none"
        self.is_loaded = False
        self.all_campaign_ids = []
        self.all_project_ids = []
        self.all_post_ids = []

    def _clip_limit(self, limit: int, default: int) -> int:
        if limit < 1:
            return default
        return min(limit, MAX_RECOMMEND_ITEMS)

    def train(self, interactions_df: pd.DataFrame, model_path: str | None = None) -> dict:
        """
        Train SVD model on user-item interaction matrix.
        Each (user_id, entity_type, entity_id) pair gets a rating = sum of weights.
        """
        if len(interactions_df) < MIN_INTERACTIONS_FOR_SVD:
            logger.warning("Not enough interactions to train SVD.")
            return {"status": "skipped", "reason": "not enough data"}

        work = interactions_df.copy()
        work["item_key"] = work.apply(
            lambda r: _item_key(str(r["entity_type"]), int(r["entity_id"])),
            axis=1,
        )

        ratings_df = (
            work.groupby(["user_id", "item_key"])["weight"]
            .sum()
            .reset_index()
            .rename(columns={"weight": "rating"})
        )

        max_rating = ratings_df["rating"].max()
        min_rating = ratings_df["rating"].min()
        ratings_df["rating_normalized"] = (
            1 + 4 * (ratings_df["rating"] - min_rating)
            / max(max_rating - min_rating, 1)
        )

        reader = Reader(rating_scale=(1, 5))
        dataset = Dataset.load_from_df(
            ratings_df[["user_id", "item_key", "rating_normalized"]],
            reader,
        )
        trainset = dataset.build_full_trainset()

        self.svd_model = SVD(
            n_factors=SVD_N_FACTORS,
            n_epochs=SVD_N_EPOCHS,
            lr_all=SVD_LR_ALL,
            reg_all=SVD_REG_ALL,
            random_state=42,
        )
        self.svd_model.fit(trainset)

        self.model_version = datetime.now().strftime("%Y%m%d_%H%M%S")
        model_data = {
            "svd": self.svd_model,
            "version": self.model_version,
            "trained_at": datetime.now().isoformat(),
            "n_samples": len(ratings_df),
        }
        path = model_path or get_model_path()
        joblib.dump(model_data, path)
        self.is_loaded = True

        logger.info(
            "Model trained. Version: %s, Samples: %s",
            self.model_version,
            len(ratings_df),
        )
        return {
            "status": "success",
            "version": self.model_version,
            "n_samples": len(ratings_df),
        }

    def load(self, model_path: str | None = None) -> bool:
        """Load saved model from disk."""
        path = model_path or get_model_path()
        if not os.path.exists(path):
            logger.warning("No saved model found.")
            return False
        try:
            model_data = joblib.load(path)
            self.svd_model = model_data["svd"]
            self.model_version = model_data["version"]
            self.is_loaded = True
            logger.info("Model loaded. Version: %s", self.model_version)
            return True
        except Exception as e:
            logger.error("Failed to load model: %s", e)
            return False

    def predict_score(self, user_id: int, entity_type: str, entity_id: int) -> float:
        """Predict how much user_id would like this entity."""
        if not self.is_loaded or self.svd_model is None:
            return 0.0
        key = _item_key(entity_type, entity_id)
        try:
            prediction = self.svd_model.predict(user_id, key)
            est = float(prediction.est)
            return max(SVD_EST_CLIP_LO, min(SVD_EST_CLIP_HI, est))
        except Exception:
            return 0.0

    def recommend_campaigns(
        self,
        user_id: int,
        campaigns_df: pd.DataFrame,
        user_votes_df: pd.DataFrame,
        limit: int = 5,
        is_cold_start: bool = False,
    ) -> List[int]:

        if campaigns_df.empty:
            return []

        limit = self._clip_limit(limit, 5)

        already_voted = set(
            user_votes_df[user_votes_df["user_id"] == user_id]["campaign_id"]
            .tolist()
        )
                           
        scores = []
        now = datetime.now()

        for _, row in campaigns_df.iterrows():
            cid = int(row["id"])
            if cid in already_voted:
                continue

            if is_cold_start:
                score = float(np.log1p(row.get("vote_count", 0)) * 2.0)
            else:
                svd_score = self.predict_score(user_id, "CAMPAIGN", cid)

                popularity = float(np.log1p(row.get("vote_count", 0)))

                created_at = pd.to_datetime(row.get("created_at", now))
                days_old = max(0, (now - created_at.to_pydatetime()).days)
                recency = max(0, (30 - days_old) * 0.3)

                score = (svd_score * 3.0) + (popularity * 1.5) + recency

            scores.append((cid, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        out = [cid for cid, _ in scores[:limit]]
        # Deduplicate while preserving order
        seen: set = set()
        return [x for x in out if not (x in seen or seen.add(x))]

    def recommend_projects(
        self,
        user_id: int,
        projects_df: pd.DataFrame,
        user_fundings_df: pd.DataFrame,
        limit: int = 5,
        is_cold_start: bool = False,
    ) -> List[int]:

        if projects_df.empty:
            return []

        limit = self._clip_limit(limit, 5)

        already_funded = set(
            user_fundings_df[user_fundings_df["user_id"] == user_id][
                "project_id"
            ].tolist()
        )

        scores = []
        now = datetime.now()

        for _, row in projects_df.iterrows():
            pid = int(row["id"])

            goal = float(row.get("goal_amount", 1))
            current = float(row.get("current_amount", 0))
            progress = current / goal if goal > 0 else 0

            if is_cold_start:
                score = progress * 5.0
                score += float(np.log1p(row.get("vote_count", 0)))
            else:
                svd_score = self.predict_score(user_id, "PROJECT", pid)

                progress_boost = 0.0
                if 0.7 <= progress < 1.0:
                    progress_boost = 3.0
                elif 0.4 <= progress < 0.7:
                    progress_boost = 1.5

                popularity = float(np.log1p(row.get("vote_count", 0)))

                start_date = pd.to_datetime(row.get("start_date", now))
                days_old = max(0, (now - start_date.to_pydatetime()).days)
                recency = max(0, (30 - days_old) * 0.2)

                funded_penalty = -3.0 if pid in already_funded else 0.0

                score = (
                    (svd_score * 3.0)
                    + progress_boost
                    + popularity
                    + recency
                    + funded_penalty
                )

            scores.append((pid, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        out = [pid for pid, _ in scores[:limit]]
        seen: set = set()
        return [x for x in out if not (x in seen or seen.add(x))]

    def recommend_posts(
        self,
        user_id: int,
        posts_df: pd.DataFrame,
        limit: int = 10,
        is_cold_start: bool = False,
    ) -> List[int]:

        if posts_df.empty:
            return []

        limit = self._clip_limit(limit, 10)

        scores = []
        now = datetime.now()

        for _, row in posts_df.iterrows():
            post_id = int(row["id"])

            if is_cold_start:
                score = float(np.log1p(row.get("likes_count", 0)) * 2.0)
            else:
                svd_score = self.predict_score(user_id, "POST", post_id)
                popularity = float(np.log1p(row.get("likes_count", 0)))

                created_at = pd.to_datetime(row.get("created_at", now))
                hours_old = max(
                    0,
                    (now - created_at.to_pydatetime()).total_seconds() / 3600,
                )
                recency = max(0, (72 - hours_old) * 0.1)

                score = (svd_score * 2.0) + popularity + recency

            scores.append((post_id, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        out = [pid for pid, _ in scores[:limit]]
        seen: set = set()
        return [x for x in out if not (x in seen or seen.add(x))]

    def recommend_events(
        self,
        user_id: int,
        events_df: pd.DataFrame,
        user_registrations_df: pd.DataFrame,
        limit: int = 9,
        is_cold_start: bool = False,
    ) -> List[int]:

        if events_df.empty:
            return []

        limit = self._clip_limit(limit, 9)

        already_in = set(
            user_registrations_df[user_registrations_df["user_id"] == user_id]["event_id"]
            .tolist()
        )

        scores = []
        now = datetime.now()

        for _, row in events_df.iterrows():
            eid = int(row["id"])
            if eid in already_in:
                continue

            cap = float(row.get("max_capacity", 1) or 1)
            cur = float(row.get("current_participants", 0) or 0)
            fullness = cur / cap if cap > 0 else 0

            if is_cold_start:
                score = float(np.log1p(cur) * 2.0) + (1.0 - fullness) * 3.0
            else:
                svd_score = self.predict_score(user_id, "EVENT", eid)
                popularity = float(np.log1p(cur))
                event_date = pd.to_datetime(row.get("date", now))
                days_until = (event_date.to_pydatetime() - now).days
                urgency = max(0, min(14, 14 - max(0, days_until))) * 0.2
                score = (svd_score * 3.0) + popularity + urgency + (1.0 - fullness) * 2.0

            scores.append((eid, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        out = [eid for eid, _ in scores[:limit]]
        seen: set = set()
        return [x for x in out if not (x in seen or seen.add(x))]


# Singleton instance
recommendation_model = RecommendationModel()

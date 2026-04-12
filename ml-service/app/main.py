import logging
import os
from contextlib import asynccontextmanager

from apscheduler.schedulers.background import BackgroundScheduler
from fastapi import BackgroundTasks, FastAPI, HTTPException

from app.data import (
    get_interaction_count,
    load_accepted_posts,
    load_active_campaigns,
    load_active_projects,
    load_interactions,
    load_user_fundings,
    load_user_votes,
)
from app.model import recommendation_model
from app.schemas import (
    HealthResponse,
    RecommendRequest,
    RecommendResponse,
    RetrainResponse,
)
from app.train import train

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

RETRAIN_HOURS = int(os.getenv("RETRAIN_INTERVAL_HOURS", "24"))


def scheduled_retrain():
    """Called automatically every RETRAIN_HOURS hours."""
    logger.info("Scheduled retraining started...")
    try:
        train()
        logger.info("Scheduled retraining completed.")
    except Exception as e:
        logger.error("Scheduled retraining failed: %s", e)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("ML Service starting up...")
    loaded = recommendation_model.load()
    if not loaded:
        logger.info("No model found — training from scratch...")
        try:
            train()
        except Exception as e:
            logger.warning("Initial training failed (not enough data?): %s", e)

    scheduler = BackgroundScheduler()
    scheduler.add_job(
        scheduled_retrain,
        "interval",
        hours=RETRAIN_HOURS,
        id="retrain_job",
    )
    scheduler.start()
    logger.info("Retraining scheduled every %s hours.", RETRAIN_HOURS)

    yield

    scheduler.shutdown()
    logger.info("ML Service shut down.")


app = FastAPI(
    title="Civic Platform ML Service",
    description="SVD-based recommendation engine for civic engagement",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="ok",
        model_loaded=recommendation_model.is_loaded,
        model_version=recommendation_model.model_version,
        total_interactions=get_interaction_count(),
    )


@app.post("/recommend", response_model=RecommendResponse)
def recommend(request: RecommendRequest):
    try:
        interactions_df = load_interactions()
        user_interactions = interactions_df[interactions_df["user_id"] == request.user_id]
        is_cold_start = len(user_interactions) == 0

        campaigns_df = load_active_campaigns()
        projects_df = load_active_projects()
        posts_df = load_accepted_posts()
        user_votes_df = load_user_votes()
        user_fundings_df = load_user_fundings()

        lc = request.limit_campaigns if request.limit_campaigns is not None else 5
        lp = request.limit_projects if request.limit_projects is not None else 5
        lposts = request.limit_posts if request.limit_posts is not None else 10

        campaign_ids = recommendation_model.recommend_campaigns(
            user_id=request.user_id,
            campaigns_df=campaigns_df,
            user_votes_df=user_votes_df,
            limit=lc,
            is_cold_start=is_cold_start,
        )
        project_ids = recommendation_model.recommend_projects(
            user_id=request.user_id,
            projects_df=projects_df,
            user_fundings_df=user_fundings_df,
            limit=lp,
            is_cold_start=is_cold_start,
        )
        post_ids = recommendation_model.recommend_posts(
            user_id=request.user_id,
            posts_df=posts_df,
            limit=lposts,
            is_cold_start=is_cold_start,
        )

        return RecommendResponse(
            user_id=request.user_id,
            recommended_campaign_ids=campaign_ids,
            recommended_project_ids=project_ids,
            recommended_post_ids=post_ids,
            model_version=recommendation_model.model_version,
            is_cold_start=is_cold_start,
        )

    except Exception as e:
        logger.error("Recommendation failed for user %s: %s", request.user_id, e)
        raise HTTPException(status_code=500, detail=str(e)) from e


@app.post("/retrain", response_model=RetrainResponse)
def retrain(background_tasks: BackgroundTasks):
    """
    Manually trigger model retraining.
    Runs in background so it does not block the response.
    """

    def do_retrain():
        try:
            result = train()
            logger.info("Manual retrain result: %s", result)
        except Exception as e:
            logger.error("Manual retrain failed: %s", e)

    background_tasks.add_task(do_retrain)

    return RetrainResponse(
        status="accepted",
        message="Retraining started in background",
        new_model_version="pending",
        training_samples=get_interaction_count(),
    )


@app.get("/model/info")
def model_info():
    return {
        "version": recommendation_model.model_version,
        "is_loaded": recommendation_model.is_loaded,
        "model_path": os.getenv("MODEL_PATH", "/app/models/svd_model.pkl"),
    }

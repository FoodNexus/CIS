"""
Run this script to train the model from scratch.
Usage: python -m app.train
"""
import logging

from app.data import load_interactions, get_interaction_count
from app.model import recommendation_model

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def train():
    count = get_interaction_count()
    logger.info("Total interactions in DB: %s", count)

    df = load_interactions()
    logger.info("Loaded %s interactions for training", len(df))

    result = recommendation_model.train(df)
    logger.info("Training result: %s", result)
    return result


if __name__ == "__main__":
    train()

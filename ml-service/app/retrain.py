"""
Scheduled retraining entry point (same as train).
Usage: python -m app.retrain
"""
from app.train import train

if __name__ == "__main__":
    train()

from sqlalchemy import create_engine, text
import pandas as pd
import os


def get_engine():
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "3306")
    name = os.getenv("DB_NAME", "civic_platform")
    user = os.getenv("DB_USER", "civic_user")
    password = os.getenv("DB_PASSWORD", "civic_password")
    url = f"mysql+pymysql://{user}:{password}@{host}:{port}/{name}"
    return create_engine(url)


def load_interactions() -> pd.DataFrame:
    """
    Load all user interactions from the database.
    Returns a DataFrame with columns:
      user_id, entity_type, entity_id, action, created_at, weight
    """
    engine = get_engine()
    query = """
        SELECT
            ui.user_id,
            ui.entity_type,
            ui.entity_id,
            ui.action,
            ui.created_at,
            CASE ui.action
                WHEN 'FUND'    THEN 4.0
                WHEN 'VOTE'    THEN 3.0
                WHEN 'COMMENT' THEN 3.0
                WHEN 'ATTEND'  THEN 2.0
                WHEN 'LIKE'    THEN 2.0
                WHEN 'VIEW'    THEN 1.0
                ELSE 1.0
            END as weight
        FROM user_interactions ui
        JOIN `user` u ON ui.user_id = u.id
        WHERE u.is_admin = false
          AND (u.user_type IS NULL OR u.user_type != 'DONOR')
        ORDER BY ui.created_at DESC
    """
    with engine.connect() as conn:
        df = pd.read_sql(text(query), conn)
    return df


def load_active_campaigns() -> pd.DataFrame:
    engine = get_engine()
    query = """
        SELECT
            c.id,
            c.name,
            c.type,
            (SELECT COUNT(*) FROM campaign_vote cv WHERE cv.campaign_id = c.id) AS vote_count,
            c.created_at,
            c.needed_amount
        FROM campaign c
        WHERE c.status = 'ACTIVE'
    """
    with engine.connect() as conn:
        return pd.read_sql(text(query), conn)


def load_active_projects() -> pd.DataFrame:
    engine = get_engine()
    query = """
        SELECT id, title, current_amount, goal_amount,
               vote_count, start_date
        FROM project
        WHERE status NOT IN ('COMPLETED', 'CANCELLED')
    """
    with engine.connect() as conn:
        return pd.read_sql(text(query), conn)


def load_accepted_posts() -> pd.DataFrame:
    engine = get_engine()
    query = """
        SELECT id, type, likes_count, created_at
        FROM post
        WHERE status = 'ACCEPTED'
        ORDER BY created_at DESC
        LIMIT 200
    """
    with engine.connect() as conn:
        return pd.read_sql(text(query), conn)


def load_user_votes() -> pd.DataFrame:
    """Load which users already voted on which campaigns"""
    engine = get_engine()
    query = "SELECT user_id, campaign_id FROM campaign_vote"
    with engine.connect() as conn:
        return pd.read_sql(text(query), conn)


def load_user_fundings() -> pd.DataFrame:
    """Load which users already funded which projects"""
    engine = get_engine()
    query = "SELECT user_id, project_id FROM project_funding"
    with engine.connect() as conn:
        return pd.read_sql(text(query), conn)


def get_interaction_count() -> int:
    engine = get_engine()
    with engine.connect() as conn:
        result = conn.execute(text("SELECT COUNT(*) FROM user_interactions"))
        return int(result.scalar() or 0)

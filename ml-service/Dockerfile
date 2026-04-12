FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
# Pre-install numpy 1.26 so scikit-surprise does not pull numpy 2.x during build
RUN pip install --no-cache-dir "numpy==1.26.4" "scipy==1.11.4" \
    && pip install --no-cache-dir -r requirements.txt
COPY app/ ./app/
RUN mkdir -p /app/models
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

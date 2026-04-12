# MLOps stack (Spring Boot + FastAPI + MariaDB)

## Start the full stack

```bash
docker-compose up --build
```

Backend listens on `http://localhost:8080/api` (context path `/api`). Set `JWT_SECRET` in the environment when running in production.

## Start only the ML service (with database)

```bash
docker-compose up ml-service mariadb
```

## Manually trigger retraining (Python API)

```bash
curl -X POST http://localhost:8000/retrain
```

## Check ML service health

```bash
curl http://localhost:8000/health
```

## Check model info

```bash
curl http://localhost:8000/model/info
```

## Get recommendations for user 1 directly from the ML API

```bash
curl -X POST http://localhost:8000/recommend \
  -H "Content-Type: application/json" \
  -d '{"user_id": 1, "limit_campaigns": 5, "limit_projects": 5, "limit_posts": 10}'
```

## FastAPI docs (Swagger)

Open `http://localhost:8000/docs` in a browser.

## CURL tests (via Spring Boot)

Replace `CITIZEN_TOKEN` / `ADMIN_TOKEN` with valid JWTs.

**ML service health**

```bash
curl http://localhost:8000/health
```

**Personalized feed (Spring Boot)**

```bash
curl -X GET http://localhost:8080/api/recommendations/feed \
  -H "Authorization: Bearer $CITIZEN_TOKEN"
```

**Admin triggers retrain**

```bash
curl -X POST http://localhost:8080/api/admin/ml/retrain \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**ML service down — feed should still return 200 with empty or fallback content**

```bash
docker-compose stop ml-service
curl -X GET http://localhost:8080/api/recommendations/feed \
  -H "Authorization: Bearer $CITIZEN_TOKEN"
docker-compose start ml-service
```

Local development without Docker: run MariaDB, start the backend on port `8081` (default in `application.yml`), and the ML service on port `8000`. Set `ML_SERVICE_URL=http://localhost:8000` if the ML API is not on the default host.

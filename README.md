# Civic Platform (CIS)

Full-stack civic engagement platform inside NutriFlow, built to manage users, campaigns, events, projects, content interactions, recommendations, and impact tracking.

This README is intentionally Git-focused and covers project overview, stack, setup, testing, and contribution workflow.

## Overview

The CIS module includes:
- A Spring Boot backend (`civic-platform-backend`)
- An Angular frontend (`civic-platform-frontend`)
- A dedicated ML service (`ml-service`)
- Local orchestration with Docker Compose (`docker-compose.dev.yml`)

### Major capabilities
- Authentication/authorization with role and user-type separation
- Campaign lifecycle and vote-based launch process
- Event registration, participation, and attendance workflows
- Project creation, voting, funding, and completion
- Posts/comments/likes with engagement signals
- Notification flows and PDF generation
- Workflow insights and recommendation endpoints (including ML-assisted flows)

### Recently added / emphasized services and features
- `Project Insight` workflow scoring endpoints
- Batch workflow ranking endpoint for projects
- ML service integration (`ml-service`) for recommendation-related use cases
- Civic feed/recommendation support consumed by frontend modules

## Repository Structure

```text
CIS/
├── civic-platform-backend/      # Spring Boot API
├── civic-platform-frontend/     # Angular app
├── ml-service/                  # ML/recommendation service
├── keycloak/                    # Local auth resources
├── scripts/                     # Utility scripts
├── docker-compose.dev.yml       # Dev orchestration
└── docker-compose.yml
```

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Backend | Java 17+ | Core programming language |
| Backend | Spring Boot 3.x | Application framework and runtime |
| Backend | Spring Security | Authentication and authorization |
| Backend | Spring Data JPA + Hibernate | ORM and persistence layer |
| Backend | MariaDB | Relational database |
| Backend | MapStruct | DTO and entity mapping |
| Backend | Bean Validation | Request and model validation |
| Backend | SpringDoc OpenAPI | API documentation |
| Backend | Lombok | Boilerplate code reduction |
| Frontend | Angular 17+ | Frontend framework |
| Frontend | TypeScript | Type-safe application code |
| Frontend | RxJS | Reactive streams and async handling |
| Frontend | Angular Router + Forms | Routing and form management |
| Frontend | TailwindCSS | Utility-first styling |
| ML / Platform | Python-based `ml-service` (Dockerized) | Recommendation and ML workflows |
| ML / Platform | Keycloak | Identity provider and SSO flows |
| ML / Platform | Docker Compose | Local multi-service orchestration |

## Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+
- Docker + Docker Compose

### Option A: Run with Docker Compose (recommended)

From `CIS/`:

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts the main CIS dependencies/services for local development (DB, backend, ML service and related containers defined in compose).

### Option B: Run services manually

#### 1) Backend

```bash
cd civic-platform-backend
mvn clean spring-boot:run
```

#### 2) Frontend

```bash
cd civic-platform-frontend
npm install
npm start
```

#### 3) ML Service

Use Docker compose for local consistency, or run manually from `ml-service/` based on that service's runtime requirements.

## API and Dev Access

- Backend API base (local): `http://localhost:8081/api` (depending on your active config)
- OpenAPI / Swagger: check backend SpringDoc path in your running profile
- Frontend (local): `http://localhost:4200`

## Testing and Quality

### Backend tests

```bash
cd civic-platform-backend
mvn test
```

Recommended for CI:

```bash
mvn clean verify
```

The backend test setup uses JUnit 5 and Mockito via `spring-boot-starter-test`.
JaCoCo is configured in Maven to:
- generate XML/HTML reports during `verify`
- enforce minimum coverage thresholds:
  - line coverage: `>= 80%`
  - branch coverage: `>= 70%`

### Frontend tests

```bash
cd civic-platform-frontend
npm test
```

### Linting

```bash
cd civic-platform-frontend
npm run lint
```

### Suggested quality baseline
- Unit/integration test coverage tracked in CI
- Static analysis enforced in pipeline (SonarQube) and during development (SonarLint)
- PRs should pass tests and lint before merge

### SonarQube / SonarLint

`civic-platform-backend` includes Maven Sonar integration (`sonar-maven-plugin`) and JaCoCo report wiring.

Typical local scan command:

```bash
cd civic-platform-backend
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://<your-sonarqube-host>:9000 \
  -Dsonar.token=<your-token>
```

Recommended quality gate:
- New Bugs: `0`
- New Vulnerabilities: `0`
- New Security Hotspots Reviewed: `100%`
- Coverage on New Code: `>= 80%`
- Duplications on New Code: `< 3%`

## Git Workflow

### Branching
- `main`: stable branch
- feature branches: `feature/<scope>-<short-description>`
- bugfix branches: `fix/<scope>-<short-description>`

### Commit style
- Use clear, scoped messages (Conventional Commits recommended):
  - `feat(campaign): add launch vote threshold validation`
  - `fix(project): prevent double-vote from same user`

### Pull requests
Each PR should include:
- Functional summary (what/why)
- Test evidence (commands + results)
- API/UI impact notes
- Migration/config notes if applicable

## Contributors

### How to contribute
1. Fork or clone the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-change
   ```
3. Implement changes and run tests/lint.
4. Commit and push your branch.
5. Open a Pull Request with context and verification notes.

### Contribution standards
- Keep changes focused and reviewable.
- Add/adjust tests for behavioral changes.
- Avoid committing secrets or environment-specific credentials.

## License

ISC (or repository-defined license).

## Support

For issues or questions, open a repository issue and include:
- Service/module concerned
- Reproduction steps
- Logs/screenshots where relevant

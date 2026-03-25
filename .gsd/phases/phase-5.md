# Phase 5: Python Backend Setup & Scraping Hooks

> **Milestone**: Android & Python Recommender MVP
> **Objective**: Initialize Python FastAPI project. Implement `aiohttp`/`playwright` scraper hooks for Myntra, Amazon, Ajio, and Nykaa with basic anti-bot handling.

## Requirements

The codebase must contain a new Python backend project in the `backend/` directory.
- A `requirements.txt` or `pyproject.toml` with `fastapi`, `uvicorn`, `aiohttp`, `playwright`, `beautifulsoup4`, `redis`.
- Project structure must be class-based as specified in `MVP.MD` and `SPEC.md`.

## Execution Plan

### 1. Initialize Python Backend Project
- **Target Directory**: `backend/`
- **Details**: 
  - Create a virtual environment (`python -m venv venv`).
  - Create `requirements.txt` with necessary dependencies.
  - Create `main.py` holding a basic FastAPI app.
  - Create `.env` for configuration (Redis URLs, etc.).

### 2. Implement Core FastAPI App
- **Target File**: `backend/main.py`
- **Details**:
  - Define a basic health check endpoint `GET /health`.
  - Define the recommendation endpoint structure `POST /api/v1/recommendations`.
  - Create Pydantic models in `models.py` to represent the `user_profile` requested from the Android app (mirroring onboarding data).

### 3. Implement ScraperManager Base
- **Target File**: `backend/scrapers/manager.py`
- **Details**:
  - Create `ScraperManager` class to orchestrate platform scrapers.
  - Implement concurrent fetching (using `asyncio.gather`).

### 4. Implement Platform Web Scrapers
- **Target Files**: `backend/scrapers/myntra.py`, `backend/scrapers/amazon.py`, `backend/scrapers/ajio.py`, `backend/scrapers/nykaa.py`
- **Details**:
  - Create base classes using `aiohttp` or `playwright`.
  - Ensure rate-limiting logic (1 req/sec).
  - Extract basic fields: Title, Image URL, Price, Brand, URL.
  - *Note: In Phase 5, we only need the hooks/infrastructure mapping out how to scrape. Perfecting selectors might require iteration, but the structure must be solid.*

## Context Needed for Execution
- **`MVP.MD`**: For the specific payload definition of user preferences.
- **Python Architecture constraints**: Class-based, environment-driven (`.env`).

## Technical Debt / Risks
- Web scraping is prone to breakages if DOM selectors change.
- Amazon India often blocks basic `aiohttp` requests. We will need robust headers or `playwright` for some requests.

## State Update
Upon completing these steps, update `STATE.md` to reflect Phase 5 completion and progress to Phase 6.

# SmartSpec

> AI-powered software requirements generator

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=flat&logo=angular&logoColor=white)](https://angular.dev/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-AI-8E75B2?style=flat&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat)](LICENSE)

---

## Overview

**SmartSpec** transforms project descriptions into structured software requirements using AI. Simply describe your project, and let Gemini generate comprehensive user stories and specifications.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.4, Spring AI, Spring Data JPA |
| Frontend | Angular 21, TypeScript |
| Database | PostgreSQL 16, Flyway |
| AI | Google Gemini via Vertex AI |
| Infra | Docker, Docker Compose |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Google AI Studio API Key

### Run

```bash
# Clone and configure
git clone <repository-url>
cd SmartSpec

# Set your API key
export GOOGLE_API_KEY=your-api-key

# Start all services
docker-compose up --build
```

### Access

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| API Docs | http://localhost:8080/swagger-ui.html |

## Project Structure

```
SmartSpec/
├── backend/          # Spring Boot API
├── frontend/         # Angular SPA
├── docs/             # Documentation
└── docker-compose.yml
```

## Development

```bash
# Backend logs
docker-compose logs -f backend

# Frontend logs
docker-compose logs -f frontend

# Rebuild specific service
docker-compose up --build backend
```

## Roadmap

- [x] Sprint 0: Infrastructure Setup
- [ ] Sprint 1: Data Layer & Core Backend
- [ ] Sprint 2: AI Integration
- [ ] Sprint 3: Frontend Implementation
- [ ] Sprint 4: Polish & Production

---

<p align="center">
  <sub>Built with Spring AI and Google Gemini</sub>
</p>

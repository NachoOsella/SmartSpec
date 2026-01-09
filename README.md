<p align="center">
  <h1 align="center">PlanAI</h1>
  <p align="center">
    <strong>AI-powered project planning assistant</strong>
  </p>
  <p align="center">
    Chat with Google Gemini to build project plans with epics, user stories, and tasks - save and revisit anytime
  </p>
</p>

<p align="center">
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"></a>
  <a href="https://angular.dev/"><img src="https://img.shields.io/badge/Angular-21-DD0031?style=for-the-badge&logo=angular&logoColor=white" alt="Angular"></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"></a>
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"></a>
</p>

<p align="center">
  <a href="https://ai.google.dev/"><img src="https://img.shields.io/badge/Google%20Gemini-AI-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white" alt="Gemini"></a>
  <a href="https://docs.docker.com/compose/"><img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"></a>
</p>

---

## About The Project

**PlanAI** is a conversational AI application that helps users plan software projects through natural dialogue. Instead of filling out forms, users simply **chat with Google Gemini** to describe their project ideas, and the AI helps structure them into a proper hierarchy of epics, user stories, and tasks.

### How It Works

1. **Start a conversation** - Describe your project idea in plain language
2. **AI builds the plan** - Gemini helps you identify epics, break them into user stories, and define tasks
3. **Refine through chat** - Ask questions, add details, or restructure as needed
4. **Save your plan** - All plans are saved to the database for future reference
5. **View anytime** - Come back to see, edit, or continue building your plans

### Key Features

- **Chat-First Planning**: Natural conversation with AI to build project plans
- **Hierarchical Structure**: Organize work as Projects → Epics → User Stories → Tasks
- **Persistent Storage**: All plans and chat history saved to PostgreSQL
- **Plan Viewer**: Browse and review saved plans with full hierarchy
- **RESTful API**: Well-documented API with Swagger UI
- **Modern UI**: Angular SPA with reactive chat interface

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                │
│                    Angular 21 + TypeScript                      │
│              Chat Interface │ Plan Viewer │ Signals             │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────▼───────────────────────────────────────┐
│                         BACKEND                                 │
│                    Spring Boot 3.4.1                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Controllers │──│  Services   │──│   Google Gemini SDK     │  │
│  │  REST API   │  │   Logic     │  │   (Chat & Planning)     │  │
│  └─────────────┘  └──────┬──────┘  └─────────────────────────┘  │
│                          │                                      │
│  ┌───────────────────────▼──────────────────────────────────┐   │
│  │              Spring Data JPA + Hibernate                 │   │
│  └───────────────────────┬──────────────────────────────────┘   │
└──────────────────────────┼──────────────────────────────────────┘
                           │ JDBC
┌──────────────────────────▼──────────────────────────────────────┐
│                      PostgreSQL 16                              │
│           Projects │ Epics │ Stories │ Tasks │ Chat             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Model

PlanAI uses a hierarchical structure to organize project plans:

```
┌─────────────────┐
│    PROJECTS     │  Container for a complete plan
├─────────────────┤
│ id (UUID)       │
│ name            │
│ description     │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │ 1:N
┌────────▼────────┐
│     EPICS       │  High-level features or themes
├─────────────────┤
│ id, project_id  │
│ title           │
│ description     │
│ priority        │  HIGH, MEDIUM, LOW
│ status          │  TODO, IN_PROGRESS, DONE
│ order           │
└────────┬────────┘
         │ 1:N
┌────────▼────────┐
│  USER STORIES   │  "As a [role], I want [goal], so that [benefit]"
├─────────────────┤
│ id, epic_id     │
│ title           │
│ as_a            │  Role (e.g., "registered user")
│ i_want          │  Goal (e.g., "to reset my password")
│ so_that         │  Benefit (e.g., "I can recover my account")
│ priority        │
│ status          │
│ order           │
└────────┬────────┘
         │ 1:N
┌────────▼────────┐
│     TASKS       │  Specific implementation work
├─────────────────┤
│ id, story_id    │
│ title           │
│ description     │
│ status          │  TODO, IN_PROGRESS, DONE
│ estimated_hours │
│ order           │
└─────────────────┘

┌─────────────────┐
│ CONVERSATIONS   │  Chat sessions with AI
├─────────────────┤
│ id, project_id  │
│ created_at      │
└────────┬────────┘
         │ 1:N
┌────────▼────────┐
│    MESSAGES     │  Individual chat messages
├─────────────────┤
│ id, conv_id     │
│ role            │  USER, ASSISTANT, SYSTEM
│ content         │
│ created_at      │
└─────────────────┘
```

---

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Java 21** | Core language with modern features |
| **Spring Boot 3.4** | Framework with auto-configuration |
| **Google Gemini SDK** | AI chat and planning capabilities |
| **Spring Data JPA** | Database access layer |
| **PostgreSQL 16** | Relational database |
| **Lombok** | Boilerplate reduction |
| **MapStruct** | DTO mapping |
| **SpringDoc OpenAPI** | API documentation |

### Frontend
| Technology | Purpose |
|------------|---------|
| **Angular 21** | Standalone components, Signals |
| **TypeScript 5.9** | Type-safe development |
| **RxJS** | Reactive programming |
| **SCSS** | Component-scoped styles |

### Infrastructure
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Service orchestration |
| **Maven** | Build tool |

---

## Project Structure

```
PlanAI/
├── backend/
│   ├── src/main/java/com/planai/
│   │   ├── config/          # Configuration (CORS, Gemini, OpenAPI)
│   │   ├── controller/      # REST API endpoints
│   │   ├── service/         # Business logic + AI service
│   │   ├── repository/      # Data access layer
│   │   ├── model/           # JPA entities (Project, Epic, Story, Task)
│   │   ├── dto/             # Request/Response DTOs
│   │   └── exception/       # Global exception handling
│   ├── src/main/resources/
│   │   ├── prompts/         # AI prompt templates
│   │   └── application.yml  # Configuration
│   └── Dockerfile
├── frontend/
│   ├── src/app/
│   │   ├── core/            # Services, models, interceptors
│   │   ├── features/
│   │   │   ├── chat/        # Chat interface components
│   │   │   ├── projects/    # Project list/detail components
│   │   │   └── plans/       # Plan viewer components
│   │   └── shared/          # Reusable UI components
│   └── Dockerfile
├── docs/
│   └── JUNIOR_IMPLEMENTATION_GUIDE.md
└── docker-compose.yml
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Google AI Studio API Key ([Get one here](https://aistudio.google.com/apikey))

### Installation

```bash
# Clone the repository
git clone https://github.com/NachoOsella/PlanAI.git
cd PlanAI

# Create environment file
echo "GOOGLE_API_KEY=your-api-key-here" > .env

# Start all services
docker-compose up --build
```

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:4200 | Angular application |
| Backend API | http://localhost:8080/api | REST endpoints |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| Health Check | http://localhost:8080/api/health | Service status |

---

## API Overview

### Projects
```
POST   /api/v1/projects              Create a new project
GET    /api/v1/projects              List all projects
GET    /api/v1/projects/{id}         Get project details
GET    /api/v1/projects/{id}/full    Get project with full plan hierarchy
PUT    /api/v1/projects/{id}         Update project
DELETE /api/v1/projects/{id}         Delete project
```

### Epics
```
POST   /api/v1/projects/{id}/epics   Create epic in project
GET    /api/v1/projects/{id}/epics   List project epics
GET    /api/v1/epics/{id}            Get epic details
PUT    /api/v1/epics/{id}            Update epic
DELETE /api/v1/epics/{id}            Delete epic
```

### User Stories
```
POST   /api/v1/epics/{id}/stories    Create story in epic
GET    /api/v1/epics/{id}/stories    List epic stories
GET    /api/v1/stories/{id}          Get story details
PUT    /api/v1/stories/{id}          Update story
DELETE /api/v1/stories/{id}          Delete story
```

### Tasks
```
POST   /api/v1/stories/{id}/tasks    Create task in story
GET    /api/v1/stories/{id}/tasks    List story tasks
GET    /api/v1/tasks/{id}            Get task details
PUT    /api/v1/tasks/{id}            Update task
DELETE /api/v1/tasks/{id}            Delete task
```

### AI Chat
```
POST   /api/v1/projects/{id}/chat    Send message to AI for planning assistance
GET    /api/v1/projects/{id}/conversations    Get chat history
```

---

## Development

### Running Locally

```bash
# Backend only (requires local PostgreSQL)
cd backend
./mvnw spring-boot:run

# Frontend only
cd frontend
npm install
npm start
```

### Useful Commands

```bash
# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Rebuild specific service
docker-compose up --build backend

# Database access
docker-compose exec db psql -U postgres -d planai_db

# Run tests
cd backend && ./mvnw test
cd frontend && npm test
```

---

## Roadmap

- [x] **Sprint 0**: Infrastructure Setup - Docker, project structure, health endpoint
- [ ] **Sprint 1**: Data Layer - Entities (Project, Epic, Story, Task), Repositories, Services
- [ ] **Sprint 2**: AI Integration - Gemini SDK, chat service, prompt engineering
- [ ] **Sprint 3**: Frontend - Chat interface, plan viewer, state management
- [ ] **Sprint 4**: Production - Error handling, validation, UX polish

See [JUNIOR_IMPLEMENTATION_GUIDE.md](docs/JUNIOR_IMPLEMENTATION_GUIDE.md) for detailed implementation steps.

---

## Author

**Nacho Osella**

[![GitHub](https://img.shields.io/badge/GitHub-NachoOsella-181717?style=for-the-badge&logo=github)](https://github.com/NachoOsella)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin)](https://linkedin.com/in/nachoosella)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Built with Google Gemini AI</sub>
</p>

# SmartSpec Development Roadmap

This guide provides a step-by-step instructions to build **SmartSpec**, an AI-powered software requirements generator using Spring Boot (Java), Angular, PostgreSQL, and Google Gemini via Spring AI.

## 🏁 Prerequisites
- Docker & Docker Compose installed.
- Google AI Studio API Key.

---

## 🚀 Sprint 0: Infrastructure Setup (Completed)
*Goal: Initialize the project structure and Docker environment.*

- [x] Create project directories (`backend`, `frontend`).
- [x] Initialize Spring Boot project (Web, JPA, Postgres, Lombok, Spring AI).
- [x] Create `docker-compose.yml` with Postgres, Backend, and Frontend services.
- [x] Initialize Angular application.
- [x] Create Backend Dockerfile (Multi-stage Maven build).

---

## 🗄️ Sprint 1: Data Layer & Core Backend
*Goal: Set up the database schema and enable project management.*

### Task 1.1: Database Configuration & Entities
**File:** `backend/src/main/resources/application.properties`
1.  Ensure datasource URL points to `jdbc:postgresql://db:5432/smartspec_db`.
2.  Set `spring.jpa.hibernate.ddl-auto=update` for MVP schema management.

**File:** `backend/src/main/java/com/smartspec/backend/model/Project.java`
1.  Create Entity class `Project` with `@Entity`.
2.  Fields: `id` (UUID, @Id), `name` (String), `description` (Text), `createdAt` (LocalDateTime).

**File:** `backend/src/main/java/com/smartspec/backend/model/GeneratedSpec.java`
1.  Create Entity class `GeneratedSpec`.
2.  Fields: `id` (Long, @Id), `project` (@ManyToOne Project), `content` (String/JSON), `createdAt`.

### Task 1.2: Repository Layer
**File:** `backend/src/main/java/com/smartspec/backend/repository/ProjectRepository.java`
1.  Interface extending `JpaRepository<Project, UUID>`.

### Task 1.3: Service & Controller
**File:** `backend/src/main/java/com/smartspec/backend/service/ProjectService.java`
1.  Methods to save and retrieve projects.

**File:** `backend/src/main/java/com/smartspec/backend/controller/ProjectController.java`
1.  `@RestController` at `/api/projects`.
2.  `POST /`: Create project.
3.  `GET /{id}`: Get project details.
4.  Enable CORS (`@CrossOrigin`).

---

## 🧠 Sprint 2: AI Integration (The Core)
*Goal: Connect to Google Gemini to generate requirements using Spring AI.*

### Task 2.1: AI Service
**File:** `backend/src/main/java/com/smartspec/backend/service/AiService.java`
1.  Inject `ChatClient` (from Spring AI).
2.  Create method `generateRequirements(String description)`.
3.  **Prompt Engineering:**
    - Use `PromptTemplate` to structure the request.
    - Ask for JSON output.
    - Example: `ChatResponse response = chatClient.call(new Prompt(template.create(model)));`

### Task 2.2: Generation Endpoint
**File:** `backend/src/main/java/com/smartspec/backend/controller/ProjectController.java`
1.  `POST /{id}/generate`:
    - Fetch project.
    - Call `AiService`.
    - Save result to `GeneratedSpec`.
    - Return result.

---

## 🎨 Sprint 3: Frontend Implementation
*Goal: Build the User Interface.*

### Task 3.1: Angular Services
1.  Generate interfaces: `ng g interface models/project`.
2.  Generate service: `ng g service services/project`.
3.  Implement methods in `ProjectService` to call `http://localhost:8080/api/projects`.

### Task 3.2: Create Project Component
1.  Generate component: `ng g c components/create-project`.
2.  HTML: Add a Form with `Title` input and `Description` textarea.
3.  TS: On submit, call `createProject`.

### Task 3.3: Project Details & Results Component
1.  Generate component: `ng g c components/project-details`.
2.  TS: Fetch project data on `ngOnInit`.
3.  HTML:
    - Display project info.
    - Add a "Generate Specs" button.
    - **Loading State:** Show a spinner while waiting for the AI.
    - **Results:** Display the generated User Stories.

---

## 🚢 Sprint 4: Polish & Docker Production
*Goal: Finalize for deployment.*

### Task 4.1: Production Dockerfile
**File:** `frontend/Dockerfile`
1.  Update to Multi-stage build.
    - Stage 1: Build Angular app (`npm run build`).
    - Stage 2: Nginx Alpine image. Copy `dist/` output to `/usr/share/nginx/html`.
    - Add `nginx.conf` to handle SPA routing.

### Task 4.2: Final Testing
1.  Run `docker-compose up --build`.
2.  Verify the full flow:
    - Create Project -> Generate Specs -> View Results.
    - Check database persistence.

---

## 🛠️ Useful Commands

```bash
# Start everything
docker-compose up --build

# Backend Logs
docker-compose logs -f backend

# Frontend Logs
docker-compose logs -f frontend
```

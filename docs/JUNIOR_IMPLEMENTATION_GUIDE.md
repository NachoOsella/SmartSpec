# PlanAI: Junior Developer Implementation Guide

> **Your Mission**: Build a conversational AI project planning tool.
> Users chat with Google Gemini to create project plans (epics, user stories, tasks) and save them for later.

---

## Table of Contents

1. [Project Understanding](#1-project-understanding)
2. [Development Environment Setup](#2-development-environment-setup)
3. [Sprint 1: Backend Data Layer](#3-sprint-1-backend-data-layer)
4. [Sprint 2: AI Integration](#4-sprint-2-ai-integration)
5. [Sprint 3: Frontend Implementation](#5-sprint-3-frontend-implementation)
6. [Sprint 4: Production Polish](#6-sprint-4-production-polish)
7. [Testing Strategy](#7-testing-strategy)
8. [Common Mistakes to Avoid](#8-common-mistakes-to-avoid)
9. [Git Workflow](#9-git-workflow)

---

## 1. Project Understanding

### 1.1 What Are We Building?

PlanAI is a web application where:

1. **User starts a chat** with AI about their project idea
2. **AI helps structure the plan** into epics, user stories, and tasks
3. **User saves the plan** to view and edit anytime
4. **Chat history is preserved** so users can continue conversations

### 1.2 The Data Hierarchy

```
PROJECT (e.g., "E-Commerce Platform")
    │
    ├── EPIC (e.g., "User Authentication")
    │       │
    │       ├── USER STORY (e.g., "As a user, I want to register...")
    │       │       │
    │       │       ├── TASK (e.g., "Create registration form")
    │       │       ├── TASK (e.g., "Implement email validation")
    │       │       └── TASK (e.g., "Add password strength check")
    │       │
    │       └── USER STORY (e.g., "As a user, I want to login...")
    │               └── ...tasks...
    │
    └── EPIC (e.g., "Product Catalog")
            └── ...stories and tasks...
```

**Why this hierarchy?**
- **Epics** = Big features that take weeks/months
- **User Stories** = Smaller deliverables following "As a... I want... So that..."
- **Tasks** = Actual work items (hours/days)

### 1.3 Current Project State (Sprint 0 Complete)

**What's Already Done:**
- [x] Docker Compose configuration (3 services: db, backend, frontend)
- [x] Backend skeleton with health endpoint
- [x] Global exception handling
- [x] CORS configuration
- [x] Frontend Angular app skeleton
- [x] Frontend Angular app skeleton

**What You Need to Build:**
- [ ] JPA Entities for all tables (JPA will auto-generate tables)
- [ ] Repositories, DTOs, Mappers
- [ ] Services with business logic
- [ ] REST Controllers
- [ ] AI Chat Service with Gemini SDK
- [ ] Frontend chat interface
- [ ] Frontend plan viewer

---

## 2. Development Environment Setup

### 2.1 Prerequisites Checklist

Before starting, verify you have:

- [x] Docker 20.x or higher (`docker --version`)
- [x] Docker Compose 2.x or higher (`docker-compose --version`)
- [x] Java 21 (`java --version`)
- [x] Node 20.x (`node --version`)
- [x] npm 10.x (`npm --version`)
- [x] Git 2.x (`git --version`)
- [x] Google AI Studio API Key from https://aistudio.google.com/apikey

### 2.2 Setup Commands

```bash
# 1. Navigate to project
cd ~/Documents/practica/PlanAi

# 2. Create .env file with your API key
echo "GOOGLE_API_KEY=your-actual-api-key" > .env
echo "POSTGRES_USER=postgres" >> .env
echo "POSTGRES_PASSWORD=postgres" >> .env
echo "POSTGRES_DB=planai_db" >> .env

# 3. Start all services
docker-compose up --build

# 4. Verify backend is running
curl http://localhost:8080/api/health
```

### 2.3 Development Options

**Option A: Full Docker (Simpler but slower rebuilds)**
```bash
docker-compose up --build
```

**Option B: Hybrid (Faster for development)**
```bash
# Terminal 1: Database only
docker-compose up db

# Terminal 2: Backend locally
cd backend && ./mvnw spring-boot:run

# Terminal 3: Frontend locally  
cd frontend && npm install && npm start
```

---

## 3. Sprint 1: Backend Data Layer

> **Goal**: Create the complete data layer with entities, repositories, services, and controllers.

### 3.1 Create Enums

**Task: Create status and priority enums**

| File to Create | Enum Values |
|----------------|-------------|
| `model/Priority.java` | HIGH, MEDIUM, LOW |
| `model/Status.java` | TODO, IN_PROGRESS, DONE |
| `model/MessageRole.java` | USER, ASSISTANT, SYSTEM |

**Why enums instead of strings?**
- Compile-time safety (typos caught immediately)
- IDE autocomplete
- All valid values documented in one place

---

### 3.2 Create JPA Entities

**Task: Create entity classes**

| Entity | File Path | Key Fields |
|--------|-----------|------------|
| Project | `model/Project.java` | id, name, description, epics (OneToMany), conversations (OneToMany) |
| Epic | `model/Epic.java` | id, project (ManyToOne), title, description, priority, status, order, stories (OneToMany) |
| UserStory | `model/UserStory.java` | id, epic (ManyToOne), title, asA, iWant, soThat, priority, status, order, tasks (OneToMany) |
| Task | `model/Task.java` | id, userStory (ManyToOne), title, description, status, estimatedHours, order |
| Conversation | `model/Conversation.java` | id, project (ManyToOne), messages (OneToMany) |
| Message | `model/Message.java` | id, conversation (ManyToOne), role, content |

**Entity Best Practices to Follow:**
- [x] Use `@Getter`, `@Setter` instead of `@Data`
- [x] Use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with only `@Id` field
- [x] Use `@ToString(exclude = {...})` to prevent infinite loops
- [x] Use `FetchType.LAZY` for all `@OneToMany` relationships
- [x] Use `@Builder.Default` for collection initializations
- [x] Add helper methods like `addEpic()`, `removeEpic()` for bidirectional relationships

---

### 3.3 Create Repositories

**Task: Create repository interfaces**

| Repository | File Path | Custom Methods |
|------------|-----------|----------------|
| ProjectRepository | `repository/ProjectRepository.java` | `findAllByOrderByCreatedAtDesc()`, `findByIdWithEpics()` |
| EpicRepository | `repository/EpicRepository.java` | `findByProjectIdOrderByOrderAsc()` |
| UserStoryRepository | `repository/UserStoryRepository.java` | `findByEpicIdOrderByOrderAsc()` |
| TaskRepository | `repository/TaskRepository.java` | `findByUserStoryIdOrderByOrderAsc()` |
| ConversationRepository | `repository/ConversationRepository.java` | `findByProjectIdOrderByCreatedAtDesc()`, `findByIdWithMessages()` |
| MessageRepository | `repository/MessageRepository.java` | `findByConversationIdOrderByCreatedAtAsc()` |

**Why custom methods?**
- Spring Data JPA generates SQL from method names
- `findByProjectIdOrderByOrderAsc` becomes `SELECT * FROM epics WHERE project_id = ? ORDER BY order ASC`

---

### 3.4 Create DTOs

**Task: Create request/response DTOs**

**Request DTOs (in `dto/request/`):**
| DTO | Purpose | Fields |
|-----|---------|--------|
| CreateProjectRequest | Create new project | name (required), description |
| UpdateProjectRequest | Update project | name, description (both optional) |
| CreateEpicRequest | Create epic | title (required), description, priority |
| CreateStoryRequest | Create story | title, asA, iWant, soThat, priority |
| CreateTaskRequest | Create task | title (required), description, estimatedHours |
| ChatRequest | Send chat message | message (required), conversationId (optional) |

**Response DTOs (in `dto/response/`):**
| DTO | Purpose |
|-----|---------|
| ProjectResponse | Basic project info + counts |
| ProjectDetailResponse | Project with full hierarchy (epics, stories, tasks) |
| EpicResponse | Epic with stories list |
| UserStoryResponse | Story with tasks list |
| TaskResponse | Task details |
| ChatResponse | Conversation ID + user/assistant messages |

**Why DTOs?**
- Security: Don't expose internal fields
- Flexibility: API can change independently of database
- Validation: Request DTOs use `@NotBlank`, `@Size`, etc.

---

### 3.5 Create ModelMapper Mappers

**Task: Create mapper classes**

| Mapper | File Path | Methods |
|--------|-----------|---------|
| ProjectMapper | `mapper/ProjectMapper.java` | `toEntity()`, `toResponse()`, `toDetailResponse()` |
| EpicMapper | `mapper/EpicMapper.java` | `toEntity()`, `toResponse()` |
| UserStoryMapper | `mapper/UserStoryMapper.java` | `toEntity()`, `toResponse()` |
| TaskMapper | `mapper/TaskMapper.java` | `toEntity()`, `toResponse()` |
| ConversationMapper | `mapper/ConversationMapper.java` | `toResponse()`, `toMessageResponse()` |

**Why ModelMapper?**
- Runtime mapping (flexible and easy to use)
- Reduces boilerplate code significantly
- Handles nested objects and collections intelligently

**Implementation Guide:**
1.  **Create Configuration:**
    Create `config/MapperConfig.java` to define the `ModelMapper` bean.
    ```java
    @Configuration
    public class MapperConfig {
        @Bean
        public ModelMapper modelMapper() {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            return modelMapper;
        }
    }
    ```

2.  **Create Mapper Classes:**
    Inject `ModelMapper` into your classes using `@RequiredArgsConstructor`.
    ```java
    @Component
    @RequiredArgsConstructor
    public class ProjectMapper {
        private final ModelMapper modelMapper;
        
        public ProjectResponse toResponse(ProjectEntity entity) {
            return modelMapper.map(entity, ProjectResponse.class);
        }
    }
    ```

---

### 3.6 Create Custom Exceptions

**Task: Create exception classes**

| Exception | HTTP Status | When to Use |
|-----------|-------------|-------------|
| ResourceNotFoundException | 404 | Entity not found by ID |
| DuplicateResourceException | 409 | Trying to create duplicate |
| AiGenerationException | 503 | AI service fails |

**Base class:** Extend the existing `ApiException` class.

---

### 3.7 Create Services

**Task: Create service classes**

| Service | File Path | Key Methods |
|---------|-----------|-------------|
| ProjectService | `service/ProjectService.java` | `getAllProjects()`, `getProjectDetail()`, `createProject()`, `updateProject()`, `deleteProject()` |
| EpicService | `service/EpicService.java` | `getProjectEpics()`, `createEpic()`, `updateEpic()`, `deleteEpic()`, `reorderEpics()` |
| UserStoryService | `service/UserStoryService.java` | `getEpicStories()`, `createStory()`, `updateStory()`, `deleteStory()` |
| TaskService | `service/TaskService.java` | `getStoryTasks()`, `createTask()`, `updateTask()`, `deleteTask()` |

**Service Best Practices:**
- [ ] Use `@Transactional(readOnly = true)` at class level
- [ ] Override with `@Transactional` for write operations
- [ ] Use `@RequiredArgsConstructor` for dependency injection
- [ ] Throw `ResourceNotFoundException` when entity not found

---

### 3.8 Create Controllers

**Task: Create REST controller classes**

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| ProjectController | `/api/v1/projects` | GET, POST, PUT, DELETE for projects |
| EpicController | `/api/v1` | `/projects/{id}/epics`, `/epics/{id}` |
| UserStoryController | `/api/v1` | `/epics/{id}/stories`, `/stories/{id}` |
| TaskController | `/api/v1` | `/stories/{id}/tasks`, `/tasks/{id}` |

**Controller Best Practices:**
- [ ] Use `@Valid` on request body parameters
- [ ] Return proper HTTP status codes (201 for create, 204 for delete)
- [ ] Add Swagger annotations (`@Operation`, `@ApiResponse`)

---

### 3.9 Sprint 1 Verification

After completing all tasks, verify:

```bash
# Rebuild and restart
docker-compose down && docker-compose up --build

# Wait for startup, then test:

# 1. Create a project
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Project", "description": "Testing the API"}'

# 2. Create an epic (use project ID from step 1)
curl -X POST http://localhost:8080/api/v1/projects/{PROJECT_ID}/epics \
  -H "Content-Type: application/json" \
  -d '{"title": "User Authentication", "priority": "HIGH"}'

# 3. Check Swagger UI: http://localhost:8080/swagger-ui.html
```

**Checklist:**
- [ ] All CRUD operations work for projects
- [ ] All CRUD operations work for epics
- [ ] All CRUD operations work for user stories
- [ ] All CRUD operations work for tasks
- [ ] Swagger UI shows all endpoints
- [ ] Cascade delete works (delete project removes all children)

---

## 4. Sprint 2: AI Integration

> **Goal**: Integrate Google Gemini for conversational planning.

### 4.1 Gemini SDK Configuration

**Task: Create AI configuration**

| File to Create | Purpose |
|----------------|---------|
| `config/GenAiConfig.java` | Configure Gemini SDK client as Spring bean |

**Configuration properties (in application.yml):**
```yaml
genai:
  api-key: ${GOOGLE_API_KEY}
  model: gemini-2.5-flash
```

**Why a config class?**
- Creates a reusable `Client` bean
- Centralizes API key handling
- Supports both Gemini API and Vertex AI modes

---

### 4.2 Prompt Templates

**Task: Create prompt template files**

| File Path | Purpose |
|-----------|---------|
| `resources/prompts/planning-assistant.txt` | System prompt for planning conversations |
| `resources/prompts/structure-plan.txt` | Prompt to extract structured data from conversation |

**Planning Assistant Prompt Should:**
- Define AI as a project planning expert
- Explain the epic/story/task hierarchy
- Guide conversation toward structured output
- Request specific format for plan elements

**Structure Plan Prompt Should:**
- Take conversation history as input
- Extract epics, stories, tasks from the discussion
- Output valid JSON matching our data model

---

### 4.3 AI Service

**Task: Create AI service class**

| File to Create | Key Methods |
|----------------|-------------|
| `service/AiService.java` | `chat()`, `extractPlan()` |

**chat() method should:**
1. Load or create conversation
2. Save user message
3. Build prompt with conversation history
4. Call Gemini SDK
5. Save assistant response
6. Return both messages

**extractPlan() method should:**
1. Get full conversation history
2. Send to AI with structure prompt
3. Parse JSON response
4. Create entities (epics, stories, tasks)
5. Return created plan

**Error Handling:**
- [ ] Retry logic with exponential backoff (3 attempts)
- [ ] Handle empty responses
- [ ] Validate JSON structure
- [ ] Throw `AiGenerationException` on failure

---

### 4.4 AI Controller

**Task: Create AI controller**

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/projects/{id}/chat` | POST | Send message, get AI response |
| `/api/v1/projects/{id}/conversations` | GET | Get chat history |
| `/api/v1/projects/{id}/extract-plan` | POST | Extract structured plan from conversation |

---

### 4.5 Sprint 2 Verification

```bash
# 1. Create a project
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Task Manager", "description": "A simple task management app"}'

# 2. Start a chat (use project ID from step 1)
curl -X POST http://localhost:8080/api/v1/projects/{PROJECT_ID}/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to build a task management app. What features should it have?"}'

# 3. Continue the conversation (use conversationId from response)
curl -X POST http://localhost:8080/api/v1/projects/{PROJECT_ID}/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Yes, I want user authentication and task categories", "conversationId": "{CONV_ID}"}'

# 4. Extract plan from conversation
curl -X POST http://localhost:8080/api/v1/projects/{PROJECT_ID}/extract-plan
```

**Checklist:**
- [ ] Chat endpoint returns AI responses
- [ ] Conversation history is preserved
- [ ] Multiple messages in same conversation work
- [ ] Plan extraction creates epics, stories, tasks
- [ ] Errors are handled gracefully (not 500s)

---

## 5. Sprint 3: Frontend Implementation

> **Goal**: Build the Angular UI with chat interface and plan viewer.

### 5.1 Core Services

**Task: Create Angular services**

| Service | File Path | Purpose |
|---------|-----------|---------|
| ApiService | `core/services/api.service.ts` | HTTP client wrapper |
| ProjectService | `features/projects/services/project.service.ts` | Project CRUD |
| EpicService | `features/plans/services/epic.service.ts` | Epic CRUD |
| ChatService | `features/chat/services/chat.service.ts` | AI chat |

---

### 5.2 TypeScript Models

**Task: Create TypeScript interfaces**

| File Path | Interfaces |
|-----------|------------|
| `core/models/project.model.ts` | Project, ProjectDetail, CreateProjectRequest |
| `core/models/epic.model.ts` | Epic, UserStory, Task |
| `core/models/chat.model.ts` | Message, Conversation, ChatRequest, ChatResponse |

---

### 5.3 State Management

**Task: Create signal-based stores**

| Store | File Path | State |
|-------|-----------|-------|
| ProjectStore | `features/projects/state/project.store.ts` | projects, selectedProject, loading, error |
| ChatStore | `features/chat/state/chat.store.ts` | messages, conversationId, sending |

**Why Signals?**
- Built into Angular (no external library)
- Simpler than RxJS BehaviorSubjects
- Better change detection performance

---

### 5.4 Shared Components

**Task: Create reusable UI components**

| Component | Purpose |
|-----------|---------|
| LoadingSpinner | Show loading state |
| ErrorMessage | Display errors with dismiss |
| Card | Container with shadow |
| Button | Styled button with variants |
| ChatBubble | Message display (user vs assistant) |

---

### 5.5 Feature Components

**Task: Create feature components**

| Component | Route | Purpose |
|-----------|-------|---------|
| ProjectList | `/projects` | List all projects |
| ProjectCreate | `/projects/new` | Create project form |
| ProjectDetail | `/projects/:id` | Project overview + chat + plan |
| ChatPanel | (embedded) | Chat interface |
| PlanViewer | (embedded) | Hierarchical plan display |
| EpicCard | (embedded) | Single epic with stories |
| StoryCard | (embedded) | Single story with tasks |

---

### 5.6 Routing

**Task: Configure routes in `app.routes.ts`**

| Path | Component | Description |
|------|-----------|-------------|
| `/` | Redirect | Redirect to /projects |
| `/projects` | ProjectList | Project list page |
| `/projects/new` | ProjectCreate | Create project page |
| `/projects/:id` | ProjectDetail | Project detail with chat |

---

### 5.7 Sprint 3 Verification

**Checklist:**
- [ ] Project list shows all projects
- [ ] Can create new project
- [ ] Project detail page loads
- [ ] Chat interface sends messages
- [ ] AI responses display correctly
- [ ] Plan viewer shows hierarchy
- [ ] Loading states work
- [ ] Error states display properly

---

## 6. Sprint 4: Production Polish

> **Goal**: Add validation, error handling, and UX improvements.

### 6.1 Backend Polish

**Tasks:**
- [ ] Add input validation to all DTOs
- [ ] Add logging to all services
- [ ] Implement proper error messages
- [ ] Add pagination to list endpoints
- [ ] Add search/filter capabilities

### 6.2 Frontend Polish

**Tasks:**
- [ ] Add form validation with error messages
- [ ] Add confirmation dialogs for delete actions
- [ ] Add toast notifications for success/error
- [ ] Implement responsive design
- [ ] Add keyboard shortcuts for chat

### 6.3 Performance

**Tasks:**
- [ ] Add loading indicators for AI calls
- [ ] Implement optimistic updates
- [ ] Add caching for project list

---

## 7. Testing Strategy

### Backend Tests

| Type | What to Test |
|------|--------------|
| Unit Tests | Service methods with mocked repositories |
| Integration Tests | Controllers with real database (Testcontainers) |
| AI Tests | Mock Gemini responses for predictable tests |

### Frontend Tests

| Type | What to Test |
|------|--------------|
| Unit Tests | Services with mocked HTTP |
| Component Tests | Components with mocked stores |

---

## 8. Common Mistakes to Avoid

### Backend Mistakes

| Mistake | Why It's Bad | How to Fix |
|---------|--------------|------------|
| Using `@Data` on entities | Breaks Hibernate proxies | Use `@Getter`, `@Setter`, explicit `@EqualsAndHashCode` |
| Forgetting `FetchType.LAZY` | Loads entire database | Always use LAZY for collections |
| No `@Transactional` | Data inconsistency | Add to service methods that write |
| Returning entities from controllers | Security risk | Always use DTOs |

### Frontend Mistakes

| Mistake | Why It's Bad | How to Fix |
|---------|--------------|------------|
| Not unsubscribing | Memory leaks | Use `takeUntilDestroyed()` or async pipe |
| Direct state mutation | Breaks change detection | Always use `signal.update()` |
| No error handling | Bad UX | Wrap API calls in try/catch |

---

## 9. Git Workflow

### Branch Naming

```
feature/sprint1-entities
feature/sprint2-ai-service
feature/sprint3-chat-ui
fix/validation-error
```

### Commit Messages

```
feat(backend): add Epic entity and repository
feat(backend): implement EpicService with CRUD operations
feat(frontend): create chat interface component
fix(backend): handle empty AI responses
docs: update README with new data model
```

### Pull Request Process

1. Create feature branch from `main`
2. Make changes with atomic commits
3. Push and create PR
4. Self-review the diff
5. Merge to main

---

## Quick Reference

### File Locations

| Type | Backend Path | Frontend Path |
|------|--------------|---------------|
| Entities | `src/main/java/com/planai/model/` | - |
| DTOs | `src/main/java/com/planai/dto/` | - |
| Services | `src/main/java/com/planai/service/` | `src/app/core/services/` |
| Controllers | `src/main/java/com/planai/controller/` | - |
| Components | - | `src/app/features/*/` |
| Models | - | `src/app/core/models/` |

### Useful Commands

```bash
# Backend
./mvnw spring-boot:run          # Run locally
./mvnw test                     # Run tests
./mvnw clean install            # Full build

# Frontend
npm start                       # Dev server
npm test                        # Run tests
npm run build                   # Production build

# Docker
docker-compose up --build       # Start all
docker-compose logs -f backend  # View logs
docker-compose down             # Stop all
```

---

**Good luck! Take it one sprint at a time.**

# SmartSpec: The Definitive Implementation Playbook

This is not just a guide; it is the **exact specification** for building SmartSpec to industry-leading standards. Every instruction here is designed to produce code that is maintainable, scalable, and secure.

---

## 🏗️ 0. Project Standards & Conventions

**Before writing a single line of code:**

*   **Java**: Version 21. Use `var` for local variables where type is obvious.
*   **Lombok**: Use `@Data` for DTOs, but **avoid** `@Data` on Entities (use `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`). **Reason**: Hibernate proxy issues.
*   **JSON**: All API responses must be wrapped in a standard envelope.
*   **API Paths**: Plural nouns. `POST /api/v1/projects`, not `/api/project/create`.
*   **Commits**: Conventional Commits. `feat(backend): add project entity`.

---

## 🗄️ Phase 1: The Domain Layer (Backend)

**Goal**: Create a data model that is impossible to corrupt.

### 1.1. Project Entity Specification
**File**: `backend/src/main/java/com/smartspec/backend/model/Project.java`

```java
package com.smartspec.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter
@ToString
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Crucial for JPA performance
public class Project {

    @Id
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Use Enums, never strings for status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 1.2. The DTO Pattern (Strict Separation)
**Concept**: We never return an Entity to the frontend.
**Library**: `MapStruct` is mandatory.

**File**: `backend/src/main/java/com/smartspec/backend/dto/ProjectRequest.java`
```java
public record ProjectRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 chars")
    String name,

    @Size(max = 2000, message = "Description too long")
    String description
) {} // Records are immutable and perfect for DTOs
```

**File**: `backend/src/main/java/com/smartspec/backend/mapper/ProjectMapper.java`
```java
@Mapper(componentModel = "spring")
public interface ProjectMapper {
    Project toEntity(ProjectRequest request);
    ProjectResponse toResponse(Project project);
}
```

### 1.3. Service Layer Transaction Management
**Rule**: All business logic happens here. Controllers are just routers.

**File**: `backend/src/main/java/com/smartspec/backend/service/ProjectService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Default to fast read-only transactions
public class ProjectService {
    private final ProjectRepository repository;
    private final ProjectMapper mapper;

    @Transactional // Override for write
    public ProjectResponse create(ProjectRequest request) {
        Project project = mapper.toEntity(request);
        // Business Rule: Check for duplicates?
        return mapper.toResponse(repository.save(project));
    }
}
```

---

## 🧠 Phase 2: AI Orchestration (Backend)

**Goal**: Robust AI interaction that handles failures gracefully.

### 2.1. The Prompt Template (Structured Output)
**Technique**: Use "Chain of Thought" and "JSON Mode" prompting.

**File**: `backend/src/main/resources/prompts/requirements-prompt.st`
```text
You are an expert Technical Business Analyst.
Analyze the following project description: {description}

Generate a comprehensive software specification in strict JSON format.
The JSON must adhere to this schema:
{
  "title": "Project Title",
  "summary": "Executive summary...",
  "modules": [
    {
      "name": "Module Name",
      "userStories": [
        "As a [role], I want [feature], so that [benefit]"
      ]
    }
  ],
  "technicalStackRecommendation": ["Tech 1", "Tech 2"]
}

Do NOT include Markdown code blocks (```json). Just the raw JSON string.
```

### 2.2. The AI Service with Retry Logic
**Library**: Spring Retry (add dependency if missing).

**File**: `backend/src/main/java/com/smartspec/backend/service/AiService.java`
```java
@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public SpecificationResponse generate(String description) {
        String prompt = ...; // Load from template
        try {
            String jsonResponse = chatClient.prompt().user(prompt).call().content();
            return jsonParser.parse(jsonResponse); // Validate JSON here
        } catch (Exception e) {
            throw new AiGenerationException("Failed to generate specs", e);
        }
    }
}
```

---

## 🎨 Phase 3: Reactive Frontend (Angular)

**Goal**: A UI that feels instant and guides the user.

### 3.1. Directory Structure (Feature-Based)
Don't group by type (components/services). Group by **Domain**.
```
src/app/
├── core/               # Singleton services (Auth, ErrorHandling)
├── shared/             # Reusable UI (Buttons, Cards, Loaders)
└── features/
    └── projects/
        ├── project-list/
        ├── project-create/
        └── project-details/
            ├── state/  # Component-specific state
            └── ui/     # Dumb components
```

### 3.2. State Management with Signals
**Pattern**: "Service with a Signal".

**File**: `frontend/src/app/features/projects/data-access/project.store.ts`
```typescript
@Injectable({ providedIn: 'root' })
export class ProjectStore {
    // The source of truth
    private state = signal<ProjectState>({ projects: [], loading: false });

    // Public read-only signals
    readonly projects = computed(() => this.state().projects);
    readonly loading = computed(() => this.state().loading);

    async loadProjects() {
        this.state.update(s => ({ ...s, loading: true }));
        try {
            const data = await firstValueFrom(this.http.get<Project[]>('/api/v1/projects'));
            this.state.update(s => ({ ...s, projects: data, loading: false }));
        } catch (err) {
            // Handle error
            this.state.update(s => ({ ...s, loading: false }));
        }
    }
}
```

### 3.3. UX Best Practices
1.  **Skeleton Loading**: While AI generates, don't show a generic spinner. Show a "Skeleton" of the spec document pulsing.
2.  **Optimistic UI**: When creating a project, add it to the list *immediately* before the server responds (revert if it fails).
3.  **Error Toasts**: Use a library like `ngx-toastr` to show non-blocking errors.

---

## 🛡️ Phase 4: Security & DevOps (Production Ready)

### 4.1. Global Exception Handling (Backend)
**File**: `backend/src/main/java/com/smartspec/backend/exception/GlobalExceptionHandler.java`

*   Catch `MethodArgumentNotValidException`: Return **400 Bad Request** with a map of field -> error message.
*   Catch `ResourceNotFoundException`: Return **404 Not Found**.
*   Catch `Exception` (fallback): Return **500 Internal Server Error** but **MASK** the internal stack trace. Log the real error, show the user "Something went wrong".

### 4.2. Docker Best Practices
**Dockerfile**:
```dockerfile
# ... build stages ...
# Runtime
FROM eclipse-temurin:21-jre-alpine
# Create non-root user (Security Best Practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
# ...
```

---

## 🧪 Phase 5: Testing Strategy (The Safety Net)

### 5.1. Integration Tests (The Gold Standard)
**Why**: Mocking everything proves nothing. Test the real Database.

**File**: `backend/src/test/java/com/smartspec/backend/ProjectIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers // Spins up real Postgres Docker container
class ProjectIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        // ...
    }

    @Test
    void shouldCreateAndRetrieveProject() {
        // 1. POST /api/v1/projects
        // 2. GET /api/v1/projects/{id}
        // 3. Assert values match
    }
}
```

This plan leaves nothing to chance. Follow it precisely.

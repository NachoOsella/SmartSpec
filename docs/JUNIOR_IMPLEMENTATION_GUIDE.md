# SmartSpec: Complete Junior Developer Implementation Guide

> **Your Mission**: Build a production-ready AI-powered software requirements generator.
> This guide assumes you know basic Java and JavaScript, but explains EVERYTHING else.

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
9. [Git Workflow & Commits](#9-git-workflow--commits)
10. [Troubleshooting Guide](#10-troubleshooting-guide)

---

## 1. Project Understanding

### 1.1 What Are We Building?

SmartSpec is a web application that:
1. **User enters** a project description (like "I want an e-commerce site")
2. **AI generates** structured user stories, modules, and technical specs
3. **User refines** the specs through conversation
4. **System stores** everything in a database for future reference

### 1.2 Current Project State

**What's Already Done (Sprint 0):**
- Docker Compose configuration (3 services: db, backend, frontend)
- Backend skeleton with health endpoint
- Global exception handling
- CORS configuration
- Database migration script (tables exist, but no entities)
- Frontend Angular app skeleton

**What You Need to Build:**
- [ ] JPA Entities (Project, Specification, Conversation, Message)
- [ ] Repositories (Data Access Layer)
- [ ] DTOs (Data Transfer Objects)
- [ ] MapStruct Mappers
- [ ] Services (Business Logic)
- [ ] REST Controllers (API Endpoints)
- [ ] AI Service (Google Gen AI SDK)
- [ ] Frontend Components
- [ ] Frontend Services
- [ ] State Management

### 1.3 Architecture Overview

```
USER BROWSER
     |
     | HTTP Requests (JSON)
     v
+--------------------+
|     FRONTEND       |  Port 4200
|    Angular 21      |
|  (UI Components)   |
+--------------------+
     |
     | REST API calls to /api/*
     v
+--------------------+
|     BACKEND        |  Port 8080
|  Spring Boot 3.4   |
|  +--------------+  |
|  | Controller   |  |  <-- Receives HTTP, validates, delegates
|  +--------------+  |
|        |           |
|  +--------------+  |
|  |   Service    |  |  <-- Business logic, transactions
|  +--------------+  |
|        |           |
|  +--------------+  |
|  | Repository   |  |  <-- Database operations
|  +--------------+  |
+--------------------+
     |
     | JDBC/SQL
     v
+--------------------+
|    PostgreSQL      |  Port 5432
|    Database        |
+--------------------+
```

### 1.4 Database Schema (Already Created via Flyway)

```
projects
  - id (UUID, Primary Key)
  - name (VARCHAR 255)
  - description (TEXT)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

specifications
  - id (UUID, Primary Key)
  - project_id (UUID, Foreign Key -> projects)
  - title (VARCHAR 255)
  - content (TEXT) -- JSON containing generated specs
  - status (VARCHAR 50) -- DRAFT, PUBLISHED, ARCHIVED
  - version (INTEGER)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

conversations
  - id (UUID, Primary Key)
  - project_id (UUID, Foreign Key -> projects, nullable)
  - specification_id (UUID, Foreign Key -> specifications, nullable)
  - created_at (TIMESTAMP)

messages
  - id (UUID, Primary Key)
  - conversation_id (UUID, Foreign Key -> conversations)
  - role (VARCHAR 20) -- USER, ASSISTANT, SYSTEM
  - content (TEXT)
  - created_at (TIMESTAMP)
```

---

## 2. Development Environment Setup

### 2.1 Prerequisites Checklist

Before starting, ensure you have:

```bash
# Check Docker
docker --version    # Should be 20.x or higher
docker-compose --version  # Should be 2.x or higher

# Check Java (for local development)
java --version     # Should be 21.x

# Check Node (for frontend development)
node --version     # Should be 20.x
npm --version      # Should be 10.x or higher

# Check Git
git --version      # Should be 2.x
```

### 2.2 Initial Setup Commands

```bash
# 1. Clone the repository (if not already done)
cd ~/Documents/practica
git clone https://github.com/NachoOsella/SmartSpec.git
cd SmartSpec

# 2. Create your .env file with API key
# Get your key from: https://aistudio.google.com/apikey
echo "GOOGLE_API_KEY=your-actual-api-key" > .env
echo "POSTGRES_USER=postgres" >> .env
echo "POSTGRES_PASSWORD=postgres" >> .env
echo "POSTGRES_DB=smartspec_db" >> .env

# 3. Start all services
docker-compose up --build

# 4. Verify everything is running
# In another terminal:
curl http://localhost:8080/api/health
# Should return: {"status":"UP","timestamp":"...","service":"smartspec-backend"}
```

### 2.3 IDE Setup Recommendations

**For IntelliJ IDEA (Recommended for Backend):**
1. Open `backend/` as a project
2. Enable Lombok plugin: File > Settings > Plugins > Search "Lombok"
3. Enable annotation processing: File > Settings > Build > Compiler > Annotation Processors > Enable

**For VS Code (Recommended for Frontend):**
1. Open `frontend/` folder
2. Install extensions: Angular Language Service, Prettier, ESLint

### 2.4 Development Workflow

You have two options:

**Option A: Docker Only (Simpler)**
```bash
# All changes require rebuild
docker-compose up --build
```

**Option B: Hybrid (Faster for development)**
```bash
# Terminal 1: Start database only
docker-compose up db

# Terminal 2: Run backend locally
cd backend
./mvnw spring-boot:run

# Terminal 3: Run frontend locally
cd frontend
npm install
npm start
```

---

## 3. Sprint 1: Backend Data Layer

> **Goal**: Create all entities, repositories, DTOs, mappers, services, and controllers for CRUD operations.

### 3.1 Understanding the Layers

```
Controller Layer (REST API)
    |
    | Uses DTOs (Data Transfer Objects)
    v
Service Layer (Business Logic)
    |
    | Uses Entities & Mappers
    v
Repository Layer (Database Access)
    |
    | Uses JPA/Hibernate
    v
Database (PostgreSQL)
```

**Why This Separation?**
- **Security**: Entities may have fields you don't want to expose (internal IDs, audit fields)
- **Flexibility**: API structure can change without changing the database
- **Validation**: DTOs handle input validation, entities handle database constraints
- **Performance**: You can load partial data in DTOs

### 3.2 Task 1: Create Enums

**File**: `backend/src/main/java/com/smartspec/model/SpecificationStatus.java`

```java
package com.smartspec.model;

/**
 * Represents the lifecycle status of a specification document.
 * 
 * WHY USE ENUM INSTEAD OF STRING?
 * 1. Compile-time safety: typos like "DRFT" won't compile
 * 2. IDE autocomplete: easy to discover valid values
 * 3. Refactoring: rename everywhere at once
 * 4. Documentation: all valid values in one place
 */
public enum SpecificationStatus {
    
    /** Initial state when AI generates specs */
    DRAFT,
    
    /** Approved and finalized by user */
    PUBLISHED,
    
    /** No longer active but kept for history */
    ARCHIVED
}
```

**File**: `backend/src/main/java/com/smartspec/model/MessageRole.java`

```java
package com.smartspec.model;

/**
 * Represents who sent a message in a conversation.
 * These values align with OpenAI/Gemini API conventions.
 */
public enum MessageRole {
    
    /** Message from the human user */
    USER,
    
    /** Message from the AI assistant */
    ASSISTANT,
    
    /** System instructions (usually not shown to user) */
    SYSTEM
}
```

### 3.3 Task 2: Create JPA Entities

**IMPORTANT LOMBOK RULES FOR ENTITIES:**
- Use `@Getter`, `@Setter`, NOT `@Data`
- Use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
- Include only the `@Id` field in equals/hashCode

**Why?** Hibernate creates proxy objects. If equals/hashCode include lazy-loaded fields, it causes:
1. Unexpected database queries
2. `LazyInitializationException` errors
3. Broken collections (HashSet/HashMap)

**File**: `backend/src/main/java/com/smartspec/model/Project.java`

```java
package com.smartspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a software project that will have AI-generated specifications.
 * 
 * ENTITY BEST PRACTICES APPLIED:
 * 1. UUID as primary key (better for distributed systems)
 * 2. Audit fields (createdAt, updatedAt) for tracking
 * 3. Bidirectional relationships with mappedBy
 * 4. CascadeType carefully chosen (not ALL for safety)
 * 5. orphanRemoval for parent-owned children
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"specifications", "conversations"}) // Prevent infinite loops
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * @CreationTimestamp: Hibernate automatically sets this on INSERT.
     * updatable = false: Prevents accidental modification.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @UpdateTimestamp: Hibernate automatically updates this on UPDATE.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * One project can have many specifications (versions).
     * 
     * mappedBy = "project": The Specification entity owns the relationship
     *                       (has the foreign key column).
     * 
     * cascade = CascadeType.ALL: When we save/delete project, 
     *                            specs are also saved/deleted.
     * 
     * orphanRemoval = true: If we remove a spec from this list,
     *                       it's deleted from the database.
     * 
     * fetch = FetchType.LAZY: Don't load specs until accessed.
     *                         ALWAYS use LAZY for collections!
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Specification> specifications = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();

    // ========== HELPER METHODS ==========
    // These maintain bidirectional relationship consistency
    
    /**
     * Adds a specification to this project.
     * WHY: When you add a spec, you must set the project on the spec too.
     *      This method ensures both sides are in sync.
     */
    public void addSpecification(Specification spec) {
        specifications.add(spec);
        spec.setProject(this);
    }

    public void removeSpecification(Specification spec) {
        specifications.remove(spec);
        spec.setProject(null);
    }

    public void addConversation(Conversation conversation) {
        conversations.add(conversation);
        conversation.setProject(this);
    }
}
```

**File**: `backend/src/main/java/com/smartspec/model/Specification.java`

```java
package com.smartspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an AI-generated specification document.
 * 
 * The 'content' field stores JSON with structure like:
 * {
 *   "summary": "Executive summary...",
 *   "modules": [
 *     {
 *       "name": "User Management",
 *       "userStories": ["As a user, I want to..."]
 *     }
 *   ],
 *   "technicalStack": ["Spring Boot", "Angular"]
 * }
 */
@Entity
@Table(name = "specifications", indexes = {
    // WHY INDEX? This query will be common: "Get all specs for project X"
    @Index(name = "idx_specifications_project_id", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"project"})
public class Specification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * The parent project. 
     * 
     * @ManyToOne: Many specs can belong to one project.
     * 
     * @JoinColumn: Specifies the foreign key column name.
     *              nullable = false: A spec MUST have a project.
     * 
     * fetch = FetchType.LAZY: Don't load project until accessed.
     *                         For @ManyToOne, LAZY is optional but good practice.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 255)
    private String title;

    /**
     * JSON content of the generated specification.
     * Using TEXT type to allow for large documents.
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * @Enumerated(EnumType.STRING): Store as "DRAFT", not as 0, 1, 2.
     * 
     * WHY STRING?
     * - If you add a new status between existing ones, ordinal values shift
     * - Strings are human-readable in the database
     * - No magic numbers
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SpecificationStatus status = SpecificationStatus.DRAFT;

    /**
     * Version number for tracking iterations.
     * Starts at 1 and increments each time user regenerates.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**File**: `backend/src/main/java/com/smartspec/model/Conversation.java`

```java
package com.smartspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chat conversation with the AI.
 * Can be linked to a project and/or a specific specification.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"project", "specification", "messages"})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Optional link to a project.
     * nullable = true: A conversation might not be about a specific project.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Optional link to a specification being refined.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id")
    private Specification specification;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC") // Messages should be in chronological order
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public void addMessage(Message message) {
        messages.add(message);
        message.setConversation(this);
    }
}
```

**File**: `backend/src/main/java/com/smartspec/model/Message.java`

```java
package com.smartspec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single message in a conversation.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"conversation"})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * Who sent this message: USER, ASSISTANT, or SYSTEM.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### 3.4 Task 3: Create Repositories

**File**: `backend/src/main/java/com/smartspec/repository/ProjectRepository.java`

```java
package com.smartspec.repository;

import com.smartspec.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Project entities.
 * 
 * JpaRepository provides:
 * - save(entity): Insert or update
 * - findById(id): Find by primary key
 * - findAll(): Get all records
 * - deleteById(id): Delete by primary key
 * - count(): Count total records
 * - existsById(id): Check if exists
 * 
 * Spring Data JPA auto-generates implementations at runtime.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Find projects by name (case-insensitive partial match).
     * 
     * Spring Data JPA Query Derivation:
     * - findBy: Start a SELECT query
     * - Name: The field to filter on
     * - ContainingIgnoreCase: SQL LIKE '%value%' case-insensitive
     * 
     * Equivalent SQL: SELECT * FROM projects WHERE LOWER(name) LIKE LOWER('%?%')
     */
    List<Project> findByNameContainingIgnoreCase(String name);

    /**
     * Check if a project with this exact name exists.
     * Useful for preventing duplicates.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find all projects ordered by most recent first.
     * 
     * OrderByCreatedAtDesc: ORDER BY created_at DESC
     */
    List<Project> findAllByOrderByCreatedAtDesc();

    /**
     * Custom JPQL query example.
     * 
     * WHY JPQL INSTEAD OF DERIVED QUERY?
     * - Complex queries are clearer in JPQL
     * - Join fetching for performance
     * - Aggregations and grouping
     * 
     * @EntityGraph alternative shown below.
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.specifications WHERE p.id = :id")
    Optional<Project> findByIdWithSpecifications(UUID id);
}
```

**File**: `backend/src/main/java/com/smartspec/repository/SpecificationRepository.java`

```java
package com.smartspec.repository;

import com.smartspec.model.Specification;
import com.smartspec.model.SpecificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, UUID> {

    /**
     * Get all specifications for a project, newest first.
     */
    List<Specification> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Get the latest specification for a project (highest version).
     */
    Optional<Specification> findFirstByProjectIdOrderByVersionDesc(UUID projectId);

    /**
     * Get specifications by status.
     */
    List<Specification> findByProjectIdAndStatus(UUID projectId, SpecificationStatus status);

    /**
     * Count specifications for a project.
     * Useful for generating next version number.
     */
    int countByProjectId(UUID projectId);
}
```

**File**: `backend/src/main/java/com/smartspec/repository/ConversationRepository.java`

```java
package com.smartspec.repository;

import com.smartspec.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Fetch conversation with all its messages in one query.
     * Prevents N+1 query problem.
     */
    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.messages WHERE c.id = :id")
    Optional<Conversation> findByIdWithMessages(UUID id);
}
```

**File**: `backend/src/main/java/com/smartspec/repository/MessageRepository.java`

```java
package com.smartspec.repository;

import com.smartspec.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Get all messages in a conversation, in chronological order.
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
```

### 3.5 Task 4: Create DTOs (Data Transfer Objects)

**Why DTOs?**
1. **Security**: Don't expose internal fields (like password hashes)
2. **Validation**: Input validation with `@Valid` annotations
3. **Flexibility**: API can evolve independently from database
4. **Performance**: Return only needed fields

**File**: `backend/src/main/java/com/smartspec/dto/request/CreateProjectRequest.java`

```java
package com.smartspec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new project.
 * 
 * Using Java Records (Java 16+):
 * - Immutable by default
 * - Auto-generates constructor, getters, equals, hashCode, toString
 * - Perfect for DTOs!
 */
public record CreateProjectRequest(

    /**
     * @NotBlank: Cannot be null, empty, or whitespace only.
     * Different from @NotNull (allows empty string) and @NotEmpty (allows whitespace).
     */
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    String name,

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    String description

) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/request/UpdateProjectRequest.java`

```java
package com.smartspec.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing project.
 * All fields are optional (null = don't update).
 */
public record UpdateProjectRequest(

    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    String name,

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    String description

) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/request/GenerateSpecRequest.java`

```java
package com.smartspec.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request to generate a specification.
 * Can include additional context/requirements.
 */
public record GenerateSpecRequest(

    @Size(max = 10000, message = "Additional requirements cannot exceed 10000 characters")
    String additionalRequirements,

    /** Optional: specify if this should be a new version or update existing */
    Boolean createNewVersion

) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/request/ChatRequest.java`

```java
package com.smartspec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request to send a chat message to refine specs.
 */
public record ChatRequest(

    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message cannot exceed 10000 characters")
    String message,

    /** Optional: continue an existing conversation */
    UUID conversationId

) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/ProjectResponse.java`

```java
package com.smartspec.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for project data.
 * 
 * Notice: We DON'T include the full specifications list here.
 * That would be a separate endpoint to avoid loading too much data.
 */
public record ProjectResponse(
    UUID id,
    String name,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    
    /** Summary info instead of full list */
    int specificationCount
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/ProjectDetailResponse.java`

```java
package com.smartspec.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed project response including specifications.
 * Used for single project view.
 */
public record ProjectDetailResponse(
    UUID id,
    String name,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<SpecificationSummaryResponse> specifications
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/SpecificationResponse.java`

```java
package com.smartspec.dto.response;

import com.smartspec.model.SpecificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SpecificationResponse(
    UUID id,
    UUID projectId,
    String title,
    String content,  // JSON string
    SpecificationStatus status,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/SpecificationSummaryResponse.java`

```java
package com.smartspec.dto.response;

import com.smartspec.model.SpecificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary version without full content (for lists).
 */
public record SpecificationSummaryResponse(
    UUID id,
    String title,
    SpecificationStatus status,
    Integer version,
    LocalDateTime createdAt
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/ConversationResponse.java`

```java
package com.smartspec.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    UUID projectId,
    UUID specificationId,
    LocalDateTime createdAt,
    List<MessageResponse> messages
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/MessageResponse.java`

```java
package com.smartspec.dto.response;

import com.smartspec.model.MessageRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    MessageRole role,
    String content,
    LocalDateTime createdAt
) {}
```

**File**: `backend/src/main/java/com/smartspec/dto/response/ChatResponse.java`

```java
package com.smartspec.dto.response;

import java.util.UUID;

/**
 * Response after sending a chat message.
 */
public record ChatResponse(
    UUID conversationId,
    MessageResponse userMessage,
    MessageResponse assistantMessage
) {}
```

### 3.6 Task 5: Create MapStruct Mappers

**Why MapStruct?**
- Generates mapping code at compile time (fast!)
- Type-safe (catches errors at compile time)
- Reduces boilerplate
- Handles nested objects automatically

**File**: `backend/src/main/java/com/smartspec/mapper/ProjectMapper.java`

```java
package com.smartspec.mapper;

import com.smartspec.dto.request.CreateProjectRequest;
import com.smartspec.dto.response.ProjectDetailResponse;
import com.smartspec.dto.response.ProjectResponse;
import com.smartspec.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for Project entity.
 * 
 * componentModel = "spring": Register as Spring bean for injection.
 * uses = {SpecificationMapper.class}: Include other mappers for nested objects.
 */
@Mapper(componentModel = "spring", uses = {SpecificationMapper.class})
public interface ProjectMapper {

    /**
     * Convert request DTO to entity.
     * ID and timestamps are NOT set here (auto-generated).
     */
    Project toEntity(CreateProjectRequest request);

    /**
     * Convert entity to response DTO.
     * 
     * @Mapping(expression = ...): Custom logic for calculated fields.
     * specifications.size(): Get count from the loaded collection.
     */
    @Mapping(target = "specificationCount", expression = "java(project.getSpecifications().size())")
    ProjectResponse toResponse(Project project);

    /**
     * Convert entity to detailed response with specs.
     */
    ProjectDetailResponse toDetailResponse(Project project);

    /**
     * Convert list of entities to list of responses.
     * MapStruct auto-generates the loop.
     */
    List<ProjectResponse> toResponseList(List<Project> projects);

    /**
     * Update existing entity from request DTO.
     * 
     * @MappingTarget: The entity to update (not create new).
     * 
     * WHY @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)?
     * If request.name is null, don't set project.name to null.
     * Only update fields that are provided.
     */
    @org.mapstruct.BeanMapping(nullValuePropertyMappingStrategy = 
            org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(com.smartspec.dto.request.UpdateProjectRequest request, 
                      @MappingTarget Project project);
}
```

**File**: `backend/src/main/java/com/smartspec/mapper/SpecificationMapper.java`

```java
package com.smartspec.mapper;

import com.smartspec.dto.response.SpecificationResponse;
import com.smartspec.dto.response.SpecificationSummaryResponse;
import com.smartspec.model.Specification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecificationMapper {

    @Mapping(source = "project.id", target = "projectId")
    SpecificationResponse toResponse(Specification specification);

    SpecificationSummaryResponse toSummaryResponse(Specification specification);

    List<SpecificationSummaryResponse> toSummaryResponseList(List<Specification> specifications);
}
```

**File**: `backend/src/main/java/com/smartspec/mapper/ConversationMapper.java`

```java
package com.smartspec.mapper;

import com.smartspec.dto.response.ConversationResponse;
import com.smartspec.dto.response.MessageResponse;
import com.smartspec.model.Conversation;
import com.smartspec.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "specification.id", target = "specificationId")
    ConversationResponse toResponse(Conversation conversation);

    MessageResponse toMessageResponse(Message message);

    List<MessageResponse> toMessageResponseList(List<Message> messages);
}
```

### 3.7 Task 6: Create Custom Exceptions

**File**: `backend/src/main/java/com/smartspec/exception/ResourceNotFoundException.java`

```java
package com.smartspec.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource doesn't exist.
 * Results in 404 Not Found response.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(
            String.format("%s not found with id: %s", resourceName, id),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
}
```

**File**: `backend/src/main/java/com/smartspec/exception/DuplicateResourceException.java`

```java
package com.smartspec.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create a resource that already exists.
 * Results in 409 Conflict response.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(
            String.format("%s already exists with %s: %s", resourceName, field, value),
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE"
        );
    }
}
```

**File**: `backend/src/main/java/com/smartspec/exception/AiGenerationException.java`

```java
package com.smartspec.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when AI generation fails.
 * Results in 503 Service Unavailable response.
 */
public class AiGenerationException extends ApiException {

    public AiGenerationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "AI_GENERATION_FAILED");
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "AI_GENERATION_FAILED");
        initCause(cause);
    }
}
```

Update the GlobalExceptionHandler to handle the new exceptions:

**File**: `backend/src/main/java/com/smartspec/exception/GlobalExceptionHandler.java` (ADD these handlers)

```java
// Add this import at the top
import com.smartspec.exception.ResourceNotFoundException;

// Add this handler method
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex, HttpServletRequest request) {
    
    log.warn("Resource not found: {}", ex.getMessage());
    
    ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
    
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
}
```

### 3.8 Task 7: Create Service Layer

**File**: `backend/src/main/java/com/smartspec/service/ProjectService.java`

```java
package com.smartspec.service;

import com.smartspec.dto.request.CreateProjectRequest;
import com.smartspec.dto.request.UpdateProjectRequest;
import com.smartspec.dto.response.ProjectDetailResponse;
import com.smartspec.dto.response.ProjectResponse;
import com.smartspec.exception.DuplicateResourceException;
import com.smartspec.exception.ResourceNotFoundException;
import com.smartspec.mapper.ProjectMapper;
import com.smartspec.model.Project;
import com.smartspec.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for Project business logic.
 * 
 * TRANSACTION RULES:
 * - @Transactional at class level: All public methods are transactional
 * - readOnly = true default: Optimizes read operations
 * - Override with @Transactional for write operations
 */
@Service
@RequiredArgsConstructor  // Lombok: creates constructor with final fields
@Slf4j                    // Lombok: creates Logger log = LoggerFactory.getLogger(...)
@Transactional(readOnly = true)  // Default: read-only transactions
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    /**
     * Get all projects, newest first.
     */
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching all projects");
        List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();
        return projectMapper.toResponseList(projects);
    }

    /**
     * Get a single project by ID.
     * Throws 404 if not found.
     */
    public ProjectResponse getProject(UUID id) {
        log.debug("Fetching project with id: {}", id);
        Project project = findProjectOrThrow(id);
        return projectMapper.toResponse(project);
    }

    /**
     * Get detailed project with specifications.
     */
    public ProjectDetailResponse getProjectDetail(UUID id) {
        log.debug("Fetching detailed project with id: {}", id);
        Project project = projectRepository.findByIdWithSpecifications(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return projectMapper.toDetailResponse(project);
    }

    /**
     * Create a new project.
     * 
     * @Transactional: Override readOnly because we're writing.
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating new project with name: {}", request.name());
        
        // Business rule: prevent duplicate names
        if (projectRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Project", "name", request.name());
        }
        
        Project project = projectMapper.toEntity(request);
        Project saved = projectRepository.save(project);
        
        log.info("Created project with id: {}", saved.getId());
        return projectMapper.toResponse(saved);
    }

    /**
     * Update an existing project.
     */
    @Transactional
    public ProjectResponse updateProject(UUID id, UpdateProjectRequest request) {
        log.info("Updating project with id: {}", id);
        
        Project project = findProjectOrThrow(id);
        
        // Check for name conflict if name is being changed
        if (request.name() != null && 
            !request.name().equalsIgnoreCase(project.getName()) &&
            projectRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Project", "name", request.name());
        }
        
        projectMapper.updateEntity(request, project);
        Project saved = projectRepository.save(project);
        
        return projectMapper.toResponse(saved);
    }

    /**
     * Delete a project.
     * CASCADE will delete related specs and conversations.
     */
    @Transactional
    public void deleteProject(UUID id) {
        log.info("Deleting project with id: {}", id);
        
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        
        projectRepository.deleteById(id);
        log.info("Deleted project with id: {}", id);
    }

    /**
     * Search projects by name.
     */
    public List<ProjectResponse> searchProjects(String query) {
        log.debug("Searching projects with query: {}", query);
        List<Project> projects = projectRepository.findByNameContainingIgnoreCase(query);
        return projectMapper.toResponseList(projects);
    }

    // ========== HELPER METHODS ==========

    /**
     * Find project or throw 404.
     * Package-private so other services can use it.
     */
    Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
```

**File**: `backend/src/main/java/com/smartspec/service/SpecificationService.java`

```java
package com.smartspec.service;

import com.smartspec.dto.response.SpecificationResponse;
import com.smartspec.dto.response.SpecificationSummaryResponse;
import com.smartspec.exception.ResourceNotFoundException;
import com.smartspec.mapper.SpecificationMapper;
import com.smartspec.model.Project;
import com.smartspec.model.Specification;
import com.smartspec.model.SpecificationStatus;
import com.smartspec.repository.SpecificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpecificationService {

    private final SpecificationRepository specificationRepository;
    private final SpecificationMapper specificationMapper;
    private final ProjectService projectService;

    /**
     * Get all specifications for a project.
     */
    public List<SpecificationSummaryResponse> getProjectSpecifications(UUID projectId) {
        log.debug("Fetching specifications for project: {}", projectId);
        // Verify project exists
        projectService.findProjectOrThrow(projectId);
        
        List<Specification> specs = specificationRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId);
        return specificationMapper.toSummaryResponseList(specs);
    }

    /**
     * Get a single specification by ID.
     */
    public SpecificationResponse getSpecification(UUID id) {
        log.debug("Fetching specification with id: {}", id);
        Specification spec = findSpecificationOrThrow(id);
        return specificationMapper.toResponse(spec);
    }

    /**
     * Get the latest specification for a project.
     */
    public SpecificationResponse getLatestSpecification(UUID projectId) {
        log.debug("Fetching latest specification for project: {}", projectId);
        return specificationRepository.findFirstByProjectIdOrderByVersionDesc(projectId)
                .map(specificationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No specifications found for project", projectId));
    }

    /**
     * Create a new specification (called by AI service after generation).
     */
    @Transactional
    public SpecificationResponse createSpecification(UUID projectId, String title, 
                                                      String content) {
        log.info("Creating specification for project: {}", projectId);
        
        Project project = projectService.findProjectOrThrow(projectId);
        
        // Calculate next version number
        int nextVersion = specificationRepository.countByProjectId(projectId) + 1;
        
        Specification spec = Specification.builder()
                .project(project)
                .title(title)
                .content(content)
                .status(SpecificationStatus.DRAFT)
                .version(nextVersion)
                .build();
        
        Specification saved = specificationRepository.save(spec);
        log.info("Created specification with id: {}, version: {}", saved.getId(), nextVersion);
        
        return specificationMapper.toResponse(saved);
    }

    /**
     * Update specification status.
     */
    @Transactional
    public SpecificationResponse updateStatus(UUID id, SpecificationStatus newStatus) {
        log.info("Updating specification {} status to {}", id, newStatus);
        
        Specification spec = findSpecificationOrThrow(id);
        spec.setStatus(newStatus);
        
        Specification saved = specificationRepository.save(spec);
        return specificationMapper.toResponse(saved);
    }

    /**
     * Update specification content.
     */
    @Transactional
    public SpecificationResponse updateContent(UUID id, String content) {
        log.info("Updating specification {} content", id);
        
        Specification spec = findSpecificationOrThrow(id);
        spec.setContent(content);
        
        Specification saved = specificationRepository.save(spec);
        return specificationMapper.toResponse(saved);
    }

    /**
     * Delete a specification.
     */
    @Transactional
    public void deleteSpecification(UUID id) {
        log.info("Deleting specification with id: {}", id);
        
        if (!specificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Specification", id);
        }
        
        specificationRepository.deleteById(id);
    }

    // ========== HELPER METHODS ==========

    Specification findSpecificationOrThrow(UUID id) {
        return specificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specification", id));
    }
}
```

### 3.9 Task 8: Create Controllers (REST API)

**File**: `backend/src/main/java/com/smartspec/controller/ProjectController.java`

```java
package com.smartspec.controller;

import com.smartspec.dto.request.CreateProjectRequest;
import com.smartspec.dto.request.UpdateProjectRequest;
import com.smartspec.dto.response.ProjectDetailResponse;
import com.smartspec.dto.response.ProjectResponse;
import com.smartspec.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for Project operations.
 * 
 * NAMING CONVENTIONS:
 * - POST /api/v1/projects       -> Create (no action verb, HTTP method implies it)
 * - GET  /api/v1/projects       -> List all
 * - GET  /api/v1/projects/{id}  -> Get one
 * - PUT  /api/v1/projects/{id}  -> Full update (replace)
 * - PATCH /api/v1/projects/{id} -> Partial update
 * - DELETE /api/v1/projects/{id} -> Delete
 * 
 * We use v1 in the path for API versioning.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "List all projects", 
               description = "Returns all projects ordered by creation date, newest first")
    @ApiResponse(responseCode = "200", description = "Projects retrieved successfully")
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @RequestParam(required = false) 
            @Parameter(description = "Optional search query for project name") 
            String search) {
        
        List<ProjectResponse> projects;
        if (search != null && !search.isBlank()) {
            projects = projectService.searchProjects(search);
        } else {
            projects = projectService.getAllProjects();
        }
        
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID",
               description = "Returns a single project with summary information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project found"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable @Parameter(description = "Project UUID") UUID id) {
        
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "Get detailed project",
               description = "Returns project with all its specifications")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectDetail(id));
    }

    @PostMapping
    @Operation(summary = "Create new project",
               description = "Creates a new project with the given name and description")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Project created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Project with this name already exists")
    })
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        
        ProjectResponse created = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project",
               description = "Updates an existing project. Null fields are ignored.")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project",
               description = "Deletes a project and all its specifications")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
```

**File**: `backend/src/main/java/com/smartspec/controller/SpecificationController.java`

```java
package com.smartspec.controller;

import com.smartspec.dto.response.SpecificationResponse;
import com.smartspec.dto.response.SpecificationSummaryResponse;
import com.smartspec.model.SpecificationStatus;
import com.smartspec.service.SpecificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Specifications", description = "Specification management endpoints")
public class SpecificationController {

    private final SpecificationService specificationService;

    @GetMapping("/projects/{projectId}/specifications")
    @Operation(summary = "List project specifications",
               description = "Returns all specifications for a project")
    public ResponseEntity<List<SpecificationSummaryResponse>> getProjectSpecifications(
            @PathVariable UUID projectId) {
        
        return ResponseEntity.ok(specificationService.getProjectSpecifications(projectId));
    }

    @GetMapping("/projects/{projectId}/specifications/latest")
    @Operation(summary = "Get latest specification",
               description = "Returns the most recent specification for a project")
    public ResponseEntity<SpecificationResponse> getLatestSpecification(
            @PathVariable UUID projectId) {
        
        return ResponseEntity.ok(specificationService.getLatestSpecification(projectId));
    }

    @GetMapping("/specifications/{id}")
    @Operation(summary = "Get specification by ID")
    public ResponseEntity<SpecificationResponse> getSpecification(@PathVariable UUID id) {
        return ResponseEntity.ok(specificationService.getSpecification(id));
    }

    @PatchMapping("/specifications/{id}/status")
    @Operation(summary = "Update specification status",
               description = "Change status to DRAFT, PUBLISHED, or ARCHIVED")
    public ResponseEntity<SpecificationResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam SpecificationStatus status) {
        
        return ResponseEntity.ok(specificationService.updateStatus(id, status));
    }

    @DeleteMapping("/specifications/{id}")
    @Operation(summary = "Delete specification")
    public ResponseEntity<Void> deleteSpecification(@PathVariable UUID id) {
        specificationService.deleteSpecification(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 3.10 Sprint 1 Verification Checklist

Before moving to Sprint 2, verify everything works:

```bash
# 1. Rebuild and restart
docker-compose down
docker-compose up --build

# 2. Wait for backend to start (check logs)
docker-compose logs -f backend
# Look for: "Started SmartSpecApplication"

# 3. Test the API endpoints

# Create a project
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "E-Commerce App", "description": "An online store for selling products"}'

# List all projects
curl http://localhost:8080/api/v1/projects

# Get single project (replace UUID)
curl http://localhost:8080/api/v1/projects/YOUR-UUID-HERE

# Update project
curl -X PUT http://localhost:8080/api/v1/projects/YOUR-UUID-HERE \
  -H "Content-Type: application/json" \
  -d '{"name": "E-Commerce Platform"}'

# Delete project
curl -X DELETE http://localhost:8080/api/v1/projects/YOUR-UUID-HERE

# 4. Check Swagger UI
# Open: http://localhost:8080/swagger-ui.html
```

---

## 4. Sprint 2: AI Integration

> **Goal**: Connect to Google Gemini to generate specifications.

### 4.1 Understanding Google Gen AI SDK

The Google Gen AI Java SDK (`com.google.genai`) provides a simple interface for integrating Google's generative models. Key concepts:

1. **Client**: Main entry point to interact with Gemini models
2. **GenerateContentResponse**: The AI's reply containing generated text
3. **client.models.generateContent()**: Method to send prompts and receive responses

The SDK supports both:
- **Gemini API** (using API key from AI Studio) - simpler setup
- **Vertex AI** (using Google Cloud project credentials) - production-ready

**Basic Usage Example:**
```java
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

// Create client with API key
Client client = Client.builder().apiKey("your-api-key").build();

// Generate content
GenerateContentResponse response = client.models.generateContent(
    "gemini-2.5-flash",  // model name
    "What is Java?",      // prompt
    null                  // optional config
);

// Get the text response
System.out.println(response.text());
```

**Maven Dependency** (already in pom.xml):
```xml
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>1.2.0</version>
</dependency>
```

### 4.2 Task 1: Create Prompt Templates

**File**: `backend/src/main/resources/prompts/generate-specification.txt`

```
You are an expert Technical Business Analyst with 20 years of experience in software requirements engineering.

CONTEXT:
A user has provided the following project description for a software application they want to build:

---
{description}
---

{additionalRequirements}

YOUR TASK:
Generate a comprehensive software specification document. Be thorough, specific, and practical.

OUTPUT FORMAT:
You MUST respond with ONLY valid JSON (no markdown, no code blocks, no explanation). The JSON must follow this exact structure:

{
  "projectTitle": "The official project name",
  "executiveSummary": "2-3 paragraph summary of what the software does and its value proposition",
  "targetAudience": ["Primary User Type 1", "Primary User Type 2"],
  "modules": [
    {
      "name": "Module Name",
      "description": "What this module does",
      "priority": "HIGH | MEDIUM | LOW",
      "userStories": [
        {
          "id": "US-001",
          "title": "Brief title",
          "story": "As a [role], I want to [action], so that [benefit]",
          "acceptanceCriteria": [
            "Given [context], when [action], then [result]"
          ],
          "priority": "HIGH | MEDIUM | LOW"
        }
      ]
    }
  ],
  "nonFunctionalRequirements": [
    {
      "category": "Performance | Security | Scalability | Usability | Reliability",
      "requirement": "Description of the requirement",
      "metric": "Measurable target (e.g., 'Page load < 2 seconds')"
    }
  ],
  "technicalRecommendations": {
    "frontend": ["Technology 1", "Technology 2"],
    "backend": ["Technology 1", "Technology 2"],
    "database": ["Technology 1"],
    "infrastructure": ["Technology 1", "Technology 2"]
  },
  "projectRisks": [
    {
      "risk": "Description of potential risk",
      "impact": "HIGH | MEDIUM | LOW",
      "mitigation": "How to address this risk"
    }
  ],
  "estimatedComplexity": "SIMPLE | MODERATE | COMPLEX | VERY_COMPLEX",
  "suggestedMvpFeatures": ["Feature 1 for MVP", "Feature 2 for MVP"]
}

IMPORTANT RULES:
1. Generate at least 3 modules
2. Each module should have 2-5 user stories
3. Include at least 5 non-functional requirements
4. Be realistic with complexity assessment
5. MVP features should be achievable in 2-4 weeks
6. Output ONLY the JSON object, nothing else
```

**File**: `backend/src/main/resources/prompts/refine-specification.txt`

```
You are an expert Technical Business Analyst helping to refine software specifications.

CURRENT SPECIFICATION:
{currentSpecification}

USER FEEDBACK:
{userMessage}

CONVERSATION HISTORY:
{conversationHistory}

YOUR TASK:
Based on the user's feedback, provide a helpful response. You may:
1. Answer questions about the specification
2. Suggest improvements or alternatives
3. Provide more detail on specific sections
4. Recommend best practices

If the user requests changes to the specification, provide the updated JSON section.

Keep your response conversational but professional. If you output JSON, make sure it's valid.
```

### 4.3 Task 2: Create AI Configuration

**File**: `backend/src/main/java/com/smartspec/config/GenAiConfig.java`

```java
package com.smartspec.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Gen AI SDK.
 * 
 * The Client can be configured for:
 * - Gemini API: Using API key from Google AI Studio
 * - Vertex AI: Using Google Cloud project credentials
 */
@Configuration
public class GenAiConfig {

    @Value("${genai.api-key:}")
    private String apiKey;

    @Value("${genai.use-vertex-ai:false}")
    private boolean useVertexAi;

    @Value("${genai.project-id:}")
    private String projectId;

    @Value("${genai.location:us-central1}")
    private String location;

    /**
     * Creates the Google Gen AI Client as a Spring bean.
     * 
     * The client is thread-safe and should be reused.
     */
    @Bean
    public Client genAiClient() {
        if (useVertexAi) {
            // Vertex AI mode: uses project and location
            return Client.builder()
                    .project(projectId)
                    .location(location)
                    .vertexAI(true)
                    .build();
        } else {
            // Gemini API mode: uses API key
            return Client.builder()
                    .apiKey(apiKey)
                    .build();
        }
    }
}
```

### 4.4 Task 3: Create AI Service

**File**: `backend/src/main/java/com/smartspec/service/AiService.java`

```java
package com.smartspec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.smartspec.dto.request.ChatRequest;
import com.smartspec.dto.request.GenerateSpecRequest;
import com.smartspec.dto.response.ChatResponse;
import com.smartspec.dto.response.SpecificationResponse;
import com.smartspec.exception.AiGenerationException;
import com.smartspec.mapper.ConversationMapper;
import com.smartspec.model.*;
import com.smartspec.repository.ConversationRepository;
import com.smartspec.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Service for AI-powered specification generation.
 * 
 * This service:
 * 1. Loads prompt templates from resources
 * 2. Calls Google Gemini via the Gen AI SDK
 * 3. Parses and validates JSON responses
 * 4. Handles retries and errors gracefully
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiService {

    private final Client genAiClient;
    private final SpecificationService specificationService;
    private final ProjectService projectService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final ObjectMapper objectMapper;  // Jackson JSON parser

    @Value("classpath:prompts/generate-specification.txt")
    private Resource generatePromptResource;

    @Value("classpath:prompts/refine-specification.txt")
    private Resource refinePromptResource;

    @Value("${app.ai.max-retries:3}")
    private int maxRetries;

    @Value("${genai.model:gemini-2.5-flash}")
    private String modelName;

    /**
     * Generate a new specification for a project.
     */
    public SpecificationResponse generateSpecification(UUID projectId, 
                                                        GenerateSpecRequest request) {
        log.info("Generating specification for project: {}", projectId);
        
        // 1. Get project data
        var project = projectService.findProjectOrThrow(projectId);
        
        // 2. Build the prompt
        String promptText = loadPromptTemplate(generatePromptResource);
        String description = project.getDescription() != null ? 
                           project.getDescription() : project.getName();
        String additionalReqs = request != null && request.additionalRequirements() != null ? 
                              "Additional requirements:\n" + request.additionalRequirements() : "";
        
        String finalPrompt = promptText
                .replace("{description}", description)
                .replace("{additionalRequirements}", additionalReqs);
        
        // 3. Call AI with retry logic
        String aiResponse = callAiWithRetry(finalPrompt);
        
        // 4. Validate JSON response
        String validJson = extractAndValidateJson(aiResponse);
        
        // 5. Extract title from response
        String title = extractTitleFromJson(validJson);
        
        // 6. Create and save specification
        return specificationService.createSpecification(projectId, title, validJson);
    }

    /**
     * Continue a conversation to refine specifications.
     */
    public ChatResponse chat(UUID projectId, ChatRequest request) {
        log.info("Processing chat for project: {}", projectId);
        
        Project project = projectService.findProjectOrThrow(projectId);
        
        // 1. Get or create conversation
        Conversation conversation;
        if (request.conversationId() != null) {
            conversation = conversationRepository.findByIdWithMessages(request.conversationId())
                    .orElseThrow(() -> new AiGenerationException("Conversation not found"));
        } else {
            conversation = Conversation.builder()
                    .project(project)
                    .build();
            conversation = conversationRepository.save(conversation);
        }
        
        // 2. Save user message
        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.message())
                .build();
        userMessage = messageRepository.save(userMessage);
        
        // 3. Build prompt with context
        String promptText = loadPromptTemplate(refinePromptResource);
        
        // Get current specification if exists
        String currentSpec = "";
        try {
            var latestSpec = specificationService.getLatestSpecification(projectId);
            currentSpec = latestSpec.content();
        } catch (Exception e) {
            currentSpec = "No specification generated yet.";
        }
        
        // Build conversation history
        StringBuilder history = new StringBuilder();
        for (var msg : conversation.getMessages()) {
            history.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }
        
        String finalPrompt = promptText
                .replace("{currentSpecification}", currentSpec)
                .replace("{userMessage}", request.message())
                .replace("{conversationHistory}", history.toString());
        
        // 4. Call AI
        String aiResponse = callAiWithRetry(finalPrompt);
        
        // 5. Save assistant message
        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(aiResponse)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);
        
        // 6. Return response
        return new ChatResponse(
            conversation.getId(),
            conversationMapper.toMessageResponse(userMessage),
            conversationMapper.toMessageResponse(assistantMessage)
        );
    }

    // ========== HELPER METHODS ==========

    /**
     * Load prompt template from resource file.
     */
    private String loadPromptTemplate(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template", e);
            throw new AiGenerationException("Failed to load prompt template", e);
        }
    }

    /**
     * Call AI with exponential backoff retry.
     */
    private String callAiWithRetry(String prompt) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                log.debug("AI call attempt {} of {}", attempt + 1, maxRetries);
                
                GenerateContentResponse response = genAiClient.models.generateContent(
                        modelName,
                        prompt,
                        null  // No additional config
                );
                
                String text = response.text();
                
                if (text == null || text.isBlank()) {
                    throw new AiGenerationException("AI returned empty response");
                }
                
                return text;
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("AI call failed (attempt {}): {}", attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff: 2s, 4s, 8s...
                        long waitTime = (long) Math.pow(2, attempt) * 1000;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiGenerationException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new AiGenerationException("AI generation failed after " + maxRetries + 
                                        " attempts: " + lastException.getMessage(), lastException);
    }

    /**
     * Extract and validate JSON from AI response.
     * AI sometimes wraps JSON in markdown code blocks.
     */
    private String extractAndValidateJson(String response) {
        String cleaned = response.trim();
        
        // Remove markdown code blocks if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        cleaned = cleaned.trim();
        
        // Validate it's valid JSON
        try {
            objectMapper.readTree(cleaned);
            return cleaned;
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON from AI: {}", cleaned);
            throw new AiGenerationException("AI returned invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Extract project title from generated JSON.
     */
    private String extractTitleFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode titleNode = root.get("projectTitle");
            if (titleNode != null && !titleNode.isNull()) {
                return titleNode.asText();
            }
            return "Generated Specification";
        } catch (JsonProcessingException e) {
            return "Generated Specification";
        }
    }
}
```

### 4.5 Task 4: Create AI Controller

**File**: `backend/src/main/java/com/smartspec/controller/AiController.java`

```java
package com.smartspec.controller;

import com.smartspec.dto.request.ChatRequest;
import com.smartspec.dto.request.GenerateSpecRequest;
import com.smartspec.dto.response.ChatResponse;
import com.smartspec.dto.response.SpecificationResponse;
import com.smartspec.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
@Tag(name = "AI Generation", description = "AI-powered specification generation")
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    @Operation(summary = "Generate specification",
               description = "Uses AI to generate a complete software specification from the project description")
    public ResponseEntity<SpecificationResponse> generateSpecification(
            @PathVariable UUID projectId,
            @Valid @RequestBody(required = false) GenerateSpecRequest request) {
        
        return ResponseEntity.ok(aiService.generateSpecification(projectId, request));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat to refine specification",
               description = "Send messages to refine and improve the generated specification")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable UUID projectId,
            @Valid @RequestBody ChatRequest request) {
        
        return ResponseEntity.ok(aiService.chat(projectId, request));
    }
}
```

### 4.6 Task 5: Configure Google Gen AI

The configuration is in `application.yml`. Make sure your `.env` file has:

```bash
# For Gemini API (simpler, recommended for development)
GOOGLE_API_KEY=your-actual-api-key

# For Vertex AI (production, requires GCP project)
# GOOGLE_CLOUD_PROJECT=your-gcp-project-id
# GOOGLE_CLOUD_LOCATION=us-central1
```

**Getting an API Key:**
1. Go to [Google AI Studio](https://aistudio.google.com/apikey)
2. Click "Create API Key"
3. Copy the key to your `.env` file

**application.yml configuration:**

```yaml
# Google Gen AI Configuration
genai:
  api-key: ${GOOGLE_API_KEY:your-api-key}
  model: gemini-2.5-flash
  # For Vertex AI instead of Gemini API, uncomment:
  # use-vertex-ai: true
  # project-id: ${GOOGLE_CLOUD_PROJECT:your-project-id}
  # location: ${GOOGLE_CLOUD_LOCATION:us-central1}
```

### 4.7 Sprint 2 Verification

```bash
# 1. Rebuild
docker-compose down
docker-compose up --build

# 2. Create a project
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Task Manager", "description": "A web application for managing personal tasks and to-dos. Users should be able to create tasks, set due dates, categorize them, and mark them complete."}'

# Note the project ID from the response

# 3. Generate specification (this calls AI - may take 10-30 seconds)
curl -X POST http://localhost:8080/api/v1/projects/YOUR-PROJECT-ID/generate \
  -H "Content-Type: application/json" \
  -d '{}'

# 4. Chat to refine
curl -X POST http://localhost:8080/api/v1/projects/YOUR-PROJECT-ID/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Can you add more detail about the notification system?"}'
```

---

## 5. Sprint 3: Frontend Implementation

> **Goal**: Build a reactive Angular UI with proper state management.

### 5.1 Angular Architecture Overview

```
src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── services/
│   │   └── api.service.ts   # HTTP client wrapper
│   └── interceptors/
│       └── error.interceptor.ts
├── shared/                  # Reusable components, pipes, directives
│   ├── components/
│   │   ├── button/
│   │   ├── card/
│   │   └── loading-spinner/
│   └── pipes/
├── features/                # Feature modules (lazy loaded)
│   ├── projects/
│   │   ├── project-list/
│   │   ├── project-create/
│   │   └── project-detail/
│   └── specifications/
│       └── spec-viewer/
├── app.routes.ts           # Routing configuration
├── app.config.ts           # App configuration
└── app.ts                  # Root component
```

### 5.2 Task 1: Create Core Services

**File**: `frontend/src/app/core/services/api.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Base API service with common HTTP methods.
 * 
 * WHY A WRAPPER?
 * 1. Centralized error handling
 * 2. Easy to change base URL
 * 3. Common headers can be added here
 * 4. Logging/monitoring hooks
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Generic GET request.
   * 
   * @example
   * this.api.get<Project[]>('/projects')
   */
  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Generic POST request.
   */
  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Generic PUT request.
   */
  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Generic PATCH request.
   */
  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${path}`, body).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Generic DELETE request.
   */
  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Centralized error handling.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = error.error.message;
    } else {
      // Server-side error
      errorMessage = error.error?.message || `Error ${error.status}: ${error.statusText}`;
    }
    
    console.error('API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
```

**File**: `frontend/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

**File**: `frontend/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: '/api/v1'  // Same domain in production
};
```

### 5.3 Task 2: Create TypeScript Interfaces

**File**: `frontend/src/app/core/models/project.model.ts`

```typescript
/**
 * Interface for Project data from API.
 * 
 * TypeScript interfaces ensure type safety.
 * If the API changes, TypeScript will catch mismatches.
 */
export interface Project {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;  // ISO date string
  updatedAt: string;
  specificationCount: number;
}

export interface ProjectDetail extends Omit<Project, 'specificationCount'> {
  specifications: SpecificationSummary[];
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}
```

**File**: `frontend/src/app/core/models/specification.model.ts`

```typescript
export type SpecificationStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface Specification {
  id: string;
  projectId: string;
  title: string;
  content: string;  // JSON string
  status: SpecificationStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface SpecificationSummary {
  id: string;
  title: string;
  status: SpecificationStatus;
  version: number;
  createdAt: string;
}

/**
 * Parsed specification content structure.
 */
export interface ParsedSpecification {
  projectTitle: string;
  executiveSummary: string;
  targetAudience: string[];
  modules: SpecModule[];
  nonFunctionalRequirements: NonFunctionalRequirement[];
  technicalRecommendations: TechnicalRecommendations;
  projectRisks: ProjectRisk[];
  estimatedComplexity: string;
  suggestedMvpFeatures: string[];
}

export interface SpecModule {
  name: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  userStories: UserStory[];
}

export interface UserStory {
  id: string;
  title: string;
  story: string;
  acceptanceCriteria: string[];
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface NonFunctionalRequirement {
  category: string;
  requirement: string;
  metric: string;
}

export interface TechnicalRecommendations {
  frontend: string[];
  backend: string[];
  database: string[];
  infrastructure: string[];
}

export interface ProjectRisk {
  risk: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
  mitigation: string;
}
```

**File**: `frontend/src/app/core/models/chat.model.ts`

```typescript
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}

export interface Conversation {
  id: string;
  projectId: string;
  specificationId: string | null;
  createdAt: string;
  messages: Message[];
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
}

export interface ChatResponse {
  conversationId: string;
  userMessage: Message;
  assistantMessage: Message;
}
```

**File**: `frontend/src/app/core/models/index.ts` (barrel export)

```typescript
export * from './project.model';
export * from './specification.model';
export * from './chat.model';
```

### 5.4 Task 3: Create Feature Services

**File**: `frontend/src/app/features/projects/services/project.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { 
  Project, 
  ProjectDetail, 
  CreateProjectRequest, 
  UpdateProjectRequest 
} from '../../../core/models';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly api = inject(ApiService);

  getAll(): Observable<Project[]> {
    return this.api.get<Project[]>('/projects');
  }

  search(query: string): Observable<Project[]> {
    return this.api.get<Project[]>(`/projects?search=${encodeURIComponent(query)}`);
  }

  getById(id: string): Observable<Project> {
    return this.api.get<Project>(`/projects/${id}`);
  }

  getDetail(id: string): Observable<ProjectDetail> {
    return this.api.get<ProjectDetail>(`/projects/${id}/detail`);
  }

  create(request: CreateProjectRequest): Observable<Project> {
    return this.api.post<Project>('/projects', request);
  }

  update(id: string, request: UpdateProjectRequest): Observable<Project> {
    return this.api.put<Project>(`/projects/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/projects/${id}`);
  }
}
```

**File**: `frontend/src/app/features/projects/services/specification.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { 
  Specification, 
  SpecificationSummary, 
  SpecificationStatus,
  ParsedSpecification 
} from '../../../core/models';

@Injectable({
  providedIn: 'root'
})
export class SpecificationService {
  private readonly api = inject(ApiService);

  getProjectSpecifications(projectId: string): Observable<SpecificationSummary[]> {
    return this.api.get<SpecificationSummary[]>(`/projects/${projectId}/specifications`);
  }

  getLatest(projectId: string): Observable<Specification> {
    return this.api.get<Specification>(`/projects/${projectId}/specifications/latest`);
  }

  getById(id: string): Observable<Specification> {
    return this.api.get<Specification>(`/specifications/${id}`);
  }

  /**
   * Get specification with parsed content.
   */
  getByIdParsed(id: string): Observable<{ spec: Specification; parsed: ParsedSpecification }> {
    return this.getById(id).pipe(
      map(spec => ({
        spec,
        parsed: JSON.parse(spec.content) as ParsedSpecification
      }))
    );
  }

  updateStatus(id: string, status: SpecificationStatus): Observable<Specification> {
    return this.api.patch<Specification>(`/specifications/${id}/status?status=${status}`, {});
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/specifications/${id}`);
  }
}
```

**File**: `frontend/src/app/features/projects/services/ai.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Specification, ChatRequest, ChatResponse } from '../../../core/models';

export interface GenerateSpecRequest {
  additionalRequirements?: string;
  createNewVersion?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AiGenerationService {
  private readonly api = inject(ApiService);

  generate(projectId: string, request?: GenerateSpecRequest): Observable<Specification> {
    return this.api.post<Specification>(`/projects/${projectId}/generate`, request || {});
  }

  chat(projectId: string, request: ChatRequest): Observable<ChatResponse> {
    return this.api.post<ChatResponse>(`/projects/${projectId}/chat`, request);
  }
}
```

### 5.5 Task 4: Create State Management with Signals

**File**: `frontend/src/app/features/projects/state/project.store.ts`

```typescript
import { Injectable, inject, computed, signal } from '@angular/core';
import { Project, ProjectDetail } from '../../../core/models';
import { ProjectService } from '../services/project.service';
import { firstValueFrom } from 'rxjs';

/**
 * State shape for projects feature.
 */
interface ProjectState {
  projects: Project[];
  selectedProject: ProjectDetail | null;
  loading: boolean;
  error: string | null;
}

/**
 * Project state management using Angular Signals.
 * 
 * SIGNALS PATTERN:
 * - Private writable signal for state
 * - Public computed signals for derived values
 * - Methods to update state
 * 
 * WHY SIGNALS OVER RXJS BEHAVIORSUBJECT?
 * - Built into Angular (no external library)
 * - Simpler syntax
 * - Better change detection performance
 * - Still works with RxJS when needed
 */
@Injectable({
  providedIn: 'root'
})
export class ProjectStore {
  private readonly projectService = inject(ProjectService);

  // Private state signal
  private readonly state = signal<ProjectState>({
    projects: [],
    selectedProject: null,
    loading: false,
    error: null
  });

  // Public read-only signals
  readonly projects = computed(() => this.state().projects);
  readonly selectedProject = computed(() => this.state().selectedProject);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);

  // Derived signals
  readonly projectCount = computed(() => this.state().projects.length);
  readonly hasProjects = computed(() => this.state().projects.length > 0);

  /**
   * Load all projects.
   */
  async loadProjects(): Promise<void> {
    this.updateState({ loading: true, error: null });
    
    try {
      const projects = await firstValueFrom(this.projectService.getAll());
      this.updateState({ projects, loading: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load projects';
      this.updateState({ loading: false, error: message });
    }
  }

  /**
   * Load a single project with details.
   */
  async loadProjectDetail(id: string): Promise<void> {
    this.updateState({ loading: true, error: null });
    
    try {
      const project = await firstValueFrom(this.projectService.getDetail(id));
      this.updateState({ selectedProject: project, loading: false });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load project';
      this.updateState({ loading: false, error: message, selectedProject: null });
    }
  }

  /**
   * Create a new project.
   * Optimistic update pattern: add to list immediately, revert on error.
   */
  async createProject(name: string, description?: string): Promise<Project> {
    this.updateState({ loading: true, error: null });
    
    try {
      const newProject = await firstValueFrom(
        this.projectService.create({ name, description })
      );
      
      // Add to beginning of list
      this.updateState({
        projects: [newProject, ...this.state().projects],
        loading: false
      });
      
      return newProject;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create project';
      this.updateState({ loading: false, error: message });
      throw err;
    }
  }

  /**
   * Delete a project.
   */
  async deleteProject(id: string): Promise<void> {
    const previousProjects = this.state().projects;
    
    // Optimistic removal
    this.updateState({
      projects: previousProjects.filter(p => p.id !== id)
    });
    
    try {
      await firstValueFrom(this.projectService.delete(id));
    } catch (err) {
      // Revert on failure
      this.updateState({ projects: previousProjects });
      throw err;
    }
  }

  /**
   * Clear selected project.
   */
  clearSelection(): void {
    this.updateState({ selectedProject: null });
  }

  /**
   * Clear error state.
   */
  clearError(): void {
    this.updateState({ error: null });
  }

  // ========== PRIVATE HELPERS ==========

  /**
   * Update state immutably.
   */
  private updateState(partial: Partial<ProjectState>): void {
    this.state.update(current => ({ ...current, ...partial }));
  }
}
```

### 5.6 Task 5: Create Shared Components

**File**: `frontend/src/app/shared/components/loading-spinner/loading-spinner.ts`

```typescript
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `
    <div class="spinner-container" [class.overlay]="overlay()">
      <div class="spinner"></div>
      @if (message()) {
        <p class="message">{{ message() }}</p>
      }
    </div>
  `,
  styles: [`
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
    }
    
    .spinner-container.overlay {
      position: fixed;
      inset: 0;
      background: rgba(255, 255, 255, 0.8);
      z-index: 1000;
    }
    
    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid #e2e8f0;
      border-top-color: #3b82f6;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    
    .message {
      margin-top: 1rem;
      color: #64748b;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinner {
  message = input<string>();
  overlay = input<boolean>(false);
}
```

**File**: `frontend/src/app/shared/components/error-message/error-message.ts`

```typescript
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-message',
  standalone: true,
  template: `
    <div class="error-container" role="alert">
      <div class="error-icon">!</div>
      <div class="error-content">
        <h4>{{ title() }}</h4>
        <p>{{ message() }}</p>
      </div>
      @if (dismissible()) {
        <button class="dismiss-btn" (click)="dismiss.emit()">
          &times;
        </button>
      }
    </div>
  `,
  styles: [`
    .error-container {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      padding: 1rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 0.5rem;
      color: #991b1b;
    }
    
    .error-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
      background: #dc2626;
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
    }
    
    .error-content {
      flex: 1;
    }
    
    .error-content h4 {
      margin: 0 0 0.25rem;
      font-weight: 600;
    }
    
    .error-content p {
      margin: 0;
    }
    
    .dismiss-btn {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: #991b1b;
      padding: 0 0.5rem;
    }
  `]
})
export class ErrorMessage {
  title = input<string>('Error');
  message = input.required<string>();
  dismissible = input<boolean>(true);
  dismiss = output<void>();
}
```

**File**: `frontend/src/app/shared/components/card/card.ts`

```typescript
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div class="card" [class.hoverable]="hoverable()">
      @if (title()) {
        <div class="card-header">
          <h3>{{ title() }}</h3>
        </div>
      }
      <div class="card-body">
        <ng-content />
      </div>
    </div>
  `,
  styles: [`
    .card {
      background: white;
      border-radius: 0.5rem;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }
    
    .card.hoverable {
      transition: box-shadow 0.2s, transform 0.2s;
      cursor: pointer;
    }
    
    .card.hoverable:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      transform: translateY(-2px);
    }
    
    .card-header {
      padding: 1rem 1.5rem;
      border-bottom: 1px solid #e2e8f0;
    }
    
    .card-header h3 {
      margin: 0;
      font-size: 1.125rem;
    }
    
    .card-body {
      padding: 1.5rem;
    }
  `]
})
export class Card {
  title = input<string>();
  hoverable = input<boolean>(false);
}
```

### 5.7 Task 6: Create Feature Components

**File**: `frontend/src/app/features/projects/project-list/project-list.ts`

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ProjectStore } from '../state/project.store';
import { Card } from '../../../shared/components/card/card';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { ErrorMessage } from '../../../shared/components/error-message/error-message';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [RouterLink, Card, LoadingSpinner, ErrorMessage, DatePipe],
  template: `
    <div class="project-list-page">
      <header class="page-header">
        <div>
          <h1>Projects</h1>
          <p>Manage your AI-generated specifications</p>
        </div>
        <a routerLink="/projects/new" class="btn btn-primary">
          + New Project
        </a>
      </header>

      @if (store.loading()) {
        <app-loading-spinner message="Loading projects..." />
      } @else if (store.error()) {
        <app-error-message 
          [message]="store.error()!" 
          (dismiss)="store.clearError()" 
        />
      } @else if (!store.hasProjects()) {
        <div class="empty-state">
          <h2>No projects yet</h2>
          <p>Create your first project to get started with AI-powered specifications.</p>
          <a routerLink="/projects/new" class="btn btn-primary">
            Create Project
          </a>
        </div>
      } @else {
        <div class="project-grid">
          @for (project of store.projects(); track project.id) {
            <app-card [hoverable]="true">
              <a [routerLink]="['/projects', project.id]" class="project-link">
                <h3>{{ project.name }}</h3>
                @if (project.description) {
                  <p class="description">{{ project.description }}</p>
                }
                <div class="project-meta">
                  <span class="spec-count">
                    {{ project.specificationCount }} specification(s)
                  </span>
                  <span class="date">
                    {{ project.createdAt | date:'mediumDate' }}
                  </span>
                </div>
              </a>
            </app-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .project-list-page {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }
    
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }
    
    .page-header h1 {
      margin: 0;
    }
    
    .page-header p {
      margin: 0.25rem 0 0;
      color: #64748b;
    }
    
    .btn {
      padding: 0.75rem 1.5rem;
      border-radius: 0.375rem;
      font-weight: 500;
      text-decoration: none;
      transition: background 0.2s;
    }
    
    .btn-primary {
      background: #3b82f6;
      color: white;
    }
    
    .btn-primary:hover {
      background: #2563eb;
    }
    
    .project-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.5rem;
    }
    
    .project-link {
      display: block;
      text-decoration: none;
      color: inherit;
    }
    
    .project-link h3 {
      margin: 0 0 0.5rem;
      color: #1e293b;
    }
    
    .description {
      color: #64748b;
      margin: 0 0 1rem;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .project-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.875rem;
      color: #94a3b8;
    }
    
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      background: #f8fafc;
      border-radius: 0.5rem;
    }
    
    .empty-state h2 {
      margin: 0 0 0.5rem;
    }
    
    .empty-state p {
      margin: 0 0 1.5rem;
      color: #64748b;
    }
  `]
})
export class ProjectList implements OnInit {
  readonly store = inject(ProjectStore);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.store.loadProjects();
  }
}
```

**File**: `frontend/src/app/features/projects/project-create/project-create.ts`

```typescript
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectStore } from '../state/project.store';
import { Card } from '../../../shared/components/card/card';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { ErrorMessage } from '../../../shared/components/error-message/error-message';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [FormsModule, Card, LoadingSpinner, ErrorMessage],
  template: `
    <div class="create-page">
      <h1>Create New Project</h1>
      
      <app-card>
        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="name">Project Name *</label>
            <input 
              id="name"
              type="text"
              [(ngModel)]="name"
              name="name"
              required
              minlength="3"
              maxlength="255"
              placeholder="e.g., E-Commerce Platform"
              [disabled]="submitting()"
            />
            <small>3-255 characters</small>
          </div>
          
          <div class="form-group">
            <label for="description">Description</label>
            <textarea 
              id="description"
              [(ngModel)]="description"
              name="description"
              rows="5"
              maxlength="5000"
              placeholder="Describe your project in detail. The more context you provide, the better the AI-generated specifications will be."
              [disabled]="submitting()"
            ></textarea>
            <small>
              {{ description.length }}/5000 characters. 
              Be detailed for better AI results!
            </small>
          </div>
          
          @if (error()) {
            <app-error-message 
              [message]="error()!" 
              (dismiss)="error.set(null)" 
            />
          }
          
          <div class="form-actions">
            <button 
              type="button" 
              class="btn btn-secondary"
              (click)="goBack()"
              [disabled]="submitting()"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              class="btn btn-primary"
              [disabled]="!isValid() || submitting()"
            >
              @if (submitting()) {
                <span class="btn-loading">Creating...</span>
              } @else {
                Create Project
              }
            </button>
          </div>
        </form>
      </app-card>
    </div>
  `,
  styles: [`
    .create-page {
      max-width: 600px;
      margin: 0 auto;
      padding: 2rem;
    }
    
    h1 {
      margin-bottom: 1.5rem;
    }
    
    .form-group {
      margin-bottom: 1.5rem;
    }
    
    label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
    }
    
    input, textarea {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 0.375rem;
      font-size: 1rem;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    
    input:focus, textarea:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
    
    input:disabled, textarea:disabled {
      background: #f1f5f9;
    }
    
    small {
      display: block;
      margin-top: 0.25rem;
      color: #64748b;
      font-size: 0.875rem;
    }
    
    .form-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-end;
      margin-top: 2rem;
    }
    
    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }
    
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    .btn-primary {
      background: #3b82f6;
      color: white;
    }
    
    .btn-primary:hover:not(:disabled) {
      background: #2563eb;
    }
    
    .btn-secondary {
      background: #e2e8f0;
      color: #1e293b;
    }
    
    .btn-secondary:hover:not(:disabled) {
      background: #cbd5e1;
    }
  `]
})
export class ProjectCreate {
  private readonly store = inject(ProjectStore);
  private readonly router = inject(Router);

  name = '';
  description = '';
  submitting = signal(false);
  error = signal<string | null>(null);

  isValid(): boolean {
    return this.name.trim().length >= 3;
  }

  async onSubmit(): Promise<void> {
    if (!this.isValid() || this.submitting()) return;

    this.submitting.set(true);
    this.error.set(null);

    try {
      const project = await this.store.createProject(
        this.name.trim(),
        this.description.trim() || undefined
      );
      this.router.navigate(['/projects', project.id]);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Failed to create project');
    } finally {
      this.submitting.set(false);
    }
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }
}
```

**File**: `frontend/src/app/features/projects/project-detail/project-detail.ts`

```typescript
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectStore } from '../state/project.store';
import { AiGenerationService } from '../services/ai.service';
import { Card } from '../../../shared/components/card/card';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { ErrorMessage } from '../../../shared/components/error-message/error-message';
import { DatePipe, JsonPipe } from '@angular/common';
import { Specification, ParsedSpecification } from '../../../core/models';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [RouterLink, Card, LoadingSpinner, ErrorMessage, DatePipe, JsonPipe],
  template: `
    <div class="detail-page">
      @if (store.loading()) {
        <app-loading-spinner message="Loading project..." />
      } @else if (store.error()) {
        <app-error-message 
          [message]="store.error()!" 
          (dismiss)="store.clearError()" 
        />
      } @else if (store.selectedProject(); as project) {
        <header class="page-header">
          <div>
            <a routerLink="/projects" class="back-link">&larr; Back to Projects</a>
            <h1>{{ project.name }}</h1>
            @if (project.description) {
              <p class="description">{{ project.description }}</p>
            }
          </div>
          <div class="header-actions">
            <button 
              class="btn btn-primary" 
              (click)="generateSpec()"
              [disabled]="generating()"
            >
              @if (generating()) {
                Generating...
              } @else {
                Generate Specification
              }
            </button>
          </div>
        </header>

        @if (generating()) {
          <app-card>
            <div class="generating-state">
              <app-loading-spinner message="AI is generating your specification..." />
              <p>This may take 15-30 seconds. Please wait...</p>
            </div>
          </app-card>
        }

        @if (generationError()) {
          <app-error-message 
            [message]="generationError()!" 
            (dismiss)="generationError.set(null)" 
          />
        }

        @if (currentSpec(); as spec) {
          <section class="specification-section">
            <h2>Generated Specification (v{{ spec.version }})</h2>
            
            @if (parsedSpec(); as parsed) {
              <app-card [title]="parsed.projectTitle">
                <div class="spec-content">
                  <section>
                    <h3>Executive Summary</h3>
                    <p>{{ parsed.executiveSummary }}</p>
                  </section>

                  <section>
                    <h3>Target Audience</h3>
                    <ul>
                      @for (audience of parsed.targetAudience; track audience) {
                        <li>{{ audience }}</li>
                      }
                    </ul>
                  </section>

                  <section>
                    <h3>Modules</h3>
                    @for (module of parsed.modules; track module.name) {
                      <div class="module-card">
                        <h4>
                          {{ module.name }}
                          <span class="priority-badge priority-{{ module.priority.toLowerCase() }}">
                            {{ module.priority }}
                          </span>
                        </h4>
                        <p>{{ module.description }}</p>
                        
                        <h5>User Stories</h5>
                        @for (story of module.userStories; track story.id) {
                          <div class="user-story">
                            <strong>{{ story.id }}: {{ story.title }}</strong>
                            <p>{{ story.story }}</p>
                            <div class="acceptance-criteria">
                              <strong>Acceptance Criteria:</strong>
                              <ul>
                                @for (criteria of story.acceptanceCriteria; track criteria) {
                                  <li>{{ criteria }}</li>
                                }
                              </ul>
                            </div>
                          </div>
                        }
                      </div>
                    }
                  </section>

                  <section>
                    <h3>Technical Recommendations</h3>
                    <div class="tech-grid">
                      <div>
                        <h4>Frontend</h4>
                        <ul>
                          @for (tech of parsed.technicalRecommendations.frontend; track tech) {
                            <li>{{ tech }}</li>
                          }
                        </ul>
                      </div>
                      <div>
                        <h4>Backend</h4>
                        <ul>
                          @for (tech of parsed.technicalRecommendations.backend; track tech) {
                            <li>{{ tech }}</li>
                          }
                        </ul>
                      </div>
                      <div>
                        <h4>Database</h4>
                        <ul>
                          @for (tech of parsed.technicalRecommendations.database; track tech) {
                            <li>{{ tech }}</li>
                          }
                        </ul>
                      </div>
                    </div>
                  </section>

                  <section>
                    <h3>Estimated Complexity</h3>
                    <p class="complexity-badge complexity-{{ parsed.estimatedComplexity.toLowerCase() }}">
                      {{ parsed.estimatedComplexity }}
                    </p>
                  </section>

                  <section>
                    <h3>Suggested MVP Features</h3>
                    <ul>
                      @for (feature of parsed.suggestedMvpFeatures; track feature) {
                        <li>{{ feature }}</li>
                      }
                    </ul>
                  </section>
                </div>
              </app-card>
            }
          </section>
        } @else if (!generating()) {
          <div class="empty-spec-state">
            <h2>No specification yet</h2>
            <p>Click "Generate Specification" to create AI-powered requirements for this project.</p>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .detail-page {
      max-width: 1000px;
      margin: 0 auto;
      padding: 2rem;
    }
    
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 2rem;
    }
    
    .back-link {
      color: #3b82f6;
      text-decoration: none;
      font-size: 0.875rem;
    }
    
    .back-link:hover {
      text-decoration: underline;
    }
    
    h1 {
      margin: 0.5rem 0;
    }
    
    .description {
      color: #64748b;
      margin: 0;
    }
    
    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
    }
    
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    .btn-primary {
      background: #3b82f6;
      color: white;
    }
    
    .generating-state {
      text-align: center;
      padding: 2rem;
    }
    
    .generating-state p {
      color: #64748b;
      margin-top: 1rem;
    }
    
    .specification-section h2 {
      margin-bottom: 1rem;
    }
    
    .spec-content section {
      margin-bottom: 2rem;
      padding-bottom: 2rem;
      border-bottom: 1px solid #e2e8f0;
    }
    
    .spec-content section:last-child {
      border-bottom: none;
    }
    
    .spec-content h3 {
      color: #1e293b;
      margin-bottom: 0.75rem;
    }
    
    .module-card {
      background: #f8fafc;
      padding: 1rem;
      border-radius: 0.5rem;
      margin-bottom: 1rem;
    }
    
    .module-card h4 {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }
    
    .priority-badge {
      font-size: 0.75rem;
      padding: 0.125rem 0.5rem;
      border-radius: 9999px;
    }
    
    .priority-high { background: #fecaca; color: #991b1b; }
    .priority-medium { background: #fef08a; color: #854d0e; }
    .priority-low { background: #bbf7d0; color: #166534; }
    
    .user-story {
      background: white;
      padding: 1rem;
      border-radius: 0.375rem;
      margin-bottom: 0.75rem;
    }
    
    .user-story strong {
      color: #1e293b;
    }
    
    .acceptance-criteria {
      margin-top: 0.5rem;
      font-size: 0.875rem;
    }
    
    .tech-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
    }
    
    .tech-grid h4 {
      margin-bottom: 0.5rem;
    }
    
    .complexity-badge {
      display: inline-block;
      padding: 0.5rem 1rem;
      border-radius: 0.375rem;
      font-weight: 600;
    }
    
    .complexity-simple { background: #bbf7d0; color: #166534; }
    .complexity-moderate { background: #fef08a; color: #854d0e; }
    .complexity-complex { background: #fed7aa; color: #9a3412; }
    .complexity-very_complex { background: #fecaca; color: #991b1b; }
    
    .empty-spec-state {
      text-align: center;
      padding: 4rem 2rem;
      background: #f8fafc;
      border-radius: 0.5rem;
    }
  `]
})
export class ProjectDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly aiService = inject(AiGenerationService);
  readonly store = inject(ProjectStore);

  generating = signal(false);
  generationError = signal<string | null>(null);
  currentSpec = signal<Specification | null>(null);
  parsedSpec = signal<ParsedSpecification | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.store.loadProjectDetail(id);
      this.loadLatestSpec(id);
    }
  }

  async generateSpec(): Promise<void> {
    const project = this.store.selectedProject();
    if (!project || this.generating()) return;

    this.generating.set(true);
    this.generationError.set(null);

    try {
      const spec = await this.aiService.generate(project.id).toPromise();
      if (spec) {
        this.currentSpec.set(spec);
        this.parsedSpec.set(JSON.parse(spec.content));
      }
    } catch (err) {
      this.generationError.set(
        err instanceof Error ? err.message : 'Failed to generate specification'
      );
    } finally {
      this.generating.set(false);
    }
  }

  private async loadLatestSpec(projectId: string): Promise<void> {
    try {
      const spec = await this.aiService
        .generate(projectId)
        // We'll use SpecificationService instead
    } catch {
      // No spec yet - that's fine
    }
  }
}
```

### 5.8 Task 7: Configure Routing

**File**: `frontend/src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'projects',
    pathMatch: 'full'
  },
  {
    path: 'projects',
    children: [
      {
        path: '',
        loadComponent: () => 
          import('./features/projects/project-list/project-list')
            .then(m => m.ProjectList)
      },
      {
        path: 'new',
        loadComponent: () => 
          import('./features/projects/project-create/project-create')
            .then(m => m.ProjectCreate)
      },
      {
        path: ':id',
        loadComponent: () => 
          import('./features/projects/project-detail/project-detail')
            .then(m => m.ProjectDetail)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'projects'
  }
];
```

### 5.9 Task 8: Update App Configuration

**File**: `frontend/src/app/app.config.ts`

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      // Add interceptors here if needed
      // withInterceptors([errorInterceptor])
    )
  ]
};
```

### 5.10 Task 9: Update Root Component

**File**: `frontend/src/app/app.html`

```html
<div class="app-container">
  <header class="app-header">
    <a routerLink="/" class="logo">SmartSpec</a>
    <nav>
      <a routerLink="/projects">Projects</a>
    </nav>
  </header>
  
  <main class="app-main">
    <router-outlet />
  </main>
  
  <footer class="app-footer">
    <p>SmartSpec - AI-Powered Software Requirements Generator</p>
  </footer>
</div>
```

**File**: `frontend/src/app/app.scss`

```scss
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: #1e293b;
  color: white;
}

.logo {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  text-decoration: none;
}

.app-header nav a {
  color: #94a3b8;
  text-decoration: none;
  margin-left: 2rem;
  transition: color 0.2s;
}

.app-header nav a:hover {
  color: white;
}

.app-main {
  flex: 1;
  background: #f1f5f9;
}

.app-footer {
  padding: 1rem 2rem;
  background: #1e293b;
  color: #94a3b8;
  text-align: center;
}

.app-footer p {
  margin: 0;
  font-size: 0.875rem;
}
```

**File**: `frontend/src/app/app.ts` (update)

```typescript
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
```

---

## 6. Sprint 4: Production Polish

### 6.1 Add HTTP Error Interceptor

**File**: `frontend/src/app/core/interceptors/error.interceptor.ts`

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An error occurred';
      
      if (error.status === 0) {
        errorMessage = 'Cannot connect to server. Please check your connection.';
      } else if (error.status === 401) {
        // Redirect to login if we add auth
        errorMessage = 'Unauthorized';
      } else if (error.status === 404) {
        errorMessage = 'Resource not found';
      } else if (error.status >= 500) {
        errorMessage = 'Server error. Please try again later.';
      } else if (error.error?.message) {
        errorMessage = error.error.message;
      }
      
      console.error(`HTTP Error ${error.status}: ${errorMessage}`);
      
      return throwError(() => new Error(errorMessage));
    })
  );
};
```

Update `app.config.ts` to use the interceptor:

```typescript
import { errorInterceptor } from './core/interceptors/error.interceptor';

// In providers array:
provideHttpClient(
  withInterceptors([errorInterceptor])
)
```

### 6.2 Production Dockerfile for Frontend

**File**: `frontend/Dockerfile.prod`

```dockerfile
# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source
COPY . .

# Build for production
RUN npm run build -- --configuration=production

# Runtime stage
FROM nginx:alpine

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built app
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Expose port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

**File**: `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Enable gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # SPA routing - redirect all requests to index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 7. Testing Strategy

### 7.1 Backend Unit Tests

**File**: `backend/src/test/java/com/smartspec/service/ProjectServiceTest.java`

```java
package com.smartspec.service;

import com.smartspec.dto.request.CreateProjectRequest;
import com.smartspec.dto.response.ProjectResponse;
import com.smartspec.exception.DuplicateResourceException;
import com.smartspec.mapper.ProjectMapper;
import com.smartspec.model.Project;
import com.smartspec.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService.
 * 
 * Uses Mockito to mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private ProjectMapper projectMapper;
    
    @InjectMocks
    private ProjectService projectService;
    
    private Project testProject;
    private CreateProjectRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(UUID.randomUUID())
                .name("Test Project")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createRequest = new CreateProjectRequest("Test Project", "Test Description");
    }
    
    @Test
    @DisplayName("Should create project successfully")
    void createProject_Success() {
        // Arrange
        when(projectRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(projectMapper.toEntity(createRequest)).thenReturn(testProject);
        when(projectRepository.save(any())).thenReturn(testProject);
        when(projectMapper.toResponse(testProject)).thenReturn(
                new ProjectResponse(testProject.getId(), testProject.getName(),
                        testProject.getDescription(), testProject.getCreatedAt(),
                        testProject.getUpdatedAt(), 0)
        );
        
        // Act
        ProjectResponse result = projectService.createProject(createRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Project");
        verify(projectRepository).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when project name already exists")
    void createProject_DuplicateName_ThrowsException() {
        // Arrange
        when(projectRepository.existsByNameIgnoreCase(any())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
```

### 7.2 Backend Integration Tests

**File**: `backend/src/test/java/com/smartspec/controller/ProjectControllerIntegrationTest.java`

```java
package com.smartspec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartspec.dto.request.CreateProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Uses application-test.yml (H2 database)
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndRetrieveProject() throws Exception {
        // Create project
        var createRequest = new CreateProjectRequest("Integration Test Project", "Test");
        
        String response = mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Project"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract ID and retrieve
        String id = objectMapper.readTree(response).get("id").asText();
        
        mockMvc.perform(get("/api/v1/projects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Project"));
    }

    @Test
    void shouldReturn404ForNonexistentProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateProjectName() throws Exception {
        var invalidRequest = new CreateProjectRequest("ab", null); // Too short
        
        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
```

### 7.3 Running Tests

```bash
# Run backend tests
cd backend
./mvnw test

# Run with coverage report
./mvnw verify

# Run frontend tests
cd frontend
npm test
```

---

## 8. Common Mistakes to Avoid

### 8.1 Backend Mistakes

| Mistake | Why It's Bad | Correct Approach |
|---------|--------------|------------------|
| Using `@Data` on entities | Proxy issues with Hibernate | Use `@Getter`, `@Setter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` |
| Returning entities from controllers | Exposes internal structure, security risk | Always use DTOs |
| `FetchType.EAGER` on collections | Loads all data even when not needed | Always use `FetchType.LAZY` |
| Catching `Exception` everywhere | Hides specific errors | Catch specific exceptions |
| Not validating input | SQL injection, invalid data | Use `@Valid` and Bean Validation |
| Hardcoding configuration | Can't change without rebuild | Use `application.yml` and env vars |
| Not using transactions | Data inconsistency | Use `@Transactional` |

### 8.2 Frontend Mistakes

| Mistake | Why It's Bad | Correct Approach |
|---------|--------------|------------------|
| Not unsubscribing from observables | Memory leaks | Use `async` pipe or `takeUntilDestroyed()` |
| Mutating state directly | Unpredictable behavior | Use immutable updates |
| Not typing API responses | Runtime errors | Use TypeScript interfaces |
| Hardcoding API URLs | Can't deploy to different envs | Use environment files |
| Not handling loading states | Bad UX | Show spinners/skeletons |
| Not handling errors | Silent failures | Show error messages to users |

### 8.3 General Mistakes

| Mistake | Why It's Bad | Correct Approach |
|---------|--------------|------------------|
| Committing `.env` files | Exposes secrets | Add to `.gitignore` |
| Not using version control | Can't track/revert changes | Commit frequently |
| Not testing | Bugs in production | Write tests as you code |
| Ignoring logs | Can't debug issues | Check logs first when debugging |
| Premature optimization | Wastes time, adds complexity | Make it work, then optimize |

---

## 9. Git Workflow & Commits

### 9.1 Conventional Commits Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, semicolons)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding tests
- `chore`: Build process or auxiliary tool changes

**Examples:**
```bash
git commit -m "feat(backend): add Project entity and repository"
git commit -m "fix(api): return 404 when project not found"
git commit -m "docs: update README with API endpoints"
git commit -m "test(backend): add ProjectService unit tests"
```

### 9.2 Recommended Commit Points

After completing each task in this guide, create a commit:

```bash
# After creating entities
git add .
git commit -m "feat(backend): add JPA entities for Project, Specification, Conversation, Message"

# After creating repositories
git add .
git commit -m "feat(backend): add Spring Data JPA repositories"

# After creating DTOs and mappers
git add .
git commit -m "feat(backend): add DTOs and MapStruct mappers"

# After creating services
git add .
git commit -m "feat(backend): implement ProjectService and SpecificationService"

# After creating controllers
git add .
git commit -m "feat(api): add REST controllers for projects and specifications"

# After AI integration
git add .
git commit -m "feat(ai): integrate Google Gen AI SDK for spec generation"

# After frontend components
git add .
git commit -m "feat(frontend): add project list, create, and detail components"
```

---

## 10. Troubleshooting Guide

### 10.1 Common Issues

#### Docker Issues

**Problem**: `Cannot connect to the Docker daemon`
```bash
# Linux: Start Docker service
sudo systemctl start docker

# Check if Docker is running
docker info
```

**Problem**: `Port already in use`
```bash
# Find what's using the port
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change ports in docker-compose.yml
```

#### Backend Issues

**Problem**: `Connection refused to database`
```bash
# Check if database is running
docker-compose ps

# Check database logs
docker-compose logs db

# Make sure backend waits for db
# (already configured with depends_on and healthcheck)
```

**Problem**: `Flyway migration failed`
```bash
# Check migration files for SQL errors
# View Flyway history
docker-compose exec db psql -U postgres -d smartspec_db -c "SELECT * FROM flyway_schema_history"

# To start fresh (DELETES ALL DATA)
docker-compose down -v
docker-compose up --build
```

**Problem**: `MapStruct mapper not generating`
```bash
# Rebuild with Maven
cd backend
./mvnw clean compile

# Check for Lombok + MapStruct binding in pom.xml
```

#### Frontend Issues

**Problem**: `CORS error`
```bash
# Check CorsConfig.java has correct origin
# Default: http://localhost:4200

# Check browser network tab for actual error
```

**Problem**: `Cannot find module`
```bash
# Clear node_modules and reinstall
cd frontend
rm -rf node_modules
npm install
```

### 10.2 Debugging Tips

#### Backend Debugging

1. **Check logs**: `docker-compose logs -f backend`
2. **Test endpoints**: Use Swagger UI at `http://localhost:8080/swagger-ui.html`
3. **Check database**: 
   ```bash
   docker-compose exec db psql -U postgres -d smartspec_db
   \dt  # List tables
   SELECT * FROM projects;
   ```

#### Frontend Debugging

1. **Open DevTools**: F12 in browser
2. **Network tab**: Check API requests/responses
3. **Console tab**: Look for JavaScript errors
4. **Angular DevTools**: Install browser extension

---

## Final Checklist

Before considering the project complete, verify:

- [ ] All Docker services start without errors
- [ ] Backend health endpoint returns `{"status":"UP"}`
- [ ] Can create a project via API
- [ ] Can generate specification via API (AI call works)
- [ ] Frontend loads at `http://localhost:4200`
- [ ] Can create project in UI
- [ ] Can generate specification in UI
- [ ] Swagger UI shows all endpoints
- [ ] All tests pass
- [ ] No hardcoded secrets in code
- [ ] README is up to date

---

**You've got this!** Follow this guide step by step, commit frequently, and don't hesitate to check the logs when something goes wrong. Good luck!

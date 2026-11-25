# Architecture & Design Documentation

## 1. System Overview

**Civic Issue Heatmap** is a high-performance, geospatial municipal issue reporting and analytics platform built on **Spring Boot 3.3.4**, **PostgreSQL 16 / PostGIS 3.4**, and a responsive **Leaflet.js** frontend.

The platform streamlines civic grievance redressal by automating duplicate detection, algorithmic severity prioritization, department routing, and real-time hotspot visualization.

```
+-------------------------------------------------------------------------------+
|                             Client Applications                               |
|        Citizen Web UI (Leaflet/JS)     |     Admin & Officer Dashboard        |
+---------------------------------------+---------------------------------------+
                                    | (REST + JWT + Multipart)
                                    v
+-------------------------------------------------------------------------------+
|                      Presentation & Security Layer                            |
|  - MdcLoggingFilter (X-Request-Id)        - RateLimiter (Sliding Window Bucket) |
|  - JwtAuthFilter & SecurityFilterChain    - GlobalExceptionHandler            |
|  - REST Controllers (Complaints, Heatmap, Auth, Admin, Departments, Files)   |
+-------------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------------+
|                        Service & Orchestration Layer                          |
|  - ComplaintService   - HeatmapService   - AuthService   - AdminService       |
|  - FileStorageService (Local Disk with sanitization & MIME enforcement)       |
+-------------------------------------------------------------------------------+
                 |                      |                      |
                 v                      v                      v
+-------------------------------------------------------------------------------+
|                        Domain Engines (Strategy Pattern)                      |
|  1. DuplicateDetector (GeoTextDuplicateDetector: Spatial + Jaccard Tokenizer) |
|  2. SeverityCalculator (WeightedSeverityCalculator: Base+Density+Votes+Words) |
|  3. RoutingStrategy (CategoryRoutingStrategy: IssueCategory -> Department)    |
|  4. HotspotClusterer (GridHotspotClusterer: Bounding Grid + Centroid + Heat)  |
+-------------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------------+
|                         Data Access & Spatial Layer                           |
|  - Spring Data JPA + Hibernate Spatial 6.5                                    |
|  - Flyway Migrations (V1 Schema, V2 Seed, V3 GIST Index, V4 Upvotes)          |
|  - Spatial Repositories (ST_DWithin, ST_Distance, ST_MakePoint, GIST Index)   |
|  - Spring Cache (ConcurrentMap / Redis-ready abstraction)                     |
+-------------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------------+
|                       PostgreSQL 16 + PostGIS 3.4                             |
|          Spatial GiST Indexes on geometry(Point, 4326) columns                |
+-------------------------------------------------------------------------------+
```

---

## 2. Multi-Layer Architecture

### A. Presentation & Web Layer
- **RESTful Endpoints**: Built with Spring MVC adhering to HTTP semantics and standardized JSON responses.
- **DTO Transformation**: Requests and responses use immutable DTO records/classes (`ComplaintCreateRequest`, `ComplaintResponse`, `ComplaintDetailResponse`, `PageResponse<T>`). Domain entities are never leaked to API consumers.
- **Exception Boundary**: Handled centrally by `GlobalExceptionHandler` mapping domain and validation exceptions to structured `ErrorResponse` payloads.

### B. Security & Cross-Cutting Layer
- **MDC Correlation Logging (`MdcLoggingFilter`)**: Assigns a unique `X-Request-Id` UUID per HTTP request, populating SLF4J MDC to correlate logs across thread pools and injecting the ID into response headers.
- **Rate Limiting (`RateLimiter`)**: Thread-safe in-memory sliding-window bucket algorithm enforcing request thresholds per client IP / user account (e.g. 10 complaint submissions/minute) to mitigate spam and DoS attacks.
- **Stateless JWT Authentication (`JwtAuthFilter`, `JwtTokenProvider`)**: HMAC-SHA256 token verification supporting three discrete roles: `CITIZEN`, `OFFICER`, and `ADMIN`.

### C. Domain Engines (Strategy Pattern)
1. **Duplicate Detection Engine (`DuplicateDetector`)**:
   - Compares incoming complaints against open/assigned issues within a configurable spatial radius (default: 50m) and temporal window (default: 14 days).
   - Computes a hybrid score: $0.6 \times \text{Distance Score} + 0.4 \times \text{Text Jaccard Similarity}$.
   - If score $\ge 0.65$, automatically merges as `DUPLICATE`, links `duplicateOf`, and upvotes the original complaint.
2. **Severity Engine (`SeverityCalculator`)**:
   - Calculates a multi-factor score $(0 - 100)$:
     $$\text{Score} = \text{BaseWeight} + \text{UpvoteContribution} + \text{DensityBoost} + \text{KeywordBoost} + \text{AgeFactor}$$
   - Maps score to `LOW` ($0-24$), `MEDIUM` ($25-49$), `HIGH` ($50-74$), or `CRITICAL` ($75-100$).
   - Scheduled daily job (`@Scheduled(cron = "0 0 2 * * *")`) periodically recalibrates aging unresolved complaints.
3. **Automatic Routing Engine (`RoutingStrategy`)**:
   - Maps `IssueCategory` to responsible municipal departments (e.g., `POTHOLE` $\to$ Roads & Infrastructure, `WATER_LEAK` $\to$ Water Supply).
   - Updates status to `ASSIGNED` and logs an immutable audit entry into `ComplaintStatusHistory`.
4. **Hotspot Clustering Engine (`HotspotClusterer`)**:
   - Aggregates geographic points into uniform Cartesian grid cells using `GeoUtils.metersToDegrees()`.
   - Calculates centroids, incident count, average severity, dominant issue category, and normalized heat intensity ($0.0 - 1.0$).

### D. Data & Spatial Layer
- **Hibernate Spatial & PostGIS**: Uses `org.locationtech.jts.geom.Point` mapped to `geometry(Point, 4326)` in PostgreSQL.
- **Spatial Queries**: Leverages PostGIS native functions (`ST_DWithin`, `ST_Distance`, `ST_MakePoint`) accelerated by spatial **GiST indexes**.
- **Flyway Migrations**: Version-controlled SQL migration scripts (`V1__init_schema.sql` through `V4__create_upvotes_table.sql`).

---

## 3. Design Patterns Applied

| Pattern | Component / Implementation | Rationale |
| :--- | :--- | :--- |
| **Strategy Pattern** | `DuplicateDetector`, `SeverityCalculator`, `RoutingStrategy`, `HotspotClusterer` | Enables pluggable algorithms (e.g., swapping Jaccard for TF-IDF/embeddings, or grid clustering for DBSCAN) without touching service orchestration code. |
| **Data Transfer Object (DTO)** | `com.civicheat.dto.*` & `ComplaintMapper` | Decouples internal database schemas and PostGIS geometry objects from external API contracts. |
| **Repository & Specification** | `ComplaintRepository`, `ComplaintSpecification` | Encapsulates SQL/JPQL queries and enables composable, dynamic multi-attribute database filtering. |
| **Finite State Machine (FSM)** | `AdminServiceImpl.ALLOWED_TRANSITIONS` | Strictly enforces valid complaint status transitions (`SUBMITTED` $\to$ `ASSIGNED` $\to$ `IN_PROGRESS` $\to$ `RESOLVED` / `REJECTED`). |
| **Filter / Interceptor Chain** | `MdcLoggingFilter`, `JwtAuthFilter` | Transparently handles cross-cutting concerns (request tracing and authentication) before hitting controllers. |
| **Sliding Window Bucket** | `RateLimiter` | High-throughput, thread-safe rate limiting with automatic timestamp pruning. |
| **Observer / Audit Trail** | `ComplaintStatusHistory`, `ComplaintUpvote` | Maintains an immutable chronological audit trail of all status modifications and upvotes. |

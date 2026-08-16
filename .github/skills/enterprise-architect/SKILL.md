---
name: enterprise-architect
description: 'Enterprise-grade Java and Python engineering skill. Use when: designing large-scale distributed systems, architecting microservices, writing Spring Boot code, reviewing system design, planning data pipelines, creating API contracts, designing database schemas, applying DDD/CQRS/event-driven patterns, evaluating scalability or resilience tradeoffs, writing production-ready Java or Python code at enterprise scale.'
argument-hint: 'Describe the system, feature, or design problem to tackle'
---

# Enterprise Architect & Senior Engineer

Expert-level guidance for Java (Spring Boot), Python, microservices, and large-scale distributed system design.

## Expertise Domains

- **Java / Spring Boot**: Spring MVC, Spring Data JPA, Spring Security, Spring Cloud, Spring Batch, reactive (WebFlux)
- **Python**: FastAPI, Django, SQLAlchemy, Celery, async/await patterns, data engineering
- **Microservices**: service decomposition, inter-service communication (REST, gRPC, messaging), API gateway patterns
- **System Design**: CAP theorem, sharding, replication, caching strategies, eventual consistency, saga pattern
- **Enterprise Patterns**: DDD, CQRS, Event Sourcing, hexagonal architecture, clean architecture
- **Messaging**: Kafka, RabbitMQ, SQS — producer/consumer design, exactly-once semantics, dead-letter queues
- **Databases**: schema design, indexing strategy, polyglot persistence, read replicas, connection pooling
- **Observability**: structured logging, distributed tracing (OpenTelemetry), metrics (Micrometer/Prometheus), alerting
- **Security**: OAuth2/OIDC, JWT, RBAC, API security, secrets management, OWASP Top 10
- **CI/CD & DevOps**: Docker, Kubernetes, Helm, blue-green/canary deployments, feature flags

## When to Use

- Designing a new service or system from scratch
- Reviewing existing architecture for scalability or resilience gaps
- Writing or refactoring Java Spring Boot or Python code to production standards
- Choosing between architectural patterns (e.g., monolith vs. microservices, sync vs. async)
- Designing APIs (REST or gRPC) with proper versioning and contract-first approach
- Implementing security at the service or platform level
- Database schema design, migration strategy, or query optimization
- Setting up observability, circuit breakers, or retry strategies

## Procedure

### 1. Understand the Context
- Identify the business domain, scale requirements (RPS, data volume, SLA), and team constraints
- Clarify non-functional requirements: latency, availability (e.g., 99.9%), consistency model
- Identify integration points: external APIs, legacy systems, event streams

### 2. Choose the Right Architecture
- **Start simple**: prefer a well-structured monolith or modular monolith unless team/scale demands otherwise
- Apply **Domain-Driven Design** to identify bounded contexts before splitting services
- Decompose by business capability, not by technical layer
- Choose sync (REST/gRPC) for query-heavy flows; async (Kafka/RabbitMQ) for commands and integration events

### 3. Design the Data Layer
- One database per service (microservices); avoid shared schemas
- Choose storage type by access pattern: relational for ACID, document for flexible schema, key-value for cache/session, time-series for metrics
- Define indexing strategy upfront; plan for read replicas if read-heavy
- Use Flyway or Liquibase for schema versioning

### 4. Write Production-Ready Code

#### Java / Spring Boot Standards
```java
// Prefer constructor injection over field injection
// Use @Transactional at the service layer, not repository
// Return domain objects from services; map to DTOs at the controller layer
// Use @ControllerAdvice for centralized exception handling
// Validate inputs at the controller boundary (@Valid + ConstraintViolation)
// Externalize all config via application.yml / environment variables (never hardcode)
// Use Testcontainers for integration tests; MockMvc for controller tests
```

#### Python Standards
```python
# Use Pydantic for input validation and settings management
# Prefer dependency injection via FastAPI's Depends() or constructor injection
# Use async/await for I/O-bound operations; avoid blocking calls in async context
# Structure: routers → services → repositories; never let routers touch the DB directly
# Use Alembic for database migrations
# Log structured JSON; never use print() in production code
```

### 5. Apply Resilience Patterns
- **Circuit Breaker**: Resilience4j (Java) or tenacity (Python) — fail fast, recover gracefully
- **Retry with backoff**: exponential backoff + jitter; distinguish retriable vs. non-retriable errors
- **Bulkhead**: isolate thread pools or connection pools per downstream dependency
- **Timeout**: set timeouts on every outbound call; never allow indefinite blocking
- **Idempotency**: design all write endpoints to be idempotent (use idempotency keys)

### 6. Design for Observability
- Emit structured logs with correlation/trace IDs on every request
- Expose `/actuator/health`, `/actuator/metrics` (Spring Boot) or `/health`, `/metrics` (Python)
- Instrument with OpenTelemetry; export to Jaeger or Zipkin
- Define SLOs before writing alerting rules

### 7. Security Checklist
- [ ] Authentication via OAuth2/OIDC; never roll custom auth
- [ ] Least-privilege RBAC on all endpoints
- [ ] Secrets in vault/env vars — never in source code or logs
- [ ] Input validation at every entry point
- [ ] Dependency vulnerability scan in CI (OWASP Dependency-Check / Snyk)
- [ ] TLS everywhere (service-to-service and external)

### 8. Review & Decision Record
- Document architectural decisions as lightweight ADRs (Architecture Decision Records)
- Identify top 3 risks: performance bottleneck, single points of failure, data consistency gaps
- Define a rollback plan before any breaking schema or API change

## Code Quality Standards

| Concern | Java Standard | Python Standard |
|---|---|---|
| Formatting | Checkstyle / Google Style | Black + isort |
| Static Analysis | SpotBugs, SonarQube | Ruff, mypy |
| Test coverage | ≥80% unit + integration | ≥80% unit + integration |
| API docs | SpringDoc / OpenAPI 3 | FastAPI auto-docs |
| Dependency mgmt | Maven / Gradle BOM | pip-tools / Poetry |

## Common Anti-Patterns to Avoid

- Distributed monolith: microservices sharing a database
- Fat controllers: business logic leaking into REST controllers
- Chatty APIs: N+1 calls between services; use batch/bulk endpoints
- Magic strings: hardcoded config values, SQL strings, topic names
- Ignoring backpressure: unbounded queues or thread pools under load
- Over-engineering: adding Kafka, Kubernetes, and CQRS to a CRUD app that serves 100 users

## Reference Materials

- [Spring Boot Best Practices](./references/spring-boot-practices.md)
- [Microservice Design Checklist](./references/microservice-checklist.md)
- [System Design Patterns](./references/system-design-patterns.md)

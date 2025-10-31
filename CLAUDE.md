# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin library (v0.6.33) providing reusable infrastructure components for backend applications. Built on Ktor 2.0 and Exposed ORM, targeting JVM 23. The SDK provides utilities for database access, HTTP handling, authentication, filtering/pagination, observability, and third-party integrations.

## Common Commands

### Building and Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ClassName"

# Run specific test method
./gradlew test --tests "ClassName.testMethodName"

# Run linting
./gradlew detektMain
./gradlew detektTest

# Update detekt baseline (to suppress existing issues)
./gradlew detektBaselineMain
./gradlew detektBaselineTest

# Build the library
./gradlew build

# Clean build artifacts
./gradlew clean
```

### Project Configuration
- **JVM Target**: 23
- **Kotlin Version**: 2.x
- **Build Tool**: Gradle with Kotlin DSL
- **Version Catalog**: `deps` in `settings.gradle.kts`
- **Testing Framework**: JUnit 5

## High-Level Architecture

### Core Design Philosophy
1. **Coroutine-first**: All async operations use suspend functions
2. **Dependency Injection**: Google Guice throughout (constructor injection preferred)
3. **Type Safety**: Sealed classes, inline classes, and generics for compile-time safety
4. **Separation of Concerns**: Clear module boundaries with interface-based design
5. **Built-in Observability**: OpenTelemetry tracing, Prometheus metrics, structured logging

### Transaction Management Pattern
The `TransactionManager` (dev.tmsoft.lib.exposed) provides the core database access pattern:
- **Write operations**: Use `TransactionManager()` for primary database
- **Read operations**: Use `readOnlyTransaction()` for automatic replica routing
- **Advisory locking**: Use `withDatabaseLock()` or `withTryDatabaseLock()` for critical sections
- All transactions automatically run on `Dispatchers.IO`
- Both async (`invoke()`) and sync (`sync()`) versions available

### Request Handling Pattern
Requests flow through: **Router** → **Controller** → **Business Logic** → **Response**
- **Router**: Defines routes and injects controllers via `ControllerPipeline`
- **Controller**: Handles HTTP requests, extracts parameters, calls business logic
- **Interceptor**: Route-scoped middleware for cross-cutting concerns
- **Response**: Sealed class hierarchy for type-safe API responses

### Response Types (dev.tmsoft.lib.ktor)
All API responses use the `Response` sealed class:
- `Response.Data<T>` - Success with data (200)
- `Response.Error` - Single error (400)
- `Response.Errors` - Multiple errors (400)
- `Response.Listing<T>` - Paginated results with metadata
- `Response.Ok` - Simple success (200)
- `Response.Empty` - No content (204)
- `Response.Either<TL, TR>` - Either monad for dual return types
- `Response.Redirect` - Redirect (302)
- `Response.File` - File download

### Query & Filtering System
**QueryObject Pattern** (dev.tmsoft.lib.exposed.query):
- Encapsulates queries as objects implementing `QueryObject<T>`
- Execute via `QueryExecutor` within transactions
- `CachedQueryObject` provides TTL-based caching (14 days default)
- `QueryFold` for loading nested entities from JOINs without N+1 queries

**Filter System** (dev.tmsoft.lib.query.filter):
- Define filterable fields declaratively: `object UserFilter : Filter(UserTable)`
- Automatic type inference from column types
- `PathValues` maps HTTP query params to filter values
- Value types: `SingleValue` (exact/LIKE), `RangeValue` (from-to), `ListValue` (OR)
- Automatic JOIN handling when filtering on related tables

**Pagination** (dev.tmsoft.lib.query.paging):
- Page numbers start at 1 (not 0)
- Default page size: 30, max: 100
- `ContinuousList<T>` contains data + metadata (hasMore, total count optional)
- Use `Query.toContinuousList(page)` for automatic pagination
- Optimized for JOINed queries using WHERE IN subqueries

### Authorization Pattern
**Activity-Based Authorization** (dev.tmsoft.lib.ktor.auth):
- Use activities instead of roles: `route.authorize(setOf("user:read", "user:write"))`
- Principal must have required activities to access route
- Activities accumulate hierarchically through route tree
- Custom validation and challenge handlers supported

### DAO Pattern Extensions
**PrivateEntityClass** (dev.tmsoft.lib.exposed.dao):
- Wraps Exposed's `EntityClass` to prevent direct instantiation outside designated scopes
- Use for enforcing entity creation through factory methods
- Prevents accidental bypass of validation/business logic

**EmbeddedColumn** (dev.tmsoft.lib.exposed.dao):
- Stores complex Kotlin objects as composite columns
- Automatic mapping between database columns and nested structures
- Support for nullable embedded objects
- Useful for value objects (Address, Money, etc.)

### Redis Integration
**RedisPersistentMap** (dev.tmsoft.lib.redis):
- Type-safe key-value storage with automatic JSON serialization
- Optional key prefixing for namespacing
- TTL support for expiring data
- Also available: `RedisPersistentSet`, `RedisPersistentHash`, `RedisPersistentList`

### Background Tasks
**Worker** (dev.tmsoft.lib.worker):
- Abstract base class for periodic background tasks
- Configurable interval and initial delay
- Automatic metrics, tracing, and error capture (Sentry)
- MDC context for correlated logging

### Observability
**Tracing** (dev.tmsoft.lib.tracing):
- OpenTelemetry integration
- Use `withTrace()` to execute code within trace context
- Use `withNewTrace()` to start new trace spans
- Automatic error recording

**Metrics** (dev.tmsoft.lib.metrics):
- Micrometer integration with Prometheus export
- `timer` - Measure operation duration with tags
- `gauge` - Time-based gauges
- `gaugeTimer` - Combined gauge and timer

**Logging** (dev.tmsoft.lib.logger):
- SLF4J wrapper with Kotlin extensions
- MDC support for request correlation
- Automatic request ID generation

## Module Organization

### Primary Modules
- `dev.tmsoft.lib.exposed` - Database access layer (Exposed ORM extensions)
- `dev.tmsoft.lib.ktor` - HTTP handling (controllers, responses, auth)
- `dev.tmsoft.lib.query` - Filtering and pagination
- `dev.tmsoft.lib.redis` - Redis client abstractions
- `dev.tmsoft.lib.email` - Email sending
- `dev.tmsoft.lib.socialauth` - OAuth integration (Google, Facebook, Twitter, Apple)
- `dev.tmsoft.lib.upload` - File storage (local, S3)
- `dev.tmsoft.lib.serialization` - Custom serializers
- `dev.tmsoft.lib.worker` - Background tasks
- `dev.tmsoft.lib.tracing` - Distributed tracing
- `dev.tmsoft.lib.metrics` - Application metrics
- `dev.tmsoft.lib.logger` - Logging utilities
- `dev.tmsoft.lib.validation` - Request validation
- `dev.tmsoft.lib.structure` - Functional utilities (Either monad)

### Key Files for Understanding Architecture
- `src/main/kotlin/dev/tmsoft/lib/exposed/TransactionManager.kt` - Database transaction patterns
- `src/main/kotlin/dev/tmsoft/lib/ktor/Response.kt` - API response structure
- `src/main/kotlin/dev/tmsoft/lib/query/filter/Filter.kt` - Dynamic query filtering
- `src/main/kotlin/dev/tmsoft/lib/query/paging/ContinuousList.kt` - Pagination implementation
- `src/main/kotlin/dev/tmsoft/lib/ktor/auth/Authorization.kt` - Activity-based authorization
- `src/main/kotlin/dev/tmsoft/lib/exposed/query/QueryFold.kt` - Nested entity loading
- `src/main/kotlin/dev/tmsoft/lib/ktor/Router.kt` - Request routing patterns
- `src/main/kotlin/dev/tmsoft/lib/exposed/dao/EmbeddedColumn.kt` - Embedded object mapping

## Technology Stack

**Core Dependencies:**
- Kotlin 2.x (JVM 23 target)
- Ktor 2.0 (Server & Client)
- Exposed ORM v1
- Kotlinx Serialization
- Google Guice (DI)
- PostgreSQL + HikariCP
- Redis (Jedis)
- RabbitMQ (AMQP)
- Valiktor (Validation)
- Hoplite (Configuration)

**Observability:**
- OpenTelemetry (Tracing)
- Micrometer + Prometheus (Metrics)
- SLF4J + Log4j2 (Logging)
- Sentry (Error tracking)

**Testing:**
- JUnit 5
- H2 (in-memory database)
- Embedded PostgreSQL
- GreenMail (email testing)
- Ktor test host
- Detekt (static analysis)

## Conventions

### String Filtering
- String filters use case-insensitive LIKE with prefix matching by default
- Override filter behavior by providing custom column function

### Pagination
- Page numbers are 1-indexed
- Use "hasMore" indicator instead of total pages for performance
- Total count is optional and calculated only when needed

### Error Handling
- Use `Response.Error` for single errors
- Use `Response.Errors` for multiple validation errors
- Error objects contain: message, property, value, parameters
- Sentry integration captures unhandled exceptions

### Serialization
- Use Kotlinx Serialization for all JSON operations
- Custom serializers in `dev.tmsoft.lib.serialization`
- `InferSerializer` for dynamic serializer resolution
- Polymorphic serialization for Principal and Session types

### Transaction Boundaries
- Keep transactions as short as possible
- Use read-only transactions for query operations
- Advisory locks for operations requiring serialization
- Avoid nested transactions (they share the same connection)

### TimescaleDB Support
- `TimeBucket` function available for time-series aggregation
- Hypertable support through standard Exposed operations
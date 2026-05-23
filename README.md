# B2Chat – E-Commerce Order Management System

> **Backend Software Developer Practical Test** — Spring Boot 3 · PostgreSQL · Redis · JWT · Docker

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Setup & Run](#setup--run)
   - [Option A – Docker Compose (recommended)](#option-a--docker-compose-recommended)
   - [Option B – Local Development](#option-b--local-development)
4. [Environment Variables](#environment-variables)
5. [API Reference](#api-reference)
   - [Authentication](#authentication)
   - [Users](#users)
   - [Products](#products)
   - [Orders](#orders)
6. [Sample Requests & Responses](#sample-requests--responses)
7. [Running Tests](#running-tests)
8. [Assumptions & Design Decisions](#assumptions--design-decisions)

---

## Tech Stack

| Layer        | Technology                            |
|--------------|---------------------------------------|
| Language     | Java 17                               |
| Framework    | Spring Boot 3.2.5                     |
| ORM          | Spring Data JPA / Hibernate           |
| Database     | PostgreSQL 16                         |
| Cache        | Redis 7 (Lettuce driver)              |
| Auth         | Spring Security + JWT (JJWT 0.11.5)  |
| API Docs     | SpringDoc OpenAPI 2 / Swagger UI      |
| Build        | Maven 3.9                             |
| Testing      | JUnit 5, Mockito, MockMvc             |
| Email        | Spring Mail + MailHog (dev)           |
| Container    | Docker + Docker Compose               |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  HTTP Client / Swagger UI                               │
└──────────────────────┬──────────────────────────────────┘
                       │ REST (JSON)
┌──────────────────────▼──────────────────────────────────┐
│  Controllers  (UserController, ProductController,       │
│               OrderController)                          │
├─────────────────────────────────────────────────────────┤
│  Services     (UserService, ProductService,             │
│               OrderService, NotificationService)        │
│               ↕ @Async thread pool for notifications    │
├─────────────────────────────────────────────────────────┤
│  Repositories (Spring Data JPA)                         │
│               ↕ pessimistic locks on stock deduction    │
├──────────────┬──────────────────────────────────────────┤
│  PostgreSQL  │  Redis Cache (product catalog)           │
└──────────────┴──────────────────────────────────────────┘
```

### Key Design Choices

- **Pessimistic write locks** on `products` during order placement prevent overselling under concurrent load.
- **`@Async` thread pool** (`orderTaskExecutor`) sends email notifications without blocking the HTTP response.
- **Status transition guard** — only legal transitions are allowed (see table below).
- **Redis caching** with 10-minute TTL on the product catalog; cache is evicted on every mutation.
- **JWT stateless auth** — no server-side session.

#### Order Status Transitions

| From          | Allowed transitions              |
|---------------|----------------------------------|
| `PENDING`     | `PROCESSING`, `CANCELLED`        |
| `PROCESSING`  | `COMPLETED`, `CANCELLED`         |
| `COMPLETED`   | *(terminal)*                     |
| `CANCELLED`   | *(terminal)*                     |

---

## Setup & Run

### Prerequisites

- Java 17+
- Maven 3.9+ *(or use the project's `mvnw` wrapper once added)*
- Docker & Docker Compose (for Option A)
- PostgreSQL 16 + Redis 7 *(for Option B local run)*

---

### Option A – Docker Compose (recommended)

```bash
# Clone / copy the project
cd ecommerce-order-system

# Start all services (app + postgres + redis + mailhog)
docker compose up --build

# App is available at:
#   API     → http://localhost:8080
#   Swagger → http://localhost:8080/swagger-ui.html
#   MailHog → http://localhost:8025
```

To stop and remove containers:

```bash
docker compose down -v
```

---

### Option B – Local Development

1. **Start infrastructure**

```bash
# PostgreSQL
docker run -d --name pg \
  -e POSTGRES_DB=ecommerce_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16-alpine

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# MailHog (optional – for email preview)
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

2. **Configure environment** (create `.env` or export variables):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ecommerce_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=b2chat-ecommerce-very-secret-key-must-be-at-least-256-bits-long-for-hs256
export MAIL_HOST=localhost
export MAIL_PORT=1025
```

3. **Run the application**

```bash
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

---

## Environment Variables

| Variable               | Default                                                       | Description                     |
|------------------------|---------------------------------------------------------------|---------------------------------|
| `DB_URL`               | `jdbc:postgresql://localhost:5432/ecommerce_db`               | JDBC connection URL             |
| `DB_USERNAME`          | `postgres`                                                    | DB username                     |
| `DB_PASSWORD`          | `postgres`                                                    | DB password                     |
| `REDIS_HOST`           | `localhost`                                                   | Redis host                      |
| `REDIS_PORT`           | `6379`                                                        | Redis port                      |
| `JWT_SECRET`           | *(see application.yml)*                                       | HS256 signing secret (min 256b) |
| `JWT_EXPIRATION`       | `86400000`                                                    | Token TTL in milliseconds       |
| `MAIL_HOST`            | `smtp.gmail.com`                                              | SMTP host                       |
| `MAIL_PORT`            | `587`                                                         | SMTP port                       |
| `MAIL_USERNAME`        | `noreply@example.com`                                         | SMTP sender address             |
| `MAIL_PASSWORD`        | *(empty)*                                                     | SMTP password                   |
| `NOTIFICATION_ENABLED` | `true`                                                        | Toggle email notifications      |

---

## API Reference

> **Base URL**: `http://localhost:8080/api/v1`
> Interactive docs: **http://localhost:8080/swagger-ui.html**

### Authentication

Obtain a JWT token via `POST /auth/login`, then pass it as:

```
Authorization: Bearer <token>
```

---

### Authentication Endpoints

| Method | Path              | Auth | Description          |
|--------|-------------------|------|----------------------|
| `POST` | `/auth/login`     | No   | Login & receive JWT  |

### Users

| Method | Path                  | Auth | Description                        |
|--------|-----------------------|------|------------------------------------|
| `POST` | `/users`              | No   | Register a new user                |
| `GET`  | `/users/{id}`         | Yes  | Get user details by ID             |
| `GET`  | `/users/{id}/orders`  | Yes  | List all orders for a user (bonus) |

### Products

| Method   | Path             | Auth | Description              |
|----------|------------------|------|--------------------------|
| `POST`   | `/products`      | Yes  | Create a new product     |
| `GET`    | `/products`      | No   | List all products        |
| `GET`    | `/products/{id}` | No   | Get product by ID        |
| `PUT`    | `/products/{id}` | Yes  | Update product details   |
| `DELETE` | `/products/{id}` | Yes  | Delete a product         |

### Orders

| Method | Path                    | Auth | Description                          |
|--------|-------------------------|------|--------------------------------------|
| `POST` | `/orders`               | Yes  | Place a new order                    |
| `GET`  | `/orders/{id}`          | Yes  | Get order details by ID              |
| `PUT`  | `/orders/{id}/status`   | Yes  | Update order status                  |

---

## Sample Requests & Responses

### Register a User

```http
POST /api/v1/users
Content-Type: application/json

{
  "name": "Alice Doe",
  "email": "alice@example.com",
  "address": "123 Main St, Bogotá",
  "password": "secret123"
}
```

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "name": "Alice Doe",
    "email": "alice@example.com",
    "address": "123 Main St, Bogotá",
    "role": "USER",
    "createdAt": "2024-01-15T10:00:00"
  },
  "timestamp": "2024-01-15T10:00:00"
}
```

---

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "secret123"
}
```

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "alice@example.com",
    "role": "USER"
  }
}
```

---

### Create a Product

```http
POST /api/v1/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Wireless Keyboard",
  "description": "Compact Bluetooth keyboard",
  "price": 49.99,
  "stockQuantity": 100
}
```

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 1,
    "name": "Wireless Keyboard",
    "description": "Compact Bluetooth keyboard",
    "price": 49.99,
    "stockQuantity": 100,
    "createdAt": "2024-01-15T10:05:00",
    "updatedAt": "2024-01-15T10:05:00"
  }
}
```

---

### Place an Order

```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": 42,
    "userId": 1,
    "userName": "Alice Doe",
    "status": "PENDING",
    "totalAmount": 149.97,
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "Wireless Keyboard",
        "quantity": 2,
        "unitPrice": 49.99,
        "subtotal": 99.98
      },
      {
        "id": 2,
        "productId": 2,
        "productName": "Mouse Pad",
        "quantity": 1,
        "unitPrice": 49.99,
        "subtotal": 49.99
      }
    ],
    "createdAt": "2024-01-15T10:10:00",
    "updatedAt": "2024-01-15T10:10:00"
  }
}
```

---

### Update Order Status

```http
PUT /api/v1/orders/42/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "PROCESSING"
}
```

---

### Insufficient Stock Error (422)

```json
{
  "success": false,
  "message": "Insufficient stock for product 'Wireless Keyboard'. Requested: 5, Available: 2",
  "timestamp": "2024-01-15T10:12:00"
}
```

### Validation Error (400)

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email must be a valid address",
    "password": "Password must be at least 6 characters"
  }
}
```

---

## Running Tests

```bash
# All tests (unit + integration) — uses H2 in-memory DB
mvn test

# Unit tests only
mvn test -Dtest="*ServiceTest"

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# With coverage report (target/site/jacoco/index.html)
mvn verify
```

---

## Assumptions & Design Decisions

1. **Authentication scope**: All write operations and order retrieval require a valid JWT. Product listing (`GET /products`) is intentionally public to support storefront use cases without login.

2. **Async order processing**: The term "handle asynchronously" in the spec is interpreted as: (a) the order itself is created synchronously within the HTTP transaction to guarantee consistency; (b) the notification email is sent asynchronously via `@Async` so it never delays the response.

3. **Stock locking strategy**: Pessimistic write locks (`SELECT FOR UPDATE`) are applied at the repository level when deducting stock, avoiding race conditions under concurrent load without requiring optimistic retry logic in the service layer.

4. **Status transition rules**: Terminal statuses (`COMPLETED`, `CANCELLED`) cannot be changed. The allowed graph is: `PENDING → PROCESSING | CANCELLED`, `PROCESSING → COMPLETED | CANCELLED`.

5. **Price snapshot**: `unitPrice` is captured at order creation time. This means product price changes after an order is placed do not retroactively affect existing orders.

6. **Email notifications**: Uses Spring Mail. In development, MailHog captures all outgoing mail and displays it at `http://localhost:8025`. Notifications can be disabled via `NOTIFICATION_ENABLED=false` without affecting order logic.

7. **Redis failure tolerance**: The `@Profile("!test")` guard on `RedisConfig` means the test profile uses Spring's simple in-memory cache, keeping tests isolated from Redis infrastructure.

8. **User ownership of orders**: There is no strict ownership check — any authenticated user can view/update any order. For production, ownership verification or admin roles should be added via `@PreAuthorize`.

9. **Pagination**: `GET /users/{id}/orders` returns a paginated response (default 10 per page, sorted by `createdAt`).

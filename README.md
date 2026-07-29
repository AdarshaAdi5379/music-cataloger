# 🎵 Music Catalog Insights Platform

A production-ready full-stack web application for discovering, saving, and analysing your personal music library. Users can search the Apple iTunes catalogue, save albums, and gain insights into their collection.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [API Reference](#api-reference)
- [Environment Variables](#environment-variables)
- [Running Locally](#running-locally)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Security](#security)
- [Roadmap](#roadmap)

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | Next.js 16, TypeScript, Tailwind CSS, Turbopack |
| **Backend** | Spring Boot 3.4.2, Java 21, Maven |
| **Database** | PostgreSQL (production) · H2 in-memory (test / local dev) |
| **Authentication** | JWT (HS256), BCrypt, Spring Security 6 — stateless |
| **HTTP Client** | Spring WebClient (Reactor Netty) |
| **Caching** | Caffeine (in-memory, 10-min TTL) |
| **Build** | Maven Wrapper (`./mvnw`) · npm |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              Frontend (Next.js)              │
│  /app  /components  /features  /hooks  ...  │
└───────────────────┬─────────────────────────┘
                    │ HTTP / REST
┌───────────────────▼─────────────────────────┐
│             Backend (Spring Boot)            │
│                                             │
│  AuthController    SearchController          │
│       │                  │                  │
│  AuthService         SearchService           │
│       │            (Caffeine cache)          │
│  UserRepository      ItunesApiClient         │
│       │                  │                  │
│  PostgreSQL         iTunes Search API        │
│  (H2 in tests)      https://itunes.apple.com │
└─────────────────────────────────────────────┘
```

---

## Features

### ✅ Implemented
- **JWT Authentication** — stateless, BCrypt hashed passwords, no sessions
  - `POST /api/auth/register` — create account, returns JWT
  - `POST /api/auth/login` — authenticate, returns JWT
  - `GET /api/auth/me` — get current user profile (requires JWT)
- **iTunes Search Proxy** — server-side proxy prevents direct Apple API calls from the browser
  - `GET /api/search?query=&type=album&limit=` — proxied + cached
  - Response caching: Caffeine, 10-minute TTL, max 500 entries
  - Artwork URL upgraded from 100 × 100 to 600 × 600 automatically
- **Security hardening**
  - User enumeration protection (same error for bad email or bad password)
  - Internal errors never exposed to callers (502 for upstream failures)
  - All inputs validated server-side with Bean Validation
  - JWT secret enforced ≥ 32 characters at startup
  - No credentials logged anywhere

### 🔲 Planned
- Personal library CRUD (save / remove albums)
- Library insights dashboard
- Frontend auth UI (login / register forms)
- Frontend search UI (album cards, debounced search)
- Frontend library UI
- Flyway database migrations
- Docker Compose for local PostgreSQL
- GitHub Actions CI pipeline

---

## API Reference

All endpoints under `/api/**` (except `/api/auth/register` and `/api/auth/login`) require a valid `Authorization: Bearer <token>` header.

### Auth

#### `POST /api/auth/register`
Register a new account.

**Request body:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "SecurePass1!"
}
```
*Constraints: name ≤ 100 chars · email valid format ≤ 255 chars · password 8–100 chars*

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "jane@example.com",
  "name": "Jane Doe"
}
```

---

#### `POST /api/auth/login`
Authenticate with existing credentials.

**Request body:**
```json
{
  "email": "jane@example.com",
  "password": "SecurePass1!"
}
```

**Response `200 OK`:** same shape as register.

**Error responses:**
| Status | Meaning |
|---|---|
| `400` | Validation failed (missing / invalid fields) |
| `401` | Invalid credentials |
| `409` | Email already registered (register only) |

---

#### `GET /api/auth/me`
Returns the currently authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Response `200 OK`:** same shape as register / login (no password).

---

### Search

#### `GET /api/search`
Proxy search to the iTunes Search API.

**Query parameters:**

| Parameter | Type | Default | Constraints | Description |
|---|---|---|---|---|
| `query` | string | required | max 200 chars | Search term |
| `type` | string | `album` | `album` only | Entity type |
| `limit` | integer | `10` | 1–25 | Number of results |

**Example:**
```
GET /api/search?query=coldplay&type=album&limit=5
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response `200 OK`:**
```json
[
  {
    "appleCatalogId": "203562704",
    "title": "Parachutes",
    "artistName": "Coldplay",
    "genre": "Alternative",
    "releaseDate": "2000-07-10T07:00:00Z",
    "trackCount": 10,
    "artworkUrl": "https://is1-ssl.mzstatic.com/image/thumb/.../600x600bb.jpg"
  }
]
```

**Error responses:**
| Status | Meaning |
|---|---|
| `400` | Blank query · limit out of range · unsupported type |
| `403` | Missing or expired JWT |
| `502` | iTunes API unavailable or timed out |

---

## Environment Variables

### Backend — required for production

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | ✅ | — | HS256 signing secret **≥ 32 characters** |
| `JWT_EXPIRATION_MS` | No | `86400000` (24 h) | Token TTL in milliseconds |
| `DB_URL` | ✅ | `jdbc:postgresql://localhost:5432/music_catalog` | PostgreSQL JDBC URL |
| `DB_USERNAME` | ✅ | `postgres` | Database username |
| `DB_PASSWORD` | ✅ | `postgres` | Database password |
| `ITUNES_BASE_URL` | No | `https://itunes.apple.com` | Override for testing/staging |
| `ITUNES_TIMEOUT_MS` | No | `5000` | iTunes API request timeout (ms) |

> ⚠️ **Never commit secrets.** Always set `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` via environment variables or a secrets manager.

---

## Running Locally

### Prerequisites
- Java 21+
- Node.js 20+
- Maven (wrapper included — `./mvnw`)
- PostgreSQL (optional — use `local` profile for H2 in-memory)

### Backend — with H2 (no PostgreSQL required)

```bash
cd backend

JWT_SECRET="your-secret-at-least-32-characters-long" \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on **http://localhost:8080**.  
H2 console available at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:localdev`).

### Backend — with PostgreSQL

```bash
cd backend

export JWT_SECRET="your-secret-at-least-32-characters-long"
export DB_URL="jdbc:postgresql://localhost:5432/music_catalog"
export DB_USERNAME="postgres"
export DB_PASSWORD="yourpassword"

./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev   # starts on http://localhost:3000
```

### Quick API test

```bash
# 1. Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"SecurePass1!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Search iTunes
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/search?query=coldplay&type=album&limit=3"
```

---

## Running Tests

```bash
cd backend
./mvnw clean verify
```

```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

Test suite covers:
- Auth — registration (valid, duplicate, bad input), login (valid, wrong password, unknown email, bad input), `/me` (valid token, no token, expired token, tampered token)
- Search — valid query, empty results, invalid type, special characters, blank query, limit validation, network failure (502), unauthenticated access (403)
- iTunes HTTP client — happy path, empty results, connection reset, HTTP 500, malformed JSON, timeout

---

## Project Structure

```
musicCataloger/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/musiccataloger/backend/
│   │   │   │   ├── client/
│   │   │   │   │   └── ItunesApiClient.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── CacheConfig.java
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   └── WebClientConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── SearchController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ApiErrorResponse.java
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── AuthResponse.java
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   └── RegisterRequest.java
│   │   │   │   │   └── search/
│   │   │   │   │       ├── AlbumDto.java
│   │   │   │   │       ├── ItunesResultItem.java
│   │   │   │   │       └── ItunesSearchResponse.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── LibraryItem.java
│   │   │   │   │   └── User.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── AuthException.java
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   └── ItunesApiException.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── LibraryItemRepository.java
│   │   │   │   │   └── UserRepository.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │   └── service/
│   │   │   │       ├── AuthService.java
│   │   │   │       └── SearchService.java
│   │   │   └── resources/
│   │   │       ├── application.properties          # PostgreSQL + JWT
│   │   │       └── application-local.properties    # H2 local dev
│   │   └── test/
│   │       ├── java/com/musiccataloger/backend/
│   │       │   ├── BackendApplicationTests.java
│   │       │   ├── auth/
│   │       │   │   └── AuthControllerTest.java     # 13 tests
│   │       │   └── search/
│   │       │       ├── ItunesApiClientTest.java    # 5 tests
│   │       │       └── SearchControllerTest.java   # 8 tests
│   │       └── resources/
│   │           └── application-test.properties     # H2 test profile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/          # Next.js App Router pages
│   │   ├── components/   # Shared UI components
│   │   ├── features/     # Feature modules (auth, library, search)
│   │   ├── hooks/        # Custom React hooks
│   │   ├── lib/          # Shared utilities / API clients
│   │   ├── services/     # API service layer
│   │   ├── store/        # State management (Zustand / Redux)
│   │   ├── styles/       # Global CSS / design tokens
│   │   ├── types/        # TypeScript type definitions
│   │   └── utils/        # Pure utility functions
│   └── package.json
│
├── .gitignore
├── README.md
└── session.md   ← local only, not tracked by git
```

---

## Security

- **No sessions** — `SessionCreationPolicy.STATELESS`
- **CSRF disabled** — REST API with JWT; no cookies
- **BCrypt cost factor 12** — future-proof password hashing
- **JWT secret validation** — startup fails if secret < 32 chars
- **User enumeration protection** — identical error for bad email vs bad password
- **Credential logging prevention** — service logs user IDs (UUIDs), never emails or passwords
- **Internal error masking** — iTunes API failures return generic 502 message
- **Input validation** — Bean Validation on all DTOs + `@Validated` on `@RequestParam`
- **JWT scope** — tokens are signed HS256; tampered or expired tokens return 403

---

## Roadmap

| Phase | Feature | Status |
|---|---|---|
| 1 | Project setup & architecture | ✅ Done |
| 1 | JPA entities (User, LibraryItem) | ✅ Done |
| 1 | Repository layer | ✅ Done |
| 2 | JWT authentication | ✅ Done |
| 2 | iTunes Search API proxy + caching | ✅ Done |
| 3 | Library CRUD (service + controller) | 🔲 Next |
| 3 | DTOs + mappers for library | 🔲 Next |
| 4 | Frontend auth UI | 🔲 Planned |
| 4 | Frontend search UI | 🔲 Planned |
| 4 | Frontend library UI | 🔲 Planned |
| 5 | Flyway migrations | 🔲 Planned |
| 5 | Docker Compose | 🔲 Planned |
| 5 | GitHub Actions CI | 🔲 Planned |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages
4. Run `./mvnw clean verify` before pushing — all 28 tests must pass
5. Open a pull request

---

## License

MIT

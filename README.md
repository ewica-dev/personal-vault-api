# Personal Vault API

A Spring Boot REST API for personal note management with authentication.

## Technology Stack

- **Java** 21
- **Spring Boot** 4.0.5
- **PostgreSQL** 16.3
- **Gradle** (build tool)
- **Docker/Docker Compose** (containerization)

### Key Dependencies

- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Web MVC
- Spring Boot Starter Mail
- PostgreSQL driver
- jjwt (JWT token handling)
- Stripe Java client
- Lombok

## Prerequisites

- Java 21
- Docker and Docker Compose
- (Optional) Gradle wrapper included

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5433/personal_vault_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | (required) |
| `JWT_SECRET` | JWT signing secret | (required) |
| `JWT_EXPIRATION_MS` | JWT token expiration in milliseconds | `900000` |
| `MAIL_HOST` | SMTP mail host | `smtp.mailtrap.io` |
| `MAIL_PORT` | SMTP mail port | `2525` |
| `MAIL_USERNAME` | SMTP username | (required) |
| `MAIL_PASSWORD` | SMTP password | (required) |
| `MAIL_FROM` | From address for emails | `noreply@personalvault.local` |

**Note**: The `.env` file contains default development values. Copy `application.properties.example` and configure appropriately for production.

## Configuration Files

- `src/main/resources/application.properties` - Main configuration
- `src/main/resources/application-local.properties` - Local development profile overrides
- `src/main/resources/application.properties.example` - Template for new configurations
- `.env` - Default development environment variables

The active Spring profile is set to `local` by default in `application.properties`.

## Running with Docker

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

This starts a PostgreSQL container on port 5433 with the database name, user, and password from the `.env` file.

### 2. Run the Application

Set the environment variables and run:

```bash
# On Windows (PowerShell)
$env:DB_PASSWORD="postgres"; $env:JWT_SECRET="your-secret"; ./gradlew bootRun

# On Linux/Mac
DB_PASSWORD=postgres JWT_SECRET=your-secret ./gradlew bootRun
```

Or build and run the JAR:

```bash
./gradlew build
java -jar build/libs/personal-vault-api-0.0.1-SNAPSHOT.jar
```

**Note**: No Dockerfile is provided in this repository. Docker deployment would need to be added separately.

## Running Locally (without Docker)

1. Start PostgreSQL locally on port 5433
2. Create a database named `personal_vault_db`
3. Configure credentials in `application-local.properties` or environment variables
4. Run:

```bash
./gradlew bootRun
```

The application runs on `http://localhost:8080`.

## API Endpoints

### Public Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/signup` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT token |
| GET | `/api/health` | Health check |

### Protected Endpoints

All other endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer <token>
```

## Testing

Run all tests:

```bash
./gradlew test
```

### Test Suites

- `PersonalVaultApiApplicationTests` - Spring context loading test
- `AuthServiceTest` - Authentication service unit tests (register, login)
- `NoteServiceTest` - Note service unit tests (CRUD operations, text normalization)
- `TextUtilTest` - Text utility unit tests (title normalization, content normalization)

### Test Results

Test results are generated in `build/test-results/test/`.

## Project Structure

```
personal-vault-api/
├── src/
│   ├── main/
│   │   ├── java/com/ewicadev/personalvaultapi/
│   │   │   ├── config/         # Security, JWT, authentication config
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/            # Data transfer objects
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── exception/     # Exception handling
│   │   │   ├── health/        # Health endpoints
│   │   │   ├── repository/     # Data repositories
│   │   │   ├── service/       # Business logic
│   │   │   ├── util/          # Utilities
│   │   │   └── validation/    # Custom validation
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       └── application.properties.example
│   └── test/
│       └── java/.../          # Test classes
├── build.gradle
├── docker-compose.yml
├── gradlew
└── .env
```

## Notes

- The application uses JWT tokens for authentication
- Password encoding uses BCrypt
- Hibernate DDL auto is set to `update`
- Security is stateless (no session)
- CSRF protection is disabled
- HTML tags and special characters in notes are preserved (XSS prevention is handled elsewhere)

## Missing/Incomplete Items

- No Dockerfile provided for containerizing the application
- No production Docker Compose configuration (only PostgreSQL)
- Mail configuration requires external SMTP service
- No API documentation endpoint (e.g., Swagger/OpenAPI)
- No database migration tool (Flyway/Liquibase)

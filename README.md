# Personal Vault API

A Spring Boot REST API for personal note management with JWT-based authentication.

## Purpose

This project was created as a **portfolio project** to practice and demonstrate skills in:
- Building RESTful APIs with Spring Boot
- JWT authentication and authorization
- PostgreSQL database integration with JPA/Hibernate
- Secure password handling with BCrypt
- Docker containerization
- Test-driven development with JUnit 5

It serves as a practical demonstration of backend development skills including API design, security implementation, database modelling, and deployment configuration.

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
| `MAIL_HOST` | SMTP mail host | `smtp-relay.brevo.com` |
| `MAIL_PORT` | SMTP mail port | `587` |
| `MAIL_USERNAME` | SMTP username | (required) |
| `MAIL_PASSWORD` | SMTP password | (required) |
| `MAIL_FROM` | From address for emails | (required) |

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

### Public Endpoints (No Authentication Required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/signup` | Register new user (sends verification email) |
| POST | `/api/v1/auth/login` | Login and get JWT token (requires verified email) |
| POST | `/api/v1/auth/verify-email` | Verify email with OTP |
| POST | `/api/v1/auth/resend-verification` | Resend verification email |
| GET | `/api/health` | Health check |

### Protected Endpoints

All other endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer <token>
```

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/notes` | Create a new note |
| GET | `/api/v1/notes` | Get all notes (paginated) |
| GET | `/api/v1/notes/{id}` | Get a specific note |
| PUT | `/api/v1/notes/{id}` | Update a note |
| DELETE | `/api/v1/notes/{id}` | Delete a note |
| GET | `/api/v1/admin/test` | Admin test endpoint |

## Testing

Run all tests:

```bash
./gradlew test
```

### Test Suites

- `PersonalVaultApiApplicationTests` - Spring context loading test
- `AuthServiceTest` - Authentication service unit tests (register, login, verify, resend)
- `AuthControllerTest` - Authentication controller tests
- `NoteServiceTest` - Note service unit tests (CRUD operations, text normalization)
- `NoteControllerTest` - Note controller tests
- `AdminControllerTest` - Admin controller tests
- `RateLimitServiceTest` - Rate limiting tests
- `OtpGeneratorTest` - OTP generation tests
- `OtpHasherTest` - OTP hashing tests
- `PasswordValidatorTest` - Password validation tests

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

## Email OTP Verification

This project implements email OTP verification for user signup using Brevo SMTP.

### Verification Flow

1. **User Signup**: User submits email and password
   - User is created with `emailVerified = false`
   - 6-digit OTP is generated and sent to user email
   - User cannot login until email is verified

2. **Verify Email**: User submits OTP to verify their email
   - OTP is validated (6-digit, expires in 10 minutes, max 5 attempts)
   - On success: `emailVerified = true`, user can login

3. **Resend Verification**: User can request a new OTP
   - 60-second cooldown between requests
   - Rate limited to prevent abuse

### Security Features

- **Password Validation**: 8-50 characters with uppercase, lowercase, digit, and special character
- **BCrypt Hashing**: Passwords and OTPs are hashed with BCrypt (work factor 10)
- **Rate Limiting**: Per-IP and per-email limits using Caffeine cache
- **Account Enumeration Protection**: Generic responses prevent email enumeration
- **Timing Attack Mitigation**: Dummy BCrypt work normalizes response times
- **Transaction Safety**: Emails sent only after DB transaction commits

### Login Requirements

- Users must verify their email before they can login
- Unverified users receive HTTP 403 Forbidden with message "Email not verified"

## Notes

- The application uses JWT tokens for authentication
- Password encoding uses BCrypt
- OTP hashing uses BCrypt with work factor 10
- Hibernate DDL auto is set to `update`
- Security is stateless (no session)
- CSRF protection is disabled
- HTML tags and special characters in notes are preserved (XSS prevention is handled elsewhere)

## Missing/Incomplete Items

- No Dockerfile provided for containerizing the application
- No production Docker Compose configuration (only PostgreSQL)
- No API documentation endpoint (e.g., Swagger/OpenAPI)
- No database migration tool (Flyway/Liquibase)
- In-memory rate limiting (Redis recommended for production)

# Supabase Spring Boot Example Service

This is a complete Spring Boot application demonstrating integration with Supabase using the `supabase-java-client` library.

## Features

- **Authentication Endpoints**: Sign up, sign in, refresh tokens, and logout
- **Database Operations**: Full CRUD operations on a `cities` table with access control via JWT tokens
- **Spring Boot Integration**: Properly configured Supabase client as a Spring Bean
- **Request/Response DTOs**: Clean API contracts with proper JSON serialization

## Prerequisites

1. Java 17+
2. Maven 3.6+
3. A Supabase project (https://supabase.com)
4. Supabase project URL and anon key

## Configuration

Update `src/main/resources/application.properties` with your Supabase credentials:

```properties
supabase.url=https://your-project.supabase.co
supabase.anon-key=your-anon-key
```

## Database Setup

Before using the database endpoints, you need to create a `cities` table in your Supabase project:

```sql
CREATE TABLE cities (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  name VARCHAR(255) NOT NULL,
  country VARCHAR(255) NOT NULL,
  population INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enable RLS (Row Level Security) if needed
ALTER TABLE cities ENABLE ROW LEVEL SECURITY;
```

## Building and Running

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication Endpoints (`/api/auth`)

#### 1. Sign Up
Create a new user account

**Request:**
```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (201 Created):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "qwerty123...",
  "expires_in": 3600,
  "token_type": "bearer",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "role": "authenticated"
  }
}
```

#### 2. Sign In
Log in with existing credentials

**Request:**
```http
POST /api/auth/signin
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "qwerty123...",
  "expires_in": 3600,
  "token_type": "bearer",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "role": "authenticated"
  }
}
```

#### 3. Refresh Token
Get a new access token using a refresh token

**Request:**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "qwerty123..."
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "qwerty123...",
  "expires_in": 3600,
  "token_type": "bearer",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "role": "authenticated"
  }
}
```

#### 4. Logout
Sign out a user (invalidates the session)

**Request:**
```http
POST /api/auth/logout
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (204 No Content):**
```
(Empty response)
```

### Database Endpoints (`/api/cities`)

All database endpoints require an `Authorization` header with a Bearer token (from authentication):

```http
Authorization: Bearer <your-access-token>
```

#### 1. Get All Cities
Retrieve all cities

**Request:**
```http
GET /api/cities
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "New York",
    "country": "USA",
    "population": 8000000,
    "created_at": "2024-01-01T10:00:00Z"
  },
  {
    "id": 2,
    "name": "London",
    "country": "UK",
    "population": 9000000,
    "created_at": "2024-01-02T10:00:00Z"
  }
]
```

#### 2. Get City by ID
Retrieve a specific city

**Request:**
```http
GET /api/cities/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "New York",
  "country": "USA",
  "population": 8000000,
  "created_at": "2024-01-01T10:00:00Z"
}
```

#### 3. Search Cities by Country
Find all cities in a specific country

**Request:**
```http
GET /api/cities/search/country/USA
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "New York",
    "country": "USA",
    "population": 8000000,
    "created_at": "2024-01-01T10:00:00Z"
  }
]
```

#### 4. Create City
Add a new city

**Request:**
```http
POST /api/cities
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "name": "Paris",
  "country": "France",
  "population": 2200000
}
```

**Response (201 Created):**
```json
{
  "id": 3,
  "name": "Paris",
  "country": "France",
  "population": 2200000,
  "created_at": "2024-01-03T10:00:00Z"
}
```

#### 5. Update City
Modify an existing city

**Request:**
```http
PUT /api/cities/3
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "name": "Paris",
  "country": "France",
  "population": 2250000
}
```

**Response (200 OK):**
```json
{
  "id": 3,
  "name": "Paris",
  "country": "France",
  "population": 2250000,
  "created_at": "2024-01-03T10:00:00Z"
}
```

#### 6. Delete City
Remove a city

**Request:**
```http
DELETE /api/cities/3
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (204 No Content):**
```
(Empty response)
```

## Testing with cURL

### Example: Complete flow

1. **Sign Up:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

2. **Create a City (using the access token from signup):**
```bash
curl -X POST http://localhost:8080/api/cities \
  -H "Authorization: Bearer <your-access-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Tokyo","country":"Japan","population":14000000}'
```

3. **Get All Cities:**
```bash
curl -X GET http://localhost:8080/api/cities \
  -H "Authorization: Bearer <your-access-token>"
```

4. **Update a City:**
```bash
curl -X PUT http://localhost:8080/api/cities/1 \
  -H "Authorization: Bearer <your-access-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Tokyo","country":"Japan","population":14500000}'
```

5. **Delete a City:**
```bash
curl -X DELETE http://localhost:8080/api/cities/1 \
  -H "Authorization: Bearer <your-access-token>"
```

6. **Logout:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"<your-access-token>"}'
```

## Project Structure

```
src/main/java/com/supabase_demo/example_service/
├── ExampleServiceApplication.java      # Spring Boot application entry point
├── config/
│   ├── SupabaseConfiguration.java      # Supabase client configuration
│   └── JacksonConfiguration.java       # Jackson/JSON configuration
├── controller/
│   ├── AuthController.java             # Authentication endpoints
│   └── CitiesController.java           # Database CRUD endpoints
├── dto/
│   ├── AuthRequest.java                # Sign up/in request
│   ├── AuthResponse.java               # Authentication response
│   ├── LogoutRequest.java              # Logout request
│   └── RefreshTokenRequest.java        # Token refresh request
└── model/
    └── City.java                       # City entity model
```

## Key Components

### SupabaseConfiguration
Provides a singleton `SupabaseClient` bean that's used throughout the application. The client is initialized with the project URL and anon key from application properties.

### AuthController
Handles user authentication:
- Sign up: Creates new users
- Sign in: Authenticates users and returns tokens
- Refresh: Exchanges refresh token for new access token
- Logout: Invalidates user session

### CitiesController
Demonstrates database operations with authentication:
- All endpoints require a valid JWT access token
- Extracts the token from the `Authorization: Bearer <token>` header
- Creates a new authenticated `SupabaseClient` instance for each request
- Supports filtering, pagination (via range headers), and CRUD operations

## Important Notes

1. **Access Control**: The database endpoints require a valid Supabase access token. For proper security, consider implementing Row Level Security (RLS) policies in your Supabase project.

2. **Token Expiry**: Access tokens expire after a certain time. Use the refresh token endpoint to get a new access token.

3. **Error Handling**: The controllers catch exceptions and return appropriate HTTP status codes:
   - 401 Unauthorized: Invalid or missing token
   - 400 Bad Request: Invalid data
   - 404 Not Found: Resource doesn't exist
   - 201 Created: Resource successfully created
   - 204 No Content: Successful deletion

4. **Jackson Configuration**: The application is configured to use snake_case naming for JSON properties, which is standard for SQL databases.

## Further Customization

You can extend this example by:
- Adding more complex filters and queries using PostgrestQueryBuilder methods
- Implementing custom error handling and validation
- Adding request logging and metrics
- Integrating with other Supabase features (storage, realtime, etc.)
- Implementing Row Level Security (RLS) for fine-grained access control


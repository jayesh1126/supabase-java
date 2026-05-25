# Quick Start Guide - Supabase Spring Boot Application

## 5-Minute Setup

### Step 1: Configure Supabase Credentials
Edit `src/main/resources/application.properties`:
```properties
supabase.url=https://your-project.supabase.co
supabase.anon-key=your-anon-key
```

Get these values from your [Supabase Dashboard](https://supabase.com/dashboard)

### Step 2: Create Database Table
In Supabase SQL Editor, run:
```sql
CREATE TABLE cities (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  name VARCHAR(255) NOT NULL,
  country VARCHAR(255) NOT NULL,
  population INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Step 3: Build & Run
```bash
cd example-service
mvn clean install
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

## Quick Test

### 1. Create an Account
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

Copy the `access_token` from the response.

### 2. Add a City
Replace `YOUR_TOKEN` with the access_token:
```bash
curl -X POST http://localhost:8080/api/cities \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Paris","country":"France","population":2200000}'
```

### 3. Get All Cities
```bash
curl -X GET http://localhost:8080/api/cities \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Update a City
```bash
curl -X PUT http://localhost:8080/api/cities/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Paris","country":"France","population":2250000}'
```

### 5. Delete a City
```bash
curl -X DELETE http://localhost:8080/api/cities/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## API Endpoints Overview

### Authentication (`/api/auth`)
- `POST /signup` - Create new account
- `POST /signin` - Login with email & password
- `POST /refresh` - Get new access token
- `POST /logout` - Sign out

### Database (`/api/cities`)
- `GET /` - List all cities
- `GET /{id}` - Get specific city
- `GET /search/country/{country}` - Filter by country
- `POST /` - Create city
- `PUT /{id}` - Update city
- `DELETE /{id}` - Delete city

**All database endpoints require**: `Authorization: Bearer <token>` header

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────────────┐
│  Spring Boot Application                │
│  ┌─────────────────────────────────────┐│
│  │ AuthController  | CitiesController  ││
│  └────────────┬────────────────────────┘│
│               │                          │
│  ┌────────────▼──────────────────────┐  │
│  │  SupabaseClient Bean              │  │
│  │ (Configured with URL & API Key)   │  │
│  └────────────┬──────────────────────┘  │
│               │                          │
│  ┌────────────▼──────────────────────┐  │
│  │ - AuthClient                      │  │
│  │ - PostgrestClient                 │  │
│  └────────────┬──────────────────────┘  │
└───────────────┼──────────────────────────┘
                │
                ▼
        ┌───────────────┐
        │  Supabase     │
        │  - Auth       │
        │  - Database   │
        └───────────────┘
```

## What's Included

### Controllers (Endpoints)
- **AuthController** - User authentication (signup, signin, refresh, logout)
- **CitiesController** - CRUD operations on cities table

### Models & DTOs
- **City** - Database entity
- **AuthRequest/Response** - Authentication payloads
- **RefreshTokenRequest** - Token refresh payload
- **LogoutRequest** - Logout payload

### Configuration
- **SupabaseConfiguration** - Provides SupabaseClient bean
- **JacksonConfiguration** - JSON serialization settings
- **application.properties** - Supabase credentials

## File Structure
```
example-service/
├── src/main/java/com/supabase_demo/example_service/
│   ├── ExampleServiceApplication.java
│   ├── config/
│   │   ├── SupabaseConfiguration.java
│   │   └── JacksonConfiguration.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── CitiesController.java
│   ├── dto/
│   │   ├── AuthRequest.java
│   │   ├── AuthResponse.java
│   │   ├── RefreshTokenRequest.java
│   │   └── LogoutRequest.java
│   └── model/
│       └── City.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
├── README_API.md (Detailed API documentation)
└── IMPLEMENTATION_SUMMARY.md (Implementation details)
```

## Common Tasks

### Add a New Database Table
1. Create table in Supabase
2. Create model class (e.g., `Restaurant.java`)
3. Create controller with endpoints
4. Use `supabaseClient.postgrest().from("table_name")`

### Enable Row Level Security
In Supabase:
```sql
ALTER TABLE cities ENABLE ROW LEVEL SECURITY;

-- Allow users to see only their own data
CREATE POLICY "Users can view all cities" ON cities
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can insert cities" ON cities
  FOR INSERT TO authenticated
  WITH CHECK (true);
```

### Add More Fields to City
1. Alter table in Supabase
2. Add field to `City.java` model
3. Controller automatically handles it

## Troubleshooting

### "Invalid or missing Authorization header"
- Make sure to include `Authorization: Bearer <token>` in request headers
- Token must be from a successful signin/signup

### "Unable to connect to Supabase"
- Check URL is correct (ends with `.supabase.co`)
- Check API key is correct
- Verify network connectivity

### "Table 'cities' does not exist"
- Run the CREATE TABLE SQL in Supabase SQL Editor
- Wait a moment for table to be created
- Restart the application

### "Java version not supported"
- Application requires Java 17+
- Check version: `java -version`

## Next Steps

1. **Read Full Documentation**: See `README_API.md` for complete API details
2. **Add Authentication**: Implement user registration verification
3. **Add Database Features**: Create more tables and CRUD operations
4. **Add Validation**: Implement Spring validation annotations
5. **Add Tests**: Create unit and integration tests
6. **Deploy**: Deploy to a cloud provider (AWS, Heroku, DigitalOcean, etc.)

## Support

For issues:
1. Check Supabase documentation: https://supabase.com/docs
2. Check Spring Boot docs: https://spring.io/projects/spring-boot
3. Check library documentation in `/supabase-java-client` directory

## Technology Stack
- Java 17
- Spring Boot 4.0.6
- Supabase Java Client 0.1.0
- Jackson (JSON)
- Lombok
- Maven

---

**Happy coding!** 🚀


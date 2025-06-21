# Heroes in Waiting - Backend API

A Node.js/Express REST API for the Heroes in Waiting anti-bullying curriculum mobile application.

## Features

- **JWT Authentication** for facilitators
- **Classroom code system** for student access (COPPA compliant)
- **Lesson content delivery** with progress tracking
- **Analytics and reporting** dashboard
- **Comprehensive security** with rate limiting and input validation
- **RESTful API design** with OpenAPI documentation
- **PostgreSQL database** with optimized schema
- **Docker containerization** for easy deployment

## Quick Start

### Prerequisites

- Node.js 18+ 
- PostgreSQL 12+
- Docker & Docker Compose (optional)

### Local Development Setup

1. **Clone and install dependencies:**
```bash
git clone <repository-url>
cd heroes_in_waiting
npm install
```

2. **Set up environment variables:**
```bash
cp .env.example .env
# Edit .env with your database credentials and JWT secret
```

3. **Start PostgreSQL and create database:**
```bash
# Using Docker:
docker run --name heroes-postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15

# Or install PostgreSQL locally and create database:
createdb heroes_in_waiting
```

4. **Run database migrations and seeds:**
```bash
npm run migrate
npm run seed
```

5. **Start the development server:**
```bash
npm run dev
```

The API will be available at `http://localhost:3000`

### Docker Development Setup

1. **Start all services:**
```bash
docker-compose up -d
```

2. **Run migrations in the container:**
```bash
docker-compose exec api npm run migrate
docker-compose exec api npm run seed
```

The API will be available at `http://localhost:3000`

## API Documentation

When the server is running with `API_DOCS_ENABLED=true`, visit:
- **Swagger UI:** `http://localhost:3000/api-docs`
- **OpenAPI Spec:** `http://localhost:3000/api-docs.json`

## Core API Endpoints

### Authentication
- `POST /api/auth/register` - Register new facilitator
- `POST /api/auth/login` - Facilitator login
- `GET /api/auth/me` - Get current facilitator profile
- `PUT /api/auth/profile` - Update facilitator profile
- `PUT /api/auth/change-password` - Change password

### Classrooms
- `GET /api/classrooms` - Get facilitator's classrooms
- `POST /api/classrooms` - Create new classroom
- `GET /api/classrooms/:id` - Get classroom details
- `PUT /api/classrooms/:id` - Update classroom
- `POST /api/classrooms/:id/regenerate-code` - Regenerate classroom code
- `DELETE /api/classrooms/:id` - Delete classroom

### Students
- `POST /api/students/enroll` - Enroll student with classroom code
- `GET /api/students/profile` - Get student profile (student auth)
- `GET /api/students/classroom/:classroomId` - Get classroom students (facilitator)
- `PUT /api/students/:studentId/status` - Update student status
- `GET /api/students/classroom/:classroomId/analytics` - Classroom demographics

### Lessons
- `GET /api/lessons` - Get all published lessons
- `GET /api/lessons/:id` - Get lesson content
- `GET /api/lessons/:id/progress` - Get student lesson progress
- `GET /api/lessons/classroom/:classroomId/progress` - Classroom lesson progress

### Progress & Feedback
- `PUT /api/progress/lesson` - Update lesson progress (student)
- `GET /api/progress/summary` - Get student progress summary
- `POST /api/progress/feedback` - Submit student feedback
- `GET /api/progress/classroom/:classroomId/lesson/:lessonId` - Lesson progress details
- `GET /api/progress/classroom/:classroomId/lesson/:lessonId/feedback` - Lesson feedback

### Analytics
- `POST /api/analytics/event` - Track analytics event
- `GET /api/analytics/facilitator/overview` - Facilitator overview dashboard
- `GET /api/analytics/classroom/:classroomId/dashboard` - Classroom analytics
- `GET /api/analytics/classroom/:classroomId/export` - Export classroom data

## Authentication

### Facilitator Authentication
Use JWT Bearer tokens in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Student Authentication
Use classroom code and student ID in headers:
```
classroomcode: ABC123
studentid: student_abc12345
```

## Database Schema

The application uses PostgreSQL with the following main tables:

- **facilitators** - Facilitator accounts and profiles
- **classrooms** - Classroom management and codes
- **students** - Anonymous student records (COPPA compliant)
- **lessons** - Curriculum content and structure
- **student_progress** - Individual lesson progress tracking
- **student_feedback** - Anonymous feedback and responses
- **classroom_sessions** - Live session management
- **analytics_events** - Detailed user interaction tracking

## Security Features

- **JWT Authentication** with secure token generation
- **Rate Limiting** (100 requests per 15 minutes by default)
- **Input Validation** using Joi and express-validator
- **SQL Injection Protection** via parameterized queries
- **CORS Configuration** for cross-origin requests
- **Helmet.js** for security headers
- **COPPA Compliance** - no PII collection from students
- **Anonymous Student IDs** for privacy protection

## Environment Configuration

Key environment variables:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=heroes_in_waiting
DB_USER=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_super_secret_key
JWT_EXPIRES_IN=24h

# Server
PORT=3000
NODE_ENV=development
CORS_ORIGIN=http://localhost:3000

# Security
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100
BCRYPT_SALT_ROUNDS=12

# Features
API_DOCS_ENABLED=true
LOG_LEVEL=info
```

## Available Scripts

```bash
npm start          # Start production server
npm run dev        # Start development server with nodemon
npm test           # Run test suite
npm run migrate    # Run database migrations
npm run seed       # Run database seeds
npm run lint       # Lint code with ESLint
npm run lint:fix   # Fix linting issues
```

## Production Deployment

### Using Docker

1. **Build and deploy:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

2. **Run migrations:**
```bash
docker-compose exec api npm run migrate
```

### Manual Deployment

1. **Set production environment variables**
2. **Install dependencies:** `npm ci --only=production`
3. **Run migrations:** `npm run migrate`
4. **Start with PM2:** `pm2 start src/app.js --name heroes-api`

## Monitoring and Logging

- **Winston logging** with rotation and levels
- **Health check endpoint:** `GET /health`
- **Process monitoring** with PM2 (recommended)
- **Database connection monitoring**
- **Error tracking** and reporting

## API Rate Limits

- **Default:** 100 requests per 15 minutes per IP
- **Configurable** via environment variables
- **Different limits** can be set per endpoint type

## Data Privacy & COPPA Compliance

- **No PII collection** from students
- **Anonymous student identifiers** only
- **Demographic data** collected for analytics (optional)
- **Secure data handling** and encryption
- **GDPR-ready** data export functionality

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes and add tests
4. Run linting: `npm run lint:fix`
5. Run tests: `npm test`
6. Commit changes: `git commit -am 'Add new feature'`
7. Push to branch: `git push origin feature/new-feature`
8. Create a Pull Request

## Support

For technical support or questions about the Heroes in Waiting API:

- **Documentation:** [API Documentation](http://localhost:3000/api-docs)
- **Issues:** Create a GitHub issue
- **Email:** support@heroesinwaiting.org

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Heroes in Waiting** - Building stronger, kinder communities through education and technology.
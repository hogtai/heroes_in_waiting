# DBA Remediation Plan
**Date:** December 2024  
**Priority:** High - Must complete before Checkpoint 5  
**Status:** In Progress

## Overview
This plan addresses all database performance, security, and optimization issues identified in the DBA review before proceeding to Checkpoint 5.

## Critical Database Issues to Address

### 1. Connection Pooling Implementation
**Priority:** Critical  
**Status:** In Progress

#### 1.1 PgBouncer Configuration
**File:** `docker-compose.yml`
```yaml
version: '3.8'
services:
  # ... existing services ...
  
  pgbouncer:
    image: edoburu/pgbouncer:1.18.0
    container_name: heroes_pgbouncer
    environment:
      - DB_HOST=postgres
      - DB_USER=heroes_user
      - DB_PASSWORD=${DB_PASSWORD}
      - DB_NAME=heroes_in_waiting
      - POOL_MODE=transaction
      - MAX_CLIENT_CONN=1000
      - DEFAULT_POOL_SIZE=20
      - RESERVE_POOL_SIZE=5
      - RESERVE_POOL_TIMEOUT=5
      - MAX_DB_CONNECTIONS=50
      - MAX_USER_CONNECTIONS=100
    ports:
      - "6432:5432"
    depends_on:
      - postgres
    networks:
      - heroes_network
    restart: unless-stopped
```

#### 1.2 Update Database Configuration
**File:** `src/config/database.js`
```javascript
const knex = require('knex');
const logger = require('../utils/logger');

const dbConfig = {
  client: 'postgresql',
  connection: {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    user: process.env.DB_USER || 'heroes_user',
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME || 'heroes_in_waiting',
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
  },
  pool: {
    min: parseInt(process.env.DB_POOL_MIN) || 2,
    max: parseInt(process.env.DB_POOL_MAX) || 10,
    acquireTimeoutMillis: 30000,
    createTimeoutMillis: 30000,
    destroyTimeoutMillis: 5000,
    idleTimeoutMillis: 30000,
    reapIntervalMillis: 1000,
    createRetryIntervalMillis: 200
  },
  migrations: {
    directory: '../database/migrations'
  },
  seeds: {
    directory: '../database/seeds'
  },
  debug: process.env.NODE_ENV === 'development'
};

const db = knex(dbConfig);

// Connection pool monitoring
db.on('query', (query) => {
  if (process.env.NODE_ENV === 'development') {
    logger.debug('Database query:', {
      sql: query.sql,
      bindings: query.bindings,
      duration: query.duration
    });
  }
});

db.on('query-error', (error, query) => {
  logger.error('Database query error:', {
    error: error.message,
    sql: query.sql,
    bindings: query.bindings
  });
});

module.exports = db;
```

### 2. Database Index Optimization
**Priority:** High  
**Status:** Pending

#### 2.1 Create Missing Indexes
**File:** `database/migrations/009_add_performance_indexes.js`
```javascript
exports.up = function(knex) {
  return knex.schema.raw(`
    -- Facilitator authentication indexes
    CREATE INDEX IF NOT EXISTS idx_facilitators_email_active ON facilitators(email, is_active);
    CREATE INDEX IF NOT EXISTS idx_facilitators_organization ON facilitators(organization);
    
    -- Classroom management indexes
    CREATE INDEX IF NOT EXISTS idx_classrooms_facilitator_id ON classrooms(facilitator_id);
    CREATE INDEX IF NOT EXISTS idx_classrooms_code_active ON classrooms(classroom_code, is_active);
    CREATE INDEX IF NOT EXISTS idx_classrooms_created_at ON classrooms(created_at);
    
    -- Student management indexes
    CREATE INDEX IF NOT EXISTS idx_students_classroom_id ON students(classroom_id);
    CREATE INDEX IF NOT EXISTS idx_students_anonymous_id ON students(anonymous_id);
    CREATE INDEX IF NOT EXISTS idx_students_active ON students(is_active);
    CREATE INDEX IF NOT EXISTS idx_students_last_active ON students(last_active_at);
    
    -- Progress tracking indexes
    CREATE INDEX IF NOT EXISTS idx_student_progress_student_lesson ON student_progress(student_id, lesson_id);
    CREATE INDEX IF NOT EXISTS idx_student_progress_completion ON student_progress(completion_status);
    CREATE INDEX IF NOT EXISTS idx_student_progress_updated_at ON student_progress(updated_at);
    
    -- Analytics indexes
    CREATE INDEX IF NOT EXISTS idx_analytics_events_timestamp ON analytics_events(timestamp);
    CREATE INDEX IF NOT EXISTS idx_analytics_events_type_category ON analytics_events(event_type, event_category);
    CREATE INDEX IF NOT EXISTS idx_analytics_events_classroom ON analytics_events(classroom_id);
    
    -- Feedback indexes
    CREATE INDEX IF NOT EXISTS idx_student_feedback_student_lesson ON student_feedback(student_id, lesson_id);
    CREATE INDEX IF NOT EXISTS idx_student_feedback_type ON student_feedback(feedback_type);
    CREATE INDEX IF NOT EXISTS idx_student_feedback_created_at ON student_feedback(created_at);
  `);
};

exports.down = function(knex) {
  return knex.schema.raw(`
    DROP INDEX IF EXISTS idx_facilitators_email_active;
    DROP INDEX IF EXISTS idx_facilitators_organization;
    DROP INDEX IF EXISTS idx_classrooms_facilitator_id;
    DROP INDEX IF EXISTS idx_classrooms_code_active;
    DROP INDEX IF EXISTS idx_classrooms_created_at;
    DROP INDEX IF EXISTS idx_students_classroom_id;
    DROP INDEX IF EXISTS idx_students_anonymous_id;
    DROP INDEX IF EXISTS idx_students_active;
    DROP INDEX IF EXISTS idx_students_last_active;
    DROP INDEX IF EXISTS idx_student_progress_student_lesson;
    DROP INDEX IF EXISTS idx_student_progress_completion;
    DROP INDEX IF EXISTS idx_student_progress_updated_at;
    DROP INDEX IF EXISTS idx_analytics_events_timestamp;
    DROP INDEX IF EXISTS idx_analytics_events_type_category;
    DROP INDEX IF EXISTS idx_analytics_events_classroom;
    DROP INDEX IF EXISTS idx_student_feedback_student_lesson;
    DROP INDEX IF EXISTS idx_student_feedback_type;
    DROP INDEX IF EXISTS idx_student_feedback_created_at;
  `);
};
```

### 3. Query Optimization
**Priority:** High  
**Status:** Pending

#### 3.1 Optimize Classroom Queries
**File:** `src/controllers/classroomController.js`
```javascript
// Optimized classroom queries with proper joins and indexing
async function getClassroomsWithStats(facilitatorId) {
  return await db('classrooms')
    .select(
      'classrooms.*',
      db.raw('COUNT(DISTINCT students.id) as student_count'),
      db.raw('COUNT(DISTINCT CASE WHEN student_progress.completion_status = ? THEN student_progress.student_id END) as active_students', ['completed'])
    )
    .leftJoin('students', 'classrooms.id', 'students.classroom_id')
    .leftJoin('student_progress', 'students.id', 'student_progress.student_id')
    .where('classrooms.facilitator_id', facilitatorId)
    .where('classrooms.is_active', true)
    .groupBy('classrooms.id')
    .orderBy('classrooms.created_at', 'desc');
}
```

#### 3.2 Optimize Analytics Queries
**File:** `src/controllers/analyticsController.js`
```javascript
// Optimized analytics queries with proper aggregation
async function getClassroomAnalytics(classroomId, startDate, endDate) {
  return await db('analytics_events')
    .select(
      'event_type',
      'event_category',
      db.raw('COUNT(*) as event_count'),
      db.raw('AVG(event_value) as avg_value')
    )
    .where('classroom_id', classroomId)
    .whereBetween('timestamp', [startDate, endDate])
    .groupBy('event_type', 'event_category')
    .orderBy('event_count', 'desc');
}
```

### 4. Database Security Enhancements
**Priority:** High  
**Status:** Pending

#### 4.1 Row Level Security (RLS)
**File:** `database/migrations/010_add_row_level_security.js`
```javascript
exports.up = function(knex) {
  return knex.schema.raw(`
    -- Enable RLS on sensitive tables
    ALTER TABLE classrooms ENABLE ROW LEVEL SECURITY;
    ALTER TABLE students ENABLE ROW LEVEL SECURITY;
    ALTER TABLE student_progress ENABLE ROW LEVEL SECURITY;
    ALTER TABLE student_feedback ENABLE ROW LEVEL SECURITY;
    
    -- Create policies for facilitators
    CREATE POLICY facilitator_classrooms ON classrooms
      FOR ALL TO heroes_user
      USING (facilitator_id = current_setting('app.facilitator_id')::uuid);
    
    CREATE POLICY facilitator_students ON students
      FOR ALL TO heroes_user
      USING (classroom_id IN (
        SELECT id FROM classrooms WHERE facilitator_id = current_setting('app.facilitator_id')::uuid
      ));
    
    CREATE POLICY facilitator_progress ON student_progress
      FOR ALL TO heroes_user
      USING (student_id IN (
        SELECT s.id FROM students s
        JOIN classrooms c ON s.classroom_id = c.id
        WHERE c.facilitator_id = current_setting('app.facilitator_id')::uuid
      ));
    
    CREATE POLICY facilitator_feedback ON student_feedback
      FOR ALL TO heroes_user
      USING (student_id IN (
        SELECT s.id FROM students s
        JOIN classrooms c ON s.classroom_id = c.id
        WHERE c.facilitator_id = current_setting('app.facilitator_id')::uuid
      ));
  `);
};

exports.down = function(knex) {
  return knex.schema.raw(`
    DROP POLICY IF EXISTS facilitator_classrooms ON classrooms;
    DROP POLICY IF EXISTS facilitator_students ON students;
    DROP POLICY IF EXISTS facilitator_progress ON student_progress;
    DROP POLICY IF EXISTS facilitator_feedback ON student_feedback;
    
    ALTER TABLE classrooms DISABLE ROW LEVEL SECURITY;
    ALTER TABLE students DISABLE ROW LEVEL SECURITY;
    ALTER TABLE student_progress DISABLE ROW LEVEL SECURITY;
    ALTER TABLE student_feedback DISABLE ROW LEVEL SECURITY;
  `);
};
```

### 5. Backup and Recovery Enhancement
**Priority:** Medium  
**Status:** Pending

#### 5.1 Automated Backup Script
**File:** `scripts/backup.sh`
```bash
#!/bin/bash

# Database backup script with encryption and compression
BACKUP_DIR="/backups"
DB_NAME="heroes_in_waiting"
DB_USER="heroes_user"
DB_HOST="localhost"
BACKUP_FILE="heroes_backup_$(date +%Y%m%d_%H%M%S).sql.gz.gpg"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Create encrypted and compressed backup
PGPASSWORD=$DB_PASSWORD pg_dump \
  -h $DB_HOST \
  -U $DB_USER \
  -d $DB_NAME \
  --verbose \
  --clean \
  --no-owner \
  --no-privileges \
  | gzip \
  | gpg --encrypt --recipient $GPG_RECIPIENT \
  > $BACKUP_DIR/$BACKUP_FILE

# Remove backups older than 30 days
find $BACKUP_DIR -name "heroes_backup_*.sql.gz.gpg" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE"
```

## Implementation Timeline

### Phase 1: Connection Pooling (Day 1)
- [ ] Update docker-compose.yml with PgBouncer
- [ ] Update database configuration
- [ ] Test connection pooling
- [ ] Monitor performance improvements

### Phase 2: Index Optimization (Day 2)
- [ ] Create performance indexes migration
- [ ] Run migration on development
- [ ] Test query performance
- [ ] Monitor index usage

### Phase 3: Query Optimization (Day 3)
- [ ] Optimize classroom queries
- [ ] Optimize analytics queries
- [ ] Test performance improvements
- [ ] Update documentation

### Phase 4: Security Enhancement (Day 4)
- [ ] Implement Row Level Security
- [ ] Test security policies
- [ ] Update backup procedures
- [ ] Security testing

## Success Criteria
- [ ] PgBouncer connection pooling working correctly
- [ ] Database query performance improved by 50%+
- [ ] All critical indexes created and optimized
- [ ] Row Level Security implemented and tested
- [ ] Backup and recovery procedures enhanced
- [ ] No database-related performance bottlenecks

## Risk Mitigation
- **Rollback Plan:** Keep database snapshots before changes
- **Testing Strategy:** Comprehensive performance testing
- **Monitoring:** Enhanced database monitoring during implementation
- **Documentation:** Update database documentation

## Next Steps
1. Begin Phase 1 implementation
2. Coordinate with SRE team for monitoring
3. Test all database changes thoroughly
4. Complete all phases before Checkpoint 5 
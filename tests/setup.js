// Test setup file
const { execSync } = require('child_process');
const db = require('../src/config/database');

// Set test environment
process.env.NODE_ENV = 'test';
process.env.DB_NAME = 'heroes_in_waiting_test';

// Global test setup
beforeAll(async () => {
  // Create test database if it doesn't exist
  try {
    execSync('createdb heroes_in_waiting_test', { stdio: 'ignore' });
  } catch (error) {
    // Database might already exist
  }

  // Run migrations
  try {
    execSync('npm run migrate', { stdio: 'ignore' });
  } catch (error) {
    console.error('Migration failed:', error.message);
  }
});

// Global test teardown
afterAll(async () => {
  // Close database connections
  await db.destroy();
});

// Reset database between tests
beforeEach(async () => {
  // Truncate all tables except migrations
  const tables = [
    'analytics_events',
    'student_feedback', 
    'student_progress',
    'classroom_sessions',
    'students',
    'lessons',
    'classrooms',
    'facilitators'
  ];

  for (const table of tables) {
    await db(table).del();
  }
});
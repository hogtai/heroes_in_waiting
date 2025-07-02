const db = require('../config/database');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');

/**
 * Create test facilitator and return JWT token
 */
async function createTestFacilitator() {
  const facilitatorData = {
    email: 'test.facilitator@heroesinwaiting.com',
    password: await bcrypt.hash('TestPassword123!', 12),
    first_name: 'Test',
    last_name: 'Facilitator',
    organization: 'Test Organization',
    role: 'facilitator',
    is_active: true
  };

  const [facilitator] = await db('facilitators')
    .insert(facilitatorData)
    .returning('*');

  const token = jwt.sign(
    { 
      facilitatorId: facilitator.id,
      email: facilitator.email,
      role: facilitator.role
    },
    process.env.JWT_SECRET || 'test-secret',
    { expiresIn: '1h' }
  );

  return { facilitator, token };
}

/**
 * Create test classroom
 */
async function createTestClassroom(facilitatorId) {
  const classroomData = {
    name: 'Test Classroom',
    description: 'Test classroom for performance testing',
    facilitator_id: facilitatorId,
    classroom_code: 'TEST123',
    is_active: true,
    settings: { max_students: 30 }
  };

  const [classroom] = await db('classrooms')
    .insert(classroomData)
    .returning('*');

  return classroom;
}

/**
 * Create test content for performance testing
 */
async function createTestContent() {
  try {
    // Create facilitator and get token
    const { facilitator, token } = await createTestFacilitator();
    
    // Create classroom
    const classroom = await createTestClassroom(facilitator.id);

    // Create test lessons
    const lessons = [];
    for (let i = 1; i <= 12; i++) {
      const lessonData = {
        title: `Test Lesson ${i}`,
        description: `Test lesson ${i} for Heroes in Waiting curriculum`,
        content: `<p>This is test content for lesson ${i}</p>`,
        lesson_order: i,
        duration_minutes: 30,
        is_active: true,
        metadata: {
          ageGroup: '4-6',
          difficulty: 'beginner',
          tags: ['test', 'performance']
        }
      };

      const [lesson] = await db('lessons')
        .insert(lessonData)
        .returning('*');
      
      lessons.push(lesson);
    }

    // Create test content management entries
    const contentEntries = [];
    for (let i = 1; i <= 5; i++) {
      const contentData = {
        title: `Test Content ${i}`,
        type: 'lesson',
        content: `<p>Test content ${i} for performance testing</p>`,
        metadata: {
          version: '1.0',
          author: 'Test Author',
          tags: ['test', 'performance']
        },
        status: 'published',
        created_by: facilitator.id,
        is_active: true
      };

      const [content] = await db('content_management')
        .insert(contentData)
        .returning('*');
      
      contentEntries.push(content);
    }

    return {
      facilitator,
      token,
      classroom,
      lessons,
      contentEntries
    };

  } catch (error) {
    console.error('Error creating test content:', error);
    throw error;
  }
}

/**
 * Clean up test data
 */
async function cleanupTestData(testData) {
  try {
    if (!testData) return;

    // Clean up in reverse order to respect foreign key constraints
    if (testData.contentEntries) {
      await db('content_management')
        .whereIn('id', testData.contentEntries.map(c => c.id))
        .del();
    }

    if (testData.lessons) {
      await db('lessons')
        .whereIn('id', testData.lessons.map(l => l.id))
        .del();
    }

    if (testData.classroom) {
      await db('classrooms')
        .where('id', testData.classroom.id)
        .del();
    }

    if (testData.facilitator) {
      await db('facilitators')
        .where('id', testData.facilitator.id)
        .del();
    }

  } catch (error) {
    console.error('Error cleaning up test data:', error);
    throw error;
  }
}

/**
 * Create test student session
 */
async function createTestStudentSession(classroomId) {
  const sessionData = {
    classroom_id: classroomId,
    student_identifier: 'test-student-' + Math.random().toString(36).substr(2, 9),
    is_active: true,
    session_data: {
      joinedAt: new Date(),
      deviceInfo: 'test-device'
    }
  };

  const [session] = await db('student_sessions')
    .insert(sessionData)
    .returning('*');

  return session;
}

/**
 * Generate performance test data
 */
async function generatePerformanceTestData(count = 100) {
  const data = [];
  
  for (let i = 0; i < count; i++) {
    data.push({
      title: `Performance Test Content ${i}`,
      content: `<p>This is performance test content ${i}</p>`.repeat(10),
      metadata: {
        index: i,
        timestamp: new Date(),
        tags: ['performance', 'test', `batch-${Math.floor(i / 10)}`]
      }
    });
  }

  return data;
}

module.exports = {
  createTestFacilitator,
  createTestClassroom,
  createTestContent,
  cleanupTestData,
  createTestStudentSession,
  generatePerformanceTestData
};
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

/**
 * Create test analytics events for behavioral tracking
 */
async function createBehavioralAnalyticsEvents(classroomId, lessonId, count = 10) {
  const crypto = require('crypto');
  const behavioralCategories = ['empathy', 'confidence', 'communication', 'leadership'];
  const events = [];

  for (let i = 0; i < count; i++) {
    const category = behavioralCategories[i % behavioralCategories.length];
    const sessionId = crypto.randomBytes(16).toString('hex');
    
    const eventData = {
      student_id: null, // Anonymous tracking
      classroom_id: classroomId,
      lesson_id: lessonId,
      session_id: sessionId,
      event_type: 'behavioral_interaction',
      event_category: category,
      event_action: `${category}_response`,
      event_label: `test_${category}_scenario_${i}`,
      event_value: Math.floor(Math.random() * 5) + 1, // 1-5 scale
      event_properties: {
        behavioral_category: category,
        scenario_id: `test_scenario_${i}`,
        response_quality: ['low', 'medium', 'high'][Math.floor(Math.random() * 3)],
        anonymous_session_id: crypto.createHash('sha256').update(sessionId + 'salt').digest('hex'),
        response_time_seconds: Math.floor(Math.random() * 30) + 5
      },
      device_type: ['tablet', 'phone', 'desktop'][Math.floor(Math.random() * 3)],
      platform: ['android', 'ios', 'web'][Math.floor(Math.random() * 3)],
      session_id_local: sessionId,
      session_duration: Math.floor(Math.random() * 1800) + 300 // 5-35 minutes
    };

    const [event] = await db('analytics_events')
      .insert(eventData)
      .returning('*');
    
    events.push(event);
  }

  return events;
}

/**
 * Create anonymous student tracking data for COPPA compliance testing
 */
async function createAnonymousStudentData(classroomId, count = 5) {
  const crypto = require('crypto');
  const students = [];

  for (let i = 0; i < count; i++) {
    const originalId = `test-student-${i}-${Math.random().toString(36).substr(2, 9)}`;
    const anonymizedId = crypto.createHash('sha256').update(originalId + 'coppa-salt').digest('hex');
    
    const studentData = {
      classroom_id: classroomId,
      anonymous_id: anonymizedId,
      grade_level: Math.floor(Math.random() * 6) + 1, // Grades 1-6
      gender: ['not_specified', 'prefer_not_to_say'][Math.floor(Math.random() * 2)], // COPPA compliant options
      has_disabilities: null, // Not collected per COPPA
      primary_language: 'en',
      total_sessions: Math.floor(Math.random() * 20) + 1,
      first_joined_at: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000), // Last 30 days
      last_active_at: new Date(),
      is_active: true
    };

    const [student] = await db('students')
      .insert(studentData)
      .returning('*');
    
    students.push(student);
  }

  return students;
}

/**
 * Create test educational impact metrics data
 */
async function createEducationalImpactData(classroomId, lessonId) {
  const impactData = {
    // Empathy Development Metrics
    empathy_pre_scores: [2, 3, 2, 4, 3], // Pre-lesson empathy scores
    empathy_post_scores: [4, 4, 3, 5, 4], // Post-lesson empathy scores
    
    // Confidence Building Metrics
    confidence_pre_scores: [2, 2, 3, 3, 2],
    confidence_post_scores: [3, 4, 4, 4, 3],
    
    // Communication Skills Metrics
    communication_interactions: [5, 8, 6, 10, 7], // Number of communication interactions
    active_listening_instances: [3, 5, 4, 7, 5],
    
    // Leadership Behavior Metrics
    leadership_initiatives: [1, 0, 2, 3, 1], // Leadership initiatives taken
    peer_collaboration_scores: [3, 4, 3, 5, 4]
  };

  // Calculate improvement metrics
  const empathyImprovement = impactData.empathy_post_scores.reduce((sum, score, i) => 
    sum + (score - impactData.empathy_pre_scores[i]), 0) / impactData.empathy_pre_scores.length;
  
  const confidenceImprovement = impactData.confidence_post_scores.reduce((sum, score, i) => 
    sum + (score - impactData.confidence_pre_scores[i]), 0) / impactData.confidence_pre_scores.length;

  return {
    ...impactData,
    empathy_improvement_average: empathyImprovement,
    confidence_improvement_average: confidenceImprovement,
    total_students_measured: impactData.empathy_pre_scores.length,
    measurement_date: new Date(),
    classroom_id: classroomId,
    lesson_id: lessonId
  };
}

/**
 * Clean up analytics test data
 */
async function cleanupAnalyticsTestData(classroomId) {
  try {
    // Clean up analytics events
    await db('analytics_events')
      .where('classroom_id', classroomId)
      .del();

    // Clean up anonymous students
    await db('students')
      .where('classroom_id', classroomId)
      .del();

    // Clean up test sessions
    await db('student_sessions')
      .where('classroom_id', classroomId)
      .del();

  } catch (error) {
    console.error('Error cleaning up analytics test data:', error);
    throw error;
  }
}

/**
 * Generate COPPA-compliant test analytics event
 */
function generateCOPPACompliantEvent(eventType = 'lesson_interaction') {
  const crypto = require('crypto');
  const sessionId = crypto.randomBytes(16).toString('hex');
  
  return {
    eventType: eventType,
    eventCategory: 'engagement',
    eventAction: 'test_action',
    eventLabel: 'coppa_compliant_test',
    eventValue: 1,
    eventProperties: {
      // Only educational, non-PII data
      behavioral_category: 'empathy',
      interaction_type: 'educational',
      anonymous_session_id: crypto.createHash('sha256').update(sessionId + 'coppa-salt').digest('hex'),
      educational_objective: 'empathy_development',
      lesson_section: 'main_activity'
    },
    deviceType: 'tablet',
    platform: 'android',
    appVersion: '1.0.0',
    sessionIdLocal: sessionId,
    sessionDuration: Math.floor(Math.random() * 600) + 60 // 1-10 minutes
  };
}

/**
 * Validate event for COPPA compliance
 */
function validateCOPPACompliance(eventData) {
  const eventString = JSON.stringify(eventData);
  
  // Check for common PII patterns
  const piiPatterns = [
    /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/, // Email
    /\b\d{3}-\d{3}-\d{4}\b/, // Phone numbers
    /\b\d{3}\s\w+\s(street|st|avenue|ave|road|rd)\b/i, // Addresses
    /\b(first_name|last_name|full_name|email|phone|address)\b/i // PII field names
  ];

  const violations = [];
  
  piiPatterns.forEach((pattern, index) => {
    if (pattern.test(eventString)) {
      violations.push(`PII Pattern ${index + 1} detected`);
    }
  });

  return {
    isCompliant: violations.length === 0,
    violations: violations
  };
}

module.exports = {
  createTestFacilitator,
  createTestClassroom,
  createTestContent,
  cleanupTestData,
  createTestStudentSession,
  generatePerformanceTestData,
  createBehavioralAnalyticsEvents,
  createAnonymousStudentData,
  createEducationalImpactData,
  cleanupAnalyticsTestData,
  generateCOPPACompliantEvent,
  validateCOPPACompliance
};
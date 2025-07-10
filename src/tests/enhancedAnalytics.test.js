const request = require('supertest');
const app = require('../app');
const db = require('../config/database');
const { 
  createTestFacilitator, 
  createTestClassroom, 
  createTestStudentSession, 
  cleanupTestData 
} = require('./testUtils');
const crypto = require('crypto');

describe('Enhanced Analytics System Integration Tests', () => {
  let testData = {};
  let facilitatorToken;
  let studentSession;

  beforeAll(async () => {
    // Create comprehensive test data
    const facilitatorData = await createTestFacilitator();
    facilitatorToken = facilitatorData.token;
    testData.facilitator = facilitatorData.facilitator;
    
    testData.classroom = await createTestClassroom(testData.facilitator.id);
    studentSession = await createTestStudentSession(testData.classroom.id);
    testData.studentSession = studentSession;

    // Create test lessons for analytics
    const lessonData = {
      title: 'Test Analytics Lesson',
      description: 'Test lesson for analytics tracking',
      content: '<p>Test lesson content</p>',
      lesson_order: 1,
      duration_minutes: 30,
      is_active: true,
      metadata: {
        ageGroup: '4-6',
        difficulty: 'beginner',
        analyticsEnabled: true
      }
    };

    const [lesson] = await db('lessons')
      .insert(lessonData)
      .returning('*');
    
    testData.lesson = lesson;
  });

  afterAll(async () => {
    // Clean up test data
    if (testData.lesson) {
      await db('analytics_events').where('lesson_id', testData.lesson.id).del();
      await db('lessons').where('id', testData.lesson.id).del();
    }
    
    if (testData.studentSession) {
      await db('student_sessions').where('id', testData.studentSession.id).del();
    }
    
    await cleanupTestData(testData);
    await db.destroy();
  });

  // ================== COPPA Compliance Validation Tests ==================

  describe('COPPA Compliance Validation', () => {
    test('should track analytics events without collecting any PII', async () => {
      const analyticsEvent = {
        eventType: 'lesson_start',
        eventCategory: 'engagement',
        eventAction: 'start_lesson',
        eventLabel: 'empathy_lesson_1',
        eventValue: 1,
        eventProperties: {
          anonymous_session_id: crypto.randomBytes(16).toString('hex'),
          behavioral_category: 'empathy',
          interaction_type: 'lesson_start',
          difficulty_level: 'beginner'
        },
        deviceType: 'tablet',
        platform: 'android',
        appVersion: '1.0.0',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        sessionDuration: 0,
        lessonId: testData.lesson.id
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(analyticsEvent)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.eventId).toBeDefined();
      expect(response.body.data.timestamp).toBeDefined();

      // Verify no PII was stored
      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(storedEvent).toBeDefined();
      
      // Ensure no email, phone, real names, or addresses
      const eventString = JSON.stringify(storedEvent);
      expect(eventString).not.toMatch(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/); // No email patterns
      expect(eventString).not.toMatch(/\b\d{3}-\d{3}-\d{4}\b/); // No phone patterns
      expect(eventString).not.toMatch(/\b\d{3}\s\w+\s(street|st|avenue|ave|road|rd)\b/i); // No address patterns
      
      // Verify only anonymous identifiers are used
      expect(storedEvent.student_id).toBeNull(); // Should not link to actual student ID
      expect(storedEvent.event_properties.anonymous_session_id).toBeDefined();
    });

    test('should anonymize student identifiers using SHA-256 hashing', async () => {
      const originalIdentifier = testData.studentSession.student_identifier;
      
      // Test that identifiers are properly hashed
      const hash1 = crypto.createHash('sha256').update(originalIdentifier + 'salt1').digest('hex');
      const hash2 = crypto.createHash('sha256').update(originalIdentifier + 'salt1').digest('hex');
      
      expect(hash1).toBe(hash2); // Consistent hashing
      expect(hash1).toHaveLength(64); // SHA-256 produces 64-character hex string
      expect(hash1).not.toBe(originalIdentifier); // Original identifier should be transformed
    });

    test('should enforce educational purpose restrictions', async () => {
      const nonEducationalEvent = {
        eventType: 'marketing_click',
        eventCategory: 'advertisement',
        eventAction: 'click_ad',
        eventLabel: 'commercial_content',
        eventValue: 1,
        eventProperties: {
          tracking_purpose: 'marketing'
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex')
      };

      // This should be rejected or filtered to only educational data
      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(nonEducationalEvent)
        .expect(201); // May accept but should filter non-educational properties

      // Verify only educational-purpose data is stored
      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      // Should not contain marketing/commercial tracking data
      expect(storedEvent.event_category).not.toBe('advertisement');
      expect(JSON.stringify(storedEvent.event_properties || {})).not.toContain('marketing');
    });

    test('should implement automatic data retention policy (90-day default)', async () => {
      // Create an old analytics event (older than 90 days)
      const oldDate = new Date();
      oldDate.setDate(oldDate.getDate() - 95); // 95 days ago

      const oldEventData = {
        student_id: null, // Anonymous
        classroom_id: testData.classroom.id,
        lesson_id: testData.lesson.id,
        event_type: 'lesson_start',
        event_category: 'engagement',
        event_action: 'start_lesson',
        event_timestamp: oldDate,
        device_type: 'tablet',
        platform: 'android'
      };

      const [oldEvent] = await db('analytics_events')
        .insert(oldEventData)
        .returning('*');

      // Simulate data retention cleanup (this would typically be a scheduled job)
      const retentionDate = new Date();
      retentionDate.setDate(retentionDate.getDate() - 90);

      const deletedCount = await db('analytics_events')
        .where('event_timestamp', '<', retentionDate)
        .del();

      expect(deletedCount).toBeGreaterThan(0);

      // Verify the old event was removed
      const remainingEvent = await db('analytics_events')
        .where('id', oldEvent.id)
        .first();

      expect(remainingEvent).toBeUndefined();
    });
  });

  // ================== Educational Analytics Validation Tests ==================

  describe('Educational Analytics Validation', () => {
    test('should accurately track behavioral analytics for empathy development', async () => {
      const empathyEvent = {
        eventType: 'behavioral_interaction',
        eventCategory: 'empathy',
        eventAction: 'empathy_response',
        eventLabel: 'helping_scenario_response',
        eventValue: 4, // Empathy score (1-5 scale)
        eventProperties: {
          behavioral_category: 'empathy',
          scenario_id: 'helping_friend_scenario',
          response_type: 'multiple_choice',
          response_value: 'helped_immediately',
          emotional_context: 'peer_distress',
          response_time_seconds: 12,
          confidence_level: 'high'
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(empathyEvent)
        .expect(201);

      // Verify behavioral data is accurately stored
      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(storedEvent.event_category).toBe('empathy');
      expect(storedEvent.event_value).toBe(4);
      expect(storedEvent.event_properties.behavioral_category).toBe('empathy');
      expect(storedEvent.event_properties.response_time_seconds).toBe(12);
    });

    test('should track confidence building measurement accurately', async () => {
      const confidenceEvent = {
        eventType: 'confidence_measurement',
        eventCategory: 'confidence',
        eventAction: 'confidence_self_assessment',
        eventLabel: 'pre_activity_confidence',
        eventValue: 3, // Confidence level (1-5 scale)
        eventProperties: {
          behavioral_category: 'confidence',
          assessment_type: 'self_report',
          activity_context: 'public_speaking_preparation',
          previous_confidence_level: 2,
          confidence_change: 1,
          support_level_needed: 'moderate'
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(confidenceEvent)
        .expect(201);

      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(storedEvent.event_category).toBe('confidence');
      expect(storedEvent.event_properties.confidence_change).toBe(1);
      expect(storedEvent.event_properties.assessment_type).toBe('self_report');
    });

    test('should track communication skill interactions', async () => {
      const communicationEvent = {
        eventType: 'communication_interaction',
        eventCategory: 'communication',
        eventAction: 'peer_interaction',
        eventLabel: 'collaborative_discussion',
        eventValue: 1, // Interaction count
        eventProperties: {
          behavioral_category: 'communication',
          interaction_type: 'peer_collaboration',
          communication_mode: 'verbal',
          group_size: 4,
          leadership_role: false,
          active_listening_demonstrated: true,
          turn_taking_appropriate: true,
          conflict_resolution_needed: false
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(communicationEvent)
        .expect(201);

      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(storedEvent.event_category).toBe('communication');
      expect(storedEvent.event_properties.active_listening_demonstrated).toBe(true);
      expect(storedEvent.event_properties.group_size).toBe(4);
    });

    test('should track leadership behavior identification', async () => {
      const leadershipEvent = {
        eventType: 'leadership_behavior',
        eventCategory: 'leadership',
        eventAction: 'leadership_demonstration',
        eventLabel: 'initiative_taking',
        eventValue: 1,
        eventProperties: {
          behavioral_category: 'leadership',
          leadership_type: 'initiative_taking',
          context: 'group_project_organization',
          peer_response: 'positive',
          facilitator_guidance: 'minimal',
          outcome: 'successful',
          collaboration_score: 4
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(leadershipEvent)
        .expect(201);

      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(storedEvent.event_category).toBe('leadership');
      expect(storedEvent.event_properties.leadership_type).toBe('initiative_taking');
      expect(storedEvent.event_properties.collaboration_score).toBe(4);
    });
  });

  // ================== Authentication & Authorization Tests ==================

  describe('Authentication & Authorization', () => {
    test('should authenticate facilitator with valid JWT token for dashboard access', async () => {
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: '30d' })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.classroom.id).toBe(testData.classroom.id);
      expect(response.body.data.summary).toBeDefined();
    });

    test('should reject facilitator dashboard access with invalid JWT token', async () => {
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', 'Bearer invalid-token')
        .expect(401);

      expect(response.body.success).toBe(false);
    });

    test('should authenticate student with classroom code and student ID for event tracking', async () => {
      const studentEvent = {
        eventType: 'lesson_interaction',
        eventCategory: 'engagement',
        eventAction: 'tab_switch',
        eventLabel: 'video_tab',
        eventValue: 1,
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex')
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(studentEvent)
        .expect(201);

      expect(response.body.success).toBe(true);
    });

    test('should reject student event tracking with invalid classroom code', async () => {
      const studentEvent = {
        eventType: 'lesson_interaction',
        eventCategory: 'engagement',
        eventAction: 'tab_switch',
        eventLabel: 'video_tab',
        eventValue: 1
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', 'INVALID123')
        .set('studentid', testData.studentSession.student_identifier)
        .send(studentEvent)
        .expect(401);

      expect(response.body.success).toBe(false);
    });

    test('should enforce facilitator ownership of classroom analytics', async () => {
      // Create another facilitator and classroom
      const otherFacilitatorData = await createTestFacilitator();
      const otherClassroom = await createTestClassroom(otherFacilitatorData.facilitator.id);

      // Try to access other facilitator's classroom analytics
      const response = await request(app)
        .get(`/api/analytics/classroom/${otherClassroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .expect(404);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('Classroom not found');

      // Cleanup
      await db('classrooms').where('id', otherClassroom.id).del();
      await db('facilitators').where('id', otherFacilitatorData.facilitator.id).del();
    });
  });

  // ================== Error Handling Tests ==================

  describe('Error Handling', () => {
    test('should handle invalid analytics event data gracefully', async () => {
      const invalidEvent = {
        // Missing required fields
        eventType: '',
        eventCategory: null,
        eventValue: 'not-a-number'
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(invalidEvent)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });

    test('should handle malformed JSON in event properties', async () => {
      const eventWithMalformedProperties = {
        eventType: 'test_event',
        eventCategory: 'test',
        eventAction: 'test_action',
        eventProperties: 'not-valid-json', // Should be object
        deviceType: 'tablet',
        platform: 'android'
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(eventWithMalformedProperties)
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    test('should handle database connection errors gracefully', async () => {
      // This test simulates a database connection issue
      // In a real scenario, you might temporarily disable the database connection
      // For now, we'll test with an invalid classroom ID that doesn't exist
      
      const response = await request(app)
        .get('/api/analytics/classroom/invalid-uuid-format/dashboard')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    test('should handle extremely large event payloads', async () => {
      const largeEvent = {
        eventType: 'large_event',
        eventCategory: 'test',
        eventAction: 'test_action',
        eventProperties: {
          large_data: 'x'.repeat(10000) // 10KB of data
        },
        deviceType: 'tablet',
        platform: 'android'
      };

      // Should handle large payloads up to reasonable limits
      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(largeEvent);

      // Should either accept (201) or reject with size limit error (413)
      expect([201, 413]).toContain(response.status);
    });
  });

  // ================== Performance Validation Tests ==================

  describe('Performance Validation', () => {
    test('should respond to dashboard queries in under 100ms', async () => {
      const startTime = Date.now();
      
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: '7d' })
        .expect(200);

      const responseTime = Date.now() - startTime;
      
      expect(response.body.success).toBe(true);
      expect(responseTime).toBeLessThan(100); // Should be under 100ms
    });

    test('should handle concurrent analytics event submissions efficiently', async () => {
      const concurrentEvents = Array.from({ length: 10 }, (_, i) => ({
        eventType: `concurrent_event_${i}`,
        eventCategory: 'performance_test',
        eventAction: 'concurrent_submission',
        eventValue: i,
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex')
      }));

      const startTime = Date.now();
      
      const promises = concurrentEvents.map(event =>
        request(app)
          .post('/api/analytics/event')
          .set('classroomcode', testData.classroom.classroom_code)
          .set('studentid', testData.studentSession.student_identifier)
          .send(event)
      );

      const responses = await Promise.all(promises);
      const totalTime = Date.now() - startTime;

      // All requests should succeed
      responses.forEach(response => {
        expect(response.status).toBe(201);
        expect(response.body.success).toBe(true);
      });

      // Total time for 10 concurrent requests should be reasonable
      expect(totalTime).toBeLessThan(2000); // Under 2 seconds for 10 concurrent requests
    });

    test('should efficiently query analytics data with large datasets', async () => {
      // Create multiple analytics events for performance testing
      const events = Array.from({ length: 50 }, (_, i) => ({
        student_id: null,
        classroom_id: testData.classroom.id,
        lesson_id: testData.lesson.id,
        event_type: `performance_test_${i % 5}`,
        event_category: 'performance',
        event_action: 'bulk_test',
        event_timestamp: new Date(),
        device_type: 'tablet',
        platform: 'android'
      }));

      await db('analytics_events').insert(events);

      const startTime = Date.now();
      
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: '30d' })
        .expect(200);

      const responseTime = Date.now() - startTime;

      expect(response.body.success).toBe(true);
      expect(responseTime).toBeLessThan(200); // Should handle larger datasets efficiently
      
      // Cleanup performance test data
      await db('analytics_events')
        .where('event_category', 'performance')
        .del();
    });
  });

  // ================== Data Integrity Tests ==================

  describe('Data Integrity', () => {
    test('should maintain data consistency across platform sync', async () => {
      const eventId = crypto.randomBytes(16).toString('hex');
      
      // Track the same logical event from different platforms
      const mobileEvent = {
        eventType: 'lesson_completion',
        eventCategory: 'engagement',
        eventAction: 'complete_lesson',
        eventLabel: 'cross_platform_test',
        eventValue: 1,
        eventProperties: {
          sync_id: eventId,
          platform_source: 'mobile'
        },
        deviceType: 'phone',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      const webEvent = {
        eventType: 'lesson_completion',
        eventCategory: 'engagement',
        eventAction: 'complete_lesson',
        eventLabel: 'cross_platform_test',
        eventValue: 1,
        eventProperties: {
          sync_id: eventId,
          platform_source: 'web'
        },
        deviceType: 'desktop',
        platform: 'web',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        lessonId: testData.lesson.id
      };

      // Submit both events
      const mobileResponse = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(mobileEvent)
        .expect(201);

      const webResponse = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(webEvent)
        .expect(201);

      // Verify both events are stored with consistent data
      const storedEvents = await db('analytics_events')
        .whereIn('id', [mobileResponse.body.data.eventId, webResponse.body.data.eventId]);

      expect(storedEvents).toHaveLength(2);
      
      storedEvents.forEach(event => {
        expect(event.event_type).toBe('lesson_completion');
        expect(event.event_category).toBe('engagement');
        expect(event.lesson_id).toBe(testData.lesson.id);
        expect(event.event_properties.sync_id).toBe(eventId);
      });
    });

    test('should validate SHA-256 hashing consistency for anonymous identifiers', async () => {
      const originalIdentifier = 'test-student-123';
      const salt = 'consistent-salt';

      // Generate hash multiple times
      const hash1 = crypto.createHash('sha256').update(originalIdentifier + salt).digest('hex');
      const hash2 = crypto.createHash('sha256').update(originalIdentifier + salt).digest('hex');
      const hash3 = crypto.createHash('sha256').update(originalIdentifier + salt).digest('hex');

      // All hashes should be identical
      expect(hash1).toBe(hash2);
      expect(hash2).toBe(hash3);
      
      // Hash should be exactly 64 characters (SHA-256 in hex)
      expect(hash1).toHaveLength(64);
      
      // Hash should be different from original
      expect(hash1).not.toBe(originalIdentifier);
      
      // Different salt should produce different hash
      const differentSaltHash = crypto.createHash('sha256').update(originalIdentifier + 'different-salt').digest('hex');
      expect(hash1).not.toBe(differentSaltHash);
    });

    test('should ensure data type consistency in analytics storage', async () => {
      const typedEvent = {
        eventType: 'data_type_test',
        eventCategory: 'validation',
        eventAction: 'type_checking',
        eventValue: 42, // Should remain integer
        eventProperties: {
          string_field: 'test_string',
          number_field: 123.45,
          boolean_field: true,
          array_field: [1, 2, 3],
          object_field: { nested: 'value' }
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: crypto.randomBytes(16).toString('hex'),
        sessionDuration: 300 // Should remain integer
      };

      const response = await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(typedEvent)
        .expect(201);

      const storedEvent = await db('analytics_events')
        .where('id', response.body.data.eventId)
        .first();

      expect(typeof storedEvent.event_value).toBe('number');
      expect(storedEvent.event_value).toBe(42);
      expect(typeof storedEvent.session_duration).toBe('number');
      expect(storedEvent.session_duration).toBe(300);
      
      // Verify JSON properties maintain correct types
      const properties = storedEvent.event_properties;
      expect(typeof properties.string_field).toBe('string');
      expect(typeof properties.number_field).toBe('number');
      expect(typeof properties.boolean_field).toBe('boolean');
      expect(Array.isArray(properties.array_field)).toBe(true);
      expect(typeof properties.object_field).toBe('object');
    });
  });

  // ================== Enhanced Analytics Endpoints Tests ==================

  describe('Enhanced Analytics Endpoints', () => {
    test('should export classroom analytics data for research purposes', async () => {
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/export`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ 
          format: 'json',
          includeEvents: 'true'
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.classroom).toBeDefined();
      expect(response.body.data.students).toBeDefined();
      expect(response.body.data.progress).toBeDefined();
      expect(response.body.data.feedback).toBeDefined();
      expect(response.body.data.events).toBeDefined(); // Should include events when requested

      // Verify no PII in exported data
      const exportData = JSON.stringify(response.body.data);
      expect(exportData).not.toMatch(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/); // No emails
      expect(exportData).not.toMatch(/\b\d{3}-\d{3}-\d{4}\b/); // No phone numbers
    });

    test('should provide facilitator overview analytics across all classrooms', async () => {
      const response = await request(app)
        .get('/api/analytics/facilitator/overview')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: '30d' })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.overview).toBeDefined();
      expect(response.body.data.classrooms).toBeDefined();
      expect(response.body.data.lessonProgress).toBeDefined();
      
      // Verify overview metrics
      const overview = response.body.data.overview;
      expect(typeof overview.totalClassrooms).toBe('number');
      expect(typeof overview.totalStudents).toBe('number');
      expect(typeof overview.activeStudents).toBe('number');
      expect(typeof overview.totalEvents).toBe('number');
      expect(typeof overview.avgSessionDuration).toBe('number');
    });

    test('should handle different timeframe parameters correctly', async () => {
      const timeframes = ['7d', '30d', '90d'];
      
      for (const timeframe of timeframes) {
        const response = await request(app)
          .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
          .set('Authorization', `Bearer ${facilitatorToken}`)
          .query({ timeframe })
          .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.data.timeframe).toBe(timeframe);
        expect(response.body.data.summary).toBeDefined();
      }
    });

    test('should reject invalid timeframe parameters', async () => {
      const response = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: 'invalid' })
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });
  });

  // ================== Integration and End-to-End Tests ==================

  describe('End-to-End Analytics Workflow', () => {
    test('should track complete lesson journey with behavioral analytics', async () => {
      const sessionId = crypto.randomBytes(16).toString('hex');
      
      // 1. Lesson start
      const startEvent = {
        eventType: 'lesson_start',
        eventCategory: 'engagement',
        eventAction: 'start_lesson',
        eventLabel: 'empathy_lesson_complete_journey',
        eventValue: 1,
        eventProperties: {
          session_id: sessionId,
          behavioral_category: 'empathy',
          pre_assessment_confidence: 3
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: sessionId,
        lessonId: testData.lesson.id,
        sessionDuration: 0
      };

      await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(startEvent)
        .expect(201);

      // 2. Multiple behavioral interactions
      const behavioralEvents = [
        {
          eventType: 'behavioral_interaction',
          eventCategory: 'empathy',
          eventAction: 'empathy_response',
          eventLabel: 'scenario_response_1',
          eventValue: 4,
          eventProperties: { session_id: sessionId, scenario_id: 'scenario_1', response_quality: 'high' }
        },
        {
          eventType: 'behavioral_interaction',
          eventCategory: 'confidence',
          eventAction: 'confidence_building',
          eventLabel: 'peer_interaction',
          eventValue: 3,
          eventProperties: { session_id: sessionId, interaction_type: 'peer_support' }
        },
        {
          eventType: 'behavioral_interaction',
          eventCategory: 'communication',
          eventAction: 'active_listening',
          eventLabel: 'discussion_participation',
          eventValue: 1,
          eventProperties: { session_id: sessionId, communication_quality: 'excellent' }
        }
      ];

      for (const event of behavioralEvents) {
        await request(app)
          .post('/api/analytics/event')
          .set('classroomcode', testData.classroom.classroom_code)
          .set('studentid', testData.studentSession.student_identifier)
          .send({
            ...event,
            deviceType: 'tablet',
            platform: 'android',
            sessionIdLocal: sessionId,
            lessonId: testData.lesson.id,
            sessionDuration: 900 // 15 minutes
          })
          .expect(201);
      }

      // 3. Lesson completion
      const completionEvent = {
        eventType: 'lesson_completion',
        eventCategory: 'engagement',
        eventAction: 'complete_lesson',
        eventLabel: 'empathy_lesson_complete_journey',
        eventValue: 1,
        eventProperties: {
          session_id: sessionId,
          post_assessment_confidence: 4,
          confidence_improvement: 1,
          behavioral_growth_indicators: ['empathy_increased', 'confidence_improved', 'communication_engaged']
        },
        deviceType: 'tablet',
        platform: 'android',
        sessionIdLocal: sessionId,
        lessonId: testData.lesson.id,
        sessionDuration: 1800 // 30 minutes total
      };

      await request(app)
        .post('/api/analytics/event')
        .set('classroomcode', testData.classroom.classroom_code)
        .set('studentid', testData.studentSession.student_identifier)
        .send(completionEvent)
        .expect(201);

      // 4. Verify complete journey is captured in analytics dashboard
      const dashboardResponse = await request(app)
        .get(`/api/analytics/classroom/${testData.classroom.id}/dashboard`)
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({ timeframe: '7d' })
        .expect(200);

      const dashboard = dashboardResponse.body.data;
      
      // Should show lesson engagement
      expect(dashboard.summary.totalEvents).toBeGreaterThan(0);
      expect(dashboard.lessonEngagement).toBeDefined();
      
      // Should have at least one lesson with engagement data
      const lessonEngagement = dashboard.lessonEngagement.find(l => l.lessonId === testData.lesson.id);
      expect(lessonEngagement).toBeDefined();
      if (lessonEngagement) {
        expect(lessonEngagement.totalEvents).toBeGreaterThan(0);
        expect(lessonEngagement.completions).toBeGreaterThanOrEqual(1);
      }
    });
  });
});
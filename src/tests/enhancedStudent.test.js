const request = require('supertest');
const app = require('../app');
const db = require('../config/database');

describe('Enhanced Student Features Tests', () => {
  let testFacilitator;
  let testClassroom;
  let authToken;

  beforeAll(async () => {
    // Create test facilitator and classroom
    const facilitatorResponse = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'teacher@school.edu',
        password: 'SecurePass123!',
        firstName: 'Test',
        lastName: 'Teacher',
        organization: 'Test Elementary'
      });

    testFacilitator = facilitatorResponse.body.facilitator;
    authToken = facilitatorResponse.body.token;

    const classroomResponse = await request(app)
      .post('/api/classrooms')
      .set('Authorization', `Bearer ${authToken}`)
      .send({
        name: 'Mrs. Teacher\'s 4th Grade',
        description: 'Test classroom for hero learning',
        gradeLevel: 4
      });

    testClassroom = classroomResponse.body.classroom;
  });

  afterAll(async () => {
    // Clean up test data
    await db('student_feedback').del();
    await db('students').del();
    await db('classrooms').del();
    await db('facilitators').del();
    await db.destroy();
  });

  describe('Classroom Preview Feature', () => {
    test('should return classroom preview with valid code', async () => {
      const response = await request(app)
        .get(`/api/enhanced-students/classroom-preview/${testClassroom.classroomCode}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.classroom).toBeDefined();
      expect(response.body.classroom.name).toBe('Mrs. Teacher\'s 4th Grade');
      expect(response.body.classroom.gradeLevel).toBe(4);
      expect(response.body.classroom.facilitatorName).toBe('Test Teacher');
      expect(response.body.classroom.welcomeMessage).toContain('Welcome to');
    });

    test('should return 404 for invalid classroom code', async () => {
      const response = await request(app)
        .get('/api/enhanced-students/classroom-preview/INVALID')
        .expect(404);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('Classroom not found or inactive');
    });

    test('should validate classroom code format', async () => {
      const response = await request(app)
        .get('/api/enhanced-students/classroom-preview/123')
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });
  });

  describe('Emotional Check-in Feature', () => {
    let testStudent;

    beforeEach(async () => {
      // Create test student
      const studentResponse = await request(app)
        .post('/api/students/enroll')
        .send({
          classroomCode: testClassroom.classroomCode,
          demographicData: {
            grade: 4,
            ageRange: '9-10'
          }
        });

      testStudent = studentResponse.body.student;
    });

    test('should accept valid emotional check-in', async () => {
      const checkinData = {
        checkinType: 'lesson_start',
        emotionalState: {
          primary_emotion: 'excited',
          energy_level: 4,
          confidence_level: 3,
          emoji_selection: '🌟'
        },
        visualResponse: {
          mood_color: '#FFD700',
          weather_metaphor: 'sunny'
        }
      };

      const response = await request(app)
        .post('/api/enhanced-students/emotional-checkin')
        .set('studentId', testStudent.anonymousId)
        .send(checkinData)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toContain('excitement');
      expect(response.body.visualFeedback).toBeDefined();
      expect(response.body.visualFeedback.icon).toBe('🌟');
    });

    test('should validate emotional state values', async () => {
      const invalidCheckinData = {
        checkinType: 'lesson_start',
        emotionalState: {
          primary_emotion: 'invalid_emotion',
          energy_level: 6, // Invalid: should be 1-5
          confidence_level: 0 // Invalid: should be 1-5
        }
      };

      const response = await request(app)
        .post('/api/enhanced-students/emotional-checkin')
        .set('studentId', testStudent.anonymousId)
        .send(invalidCheckinData)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });

    test('should handle different emotional states appropriately', async () => {
      const emotions = ['excited', 'happy', 'calm', 'nervous'];
      
      for (const emotion of emotions) {
        const checkinData = {
          checkinType: 'lesson_start',
          emotionalState: {
            primary_emotion: emotion,
            energy_level: 3,
            confidence_level: 3
          },
          visualResponse: {
            mood_color: '#FFD700'
          }
        };

        const response = await request(app)
          .post('/api/enhanced-students/emotional-checkin')
          .set('studentId', testStudent.anonymousId)
          .send(checkinData)
          .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBeDefined();
      }
    });
  });

  describe('Mobile-Optimized Lesson Content', () => {
    let testLesson;

    beforeAll(async () => {
      // Get a sample lesson
      const lessons = await db('lessons').where({ is_published: true }).first();
      testLesson = lessons;
    });

    test('should return mobile-optimized lesson content', async () => {
      if (!testLesson) {
        // Skip if no lessons in database
        return;
      }

      const response = await request(app)
        .get(`/api/enhanced-students/lessons/${testLesson.id}/mobile-optimized`)
        .query({ deviceType: 'phone' })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.lesson).toBeDefined();
      expect(response.body.lesson.microSessions).toBeDefined();
      expect(response.body.lesson.visualProgressMilestones).toBeDefined();
      expect(response.body.lesson.mobileFeatures).toBeDefined();
      expect(response.body.lesson.mobileFeatures.deviceType).toBe('phone');
    });

    test('should validate lesson ID format', async () => {
      const response = await request(app)
        .get('/api/enhanced-students/lessons/invalid-id/mobile-optimized')
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });

    test('should return 404 for non-existent lesson', async () => {
      const fakeUuid = '123e4567-e89b-12d3-a456-426614174000';
      
      const response = await request(app)
        .get(`/api/enhanced-students/lessons/${fakeUuid}/mobile-optimized`)
        .expect(404);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('Lesson not found');
    });
  });

  describe('COPPA Compliance in Enhanced Features', () => {
    test('should not expose student PII in emotional check-ins', async () => {
      // Create student and submit check-in
      const studentResponse = await request(app)
        .post('/api/students/enroll')
        .send({
          classroomCode: testClassroom.classroomCode,
          demographicData: {
            grade: 4,
            ageRange: '9-10'
          }
        });

      const checkinData = {
        checkinType: 'lesson_start',
        emotionalState: {
          primary_emotion: 'excited',
          energy_level: 4,
          confidence_level: 3
        }
      };

      await request(app)
        .post('/api/enhanced-students/emotional-checkin')
        .set('studentId', studentResponse.body.student.anonymousId)
        .send(checkinData)
        .expect(200);

      // Verify no PII in stored feedback
      const feedback = await db('student_feedback')
        .where({ student_session_id: studentResponse.body.student.id })
        .first();

      expect(feedback).toBeDefined();
      
      const content = JSON.parse(feedback.content);
      expect(content).not.toHaveProperty('name');
      expect(content).not.toHaveProperty('email');
      expect(content).not.toHaveProperty('address');
    });

    test('should only collect age-appropriate demographic data', async () => {
      const response = await request(app)
        .get(`/api/enhanced-students/classroom-preview/${testClassroom.classroomCode}`)
        .expect(200);

      // Verify classroom preview doesn't expose sensitive data
      expect(response.body.classroom).not.toHaveProperty('studentNames');
      expect(response.body.classroom).not.toHaveProperty('studentEmails');
      expect(response.body.classroom.activeStudentCount).toBeGreaterThanOrEqual(0);
    });
  });
});
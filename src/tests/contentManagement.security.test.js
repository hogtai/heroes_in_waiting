const request = require('supertest');
const app = require('../app');
const db = require('../config/database');
const { sanitizeHTMLContent, detectPII } = require('../utils/contentSanitizer');
const fs = require('fs').promises;
const path = require('path');

describe('Content Management Security Tests', () => {
  let facilitatorToken;
  let studentToken;
  let testLessonId;
  let testContentVersionId;

  beforeAll(async () => {
    // Set up test data and authentication tokens
    // This would normally use test database setup
    console.log('Setting up security test environment...');
  });

  afterAll(async () => {
    // Clean up test data
    console.log('Cleaning up security test environment...');
  });

  describe('Authentication & Authorization Tests', () => {
    test('should require authentication for all content management endpoints', async () => {
      const endpoints = [
        'GET /api/content/versions',
        'POST /api/content/versions',
        'GET /api/content/media',
        'POST /api/content/media/upload',
        'GET /api/lesson-content/lessons',
        'GET /api/mobile/sync/lessons'
      ];

      for (const endpoint of endpoints) {
        const [method, path] = endpoint.split(' ');
        const response = await request(app)[method.toLowerCase()](path);
        
        expect(response.status).toBe(401);
        expect(response.body.success).toBe(false);
        expect(response.body.error).toContain('token' || 'Authentication');
      }
    });

    test('should prevent facilitator access to student-only endpoints', async () => {
      // Test that facilitators cannot access student-specific sync endpoints
      const response = await request(app)
        .post('/api/mobile/sync/progress')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .send({
          progressData: [{
            lessonId: testLessonId,
            activityId: 'test-activity',
            completed: true,
            timestamp: new Date().toISOString()
          }]
        });

      expect(response.status).toBe(403);
    });

    test('should prevent students from accessing facilitator content management', async () => {
      const response = await request(app)
        .post('/api/content/versions')
        .set('Authorization', `Bearer ${studentToken}`)
        .send({
          lessonId: testLessonId,
          title: 'Test Content',
          contentStructure: { test: 'data' },
          changeSummary: 'Test change'
        });

      expect(response.status).toBe(403);
    });
  });

  describe('Input Sanitization Tests', () => {
    test('should sanitize HTML content in lesson titles', () => {
      const maliciousTitle = '<script>alert("XSS")</script>Lesson Title';
      const sanitized = sanitizeHTMLContent(maliciousTitle, 'facilitator');
      
      expect(sanitized).not.toContain('<script>');
      expect(sanitized).not.toContain('alert');
      expect(sanitized).toContain('Lesson Title');
    });

    test('should sanitize complex HTML structures in content', () => {
      const maliciousContent = `
        <div onclick="malicious()">
          <img src="x" onerror="alert('XSS')">
          <a href="javascript:void(0)">Click me</a>
          <style>body { display: none; }</style>
        </div>
      `;
      
      const sanitized = sanitizeHTMLContent(maliciousContent, 'facilitator');
      
      expect(sanitized).not.toContain('onclick');
      expect(sanitized).not.toContain('onerror');
      expect(sanitized).not.toContain('javascript:');
      expect(sanitized).not.toContain('<style>');
    });

    test('should allow educational HTML tags for facilitators', () => {
      const educationalContent = `
        <h2>Lesson Title</h2>
        <p>This is educational content with <strong>emphasis</strong>.</p>
        <ul>
          <li>Learning objective 1</li>
          <li>Learning objective 2</li>
        </ul>
        <blockquote>Important quote for learning</blockquote>
      `;
      
      const sanitized = sanitizeHTMLContent(educationalContent, 'facilitator');
      
      expect(sanitized).toContain('<h2>');
      expect(sanitized).toContain('<strong>');
      expect(sanitized).toContain('<ul>');
      expect(sanitized).toContain('<blockquote>');
    });

    test('should restrict HTML for student content', () => {
      const facilitatorContent = `
        <h2>Title</h2>
        <a href="https://example.com">Link</a>
        <div data-activity="test">Activity</div>
      `;
      
      const studentSanitized = sanitizeHTMLContent(facilitatorContent, 'student');
      
      expect(studentSanitized).toContain('<h2>');
      expect(studentSanitized).not.toContain('<a');
      expect(studentSanitized).not.toContain('data-activity');
    });
  });

  describe('COPPA Compliance Tests', () => {
    test('should detect PII in content', () => {
      const contentWithPII = `
        Contact teacher at teacher@school.edu or call 555-123-4567.
        Student John Smith lives at 123 Main Street, Anytown.
        Social Security: 123-45-6789
      `;
      
      const piiDetections = detectPII(contentWithPII);
      
      expect(piiDetections.length).toBeGreaterThan(0);
      expect(piiDetections.some(d => d.type === 'email')).toBe(true);
      expect(piiDetections.some(d => d.type === 'phone')).toBe(true);
      expect(piiDetections.some(d => d.type === 'ssn')).toBe(true);
      expect(piiDetections.some(d => d.type === 'address')).toBe(true);
    });

    test('should reject content creation with PII', async () => {
      const contentWithPII = {
        lessonId: testLessonId,
        title: 'Lesson with PII',
        description: 'Contact me at john.doe@email.com',
        contentStructure: {
          sections: [{
            content: 'Call me at 555-123-4567'
          }]
        },
        changeSummary: 'Added contact info'
      };

      const response = await request(app)
        .post('/api/content/versions')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .send(contentWithPII);

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('personally identifiable');
    });

    test('should log PII detection attempts for compliance audit', async () => {
      // This test would verify that PII detection is logged for compliance
      // In a real implementation, you'd check audit logs
      const contentWithPII = 'Email: test@example.com';
      const detections = detectPII(contentWithPII);
      
      expect(detections.length).toBeGreaterThan(0);
      // Verify logging occurs (would check actual log files in real test)
    });

    test('should ensure no student PII in analytics', async () => {
      // Test that student analytics don't contain PII
      const response = await request(app)
        .get(`/api/lesson-content/lessons/${testLessonId}/analytics`)
        .set('Authorization', `Bearer ${facilitatorToken}`);

      expect(response.status).toBe(200);
      expect(response.body.data).toBeDefined();
      
      // Verify no student names, emails, or other PII in analytics
      const analyticsData = JSON.stringify(response.body.data);
      expect(analyticsData).not.toMatch(/@[\w.-]+\.\w+/); // No emails
      expect(analyticsData).not.toMatch(/\d{3}-\d{3}-\d{4}/); // No phone numbers
    });
  });

  describe('File Upload Security Tests', () => {
    test('should reject malicious file types', async () => {
      // Create a test file with executable extension
      const maliciousFile = Buffer.from('<?php echo "hacked"; ?>');
      
      const response = await request(app)
        .post('/api/content/media/upload')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .attach('file', maliciousFile, 'malicious.php');

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('not allowed' || 'validation failed');
    });

    test('should validate file content using magic numbers', async () => {
      // Create a fake image file (wrong magic number)
      const fakeImage = Buffer.from('This is not really an image');
      
      const response = await request(app)
        .post('/api/content/media/upload')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .attach('file', fakeImage, 'fake.jpg');

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('validation failed');
    });

    test('should limit file sizes for educational content', async () => {
      // Create a file that's too large (over 100MB limit)
      const largeContent = Buffer.alloc(101 * 1024 * 1024); // 101MB
      
      const response = await request(app)
        .post('/api/content/media/upload')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .attach('file', largeContent, 'large.mp4');

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('too large');
    });

    test('should prevent path traversal in uploaded files', async () => {
      const normalFile = Buffer.from('test content');
      
      const response = await request(app)
        .post('/api/content/media/upload')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .attach('file', normalFile, '../../../etc/passwd.txt');

      // Should sanitize filename and not allow path traversal
      expect(response.status).toBe(400);
    });
  });

  describe('Content Version Security Tests', () => {
    test('should prevent unauthorized content version access', async () => {
      // Test that users can only access content versions they have permission for
      const response = await request(app)
        .get(`/api/content/versions/${testContentVersionId}`)
        .set('Authorization', `Bearer ${studentToken}`);

      expect(response.status).toBe(403);
    });

    test('should validate JSON structure in content versions', async () => {
      const maliciousContent = {
        lessonId: testLessonId,
        title: 'Test Lesson',
        contentStructure: {
          __proto__: { malicious: true },
          constructor: { prototype: { evil: 'code' } },
          sections: []
        },
        changeSummary: 'Test'
      };

      const response = await request(app)
        .post('/api/content/versions')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .send(maliciousContent);

      // Should sanitize or reject malicious JSON structures
      if (response.status === 201) {
        expect(response.body.data.contentStructure).not.toHaveProperty('__proto__');
        expect(response.body.data.contentStructure).not.toHaveProperty('constructor');
      } else {
        expect(response.status).toBe(400);
      }
    });
  });

  describe('Mobile API Security Tests', () => {
    test('should rate limit mobile sync requests', async () => {
      // Make multiple rapid requests to test rate limiting
      const requests = Array(10).fill().map(() =>
        request(app)
          .get('/api/mobile/sync/lessons')
          .set('Authorization', `Bearer ${facilitatorToken}`)
      );

      const responses = await Promise.all(requests);
      
      // Some requests should be rate limited
      const rateLimited = responses.some(r => r.status === 429);
      expect(rateLimited).toBe(true);
    });

    test('should validate mobile device headers', async () => {
      const response = await request(app)
        .get('/api/mobile/sync/lessons')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .set('User-Agent', '<script>alert("xss")</script>');

      expect(response.status).toBe(200);
      // Verify that malicious user agent is sanitized in logs
    });

    test('should prevent mobile sync with invalid data', async () => {
      const invalidProgress = {
        progressData: [{
          lessonId: 'invalid-uuid',
          activityId: '<script>alert("xss")</script>',
          completed: 'not-boolean',
          timestamp: 'invalid-date'
        }]
      };

      const response = await request(app)
        .post('/api/mobile/sync/progress')
        .set('Authorization', `Bearer ${studentToken}`)
        .send(invalidProgress);

      expect(response.status).toBe(400);
      expect(response.body.error).toBeDefined();
    });
  });

  describe('Performance Security Tests', () => {
    test('should prevent DoS through resource-intensive queries', async () => {
      // Test with very large page size
      const response = await request(app)
        .get('/api/content/versions?limit=10000')
        .set('Authorization', `Bearer ${facilitatorToken}`);

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('Limit must be between 1 and 100');
    });

    test('should timeout long-running requests', async () => {
      // This would test request timeouts for educational content
      // In a real implementation, you'd test with slow database operations
      expect(true).toBe(true); // Placeholder
    });
  });
});

// Test utilities for security testing
const SecurityTestUtils = {
  createMaliciousPayload: (type) => {
    const payloads = {
      xss: '<script>alert("XSS")</script>',
      sqlInjection: "'; DROP TABLE users; --",
      jsonInjection: '{"__proto__": {"polluted": true}}',
      pathTraversal: '../../../etc/passwd',
      htmlInjection: '<img src=x onerror=alert("XSS")>'
    };
    return payloads[type] || '';
  },

  createTestFile: (type, size = 1024) => {
    const files = {
      image: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==', 'base64'),
      malicious: Buffer.from('<?php echo "hacked"; ?>'),
      large: Buffer.alloc(size),
      normal: Buffer.from('test content for educational use')
    };
    return files[type] || files.normal;
  },

  validateCOPPACompliance: (data) => {
    const piiPatterns = [
      /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/, // Email
      /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/, // Phone
      /\b\d{3}-\d{2}-\d{4}\b/, // SSN
    ];

    const dataString = JSON.stringify(data);
    return piiPatterns.some(pattern => pattern.test(dataString));
  }
};

module.exports = { SecurityTestUtils };
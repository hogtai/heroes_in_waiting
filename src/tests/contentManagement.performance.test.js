const request = require('supertest');
const app = require('../app');
const db = require('../config/database');
const { createTestContent, cleanupTestData } = require('./testUtils');

describe('Content Management Performance Tests', () => {
  let facilitatorToken;
  let testData = {};

  beforeAll(async () => {
    // Set up test environment
    facilitatorToken = process.env.TEST_FACILITATOR_TOKEN || 'test-token';
    testData = await createTestContent();
  });

  afterAll(async () => {
    await cleanupTestData(testData);
  });

  describe('Concurrent Content Operations', () => {
    test('should handle multiple simultaneous content version creates', async () => {
      const concurrentRequests = 10;
      const promises = [];

      const startTime = Date.now();

      // Create multiple content versions simultaneously
      for (let i = 0; i < concurrentRequests; i++) {
        promises.push(
          request(app)
            .post('/api/content/versions')
            .set('Authorization', `Bearer ${facilitatorToken}`)
            .send({
              lessonId: testData.lessonId,
              title: `Concurrent Test Lesson ${i}`,
              description: `Performance test description ${i}`,
              contentStructure: {
                sections: [{
                  id: `section-${i}`,
                  title: `Section ${i}`,
                  content: `Educational content for performance testing ${i}`
                }]
              },
              changeSummary: `Performance test change ${i}`
            })
        );
      }

      const responses = await Promise.all(promises);
      const endTime = Date.now();
      const totalTime = endTime - startTime;

      // Validate all requests succeeded
      responses.forEach((response, index) => {
        expect(response.status).toBe(201);
        expect(response.body.success).toBe(true);
        expect(response.body.data.title).toContain(`${index}`);
      });

      // Performance assertions
      expect(totalTime).toBeLessThan(15000); // Should complete within 15 seconds
      const avgResponseTime = totalTime / concurrentRequests;
      expect(avgResponseTime).toBeLessThan(3000); // Average under 3 seconds per request

      console.log(`Concurrent content creation performance: ${totalTime}ms total, ${avgResponseTime}ms average`);
    });

    test('should handle concurrent media file uploads', async () => {
      const concurrentUploads = 5;
      const promises = [];
      const testImageBuffer = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==', 'base64');

      const startTime = Date.now();

      for (let i = 0; i < concurrentUploads; i++) {
        promises.push(
          request(app)
            .post('/api/content/media/upload')
            .set('Authorization', `Bearer ${facilitatorToken}`)
            .attach('file', testImageBuffer, `test-image-${i}.png`)
            .field('description', `Performance test image ${i}`)
            .field('tags', 'performance,test,educational')
        );
      }

      const responses = await Promise.all(promises);
      const endTime = Date.now();
      const totalTime = endTime - startTime;

      // Validate uploads
      responses.forEach(response => {
        expect(response.status).toBe(201);
        expect(response.body.success).toBe(true);
        expect(response.body.data.fileUrl).toBeDefined();
      });

      // Performance validation
      expect(totalTime).toBeLessThan(20000); // 20 seconds for 5 uploads
      console.log(`Concurrent media upload performance: ${totalTime}ms for ${concurrentUploads} files`);
    });

    test('should maintain database integrity under concurrent operations', async () => {
      const concurrentOperations = 8;
      const promises = [];

      // Mix of different operations
      for (let i = 0; i < concurrentOperations; i++) {
        if (i % 2 === 0) {
          // Content version creation
          promises.push(
            request(app)
              .post('/api/content/versions')
              .set('Authorization', `Bearer ${facilitatorToken}`)
              .send({
                lessonId: testData.lessonId,
                title: `DB Integrity Test ${i}`,
                contentStructure: { test: `data-${i}` },
                changeSummary: `Test ${i}`
              })
          );
        } else {
          // Content approval
          promises.push(
            request(app)
              .post(`/api/content/versions/${testData.contentVersionId}/approve`)
              .set('Authorization', `Bearer ${facilitatorToken}`)
              .send({
                approved: i % 4 === 1,
                comments: `Concurrent approval test ${i}`
              })
          );
        }
      }

      const responses = await Promise.all(promises);

      // Verify database consistency
      const versionCount = await db('content_versions').count().first();
      const approvalCount = await db('content_approvals').count().first();

      expect(parseInt(versionCount.count)).toBeGreaterThan(0);
      expect(parseInt(approvalCount.count)).toBeGreaterThan(0);

      // All operations should complete successfully
      responses.forEach(response => {
        expect([200, 201]).toContain(response.status);
      });
    });
  });

  describe('Content Query Performance', () => {
    test('should efficiently query large content datasets', async () => {
      const startTime = Date.now();

      // Query all content versions with filters
      const response = await request(app)
        .get('/api/content/versions')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({
          limit: 50,
          offset: 0,
          status: 'published',
          sortBy: 'updated_at',
          sortOrder: 'desc'
        });

      const queryTime = Date.now() - startTime;

      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(queryTime).toBeLessThan(2000); // Should respond within 2 seconds

      console.log(`Content query performance: ${queryTime}ms for filtered results`);
    });

    test('should handle complex lesson content aggregation efficiently', async () => {
      const startTime = Date.now();

      const response = await request(app)
        .get(`/api/lesson-content/lessons/${testData.lessonId}/analytics`)
        .set('Authorization', `Bearer ${facilitatorToken}`);

      const aggregationTime = Date.now() - startTime;

      expect(response.status).toBe(200);
      expect(response.body.data).toBeDefined();
      expect(aggregationTime).toBeLessThan(3000); // Complex queries under 3 seconds

      console.log(`Lesson analytics aggregation performance: ${aggregationTime}ms`);
    });

    test('should optimize media file listing performance', async () => {
      const startTime = Date.now();

      const response = await request(app)
        .get('/api/content/media')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .query({
          limit: 100,
          type: 'image',
          sortBy: 'created_at'
        });

      const listingTime = Date.now() - startTime;

      expect(response.status).toBe(200);
      expect(listingTime).toBeLessThan(1500); // Media listing under 1.5 seconds

      console.log(`Media listing performance: ${listingTime}ms for 100 items`);
    });
  });

  describe('Memory and Resource Usage', () => {
    test('should not leak memory during content operations', async () => {
      const initialMemory = process.memoryUsage();
      
      // Perform multiple operations
      for (let i = 0; i < 20; i++) {
        await request(app)
          .get('/api/lesson-content/lessons')
          .set('Authorization', `Bearer ${facilitatorToken}`);
      }

      const finalMemory = process.memoryUsage();
      const memoryIncrease = finalMemory.heapUsed - initialMemory.heapUsed;
      
      // Memory increase should be reasonable (less than 50MB)
      expect(memoryIncrease).toBeLessThan(50 * 1024 * 1024);
      
      console.log(`Memory usage increase: ${Math.round(memoryIncrease / 1024 / 1024)}MB`);
    });

    test('should handle large content payloads efficiently', async () => {
      const largeContent = {
        lessonId: testData.lessonId,
        title: 'Large Content Test',
        description: 'A' * 10000, // 10KB description
        contentStructure: {
          sections: Array(100).fill().map((_, i) => ({
            id: `section-${i}`,
            title: `Section ${i}`,
            content: 'B' * 1000 // 1KB per section
          }))
        },
        changeSummary: 'Large content performance test'
      };

      const startTime = Date.now();
      
      const response = await request(app)
        .post('/api/content/versions')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .send(largeContent);

      const processingTime = Date.now() - startTime;

      expect(response.status).toBe(201);
      expect(processingTime).toBeLessThan(5000); // Large content under 5 seconds

      console.log(`Large content processing: ${processingTime}ms for ~100KB payload`);
    });
  });

  describe('Database Performance Monitoring', () => {
    test('should track content operation performance metrics', async () => {
      const startTime = Date.now();

      // Perform operations that should be tracked
      await request(app)
        .get(`/api/lesson-content/lessons/${testData.lessonId}`)
        .set('Authorization', `Bearer ${facilitatorToken}`);

      await request(app)
        .post('/api/content/versions')
        .set('Authorization', `Bearer ${facilitatorToken}`)
        .send({
          lessonId: testData.lessonId,
          title: 'Performance Tracking Test',
          contentStructure: { tracked: true },
          changeSummary: 'Tracking test'
        });

      // Verify performance data was recorded
      const performanceRecords = await db('performance_metrics')
        .where('timestamp', '>=', new Date(startTime))
        .where('content_type', 'lesson_content');

      expect(performanceRecords.length).toBeGreaterThan(0);
      
      // Verify performance alerts for slow operations
      const alerts = await db('performance_alerts')
        .where('created_at', '>=', new Date(startTime));

      console.log(`Performance tracking: ${performanceRecords.length} metrics, ${alerts.length} alerts`);
    });

    test('should maintain optimal database connection pool usage', async () => {
      const promises = [];
      
      // Create 20 simultaneous database operations
      for (let i = 0; i < 20; i++) {
        promises.push(
          request(app)
            .get('/api/content/versions')
            .set('Authorization', `Bearer ${facilitatorToken}`)
            .query({ limit: 1 })
        );
      }

      const startTime = Date.now();
      const responses = await Promise.all(promises);
      const totalTime = Date.now() - startTime;

      // All should succeed
      responses.forEach(response => {
        expect(response.status).toBe(200);
      });

      // Should handle connection pooling efficiently
      expect(totalTime).toBeLessThan(10000); // 20 queries under 10 seconds
      
      console.log(`Connection pooling performance: ${totalTime}ms for 20 concurrent queries`);
    });
  });
});
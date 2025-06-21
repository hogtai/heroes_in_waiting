const request = require('supertest');
const app = require('../app');
const db = require('../config/database');

describe('Authentication System Tests', () => {
  
  beforeEach(async () => {
    // Clean up test data
    await db('facilitators').del();
  });

  afterAll(async () => {
    // Close database connection
    await db.destroy();
  });

  describe('Facilitator Registration', () => {
    test('should register a new facilitator with valid data', async () => {
      const facilitatorData = {
        email: 'test@school.edu',
        password: 'SecurePass123!',
        firstName: 'John',
        lastName: 'Smith',
        organization: 'Test Elementary'
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(facilitatorData)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.facilitator.email).toBe(facilitatorData.email);
      expect(response.body.token).toBeDefined();
    });

    test('should reject registration with weak password', async () => {
      const facilitatorData = {
        email: 'test@school.edu',
        password: 'weak',
        firstName: 'John',
        lastName: 'Smith',
        organization: 'Test Elementary'
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(facilitatorData)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.errors).toBeDefined();
    });

    test('should reject duplicate email registration', async () => {
      const facilitatorData = {
        email: 'duplicate@school.edu',
        password: 'SecurePass123!',
        firstName: 'John',
        lastName: 'Smith',
        organization: 'Test Elementary'
      };

      // First registration
      await request(app)
        .post('/api/auth/register')
        .send(facilitatorData)
        .expect(201);

      // Duplicate registration
      const response = await request(app)
        .post('/api/auth/register')
        .send(facilitatorData)
        .expect(400);

      expect(response.body.success).toBe(false);
    });
  });

  describe('Facilitator Login', () => {
    beforeEach(async () => {
      // Create test facilitator
      await request(app)
        .post('/api/auth/register')
        .send({
          email: 'login@school.edu',
          password: 'SecurePass123!',
          firstName: 'Test',
          lastName: 'User',
          organization: 'Test School'
        });
    });

    test('should login with valid credentials', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'login@school.edu',
          password: 'SecurePass123!'
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.token).toBeDefined();
      expect(response.body.facilitator.email).toBe('login@school.edu');
    });

    test('should reject login with invalid password', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'login@school.edu',
          password: 'wrongpassword'
        })
        .expect(401);

      expect(response.body.success).toBe(false);
    });

    test('should reject login with non-existent email', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'nonexistent@school.edu',
          password: 'SecurePass123!'
        })
        .expect(401);

      expect(response.body.success).toBe(false);
    });
  });

  describe('JWT Token Validation', () => {
    let authToken;

    beforeEach(async () => {
      // Register and login to get token
      await request(app)
        .post('/api/auth/register')
        .send({
          email: 'token@school.edu',
          password: 'SecurePass123!',
          firstName: 'Token',
          lastName: 'Test',
          organization: 'Test School'
        });

      const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'token@school.edu',
          password: 'SecurePass123!'
        });

      authToken = loginResponse.body.token;
    });

    test('should access protected route with valid token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.facilitator.email).toBe('token@school.edu');
    });

    test('should reject protected route without token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .expect(401);

      expect(response.body.success).toBe(false);
    });

    test('should reject protected route with invalid token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid-token')
        .expect(401);

      expect(response.body.success).toBe(false);
    });
  });
});
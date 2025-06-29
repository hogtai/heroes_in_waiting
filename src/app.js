require('dotenv').config();

// Security: Validate critical environment variables
const requiredEnvVars = ['JWT_SECRET', 'DB_PASSWORD'];
requiredEnvVars.forEach(envVar => {
  if (!process.env[envVar]) {
    console.error(`SECURITY ERROR: Required environment variable ${envVar} is not set`);
    process.exit(1);
  }
});

// Security: Validate JWT secret strength
if (process.env.JWT_SECRET.length < 32) {
  console.error('SECURITY ERROR: JWT_SECRET must be at least 32 characters for security');
  process.exit(1);
}
const express = require('express');
const cors = require('cors');
const compression = require('compression');
const rateLimit = require('express-rate-limit');

// Import middleware
const errorHandler = require('./middleware/errorHandler');
const logger = require('./utils/logger');
const { validateRequest } = require('./middleware/validation');
const securityMiddleware = require('./middleware/security');

// Import routes
const authRoutes = require('./routes/auth');
const facilitatorRoutes = require('./routes/facilitators');
const studentRoutes = require('./routes/students');
const classroomRoutes = require('./routes/classrooms');
const lessonRoutes = require('./routes/lessons');
const progressRoutes = require('./routes/progress');
const analyticsRoutes = require('./routes/analytics');
const enhancedStudentRoutes = require('./routes/enhancedStudent');
const contentManagementRoutes = require('./routes/contentManagement');
const lessonContentRoutes = require('./routes/lessonContent');
const testRoutes = require('./routes/test');

// Import API documentation
const swaggerSpec = require('./config/swagger');
const swaggerUi = require('swagger-ui-express');

// Database connection
const db = require('./config/database');

// Initialize Express app
const app = express();

// Enhanced security middleware
app.use(securityMiddleware);

// CORS configuration
const corsOptions = {
  origin: process.env.CORS_ORIGIN || 'http://localhost:3000',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
};
app.use(cors(corsOptions));

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15 minutes
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100, // limit each IP to 100 requests per windowMs
  message: {
    error: 'Too many requests from this IP, please try again later.'
  },
  standardHeaders: true,
  legacyHeaders: false
});
app.use('/api', limiter);

// Body parsing middleware
app.use(compression());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request logging
app.use((req, res, next) => {
  logger.info(`${req.method} ${req.path}`, {
    ip: req.ip,
    userAgent: req.get('User-Agent')
  });
  next();
});

// Health check endpoint
app.get('/health', async (req, res) => {
  try {
    // Basic health check
    const healthStatus = {
      status: 'ok',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      environment: process.env.NODE_ENV || 'development',
      services: {}
    };

    // Database health check
    try {
      await db.raw('SELECT 1');
      healthStatus.services.database = { status: 'healthy', timestamp: new Date().toISOString() };
    } catch (error) {
      healthStatus.services.database = { status: 'unhealthy', error: error.message, timestamp: new Date().toISOString() };
      healthStatus.status = 'degraded';
    }

    // Content management tables health check
    try {
      await db('content_versions').count('* as count').first();
      await db('media_files').count('* as count').first();
      await db('content_approvals').count('* as count').first();
      healthStatus.services.contentManagement = { status: 'healthy', timestamp: new Date().toISOString() };
    } catch (error) {
      healthStatus.services.contentManagement = { status: 'unhealthy', error: error.message, timestamp: new Date().toISOString() };
      healthStatus.status = 'degraded';
    }

    const httpStatus = healthStatus.status === 'ok' ? 200 : 503;
    res.status(httpStatus).json(healthStatus);
  } catch (error) {
    res.status(503).json({
      status: 'unhealthy',
      timestamp: new Date().toISOString(),
      error: error.message
    });
  }
});

// API Documentation
if (process.env.API_DOCS_ENABLED === 'true') {
  app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
}

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/facilitators', facilitatorRoutes);
app.use('/api/students', studentRoutes);
app.use('/api/classrooms', classroomRoutes);
app.use('/api/lessons', lessonRoutes);
app.use('/api/progress', progressRoutes);
app.use('/api/analytics', analyticsRoutes);
app.use('/api/enhanced-students', enhancedStudentRoutes);
app.use('/api/content', contentManagementRoutes);
app.use('/api/lesson-content', lessonContentRoutes);
app.use('/api/test', testRoutes);

// 404 handler for API routes
app.use('/api/*', (req, res) => {
  res.status(404).json({
    error: 'API endpoint not found',
    path: req.path,
    method: req.method
  });
});

// Global error handler
app.use(errorHandler);

// Database connection test
async function testDatabaseConnection() {
  try {
    await db.raw('SELECT 1');
    logger.info('Database connection established successfully');
  } catch (error) {
    logger.error('Database connection failed:', error);
    process.exit(1);
  }
}

// Start server
const PORT = process.env.PORT || 3000;

async function startServer() {
  try {
    await testDatabaseConnection();
    
    app.listen(PORT, () => {
      logger.info(`Heroes in Waiting API server running on port ${PORT}`);
      logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
      
      if (process.env.API_DOCS_ENABLED === 'true') {
        logger.info(`API Documentation available at http://localhost:${PORT}/api-docs`);
      }
    });
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Handle graceful shutdown
process.on('SIGINT', async () => {
  logger.info('Shutting down server...');
  try {
    await db.destroy();
    logger.info('Database connections closed');
    process.exit(0);
  } catch (error) {
    logger.error('Error during shutdown:', error);
    process.exit(1);
  }
});

process.on('SIGTERM', async () => {
  logger.info('SIGTERM received, shutting down gracefully...');
  try {
    await db.destroy();
    process.exit(0);
  } catch (error) {
    logger.error('Error during SIGTERM shutdown:', error);
    process.exit(1);
  }
});

// Start the server
if (require.main === module) {
  startServer();
}

module.exports = app;
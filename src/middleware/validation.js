const { body, param, query, validationResult } = require('express-validator');
const logger = require('../utils/logger');

/**
 * Input sanitization middleware to prevent XSS attacks
 */
function sanitizeInput(req, res, next) {
    // Sanitize all string inputs in body
    if (req.body) {
        Object.keys(req.body).forEach(key => {
            if (typeof req.body[key] === 'string') {
                req.body[key] = req.body[key]
                    .replace(/[<>]/g, '') // Remove potential HTML tags
                    .trim();
            }
        });
    }
    
    // Sanitize all string inputs in query
    if (req.query) {
        Object.keys(req.query).forEach(key => {
            if (typeof req.query[key] === 'string') {
                req.query[key] = req.query[key]
                    .replace(/[<>]/g, '') // Remove potential HTML tags
                    .trim();
            }
        });
    }
    
    next();
}

/**
 * Middleware to handle validation errors
 */
function validateRequest(req, res, next) {
  const errors = validationResult(req);
  
  if (!errors.isEmpty()) {
    logger.warn('Validation failed:', {
      errors: errors.array(),
      url: req.url,
      method: req.method,
      ip: req.ip
    });
    
    return res.status(400).json({
      success: false,
      error: 'Validation failed',
      details: errors.array()
    });
  }
  
  next();
}

/**
 * Validation rules for facilitator registration
 */
const validateFacilitatorRegistration = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Valid email is required'),
  body('password')
    .isLength({ min: 8 })
    .withMessage('Password must be at least 8 characters long')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
    .withMessage('Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character'),
  body('firstName')
    .trim()
    .isLength({ min: 2, max: 100 })
    .withMessage('First name must be between 2 and 100 characters'),
  body('lastName')
    .trim()
    .isLength({ min: 2, max: 100 })
    .withMessage('Last name must be between 2 and 100 characters'),
  body('organization')
    .optional()
    .trim()
    .isLength({ max: 255 })
    .withMessage('Organization name must be less than 255 characters'),
  body('role')
    .optional()
    .trim()
    .isLength({ max: 100 })
    .withMessage('Role must be less than 100 characters')
];

/**
 * Validation rules for facilitator login
 */
const validateFacilitatorLogin = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Valid email is required'),
  body('password')
    .notEmpty()
    .withMessage('Password is required')
];

/**
 * Validation rules for classroom creation
 */
const validateClassroomCreation = [
  body('name')
    .trim()
    .isLength({ min: 1, max: 255 })
    .withMessage('Classroom name is required and must be less than 255 characters'),
  body('description')
    .optional()
    .trim()
    .isLength({ max: 1000 })
    .withMessage('Description must be less than 1000 characters'),
  body('gradeLevel')
    .optional()
    .isInt({ min: 1, max: 12 })
    .withMessage('Grade level must be between 1 and 12'),
  body('studentCapacity')
    .optional()
    .isInt({ min: 1, max: 100 })
    .withMessage('Student capacity must be between 1 and 100')
];

/**
 * Validation rules for student enrollment
 */
const validateStudentEnrollment = [
  body('classroomCode')
    .trim()
    .isLength({ min: 6, max: 10 })
    .withMessage('Classroom code must be between 6 and 10 characters'),
  body('gradeLevel')
    .optional()
    .isInt({ min: 1, max: 12 })
    .withMessage('Grade level must be between 1 and 12'),
  body('gender')
    .optional()
    .isIn(['male', 'female', 'non-binary', 'prefer-not-to-say'])
    .withMessage('Invalid gender option'),
  body('ethnicity')
    .optional()
    .trim()
    .isLength({ max: 50 })
    .withMessage('Ethnicity must be less than 50 characters'),
  body('primaryLanguage')
    .optional()
    .trim()
    .isLength({ max: 50 })
    .withMessage('Primary language must be less than 50 characters')
];

/**
 * Validation rules for progress tracking
 */
const validateProgressUpdate = [
  body('lessonId')
    .isUUID()
    .withMessage('Valid lesson ID is required'),
  body('completionStatus')
    .isIn(['not_started', 'in_progress', 'completed'])
    .withMessage('Invalid completion status'),
  body('progressPercentage')
    .optional()
    .isInt({ min: 0, max: 100 })
    .withMessage('Progress percentage must be between 0 and 100'),
  body('timeSpentMinutes')
    .optional()
    .isInt({ min: 0 })
    .withMessage('Time spent must be a positive number'),
  body('videoWatchPercentage')
    .optional()
    .isInt({ min: 0, max: 100 })
    .withMessage('Video watch percentage must be between 0 and 100')
];

/**
 * Validation rules for feedback submission
 */
const validateFeedbackSubmission = [
  body('lessonId')
    .isUUID()
    .withMessage('Valid lesson ID is required'),
  body('feedbackType')
    .isIn(['mood_checkin', 'reflection', 'activity_response', 'lesson_rating'])
    .withMessage('Invalid feedback type'),
  body('ratingValue')
    .optional()
    .isInt({ min: 1, max: 5 })
    .withMessage('Rating value must be between 1 and 5'),
  body('textResponse')
    .optional()
    .trim()
    .isLength({ max: 1000 })
    .withMessage('Text response must be less than 1000 characters'),
  body('moodIndicator')
    .optional()
    .isIn(['happy', 'sad', 'confused', 'excited', 'angry', 'worried', 'proud'])
    .withMessage('Invalid mood indicator')
];

/**
 * Validation rules for analytics events
 */
const validateAnalyticsEvent = [
  body('eventType')
    .trim()
    .isLength({ min: 1, max: 100 })
    .withMessage('Event type is required and must be less than 100 characters'),
  body('eventCategory')
    .trim()
    .isLength({ min: 1, max: 50 })
    .withMessage('Event category is required and must be less than 50 characters'),
  body('eventAction')
    .trim()
    .isLength({ min: 1, max: 100 })
    .withMessage('Event action is required and must be less than 100 characters'),
  body('eventValue')
    .optional()
    .isInt()
    .withMessage('Event value must be an integer'),
  body('deviceType')
    .optional()
    .isIn(['mobile', 'tablet', 'chromebook', 'desktop'])
    .withMessage('Invalid device type'),
  body('platform')
    .optional()
    .isIn(['android', 'ios', 'web'])
    .withMessage('Invalid platform')
];

/**
 * Validation rules for UUID parameters
 */
const validateUuidParam = (paramName) => [
  param(paramName)
    .isUUID()
    .withMessage(`${paramName} must be a valid UUID`)
];

/**
 * Validation rules for pagination
 */
const validatePagination = [
  query('page')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Page must be a positive integer'),
  query('limit')
    .optional()
    .isInt({ min: 1, max: 100 })
    .withMessage('Limit must be between 1 and 100'),
  query('sortBy')
    .optional()
    .trim()
    .isLength({ min: 1, max: 50 })
    .withMessage('Sort by field must be specified'),
  query('sortOrder')
    .optional()
    .isIn(['asc', 'desc'])
    .withMessage('Sort order must be asc or desc')
];

module.exports = {
  validateRequest,
  sanitizeInput,
  validateFacilitatorRegistration,
  validateFacilitatorLogin,
  validateClassroomCreation,
  validateStudentEnrollment,
  validateProgressUpdate,
  validateFeedbackSubmission,
  validateAnalyticsEvent,
  validateUuidParam,
  validatePagination
};
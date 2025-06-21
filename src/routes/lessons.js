const express = require('express');
const router = express.Router();

const {
  getLessons,
  getLesson,
  getLessonProgress,
  createLesson,
  updateLesson,
  getClassroomLessonProgress
} = require('../controllers/lessonController');

const { authenticateFacilitator, authenticateStudent } = require('../middleware/auth');
const {
  validateUuidParam,
  validateRequest,
  validatePagination
} = require('../middleware/validation');

const { body, query } = require('express-validator');

// Routes accessible by both facilitators and students
router.get('/', 
  [
    query('sortBy').optional().isIn(['lesson_number', 'title', 'created_at']),
    query('sortOrder').optional().isIn(['asc', 'desc'])
  ],
  validateRequest,
  (req, res, next) => {
    // Check if user is authenticated as either facilitator or student
    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
      // Facilitator authentication
      return authenticateFacilitator(req, res, next);
    } else if (req.headers.classroomcode && req.headers.studentid) {
      // Student authentication
      return authenticateStudent(req, res, next);
    } else {
      return res.status(401).json({
        success: false,
        error: 'Authentication required'
      });
    }
  },
  getLessons
);

router.get('/:id',
  validateUuidParam('id'),
  validateRequest,
  (req, res, next) => {
    // Check if user is authenticated as either facilitator or student
    if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
      // Facilitator authentication
      return authenticateFacilitator(req, res, next);
    } else if (req.headers.classroomcode && req.headers.studentid) {
      // Student authentication
      return authenticateStudent(req, res, next);
    } else {
      return res.status(401).json({
        success: false,
        error: 'Authentication required'
      });
    }
  },
  getLesson
);

// Student-only routes
router.get('/:id/progress',
  validateUuidParam('id'),
  validateRequest,
  authenticateStudent,
  getLessonProgress
);

// Facilitator-only routes
router.get('/classroom/:classroomId/progress',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  validateRequest,
  getClassroomLessonProgress
);

// Admin/Content Management routes (these would typically require admin authentication)
// For now, using facilitator authentication as a placeholder
router.post('/',
  authenticateFacilitator,
  [
    body('lessonNumber').isInt({ min: 1 }).withMessage('Lesson number must be a positive integer'),
    body('title').trim().isLength({ min: 1, max: 255 }).withMessage('Title is required and must be less than 255 characters'),
    body('description').optional().trim().isLength({ max: 1000 }).withMessage('Description must be less than 1000 characters'),
    body('learningObjectives').optional().trim(),
    body('durationMinutes').optional().isInt({ min: 1 }).withMessage('Duration must be a positive integer'),
    body('difficultyLevel').optional().isIn(['beginner', 'intermediate', 'advanced']).withMessage('Invalid difficulty level'),
    body('videoUrl').optional().isURL().withMessage('Video URL must be a valid URL'),
    body('videoDurationSeconds').optional().isInt({ min: 1 }).withMessage('Video duration must be a positive integer'),
    body('isPublished').optional().isBoolean().withMessage('isPublished must be a boolean'),
    body('sortOrder').optional().isInt({ min: 1 }).withMessage('Sort order must be a positive integer')
  ],
  validateRequest,
  createLesson
);

router.put('/:id',
  authenticateFacilitator,
  validateUuidParam('id'),
  [
    body('lessonNumber').optional().isInt({ min: 1 }).withMessage('Lesson number must be a positive integer'),
    body('title').optional().trim().isLength({ min: 1, max: 255 }).withMessage('Title must be less than 255 characters'),
    body('description').optional().trim().isLength({ max: 1000 }).withMessage('Description must be less than 1000 characters'),
    body('durationMinutes').optional().isInt({ min: 1 }).withMessage('Duration must be a positive integer'),
    body('difficultyLevel').optional().isIn(['beginner', 'intermediate', 'advanced']).withMessage('Invalid difficulty level'),
    body('videoUrl').optional().isURL().withMessage('Video URL must be a valid URL'),
    body('videoDurationSeconds').optional().isInt({ min: 1 }).withMessage('Video duration must be a positive integer'),
    body('isPublished').optional().isBoolean().withMessage('isPublished must be a boolean'),
    body('sortOrder').optional().isInt({ min: 1 }).withMessage('Sort order must be a positive integer')
  ],
  validateRequest,
  updateLesson
);

module.exports = router;
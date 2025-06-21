const express = require('express');
const router = express.Router();
const { body, param } = require('express-validator');

const {
  getClassroomPreview,
  submitEmotionalCheckin,
  getMobileOptimizedLesson
} = require('../controllers/enhancedStudentController');

const { validateRequest } = require('../middleware/validation');

// Enhanced classroom preview - no auth required for preview
router.get('/classroom-preview/:code',
  [
    param('code')
      .isLength({ min: 6, max: 8 })
      .withMessage('Classroom code must be 6-8 characters')
      .matches(/^[A-Z0-9]+$/)
      .withMessage('Classroom code must contain only uppercase letters and numbers')
  ],
  validateRequest,
  getClassroomPreview
);

// Enhanced emotional check-in
router.post('/emotional-checkin',
  [
    body('checkinType')
      .isIn(['lesson_start', 'lesson_end', 'activity_complete', 'reflection_time'])
      .withMessage('Invalid check-in type'),
    body('emotionalState.primary_emotion')
      .isIn(['excited', 'happy', 'calm', 'curious', 'nervous', 'tired', 'confused', 'proud'])
      .withMessage('Invalid primary emotion'),
    body('emotionalState.energy_level')
      .isInt({ min: 1, max: 5 })
      .withMessage('Energy level must be between 1 and 5'),
    body('emotionalState.confidence_level')
      .isInt({ min: 1, max: 5 })
      .withMessage('Confidence level must be between 1 and 5'),
    body('visualResponse.mood_color')
      .optional()
      .matches(/^#[0-9A-F]{6}$/i)
      .withMessage('Mood color must be a valid hex color'),
    body('lessonId')
      .optional()
      .isUUID()
      .withMessage('Lesson ID must be a valid UUID')
  ],
  validateRequest,
  submitEmotionalCheckin
);

// Mobile-optimized lesson content
router.get('/lessons/:id/mobile-optimized',
  [
    param('id')
      .isUUID()
      .withMessage('Lesson ID must be a valid UUID')
  ],
  validateRequest,
  getMobileOptimizedLesson
);

module.exports = router;
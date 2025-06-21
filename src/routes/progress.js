const express = require('express');
const router = express.Router();

const {
  updateLessonProgress,
  getProgressSummary,
  submitFeedback,
  getClassroomLessonProgress,
  getClassroomLessonFeedback
} = require('../controllers/progressController');

const { authenticateFacilitator, authenticateStudent } = require('../middleware/auth');
const {
  validateProgressUpdate,
  validateFeedbackSubmission,
  validateUuidParam,
  validateRequest
} = require('../middleware/validation');

// Student-only routes
router.put('/lesson', 
  authenticateStudent,
  validateProgressUpdate,
  validateRequest,
  updateLessonProgress
);

router.get('/summary',
  authenticateStudent,
  getProgressSummary
);

router.post('/feedback',
  authenticateStudent,
  validateFeedbackSubmission,
  validateRequest,
  submitFeedback
);

// Facilitator-only routes
router.get('/classroom/:classroomId/lesson/:lessonId',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  validateUuidParam('lessonId'),
  validateRequest,
  getClassroomLessonProgress
);

router.get('/classroom/:classroomId/lesson/:lessonId/feedback',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  validateUuidParam('lessonId'),
  validateRequest,
  getClassroomLessonFeedback
);

module.exports = router;
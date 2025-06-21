const express = require('express');
const router = express.Router();

const {
  enrollStudent,
  getStudentProfile,
  getClassroomStudents,
  updateStudentStatus,
  getClassroomAnalytics
} = require('../controllers/studentController');

const { authenticateFacilitator, authenticateStudent } = require('../middleware/auth');
const {
  validateStudentEnrollment,
  validateUuidParam,
  validateRequest,
  validatePagination
} = require('../middleware/validation');

const { body } = require('express-validator');

// Public route for student enrollment
router.post('/enroll', validateStudentEnrollment, validateRequest, enrollStudent);

// Student-only routes
router.get('/profile', authenticateStudent, getStudentProfile);

// Facilitator-only routes
router.get('/classroom/:classroomId',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  validatePagination,
  validateRequest,
  getClassroomStudents
);

router.put('/:studentId/status',
  authenticateFacilitator,
  validateUuidParam('studentId'),
  [
    body('isActive').isBoolean().withMessage('isActive must be a boolean value')
  ],
  validateRequest,
  updateStudentStatus
);

router.get('/classroom/:classroomId/analytics',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  validateRequest,
  getClassroomAnalytics
);

module.exports = router;
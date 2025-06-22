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

// COPPA compliance middleware
const { 
  validateNoPII, 
  validateStudentDemographics, 
  logStudentDataAccess 
} = require('../middleware/coppaCompliance');

const { body } = require('express-validator');

// Public route for student enrollment - with COPPA compliance
router.post('/enroll', 
  validateNoPII,
  validateStudentDemographics,
  validateStudentEnrollment, 
  validateRequest, 
  enrollStudent
);

// Student-only routes - with COPPA compliance
router.get('/profile', 
  authenticateStudent, 
  logStudentDataAccess,
  getStudentProfile
);

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
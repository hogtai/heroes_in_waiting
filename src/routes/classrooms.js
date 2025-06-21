const express = require('express');
const router = express.Router();

const {
  createClassroom,
  getFacilitatorClassrooms,
  getClassroom,
  updateClassroom,
  regenerateClassroomCode,
  deleteClassroom
} = require('../controllers/classroomController');

const { authenticateFacilitator } = require('../middleware/auth');
const {
  validateClassroomCreation,
  validateUuidParam,
  validateRequest,
  validatePagination
} = require('../middleware/validation');

const { body } = require('express-validator');

// All routes require facilitator authentication
router.use(authenticateFacilitator);

// Get all classrooms for facilitator
router.get('/', validatePagination, validateRequest, getFacilitatorClassrooms);

// Create new classroom
router.post('/', validateClassroomCreation, validateRequest, createClassroom);

// Get specific classroom
router.get('/:id', validateUuidParam('id'), validateRequest, getClassroom);

// Update classroom
router.put('/:id',
  validateUuidParam('id'),
  [
    body('name').optional().trim().isLength({ min: 1, max: 255 }),
    body('description').optional().trim().isLength({ max: 1000 }),
    body('gradeLevel').optional().isInt({ min: 1, max: 12 }),
    body('studentCapacity').optional().isInt({ min: 1, max: 100 }),
    body('isActive').optional().isBoolean()
  ],
  validateRequest,
  updateClassroom
);

// Regenerate classroom code
router.post('/:id/regenerate-code',
  validateUuidParam('id'),
  validateRequest,
  regenerateClassroomCode
);

// Delete classroom
router.delete('/:id', validateUuidParam('id'), validateRequest, deleteClassroom);

module.exports = router;
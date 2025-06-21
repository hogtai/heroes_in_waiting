const express = require('express');
const router = express.Router();

const {
  trackEvent,
  getClassroomDashboard,
  getFacilitatorOverview,
  exportClassroomData
} = require('../controllers/analyticsController');

const { authenticateFacilitator, authenticateStudent } = require('../middleware/auth');
const {
  validateAnalyticsEvent,
  validateUuidParam,
  validateRequest
} = require('../middleware/validation');

const { query } = require('express-validator');

// Routes for both students and facilitators
router.post('/event',
  [
    validateAnalyticsEvent,
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
    }
  ],
  trackEvent
);

// Facilitator-only routes
router.get('/facilitator/overview',
  authenticateFacilitator,
  [
    query('timeframe').optional().isIn(['7d', '30d', '90d']).withMessage('Invalid timeframe')
  ],
  validateRequest,
  getFacilitatorOverview
);

router.get('/classroom/:classroomId/dashboard',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  [
    query('timeframe').optional().isIn(['7d', '30d', '90d']).withMessage('Invalid timeframe')
  ],
  validateRequest,
  getClassroomDashboard
);

router.get('/classroom/:classroomId/export',
  authenticateFacilitator,
  validateUuidParam('classroomId'),
  [
    query('format').optional().isIn(['json', 'csv']).withMessage('Invalid format'),
    query('includeEvents').optional().isIn(['true', 'false']).withMessage('includeEvents must be true or false')
  ],
  validateRequest,
  exportClassroomData
);

module.exports = router;
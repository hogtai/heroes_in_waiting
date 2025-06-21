const express = require('express');
const router = express.Router();

const {
  registerFacilitator,
  loginFacilitator,
  getCurrentFacilitator,
  updateFacilitatorProfile,
  changePassword
} = require('../controllers/authController');

const { authenticateFacilitator } = require('../middleware/auth');
const {
  validateFacilitatorRegistration,
  validateFacilitatorLogin,
  validateRequest
} = require('../middleware/validation');

const { body } = require('express-validator');

// Public routes
router.post('/register', validateFacilitatorRegistration, validateRequest, registerFacilitator);
router.post('/login', validateFacilitatorLogin, validateRequest, loginFacilitator);

// Protected routes (require authentication)
router.get('/me', authenticateFacilitator, getCurrentFacilitator);
router.put('/profile', 
  authenticateFacilitator,
  [
    body('firstName').optional().trim().isLength({ min: 2, max: 100 }),
    body('lastName').optional().trim().isLength({ min: 2, max: 100 }),
    body('organization').optional().trim().isLength({ max: 255 }),
    body('role').optional().trim().isLength({ max: 100 })
  ],
  validateRequest,
  updateFacilitatorProfile
);

router.put('/change-password',
  authenticateFacilitator,
  [
    body('currentPassword').notEmpty().withMessage('Current password is required'),
    body('newPassword')
      .isLength({ min: 8 })
      .withMessage('New password must be at least 8 characters long')
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
      .withMessage('New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character')
  ],
  validateRequest,
  changePassword
);

module.exports = router;
const db = require('../config/database');
const { hashPassword, comparePassword, generateToken } = require('../utils/auth');
const logger = require('../utils/logger');

/**
 * @desc    Register a new facilitator
 * @route   POST /api/auth/register
 * @access  Public
 */
async function registerFacilitator(req, res) {
  try {
    const { email, password, firstName, lastName, organization, role } = req.body;

    // Check if facilitator already exists
    const existingFacilitator = await db('facilitators')
      .where({ email })
      .first();

    if (existingFacilitator) {
      return res.status(409).json({
        success: false,
        error: 'Facilitator with this email already exists'
      });
    }

    // Hash password
    const passwordHash = await hashPassword(password);

    // Create facilitator
    const [facilitator] = await db('facilitators')
      .insert({
        email,
        password_hash: passwordHash,
        first_name: firstName,
        last_name: lastName,
        organization,
        role
      })
      .returning(['id', 'email', 'first_name', 'last_name', 'organization', 'role', 'created_at']);

    // Generate JWT token
    const token = generateToken({
      id: facilitator.id,
      email: facilitator.email,
      role: 'facilitator'
    });

    logger.info('New facilitator registered:', {
      facilitatorId: facilitator.id,
      email: facilitator.email,
      organization: facilitator.organization
    });

    res.status(201).json({
      success: true,
      data: {
        facilitator: {
          id: facilitator.id,
          email: facilitator.email,
          firstName: facilitator.first_name,
          lastName: facilitator.last_name,
          organization: facilitator.organization,
          role: facilitator.role,
          createdAt: facilitator.created_at
        },
        token
      }
    });
  } catch (error) {
    logger.error('Facilitator registration failed:', error);
    res.status(500).json({
      success: false,
      error: 'Registration failed'
    });
  }
}

/**
 * @desc    Login facilitator
 * @route   POST /api/auth/login
 * @access  Public
 */
async function loginFacilitator(req, res) {
  try {
    const { email, password } = req.body;

    // Find facilitator by email
    const facilitator = await db('facilitators')
      .where({ email, is_active: true })
      .first();

    if (!facilitator) {
      return res.status(401).json({
        success: false,
        error: 'Invalid credentials'
      });
    }

    // Check password
    const isPasswordValid = await comparePassword(password, facilitator.password_hash);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        error: 'Invalid credentials'
      });
    }

    // Update last login timestamp
    await db('facilitators')
      .where({ id: facilitator.id })
      .update({ last_login: new Date() });

    // Generate JWT token
    const token = generateToken({
      id: facilitator.id,
      email: facilitator.email,
      role: 'facilitator'
    });

    logger.info('Facilitator logged in:', {
      facilitatorId: facilitator.id,
      email: facilitator.email
    });

    res.json({
      success: true,
      data: {
        facilitator: {
          id: facilitator.id,
          email: facilitator.email,
          firstName: facilitator.first_name,
          lastName: facilitator.last_name,
          organization: facilitator.organization,
          role: facilitator.role,
          lastLogin: facilitator.last_login
        },
        token
      }
    });
  } catch (error) {
    logger.error('Facilitator login failed:', error);
    res.status(500).json({
      success: false,
      error: 'Login failed'
    });
  }
}

/**
 * @desc    Get current facilitator profile
 * @route   GET /api/auth/me
 * @access  Private (Facilitator)
 */
async function getCurrentFacilitator(req, res) {
  try {
    const facilitator = await db('facilitators')
      .select(['id', 'email', 'first_name', 'last_name', 'organization', 'role', 'last_login', 'created_at'])
      .where({ id: req.facilitator.id })
      .first();

    if (!facilitator) {
      return res.status(404).json({
        success: false,
        error: 'Facilitator not found'
      });
    }

    res.json({
      success: true,
      data: {
        facilitator: {
          id: facilitator.id,
          email: facilitator.email,
          firstName: facilitator.first_name,
          lastName: facilitator.last_name,
          organization: facilitator.organization,
          role: facilitator.role,
          lastLogin: facilitator.last_login,
          createdAt: facilitator.created_at
        }
      }
    });
  } catch (error) {
    logger.error('Get current facilitator failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get facilitator profile'
    });
  }
}

/**
 * @desc    Update facilitator profile
 * @route   PUT /api/auth/profile
 * @access  Private (Facilitator)
 */
async function updateFacilitatorProfile(req, res) {
  try {
    const { firstName, lastName, organization, role } = req.body;
    const facilitatorId = req.facilitator.id;

    const updatedData = {};
    if (firstName) updatedData.first_name = firstName;
    if (lastName) updatedData.last_name = lastName;
    if (organization) updatedData.organization = organization;
    if (role) updatedData.role = role;
    updatedData.updated_at = new Date();

    const [facilitator] = await db('facilitators')
      .where({ id: facilitatorId })
      .update(updatedData)
      .returning(['id', 'email', 'first_name', 'last_name', 'organization', 'role', 'updated_at']);

    if (!facilitator) {
      return res.status(404).json({
        success: false,
        error: 'Facilitator not found'
      });
    }

    logger.info('Facilitator profile updated:', {
      facilitatorId: facilitator.id,
      email: facilitator.email
    });

    res.json({
      success: true,
      data: {
        facilitator: {
          id: facilitator.id,
          email: facilitator.email,
          firstName: facilitator.first_name,
          lastName: facilitator.last_name,
          organization: facilitator.organization,
          role: facilitator.role,
          updatedAt: facilitator.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Update facilitator profile failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update profile'
    });
  }
}

/**
 * @desc    Change facilitator password
 * @route   PUT /api/auth/change-password
 * @access  Private (Facilitator)
 */
async function changePassword(req, res) {
  try {
    const { currentPassword, newPassword } = req.body;
    const facilitatorId = req.facilitator.id;

    // Get current facilitator
    const facilitator = await db('facilitators')
      .where({ id: facilitatorId })
      .first();

    if (!facilitator) {
      return res.status(404).json({
        success: false,
        error: 'Facilitator not found'
      });
    }

    // Verify current password
    const isCurrentPasswordValid = await comparePassword(currentPassword, facilitator.password_hash);

    if (!isCurrentPasswordValid) {
      return res.status(400).json({
        success: false,
        error: 'Current password is incorrect'
      });
    }

    // Hash new password
    const newPasswordHash = await hashPassword(newPassword);

    // Update password
    await db('facilitators')
      .where({ id: facilitatorId })
      .update({ 
        password_hash: newPasswordHash,
        updated_at: new Date()
      });

    logger.info('Facilitator password changed:', {
      facilitatorId: facilitator.id,
      email: facilitator.email
    });

    res.json({
      success: true,
      message: 'Password changed successfully'
    });
  } catch (error) {
    logger.error('Change password failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to change password'
    });
  }
}

module.exports = {
  registerFacilitator,
  loginFacilitator,
  getCurrentFacilitator,
  updateFacilitatorProfile,
  changePassword
};
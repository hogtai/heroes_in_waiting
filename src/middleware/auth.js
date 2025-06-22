const jwt = require('jsonwebtoken');
const jwtConfig = require('../config/auth');
const db = require('../config/database');
const logger = require('../utils/logger');

/**
 * Enhanced JWT token validation
 */
function validateToken(token) {
    try {
        const decoded = jwt.verify(token, jwtConfig.secret, {
            issuer: jwtConfig.issuer,
            audience: jwtConfig.audience,
            algorithms: [jwtConfig.algorithm],
            clockTolerance: jwtConfig.clockTolerance
        });
        return decoded;
    } catch (error) {
        if (error.name === 'TokenExpiredError') {
            throw new Error('Token expired');
        }
        if (error.name === 'JsonWebTokenError') {
            throw new Error('Invalid token');
        }
        if (error.name === 'NotBeforeError') {
            throw new Error('Token not yet valid');
        }
        throw new Error('Token validation failed');
    }
}

/**
 * Middleware to authenticate facilitators using JWT
 */
async function authenticateFacilitator(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader) {
      return res.status(401).json({
        success: false,
        error: 'Access token is required'
      });
    }

    const token = authHeader.replace('Bearer ', '');
    
    if (!token) {
      return res.status(401).json({
        success: false,
        error: 'Access token is required'
      });
    }

    try {
      const decoded = validateToken(token);
      
      // Verify facilitator exists and is active
      const facilitator = await db('facilitators')
        .where({ id: decoded.id, is_active: true })
        .first();

      if (!facilitator) {
        return res.status(401).json({
          success: false,
          error: 'Invalid token or facilitator not found'
        });
      }

      // Attach facilitator info to request
      req.facilitator = {
        id: facilitator.id,
        email: facilitator.email,
        firstName: facilitator.first_name,
        lastName: facilitator.last_name,
        organization: facilitator.organization,
        role: facilitator.role
      };

      next();
    } catch (jwtError) {
      logger.error('JWT verification failed:', jwtError);
      return res.status(401).json({
        success: false,
        error: jwtError.message || 'Invalid token'
      });
    }
  } catch (error) {
    logger.error('Authentication middleware error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authentication failed'
    });
  }
}

/**
 * Middleware to authenticate students using classroom code
 */
async function authenticateStudent(req, res, next) {
  try {
    const { classroomCode, studentId } = req.headers;
    
    if (!classroomCode || !studentId) {
      return res.status(401).json({
        success: false,
        error: 'Classroom code and student ID are required'
      });
    }

    // Verify classroom exists and is active
    const classroom = await db('classrooms')
      .where({ 
        classroom_code: classroomCode, 
        is_active: true 
      })
      .first();

    if (!classroom) {
      return res.status(401).json({
        success: false,
        error: 'Invalid classroom code'
      });
    }

    // Check if classroom code is expired
    if (classroom.code_expires_at && new Date() > new Date(classroom.code_expires_at)) {
      return res.status(401).json({
        success: false,
        error: 'Classroom code has expired'
      });
    }

    // Verify student exists and belongs to classroom
    const student = await db('students')
      .where({ 
        anonymous_id: studentId, 
        classroom_id: classroom.id,
        is_active: true 
      })
      .first();

    if (!student) {
      return res.status(401).json({
        success: false,
        error: 'Student not found in classroom'
      });
    }

    // Update last active timestamp
    await db('students')
      .where({ id: student.id })
      .update({ last_active_at: new Date() });

    // Attach student and classroom info to request
    req.student = {
      id: student.id,
      anonymousId: student.anonymous_id,
      classroomId: classroom.id,
      gradeLevel: student.grade_level
    };

    req.classroom = {
      id: classroom.id,
      name: classroom.name,
      facilitatorId: classroom.facilitator_id
    };

    next();
  } catch (error) {
    logger.error('Student authentication middleware error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authentication failed'
    });
  }
}

/**
 * Middleware to check if facilitator owns the classroom
 */
async function checkClassroomOwnership(req, res, next) {
  try {
    const classroomId = req.params.classroomId || req.body.classroomId;
    
    if (!classroomId) {
      return res.status(400).json({
        success: false,
        error: 'Classroom ID is required'
      });
    }

    const classroom = await db('classrooms')
      .where({ 
        id: classroomId, 
        facilitator_id: req.facilitator.id 
      })
      .first();

    if (!classroom) {
      return res.status(403).json({
        success: false,
        error: 'Access denied: You do not own this classroom'
      });
    }

    req.classroom = classroom;
    next();
  } catch (error) {
    logger.error('Classroom ownership check error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authorization check failed'
    });
  }
}

module.exports = {
  authenticateFacilitator,
  authenticateStudent,
  checkClassroomOwnership
};
const logger = require('../utils/logger');

/**
 * COPPA Compliance Middleware
 * Ensures no Personally Identifiable Information (PII) is collected from students
 * and enforces age-appropriate data handling practices
 */

/**
 * Validate that no PII is being collected from student requests
 */
function validateNoPII(req, res, next) {
  // List of fields that could contain PII
  const piiFields = [
    'firstName', 'first_name', 'lastName', 'last_name', 'name',
    'email', 'phone', 'address', 'birthDate', 'birth_date',
    'socialSecurity', 'ssn', 'studentId', 'student_id'
  ];

  // Check request body for PII fields
  if (req.body) {
    const foundPII = piiFields.filter(field => 
      req.body.hasOwnProperty(field) && req.body[field]
    );
    
    if (foundPII.length > 0) {
      logger.warn('PII collection attempt detected:', {
        fields: foundPII,
        ip: req.ip,
        userAgent: req.get('User-Agent')
      });
      
      return res.status(400).json({
        success: false,
        error: 'Collection of personally identifiable information is not permitted for students'
      });
    }
  }

  // Check query parameters for PII
  if (req.query) {
    const foundPII = piiFields.filter(field => 
      req.query.hasOwnProperty(field) && req.query[field]
    );
    
    if (foundPII.length > 0) {
      logger.warn('PII collection attempt in query params:', {
        fields: foundPII,
        ip: req.ip
      });
      
      return res.status(400).json({
        success: false,
        error: 'Collection of personally identifiable information is not permitted for students'
      });
    }
  }

  next();
}

/**
 * Validate student demographic data collection
 * Only allows non-PII demographic information
 */
function validateStudentDemographics(req, res, next) {
  const allowedDemographics = [
    'gradeLevel', 'grade_level', 'age', 'gender', 'ethnicity',
    'schoolType', 'school_type', 'location', 'region'
  ];

  if (req.body.demographicInfo) {
    const providedFields = Object.keys(req.body.demographicInfo);
    const invalidFields = providedFields.filter(field => 
      !allowedDemographics.includes(field)
    );

    if (invalidFields.length > 0) {
      logger.warn('Invalid demographic fields attempted:', {
        fields: invalidFields,
        ip: req.ip
      });
      
      return res.status(400).json({
        success: false,
        error: 'Only non-identifying demographic information is permitted'
      });
    }
  }

  next();
}

/**
 * Log student data access for audit purposes
 */
function logStudentDataAccess(req, res, next) {
  const originalSend = res.send;
  
  res.send = function(data) {
    // Log student data access (without PII)
    if (req.student) {
      logger.info('Student data access:', {
        studentId: req.student.anonymousId,
        classroomId: req.student.classroomId,
        endpoint: req.path,
        method: req.method,
        ip: req.ip,
        timestamp: new Date().toISOString()
      });
    }
    
    originalSend.call(this, data);
  };
  
  next();
}

/**
 * Ensure student session data is properly anonymized
 */
function anonymizeStudentData(data) {
  if (!data) return data;
  
  // Remove any potential PII fields
  const sanitized = { ...data };
  const piiFields = ['name', 'email', 'phone', 'address', 'birthDate'];
  
  piiFields.forEach(field => {
    delete sanitized[field];
  });
  
  return sanitized;
}

/**
 * Validate classroom code format and security
 */
function validateClassroomCode(code) {
  // Classroom codes should be 6-8 characters, alphanumeric
  const codePattern = /^[A-Z0-9]{6,8}$/;
  return codePattern.test(code);
}

module.exports = {
  validateNoPII,
  validateStudentDemographics,
  logStudentDataAccess,
  anonymizeStudentData,
  validateClassroomCode
}; 
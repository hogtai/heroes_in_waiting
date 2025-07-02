# Security Documentation - Heroes in Waiting

## Overview

This document outlines the security measures implemented in the Heroes in Waiting application to ensure compliance with educational privacy regulations and protect user data.

## Compliance Standards

### COPPA (Children's Online Privacy Protection Act)
- **No PII Collection**: The application does not collect personally identifiable information from students under 13
- **Anonymous Student IDs**: Students are identified using randomly generated anonymous IDs
- **Parental Consent**: Facilitators (teachers/adults) provide consent for classroom participation
- **Data Minimization**: Only non-identifying demographic data is collected for educational purposes

### Educational Privacy
- **FERPA Compliance**: While not directly subject to FERPA, the application follows FERPA principles
- **Student Data Protection**: All student data is anonymized and aggregated
- **Audit Logging**: Comprehensive logging of data access without PII

## Security Architecture

### Authentication & Authorization

#### Facilitator Authentication
- **JWT Tokens**: Secure JWT-based authentication for adult facilitators
- **Password Security**: bcrypt hashing with 12 salt rounds
- **Session Management**: Configurable token expiration (default: 24 hours)
- **Role-Based Access**: Facilitator and admin role support

#### Student Authentication
- **Classroom Codes**: Time-limited, randomly generated codes for classroom access
- **Anonymous Sessions**: No persistent authentication for students
- **Session Validation**: Real-time validation of classroom codes and student membership

### Data Protection

#### Encryption
- **Transport Layer**: HTTPS/TLS 1.3 for all communications
- **Data at Rest**: Database encryption for sensitive data
- **Password Storage**: bcrypt hashing for facilitator passwords

#### Data Anonymization
- **Student Records**: No names, emails, or identifying information
- **Demographic Data**: Only non-identifying information (grade level, general location)
- **Analytics**: Aggregated data only, no individual student tracking

### Network Security

#### API Security
- **Rate Limiting**: 100 requests per 15 minutes per IP
- **Input Validation**: Comprehensive validation using Joi and express-validator
- **SQL Injection Protection**: Parameterized queries via Knex.js
- **CORS Configuration**: Restricted cross-origin requests

#### Android Security
- **Network Security Config**: Enforced HTTPS for production API
- **Certificate Pinning**: Planned implementation for API domain
- **Code Obfuscation**: ProGuard/R8 configuration for release builds
- **Cleartext Traffic**: Disabled for production builds

## Security Controls

### Input Validation
```javascript
// Example: Student enrollment validation
const validateStudentEnrollment = [
  body('classroomCode')
    .isLength({ min: 6, max: 8 })
    .matches(/^[A-Z0-9]+$/)
    .withMessage('Classroom code must be 6-8 alphanumeric characters'),
  body('demographicInfo.gradeLevel')
    .isIn(['4', '5', '6'])
    .withMessage('Grade level must be 4, 5, or 6')
];
```

### COPPA Compliance Checks
```javascript
// PII detection middleware
function validateNoPII(req, res, next) {
  const piiFields = ['firstName', 'lastName', 'email', 'phone', 'address'];
  const foundPII = piiFields.filter(field => req.body[field]);
  
  if (foundPII.length > 0) {
    return res.status(400).json({
      error: 'Collection of PII is not permitted for students'
    });
  }
  next();
}
```

### Audit Logging
```javascript
// Student data access logging
logger.info('Student data access:', {
  studentId: req.student.anonymousId,
  classroomId: req.student.classroomId,
  endpoint: req.path,
  timestamp: new Date().toISOString()
});
```

## Security Testing

### Automated Security Scans
- **SAST**: Static Application Security Testing via npm audit and ESLint
- **DAST**: Dynamic Application Security Testing via OWASP ZAP
- **Dependency Scanning**: Automated vulnerability scanning via Snyk
- **Secret Scanning**: TruffleHog integration for detecting hardcoded secrets

### Manual Security Reviews
- **Code Reviews**: Security-focused code review process
- **Penetration Testing**: Regular security assessments
- **Compliance Audits**: COPPA compliance verification

## Incident Response

### Security Incident Classification
1. **Critical**: Data breach, authentication bypass
2. **High**: Unauthorized access, PII exposure
3. **Medium**: Security misconfiguration, vulnerability
4. **Low**: Minor security issues, best practice violations

### Response Procedures
1. **Detection**: Automated monitoring and alerting
2. **Assessment**: Impact analysis and severity classification
3. **Containment**: Immediate mitigation measures
4. **Eradication**: Root cause analysis and remediation
5. **Recovery**: System restoration and monitoring
6. **Lessons Learned**: Process improvement and documentation

## Security Monitoring

### Log Monitoring
- **Authentication Events**: Failed login attempts, token validation
- **Data Access**: Student data access patterns
- **API Usage**: Rate limit violations, suspicious requests
- **Error Logs**: Security-related errors and exceptions

### Alerting
- **Failed Authentication**: Multiple failed login attempts
- **PII Detection**: Attempts to collect prohibited data
- **Rate Limit Violations**: Potential abuse or attack
- **System Errors**: Security-related system failures

## Environment Security

### Production Environment
- **Environment Variables**: All secrets stored as environment variables
- **Database Security**: Restricted access, encrypted connections
- **Network Security**: Firewall rules, VPN access
- **Backup Security**: Encrypted backups, secure storage

### Development Environment
- **Local Development**: No production data access
- **Test Data**: Synthetic data only, no real student information
- **Code Security**: No secrets in source code
- **Access Control**: Limited development environment access

## Security Best Practices

### For Developers
1. **Never commit secrets** to version control
2. **Use environment variables** for all configuration
3. **Validate all inputs** from external sources
4. **Follow principle of least privilege** for data access
5. **Log security events** for monitoring and audit

### For Facilitators
1. **Keep classroom codes secure** and don't share publicly
2. **Monitor student activity** for unusual patterns
3. **Report security concerns** immediately
4. **Use strong passwords** for facilitator accounts
5. **Log out** when not actively using the application

### For Students
1. **Never share personal information** in the application
2. **Use only provided classroom codes** for access
3. **Report suspicious activity** to facilitators
4. **Log out** when finished using the application

## Compliance Documentation

### COPPA Compliance Checklist
- [x] No collection of PII from students under 13
- [x] Anonymous student identification system
- [x] Parental consent through facilitator registration
- [x] Data minimization practices
- [x] Secure data handling procedures
- [x] Regular compliance audits

### Data Retention Policy
- **Student Data**: Retained for educational purposes only
- **Analytics Data**: Aggregated and anonymized
- **Audit Logs**: Retained for security monitoring
- **Backup Data**: Encrypted and securely stored

## Contact Information

For security concerns or questions:
- **Security Team**: security@heroesinwaiting.org
- **Privacy Officer**: privacy@heroesinwaiting.org
- **Emergency Contact**: +1-XXX-XXX-XXXX

## Security Updates

This document is updated regularly to reflect current security measures and compliance requirements. Last updated: January 2025 
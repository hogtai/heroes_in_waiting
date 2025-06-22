# Cybersecurity Remediation Plan
**Date:** December 2024  
**Priority:** High - Must complete before Checkpoint 5  
**Status:** In Progress

## Overview
This plan addresses all security vulnerabilities and hardening measures identified in the cybersecurity review before proceeding to Checkpoint 5.

## Critical Security Issues to Address

### 1. Dependency Vulnerabilities
**Priority:** Critical  
**Status:** In Progress

#### 1.1 Update axios (vulnerable to SSRF)
```bash
# Current: axios@0.21.1 (vulnerable)
# Target: axios@1.6.0+ (latest stable)
npm update axios
```

**Files to update:**
- `package.json` - Update axios version
- `src/controllers/*.js` - Review axios usage for SSRF protection
- `src/services/*.js` - Ensure proper URL validation

#### 1.2 Update lodash (vulnerable to prototype pollution)
```bash
# Current: lodash@4.17.21 (vulnerable)
# Target: lodash@4.17.21+ (patched version)
npm update lodash
```

**Files to update:**
- `package.json` - Update lodash version
- Review all lodash usage for safe practices

### 2. Android Security Hardening
**Priority:** High  
**Status:** Pending

#### 2.1 Enable Code Obfuscation
**File:** `android/app/build.gradle`
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### 2.2 Implement Certificate Pinning
**File:** `android/app/src/main/java/com/lifechurch/heroesinwaiting/network/CertificatePinner.kt`
```kotlin
class CertificatePinner {
    companion object {
        private const val HOSTNAME = "api.heroesinwaiting.com"
        private const val CERT_SHA256 = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        
        fun getCertificatePinner(): CertificatePinner {
            return CertificatePinner.Builder()
                .add(HOSTNAME, CERT_SHA256)
                .build()
        }
    }
}
```

#### 2.3 Update Network Security Config
**File:** `android/app/src/main/res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.heroesinwaiting.com</domain>
        <pin-set expiration="2025-12-31">
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <pin digest="SHA-256">BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

### 3. Enhanced Security Headers
**Priority:** High  
**Status:** Pending

#### 3.1 Update Security Middleware
**File:** `src/middleware/security.js`
```javascript
const helmet = require('helmet');

const securityMiddleware = [
    helmet({
        contentSecurityPolicy: {
            directives: {
                defaultSrc: ["'self'"],
                styleSrc: ["'self'", "'unsafe-inline'"],
                scriptSrc: ["'self'"],
                imgSrc: ["'self'", "data:", "https:"],
                connectSrc: ["'self'"],
                fontSrc: ["'self'"],
                objectSrc: ["'none'"],
                mediaSrc: ["'self'"],
                frameSrc: ["'none'"],
            },
        },
        hsts: {
            maxAge: 31536000,
            includeSubDomains: true,
            preload: true
        },
        noSniff: true,
        referrerPolicy: { policy: 'strict-origin-when-cross-origin' }
    }),
    (req, res, next) => {
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('X-Frame-Options', 'DENY');
        res.setHeader('X-XSS-Protection', '1; mode=block');
        res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
        next();
    }
];

module.exports = securityMiddleware;
```

#### 3.2 Update App.js
**File:** `src/app.js`
```javascript
const securityMiddleware = require('./middleware/security');

// Apply security middleware
app.use(securityMiddleware);
```

### 4. JWT Security Enhancements
**Priority:** High  
**Status:** Pending

#### 4.1 Update JWT Configuration
**File:** `src/config/auth.js`
```javascript
const jwtConfig = {
    secret: process.env.JWT_SECRET,
    expiresIn: '1h',
    issuer: 'heroes-in-waiting',
    audience: 'heroes-in-waiting-app',
    algorithm: 'HS256',
    clockTolerance: 30,
    maxAge: 3600000 // 1 hour in milliseconds
};

module.exports = jwtConfig;
```

#### 4.2 Enhanced JWT Validation
**File:** `src/middleware/auth.js`
```javascript
const jwt = require('jsonwebtoken');
const jwtConfig = require('../config/auth');

const validateToken = (req, res, next) => {
    try {
        const token = req.headers.authorization?.replace('Bearer ', '');
        
        if (!token) {
            return res.status(401).json({ error: 'No token provided' });
        }

        const decoded = jwt.verify(token, jwtConfig.secret, {
            issuer: jwtConfig.issuer,
            audience: jwtConfig.audience,
            algorithms: [jwtConfig.algorithm],
            clockTolerance: jwtConfig.clockTolerance
        });

        req.user = decoded;
        next();
    } catch (error) {
        if (error.name === 'TokenExpiredError') {
            return res.status(401).json({ error: 'Token expired' });
        }
        if (error.name === 'JsonWebTokenError') {
            return res.status(401).json({ error: 'Invalid token' });
        }
        return res.status(500).json({ error: 'Token validation failed' });
    }
};
```

### 5. Input Validation and Sanitization
**Priority:** Medium  
**Status:** Pending

#### 5.1 Enhanced Validation Middleware
**File:** `src/middleware/validation.js`
```javascript
const { body, validationResult } = require('express-validator');
const xss = require('xss');

const sanitizeInput = (req, res, next) => {
    // Sanitize all string inputs
    Object.keys(req.body).forEach(key => {
        if (typeof req.body[key] === 'string') {
            req.body[key] = xss(req.body[key]);
        }
    });
    
    Object.keys(req.query).forEach(key => {
        if (typeof req.query[key] === 'string') {
            req.query[key] = xss(req.query[key]);
        }
    });
    
    next();
};

const validateClassroom = [
    body('name').trim().isLength({ min: 1, max: 100 }).escape(),
    body('description').optional().trim().isLength({ max: 500 }).escape(),
    sanitizeInput,
    (req, res, next) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }
        next();
    }
];

module.exports = {
    sanitizeInput,
    validateClassroom
};
```

## Implementation Timeline

### Phase 1: Critical Dependencies (Day 1)
- [ ] Update axios to latest version
- [ ] Update lodash to latest version
- [ ] Test all API endpoints for compatibility
- [ ] Update package-lock.json

### Phase 2: Android Security (Day 2)
- [ ] Enable code obfuscation
- [ ] Implement certificate pinning
- [ ] Update network security config
- [ ] Test Android app security features

### Phase 3: Backend Security (Day 3)
- [ ] Implement enhanced security headers
- [ ] Update JWT configuration
- [ ] Enhance input validation
- [ ] Test all security middleware

### Phase 4: Testing and Validation (Day 4)
- [ ] Security testing with OWASP ZAP
- [ ] Vulnerability scanning
- [ ] Penetration testing
- [ ] Performance impact assessment

## Success Criteria
- [ ] All dependency vulnerabilities resolved
- [ ] Android app passes security scan
- [ ] Backend passes security audit
- [ ] No critical or high severity vulnerabilities
- [ ] All security headers properly configured
- [ ] Certificate pinning working correctly
- [ ] Code obfuscation enabled and tested

## Risk Mitigation
- **Rollback Plan:** Keep previous versions in git history
- **Testing Strategy:** Comprehensive testing after each phase
- **Monitoring:** Enhanced logging during implementation
- **Documentation:** Update security documentation

## Next Steps
1. Begin Phase 1 implementation
2. Coordinate with QA team for testing
3. Monitor for any issues during deployment
4. Complete all phases before Checkpoint 5 
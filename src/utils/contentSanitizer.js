const createDOMPurify = require('dompurify');
const { JSDOM } = require('jsdom');
const logger = require('./logger');

// Create JSDOM window for server-side use
const window = new JSDOM('').window;
const DOMPurify = createDOMPurify(window);

// Educational content sanitization configuration
const EDUCATIONAL_CONFIG = {
  ALLOWED_TAGS: [
    // Basic text formatting
    'p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    // Lists
    'ul', 'ol', 'li',
    // Educational elements
    'blockquote', 'code', 'pre',
    // Safe media (sanitized separately)
    'img', 'a',
    // Tables for educational content
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    // Divs and spans for layout (with restricted attributes)
    'div', 'span'
  ],
  ALLOWED_ATTR: [
    // Basic attributes
    'class', 'id',
    // Link attributes (restricted)
    'href', 'target',
    // Image attributes (restricted)
    'src', 'alt', 'width', 'height',
    // Educational attributes
    'title', 'lang',
    // Data attributes for educational interactions (prefixed)
    'data-lesson', 'data-activity', 'data-level'
  ],
  FORBIDDEN_TAGS: [
    'script', 'object', 'embed', 'iframe', 'form', 'input', 'button',
    'style', 'link', 'meta', 'base', 'applet', 'audio', 'video'
  ]
};

// COPPA-compliant configuration for student content
const STUDENT_CONFIG = {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'h1', 'h2', 'h3',
    'ul', 'ol', 'li', 'blockquote', 'img'
  ],
  ALLOWED_ATTR: [
    'class', 'alt', 'title'
  ],
  FORBIDDEN_TAGS: [
    'script', 'object', 'embed', 'iframe', 'form', 'input', 'button',
    'style', 'link', 'meta', 'base', 'applet', 'audio', 'video', 'a'
  ]
};

/**
 * Sanitize HTML content for educational use
 * @param {string} htmlContent - Raw HTML content
 * @param {string} userType - 'facilitator' or 'student'
 * @param {Object} options - Additional sanitization options
 * @returns {string} - Sanitized HTML content
 */
function sanitizeHTMLContent(htmlContent, userType = 'facilitator', options = {}) {
  try {
    if (!htmlContent || typeof htmlContent !== 'string') {
      return '';
    }

    // Choose configuration based on user type
    const config = userType === 'student' ? STUDENT_CONFIG : EDUCATIONAL_CONFIG;
    
    // Configure DOMPurify
    const sanitizeOptions = {
      ALLOWED_TAGS: config.ALLOWED_TAGS,
      ALLOWED_ATTR: config.ALLOWED_ATTR,
      FORBID_TAGS: config.FORBIDDEN_TAGS,
      FORBID_ATTR: ['onclick', 'onload', 'onerror', 'onmouseover', 'style'],
      ALLOW_DATA_ATTR: userType === 'facilitator', // Only facilitators can use data attributes
      RETURN_DOM: false,
      RETURN_DOM_FRAGMENT: false,
      RETURN_DOM_IMPORT: false,
      SANITIZE_DOM: true,
      ...options
    };

    // Sanitize the content
    const sanitized = DOMPurify.sanitize(htmlContent, sanitizeOptions);

    // Additional validation for educational content
    if (userType === 'student') {
      // Remove any remaining links for student content (COPPA compliance)
      return sanitized.replace(/<a\b[^>]*>/gi, '').replace(/<\/a>/gi, '');
    }

    // For facilitator content, validate links
    if (userType === 'facilitator') {
      return validateEducationalLinks(sanitized);
    }

    return sanitized;

  } catch (error) {
    logger.error('Content sanitization failed:', {
      error: error.message,
      userType: userType,
      contentLength: htmlContent ? htmlContent.length : 0
    });
    
    // Return empty string if sanitization fails for security
    return '';
  }
}

/**
 * Validate and sanitize links in educational content
 * @param {string} content - HTML content with potential links
 * @returns {string} - Content with validated links
 */
function validateEducationalLinks(content) {
  // Only allow educational and safe domains
  const allowedDomains = [
    'edu', 'gov', 'khanacademy.org', 'nationalgeographic.org',
    'smithsonian.edu', 'cdc.gov', 'pbskids.org'
  ];

  return content.replace(/<a\s+href=["'](.*?)["'][^>]*>/gi, (match, url) => {
    try {
      const urlObj = new URL(url);
      const isAllowed = allowedDomains.some(domain => 
        urlObj.hostname.endsWith(domain) || urlObj.hostname === domain
      );

      if (isAllowed && (urlObj.protocol === 'https:' || urlObj.protocol === 'http:')) {
        return `<a href="${url}" target="_blank" rel="noopener noreferrer">`;
      } else {
        // Remove disallowed links but keep the text content
        return '';
      }
    } catch (error) {
      // Invalid URL, remove the link
      return '';
    }
  });
}

/**
 * Sanitize JSON content structure for content versions
 * @param {Object} contentStructure - Content structure object
 * @param {string} userType - 'facilitator' or 'student'
 * @returns {Object} - Sanitized content structure
 */
function sanitizeContentStructure(contentStructure, userType = 'facilitator') {
  try {
    if (!contentStructure || typeof contentStructure !== 'object') {
      return {};
    }

    const sanitized = JSON.parse(JSON.stringify(contentStructure));

    // Recursively sanitize HTML content in the structure
    function sanitizeRecursive(obj) {
      if (Array.isArray(obj)) {
        return obj.map(sanitizeRecursive);
      } else if (obj && typeof obj === 'object') {
        const result = {};
        for (const [key, value] of Object.entries(obj)) {
          if (typeof value === 'string' && (
            key.includes('content') || 
            key.includes('html') || 
            key.includes('description') ||
            key.includes('text')
          )) {
            result[key] = sanitizeHTMLContent(value, userType);
          } else if (typeof value === 'object') {
            result[key] = sanitizeRecursive(value);
          } else {
            result[key] = value;
          }
        }
        return result;
      }
      return obj;
    }

    return sanitizeRecursive(sanitized);

  } catch (error) {
    logger.error('Content structure sanitization failed:', {
      error: error.message,
      userType: userType
    });
    return {};
  }
}

/**
 * Sanitize metadata object for safe storage
 * @param {Object} metadata - Metadata object
 * @returns {Object} - Sanitized metadata
 */
function sanitizeMetadata(metadata) {
  try {
    if (!metadata || typeof metadata !== 'object') {
      return {};
    }

    const sanitized = {};
    const allowedKeys = [
      'title', 'description', 'keywords', 'difficulty', 'duration',
      'ageGroup', 'subject', 'language', 'version', 'author',
      'lastModified', 'tags', 'category', 'educationalUse'
    ];

    for (const [key, value] of Object.entries(metadata)) {
      if (allowedKeys.includes(key)) {
        if (typeof value === 'string') {
          // Remove HTML tags from metadata strings
          sanitized[key] = DOMPurify.sanitize(value, { ALLOWED_TAGS: [] });
        } else if (Array.isArray(value)) {
          // Sanitize arrays of strings
          sanitized[key] = value
            .filter(item => typeof item === 'string')
            .map(item => DOMPurify.sanitize(item, { ALLOWED_TAGS: [] }));
        } else if (typeof value === 'number' || typeof value === 'boolean') {
          sanitized[key] = value;
        }
      }
    }

    return sanitized;

  } catch (error) {
    logger.error('Metadata sanitization failed:', {
      error: error.message
    });
    return {};
  }
}

/**
 * Detect potential PII in content (COPPA compliance)
 * @param {string} content - Content to scan
 * @returns {Array} - Array of potential PII detections
 */
function detectPII(content) {
  const piiPatterns = [
    { type: 'email', pattern: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g },
    { type: 'phone', pattern: /\b\d{3}[-.]?\d{3}[-.]?\d{4}\b/g },
    { type: 'ssn', pattern: /\b\d{3}-\d{2}-\d{4}\b/g },
    { type: 'address', pattern: /\b\d+\s+[A-Za-z\s]+(?:Street|St|Avenue|Ave|Road|Rd|Lane|Ln|Drive|Dr|Court|Ct|Place|Pl)\b/gi }
  ];

  const detections = [];
  const strippedContent = DOMPurify.sanitize(content, { ALLOWED_TAGS: [] });

  piiPatterns.forEach(({ type, pattern }) => {
    const matches = strippedContent.match(pattern);
    if (matches) {
      detections.push({
        type,
        count: matches.length,
        examples: matches.slice(0, 3) // Only first 3 examples for logging
      });
    }
  });

  return detections;
}

module.exports = {
  sanitizeHTMLContent,
  sanitizeContentStructure,
  sanitizeMetadata,
  detectPII,
  validateEducationalLinks
};
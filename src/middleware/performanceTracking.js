const PerformanceMonitoringService = require('../services/performanceMonitoringService');
const logger = require('../utils/logger');

/**
 * Middleware to track API performance and content delivery metrics
 * Optimized for educational content delivery to elementary students
 */

/**
 * Track API response times for performance monitoring
 */
const trackAPIPerformance = (req, res, next) => {
  const startTime = Date.now();
  
  // Store original end function
  const originalEnd = res.end;
  const originalJson = res.json;
  
  // Override res.end to capture response time
  res.end = function(chunk, encoding) {
    const responseTime = Date.now() - startTime;
    
    // Track performance metrics
    try {
      PerformanceMonitoringService.trackAPIResponse(req, res, responseTime);
      
      // Log slow responses for educational content (important for student engagement)
      if (responseTime > 2000) {
        logger.warn('Slow API response detected:', {
          endpoint: `${req.method} ${req.originalUrl}`,
          responseTime: responseTime,
          statusCode: res.statusCode,
          userType: req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous'),
          educationalImpact: 'May affect student attention and lesson flow'
        });
      }
    } catch (error) {
      logger.error('Failed to track API performance:', error);
    }
    
    // Call original end function
    originalEnd.call(this, chunk, encoding);
  };
  
  // Override res.json to track JSON response performance
  res.json = function(obj) {
    const responseTime = Date.now() - startTime;
    
    // Add performance headers for debugging
    res.set('X-Response-Time', `${responseTime}ms`);
    res.set('X-Content-Type', 'educational-api');
    
    // Track content-specific performance
    if (req.originalUrl.includes('/lesson') || req.originalUrl.includes('/content')) {
      try {
        const contentType = req.originalUrl.includes('/lesson') ? 'lesson_content' : 'general_content';
        const deviceInfo = getDeviceInfo(req);
        const userType = req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous');
        
        PerformanceMonitoringService.trackContentPerformance(
          contentType,
          responseTime,
          deviceInfo,
          userType
        );
      } catch (error) {
        logger.error('Failed to track content performance:', error);
      }
    }
    
    // Call original json function
    originalJson.call(this, obj);
  };
  
  next();
};

/**
 * Track mobile-specific performance metrics
 */
const trackMobilePerformance = (req, res, next) => {
  const deviceInfo = getDeviceInfo(req);
  
  // Add mobile-specific headers
  if (deviceInfo.type === 'mobile' || deviceInfo.type === 'tablet') {
    res.set('X-Mobile-Optimized', 'true');
    res.set('X-Device-Type', deviceInfo.type);
    
    // Set cache headers for mobile content
    if (req.originalUrl.includes('/mobile/') || req.originalUrl.includes('/lesson-content/')) {
      res.set('Cache-Control', 'public, max-age=3600'); // 1 hour cache for mobile
      res.set('X-Educational-Cache', 'enabled');
    }
  }
  
  // Track device-specific metrics
  req.deviceInfo = deviceInfo;
  
  next();
};

/**
 * Monitor content download performance for offline caching
 */
const trackContentDownload = (req, res, next) => {
  const startTime = Date.now();
  
  // Only track for content download endpoints
  if (!req.originalUrl.includes('/download') && !req.query.offline) {
    return next();
  }
  
  const originalEnd = res.end;
  
  res.end = function(chunk, encoding) {
    const downloadTime = Date.now() - startTime;
    const contentSize = res.get('Content-Length') || (chunk ? chunk.length : 0);
    
    try {
      // Track download performance
      logger.info('Content download tracked:', {
        url: req.originalUrl,
        downloadTime: downloadTime,
        contentSize: contentSize,
        downloadSpeed: contentSize > 0 ? Math.round(contentSize / downloadTime * 1000) : 0, // bytes per second
        deviceType: req.deviceInfo?.type || 'unknown',
        userType: req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous'),
        offline: req.query.offline === 'true'
      });
      
      // Alert for slow downloads (important for student patience)
      if (downloadTime > 10000) { // 10 seconds
        logger.warn('Slow content download detected:', {
          url: req.originalUrl,
          downloadTime: downloadTime,
          educationalNote: 'Slow downloads may impact offline lesson availability for students'
        });
      }
    } catch (error) {
      logger.error('Failed to track download performance:', error);
    }
    
    originalEnd.call(this, chunk, encoding);
  };
  
  next();
};

/**
 * Performance monitoring for lesson synchronization
 */
const trackLessonSync = (req, res, next) => {
  // Only track for sync endpoints
  if (!req.originalUrl.includes('/sync')) {
    return next();
  }
  
  const startTime = Date.now();
  const originalJson = res.json;
  
  res.json = function(obj) {
    const syncTime = Date.now() - startTime;
    
    try {
      // Extract sync metrics from response
      const syncData = obj?.data?.syncInfo || {};
      const lessonsCount = obj?.data?.lessons?.length || 0;
      
      logger.info('Lesson sync performance:', {
        syncTime: syncTime,
        lessonsCount: lessonsCount,
        syncType: syncData.syncType || 'unknown',
        deviceType: req.deviceInfo?.type || 'unknown',
        userType: req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous'),
        performanceCategory: getSyncPerformanceCategory(syncTime, lessonsCount)
      });
      
      // Track in performance monitoring service
      PerformanceMonitoringService.trackContentPerformance(
        'lesson_sync',
        syncTime,
        req.deviceInfo || {},
        req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous')
      );
      
    } catch (error) {
      logger.error('Failed to track sync performance:', error);
    }
    
    originalJson.call(this, obj);
  };
  
  next();
};

/**
 * Extract device information from request
 */
function getDeviceInfo(req) {
  const userAgent = req.get('User-Agent') || '';
  
  let deviceType = 'unknown';
  if (/Mobile|Android|iPhone|iPad/.test(userAgent)) {
    if (/iPad|tablet/i.test(userAgent)) {
      deviceType = 'tablet';
    } else {
      deviceType = 'mobile';
    }
  } else {
    deviceType = 'desktop';
  }
  
  return {
    type: deviceType,
    userAgent: userAgent,
    network: req.get('Connection') || 'unknown',
    screenSize: req.get('X-Screen-Size') || 'unknown',
    memoryUsage: req.get('X-Memory-Usage') || 'unknown'
  };
}

/**
 * Categorize sync performance for educational context
 */
function getSyncPerformanceCategory(syncTime, lessonsCount) {
  const timePerLesson = lessonsCount > 0 ? syncTime / lessonsCount : syncTime;
  
  if (timePerLesson < 500) return 'excellent'; // Under 0.5s per lesson
  if (timePerLesson < 1000) return 'good';     // Under 1s per lesson
  if (timePerLesson < 2000) return 'fair';     // Under 2s per lesson
  return 'poor'; // Over 2s per lesson - may impact student experience
}

/**
 * Educational performance middleware - sets appropriate timeouts and expectations
 */
const educationalPerformanceSettings = (req, res, next) => {
  // Set educational context headers
  res.set('X-Educational-Platform', 'heroes-in-waiting');
  res.set('X-Target-Audience', 'grades-4-6');
  res.set('X-Performance-Target', '2000ms'); // 2 second target for student engagement
  
  // Add COPPA compliance headers
  res.set('X-COPPA-Compliant', 'true');
  res.set('X-PII-Collection', 'none');
  
  // Set request timeout for educational content (slightly longer for large lesson content)
  req.setTimeout(30000); // 30 seconds max
  
  next();
};

module.exports = {
  trackAPIPerformance,
  trackMobilePerformance,
  trackContentDownload,
  trackLessonSync,
  educationalPerformanceSettings
};
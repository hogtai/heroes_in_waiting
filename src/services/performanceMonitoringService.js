const db = require('../config/database');
const logger = require('../utils/logger');

/**
 * Performance monitoring service for content delivery optimization
 * Tracks API response times, content loading, and mobile performance
 */
class PerformanceMonitoringService {

  /**
   * Track API response time
   */
  static trackAPIResponse(req, res, responseTime) {
    const performanceData = {
      endpoint: `${req.method} ${req.route?.path || req.path}`,
      responseTime: responseTime,
      statusCode: res.statusCode,
      userAgent: req.get('User-Agent'),
      contentLength: res.get('Content-Length'),
      timestamp: new Date(),
      userType: req.facilitator ? 'facilitator' : (req.student ? 'student' : 'anonymous')
    };

    // Log performance metrics
    logger.info('API Performance:', performanceData);

    // Store critical performance data for analysis
    if (responseTime > 2000 || res.statusCode >= 400) {
      this.storePerformanceAlert(performanceData);
    }

    return performanceData;
  }

  /**
   * Track content loading performance for mobile devices
   */
  static async trackContentPerformance(contentType, loadTime, deviceInfo, userType = 'anonymous') {
    try {
      const performanceRecord = {
        content_type: contentType,
        load_time_ms: loadTime,
        device_type: deviceInfo.type || 'unknown',
        network_type: deviceInfo.network || 'unknown',
        user_type: userType,
        timestamp: new Date(),
        metadata: {
          userAgent: deviceInfo.userAgent,
          screenSize: deviceInfo.screenSize,
          memoryUsage: deviceInfo.memoryUsage
        }
      };

      // Store in database for analysis
      await db('performance_metrics').insert(performanceRecord);

      // Alert if performance is below threshold
      if (loadTime > 5000) { // 5 seconds threshold
        logger.warn('Slow content loading detected:', {
          contentType,
          loadTime,
          deviceType: deviceInfo.type,
          userType
        });
      }

      return performanceRecord;
    } catch (error) {
      logger.error('Failed to track content performance:', error);
    }
  }

  /**
   * Get performance analytics for content delivery
   */
  static async getPerformanceAnalytics(timeframe = '24h') {
    try {
      let dateFilter;
      switch (timeframe) {
        case '1h':
          dateFilter = new Date(Date.now() - 60 * 60 * 1000);
          break;
        case '24h':
          dateFilter = new Date(Date.now() - 24 * 60 * 60 * 1000);
          break;
        case '7d':
          dateFilter = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
          break;
        default:
          dateFilter = new Date(Date.now() - 24 * 60 * 60 * 1000);
      }

      // Get API performance metrics
      const apiMetrics = await db('performance_metrics')
        .where('timestamp', '>=', dateFilter)
        .where('content_type', 'api')
        .select([
          db.raw('AVG(load_time_ms) as avg_response_time'),
          db.raw('MAX(load_time_ms) as max_response_time'),
          db.raw('MIN(load_time_ms) as min_response_time'),
          db.raw('COUNT(*) as total_requests')
        ])
        .first();

      // Get content loading metrics
      const contentMetrics = await db('performance_metrics')
        .where('timestamp', '>=', dateFilter)
        .whereNot('content_type', 'api')
        .select([
          'content_type',
          db.raw('AVG(load_time_ms) as avg_load_time'),
          db.raw('COUNT(*) as total_loads')
        ])
        .groupBy('content_type');

      // Get device type breakdown
      const deviceMetrics = await db('performance_metrics')
        .where('timestamp', '>=', dateFilter)
        .select([
          'device_type',
          db.raw('AVG(load_time_ms) as avg_load_time'),
          db.raw('COUNT(*) as requests')
        ])
        .groupBy('device_type');

      // Get slow performance alerts
      const slowRequests = await db('performance_metrics')
        .where('timestamp', '>=', dateFilter)
        .where('load_time_ms', '>', 3000)
        .count('* as count')
        .first();

      return {
        timeframe,
        api: {
          averageResponseTime: Math.round(apiMetrics?.avg_response_time || 0),
          maxResponseTime: apiMetrics?.max_response_time || 0,
          minResponseTime: apiMetrics?.min_response_time || 0,
          totalRequests: parseInt(apiMetrics?.total_requests || 0)
        },
        content: contentMetrics.map(metric => ({
          type: metric.content_type,
          averageLoadTime: Math.round(metric.avg_load_time),
          totalLoads: parseInt(metric.total_loads)
        })),
        devices: deviceMetrics.map(metric => ({
          type: metric.device_type,
          averageLoadTime: Math.round(metric.avg_load_time),
          requests: parseInt(metric.requests)
        })),
        alerts: {
          slowRequests: parseInt(slowRequests?.count || 0),
          threshold: '3000ms'
        }
      };
    } catch (error) {
      logger.error('Failed to get performance analytics:', error);
      throw error;
    }
  }

  /**
   * Store performance alert for slow operations
   */
  static async storePerformanceAlert(performanceData) {
    try {
      await db('performance_alerts').insert({
        alert_type: performanceData.statusCode >= 400 ? 'error' : 'slow_response',
        endpoint: performanceData.endpoint,
        response_time: performanceData.responseTime,
        status_code: performanceData.statusCode,
        user_type: performanceData.userType,
        user_agent: performanceData.userAgent,
        created_at: performanceData.timestamp
      });
    } catch (error) {
      logger.error('Failed to store performance alert:', error);
    }
  }

  /**
   * Monitor content cache performance
   */
  static async trackCachePerformance(cacheKey, hit, loadTime) {
    try {
      const cacheMetric = {
        cache_key: cacheKey,
        cache_hit: hit,
        load_time_ms: loadTime,
        timestamp: new Date()
      };

      await db('cache_performance').insert(cacheMetric);

      // Log cache misses for optimization
      if (!hit) {
        logger.info('Cache miss detected:', { cacheKey, loadTime });
      }

      return cacheMetric;
    } catch (error) {
      logger.error('Failed to track cache performance:', error);
    }
  }

  /**
   * Get mobile-specific performance insights
   */
  static async getMobilePerformanceInsights() {
    try {
      const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000);

      // Mobile vs desktop performance comparison
      const mobilePerf = await db('performance_metrics')
        .where('timestamp', '>=', yesterday)
        .where('device_type', 'mobile')
        .avg('load_time_ms as avg_time')
        .count('* as requests')
        .first();

      const tabletPerf = await db('performance_metrics')
        .where('timestamp', '>=', yesterday)
        .where('device_type', 'tablet')
        .avg('load_time_ms as avg_time')
        .count('* as requests')
        .first();

      // Network type impact
      const networkPerf = await db('performance_metrics')
        .where('timestamp', '>=', yesterday)
        .select([
          'metadata',
          db.raw('AVG(load_time_ms) as avg_time'),
          db.raw('COUNT(*) as requests')
        ])
        .whereNotNull('metadata')
        .groupByRaw("metadata->>'networkType'");

      // Content type performance on mobile
      const mobileContentPerf = await db('performance_metrics')
        .where('timestamp', '>=', yesterday)
        .whereIn('device_type', ['mobile', 'tablet'])
        .select([
          'content_type',
          db.raw('AVG(load_time_ms) as avg_time'),
          db.raw('COUNT(*) as requests')
        ])
        .groupBy('content_type');

      return {
        devices: {
          mobile: {
            averageTime: Math.round(mobilePerf?.avg_time || 0),
            requests: parseInt(mobilePerf?.requests || 0)
          },
          tablet: {
            averageTime: Math.round(tabletPerf?.avg_time || 0),
            requests: parseInt(tabletPerf?.requests || 0)
          }
        },
        contentTypes: mobileContentPerf.map(perf => ({
          type: perf.content_type,
          averageTime: Math.round(perf.avg_time),
          requests: parseInt(perf.requests)
        })),
        recommendations: this.generatePerformanceRecommendations(mobilePerf, tabletPerf, mobileContentPerf)
      };
    } catch (error) {
      logger.error('Failed to get mobile performance insights:', error);
      throw error;
    }
  }

  /**
   * Generate performance optimization recommendations
   */
  static generatePerformanceRecommendations(mobilePerf, tabletPerf, contentPerf) {
    const recommendations = [];

    // Check mobile performance
    if (mobilePerf?.avg_time > 3000) {
      recommendations.push({
        type: 'mobile_optimization',
        priority: 'high',
        message: 'Mobile response times are above 3 seconds. Consider content compression and caching.',
        impact: 'High - affects elementary student engagement'
      });
    }

    // Check content type performance
    const slowContent = contentPerf?.filter(perf => perf.avg_time > 5000) || [];
    if (slowContent.length > 0) {
      recommendations.push({
        type: 'content_optimization',
        priority: 'high',
        message: `Slow loading content detected: ${slowContent.map(c => c.content_type).join(', ')}`,
        impact: 'High - affects lesson delivery quality'
      });
    }

    // Network-specific recommendations
    if (mobilePerf?.avg_time > tabletPerf?.avg_time * 1.5) {
      recommendations.push({
        type: 'network_optimization',
        priority: 'medium',
        message: 'Mobile devices showing significantly slower performance than tablets',
        impact: 'Medium - may affect mobile user experience'
      });
    }

    // Educational content specific recommendations
    recommendations.push({
      type: 'educational_optimization',
      priority: 'medium',
      message: 'For elementary students (grades 4-6), prioritize content loading under 2 seconds',
      impact: 'Medium - maintains student attention and engagement'
    });

    return recommendations;
  }
}

module.exports = PerformanceMonitoringService;
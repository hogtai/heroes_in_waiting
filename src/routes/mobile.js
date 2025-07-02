const express = require('express');
const { body, param, query } = require('express-validator');
const { authenticateFacilitator, authenticateStudent } = require('../middleware/auth');
const { validateRequest } = require('../middleware/validation');
const lessonContentService = require('../services/lessonContentService');
const db = require('../config/database');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * @route   GET /api/mobile/sync/lessons
 * @desc    Get optimized lesson data for mobile synchronization
 * @access  Private (Facilitator/Student)
 */
router.get('/sync/lessons',
  // Allow both facilitator and student access
  (req, res, next) => {
    // Try facilitator auth first, then student auth
    authenticateFacilitator(req, res, (err) => {
      if (err || !req.facilitator) {
        // Try student authentication
        authenticateStudent(req, res, (err) => {
          if (err || !req.student) {
            return res.status(401).json({
              success: false,
              error: 'Authentication required'
            });
          }
          next();
        });
      } else {
        next();
      }
    });
  },
  [
    query('lastSync').optional().isISO8601().withMessage('Last sync must be a valid ISO date'),
    query('deviceType').optional().isIn(['phone', 'tablet']).withMessage('Device type must be phone or tablet'),
    query('quality').optional().isIn(['low', 'medium', 'high']).withMessage('Quality must be low, medium, or high')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lastSync, deviceType = 'phone', quality = 'medium' } = req.query;
      const userType = req.facilitator ? 'facilitator' : 'student';
      const userId = req.facilitator?.id || req.student?.id;

      // Get lessons with mobile-optimized data
      const lessons = await lessonContentService.getAllLessonsWithContent();
      
      // Filter based on last sync if provided
      let filteredLessons = lessons;
      if (lastSync) {
        const syncDate = new Date(lastSync);
        filteredLessons = lessons.filter(lesson => 
          lesson.currentContent && new Date(lesson.currentContent.lastUpdated) > syncDate
        );
      }

      // Optimize content based on device type and quality
      const optimizedLessons = filteredLessons.map(lesson => {
        const optimized = {
          id: lesson.lessonId,
          number: lesson.lessonNumber,
          title: lesson.title,
          description: lesson.description,
          duration: lesson.durationMinutes,
          difficulty: lesson.difficultyLevel,
          published: lesson.isPublished,
          sortOrder: lesson.sortOrder
        };

        // Add content based on user type and device capabilities
        if (lesson.currentContent) {
          optimized.content = {
            versionId: lesson.currentContent.versionId,
            lastUpdated: lesson.currentContent.lastUpdated
          };

          // Adaptive quality for different devices and network conditions
          if (lesson.videoUrl) {
            optimized.media = {
              video: {
                url: getOptimizedVideoUrl(lesson.videoUrl, quality, deviceType),
                thumbnail: lesson.videoThumbnail,
                duration: lesson.videoDurationSeconds,
                quality: quality
              }
            };
          }

          // Age-appropriate content structure for students
          if (userType === 'student') {
            optimized.studentContent = getStudentOptimizedContent(lesson.currentContent.contentStructure);
          } else {
            optimized.facilitatorContent = lesson.currentContent.contentStructure;
          }
        }

        return optimized;
      });

      // Track sync event for analytics (COPPA compliant)
      if (optimizedLessons.length > 0) {
        // Log sync without PII
        logger.info('Mobile lesson sync:', {
          userType,
          userId: userType === 'facilitator' ? userId : 'anonymous', // No student PII
          deviceType,
          quality,
          lessonsCount: optimizedLessons.length,
          syncType: lastSync ? 'incremental' : 'full'
        });
      }

      res.json({
        success: true,
        data: {
          lessons: optimizedLessons,
          syncInfo: {
            timestamp: new Date().toISOString(),
            syncType: lastSync ? 'incremental' : 'full',
            count: optimizedLessons.length,
            quality: quality,
            deviceType: deviceType,
            nextSyncRecommended: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString() // 24 hours
          }
        }
      });
    } catch (error) {
      logger.error('Mobile lesson sync failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to sync lessons'
      });
    }
  }
);

/**
 * @route   GET /api/mobile/lesson/:lessonId/content
 * @desc    Get mobile-optimized lesson content
 * @access  Private (Facilitator/Student)
 */
router.get('/lesson/:lessonId/content',
  // Allow both facilitator and student access
  (req, res, next) => {
    authenticateFacilitator(req, res, (err) => {
      if (err || !req.facilitator) {
        authenticateStudent(req, res, (err) => {
          if (err || !req.student) {
            return res.status(401).json({
              success: false,
              error: 'Authentication required'
            });
          }
          next();
        });
      } else {
        next();
      }
    });
  },
  [
    param('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    query('format').optional().isIn(['mobile', 'tablet']).withMessage('Format must be mobile or tablet'),
    query('offline').optional().isBoolean().withMessage('Offline must be a boolean')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lessonId } = req.params;
      const { format = 'mobile', offline = false } = req.query;
      const userType = req.facilitator ? 'facilitator' : 'student';

      // Get lesson with current content
      const lesson = await db('lessons')
        .select([
          'lessons.*',
          'cv.id as content_version_id',
          'cv.content_structure',
          'cv.metadata'
        ])
        .leftJoin(
          db('content_versions as cv')
            .where('status', 'published')
            .orderBy('version_number', 'desc')
            .limit(1)
            .as('cv'),
          'lessons.id', 'cv.lesson_id'
        )
        .where('lessons.id', lessonId)
        .where('lessons.is_published', true)
        .first();

      if (!lesson) {
        return res.status(404).json({
          success: false,
          error: 'Lesson not found or not published'
        });
      }

      // Optimize content for mobile viewing
      const mobileContent = {
        id: lesson.id,
        number: lesson.lesson_number,
        title: lesson.title,
        description: lesson.description,
        duration: lesson.duration_minutes,
        difficulty: lesson.difficulty_level
      };

      // Add age-appropriate content structure
      if (lesson.content_structure) {
        if (userType === 'student') {
          mobileContent.activities = getStudentActivities(lesson.content_structure, format);
          mobileContent.discussion = getStudentDiscussion(lesson.discussion_questions);
        } else {
          mobileContent.activities = lesson.activities;
          mobileContent.discussion = lesson.discussion_questions;
          mobileContent.facilitatorNotes = getFacilitatorNotes(lesson.content_structure);
        }
      }

      // Add offline-optimized media
      if (lesson.video_url && offline) {
        mobileContent.offlineMedia = {
          video: {
            downloadUrl: lesson.video_url,
            thumbnail: lesson.video_thumbnail,
            size: lesson.video_duration_seconds * 1000, // Estimate size
            quality: 'medium' // Default for offline
          }
        };
      }

      // Track content access (COPPA compliant)
      if (lesson.content_version_id) {
        await db('content_analytics').insert({
          content_version_id: lesson.content_version_id,
          classroom_id: req.student?.classroom_id || null,
          event_type: offline ? 'downloaded' : 'viewed',
          event_data: {
            format: format,
            userType: userType,
            deviceInfo: {
              userAgent: req.get('User-Agent'),
              offline: offline
            }
          },
          user_type: userType,
          user_id: userType === 'facilitator' ? req.facilitator.id : null // No student PII
        });
      }

      res.json({
        success: true,
        data: mobileContent
      });
    } catch (error) {
      logger.error('Get mobile lesson content failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to get lesson content'
      });
    }
  }
);

/**
 * @route   POST /api/mobile/sync/progress
 * @desc    Sync offline progress data to server
 * @access  Private (Student)
 */
router.post('/sync/progress',
  authenticateStudent,
  [
    body('progressData').isArray().withMessage('Progress data must be an array'),
    body('progressData.*.lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    body('progressData.*.activityId').isString().withMessage('Activity ID is required'),
    body('progressData.*.completed').isBoolean().withMessage('Completed must be a boolean'),
    body('progressData.*.timestamp').isISO8601().withMessage('Timestamp must be a valid ISO date')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { progressData } = req.body;
      const studentId = req.student.id;
      const classroomId = req.student.classroom_id;

      const syncResults = [];

      for (const progress of progressData) {
        try {
          // Check if progress already exists (avoid duplicates)
          const existingProgress = await db('student_progress')
            .where({
              student_id: studentId,
              lesson_id: progress.lessonId,
              activity_id: progress.activityId
            })
            .first();

          if (!existingProgress) {
            await db('student_progress').insert({
              student_id: studentId,
              lesson_id: progress.lessonId,
              activity_id: progress.activityId,
              classroom_id: classroomId,
              completed: progress.completed,
              completed_at: progress.timestamp,
              synced_at: new Date()
            });

            syncResults.push({
              lessonId: progress.lessonId,
              activityId: progress.activityId,
              status: 'synced'
            });
          } else {
            syncResults.push({
              lessonId: progress.lessonId,
              activityId: progress.activityId,
              status: 'already_exists'
            });
          }
        } catch (error) {
          logger.error('Failed to sync individual progress:', error);
          syncResults.push({
            lessonId: progress.lessonId,
            activityId: progress.activityId,
            status: 'error',
            error: error.message
          });
        }
      }

      logger.info('Mobile progress sync completed:', {
        studentId: 'anonymous', // COPPA compliance
        classroomId,
        totalItems: progressData.length,
        synced: syncResults.filter(r => r.status === 'synced').length,
        errors: syncResults.filter(r => r.status === 'error').length
      });

      res.json({
        success: true,
        data: {
          syncResults,
          summary: {
            total: progressData.length,
            synced: syncResults.filter(r => r.status === 'synced').length,
            skipped: syncResults.filter(r => r.status === 'already_exists').length,
            errors: syncResults.filter(r => r.status === 'error').length
          }
        }
      });
    } catch (error) {
      logger.error('Mobile progress sync failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to sync progress data'
      });
    }
  }
);

// Helper functions for content optimization

function getOptimizedVideoUrl(originalUrl, quality, deviceType) {
  // In a real implementation, this would return different quality versions
  // For now, return the original URL with quality parameters
  const url = new URL(originalUrl);
  url.searchParams.set('quality', quality);
  url.searchParams.set('device', deviceType);
  return url.toString();
}

function getStudentOptimizedContent(contentStructure) {
  // Filter and simplify content for elementary students
  if (!contentStructure || typeof contentStructure !== 'object') {
    return {};
  }

  return {
    sections: contentStructure.sections || [],
    activities: (contentStructure.activities || []).map(activity => ({
      id: activity.id,
      title: activity.title,
      type: activity.type,
      duration: activity.duration,
      instructions: activity.instructions || activity.description
    }))
  };
}

function getStudentActivities(contentStructure, format) {
  if (!contentStructure || !contentStructure.activities) {
    return [];
  }

  return contentStructure.activities.map(activity => ({
    ...activity,
    instructions: simplifyInstructions(activity.instructions || activity.description, format)
  }));
}

function getStudentDiscussion(discussionQuestions) {
  if (!Array.isArray(discussionQuestions)) {
    return [];
  }

  // Limit to age-appropriate questions for grades 4-6
  return discussionQuestions.slice(0, 3).map(question => ({
    question: question,
    gradeLevel: 'elementary'
  }));
}

function getFacilitatorNotes(contentStructure) {
  return contentStructure?.facilitatorNotes || {
    preparation: 'Review lesson content and materials',
    timing: 'Allow flexibility for student engagement',
    adaptations: 'Consider individual student needs'
  };
}

function simplifyInstructions(instructions, format) {
  if (!instructions) return '';
  
  // Simplify language for elementary students
  return instructions
    .replace(/\b(utilize|demonstrate|comprehend)\b/gi, match => {
      const simple = { utilize: 'use', demonstrate: 'show', comprehend: 'understand' };
      return simple[match.toLowerCase()] || match;
    })
    .split('. ')
    .slice(0, format === 'mobile' ? 2 : 3) // Shorter instructions on mobile
    .join('. ');
}

module.exports = router;
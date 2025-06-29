const express = require('express');
const { body, param, query } = require('express-validator');
const { authenticateFacilitator } = require('../middleware/auth');
const { validateRequest } = require('../middleware/validation');
const lessonContentService = require('../services/lessonContentService');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * @route   GET /api/lesson-content/lessons
 * @desc    Get all 12 lessons with their current content versions
 * @access  Private (Facilitator)
 */
router.get('/lessons',
  authenticateFacilitator,
  async (req, res) => {
    try {
      const lessons = await lessonContentService.getAllLessonsWithContent();
      
      res.json({
        success: true,
        data: {
          lessons,
          totalLessons: lessons.length,
          curriculum: 'Heroes in Waiting Anti-Bullying'
        }
      });
    } catch (error) {
      logger.error('Get lessons with content failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to get lessons with content'
      });
    }
  }
);

/**
 * @route   GET /api/lesson-content/lessons/:lessonId/history
 * @desc    Get content version history for a specific lesson
 * @access  Private (Facilitator)
 */
router.get('/lessons/:lessonId/history',
  authenticateFacilitator,
  [
    param('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lessonId } = req.params;
      const history = await lessonContentService.getLessonContentHistory(lessonId);
      
      res.json({
        success: true,
        data: {
          lessonId,
          versions: history
        }
      });
    } catch (error) {
      logger.error('Get lesson content history failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to get lesson content history'
      });
    }
  }
);

/**
 * @route   POST /api/lesson-content/lessons/:lessonId/versions
 * @desc    Create new content version for lesson updates
 * @access  Private (Facilitator)
 */
router.post('/lessons/:lessonId/versions',
  authenticateFacilitator,
  [
    param('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    body('title').optional().trim().isLength({ min: 1, max: 255 }).withMessage('Title must be between 1 and 255 characters'),
    body('description').optional().trim().isLength({ max: 1000 }).withMessage('Description must be less than 1000 characters'),
    body('contentStructure').isObject().withMessage('Content structure is required'),
    body('changeSummary').trim().isLength({ min: 1, max: 500 }).withMessage('Change summary is required and must be less than 500 characters'),
    body('metadata').optional().isObject().withMessage('Metadata must be an object')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lessonId } = req.params;
      const facilitatorId = req.facilitator.id;
      
      const newVersion = await lessonContentService.createLessonContentVersion(
        lessonId,
        req.body,
        facilitatorId
      );
      
      res.status(201).json({
        success: true,
        data: {
          id: newVersion.id,
          lessonId: newVersion.lesson_id,
          versionNumber: newVersion.version_number,
          title: newVersion.title,
          status: newVersion.status,
          createdAt: newVersion.created_at
        }
      });
    } catch (error) {
      logger.error('Create lesson content version failed:', error);
      
      if (error.message.includes('PII') || error.message.includes('personally identifiable')) {
        return res.status(400).json({
          success: false,
          error: error.message
        });
      }
      
      res.status(500).json({
        success: false,
        error: 'Failed to create lesson content version'
      });
    }
  }
);

/**
 * @route   PUT /api/lesson-content/lessons/:lessonId/media
 * @desc    Update lesson media content (videos, thumbnails)
 * @access  Private (Facilitator)
 */
router.put('/lessons/:lessonId/media',
  authenticateFacilitator,
  [
    param('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    body('videoUrl').optional().isURL().withMessage('Video URL must be a valid URL'),
    body('videoThumbnail').optional().isURL().withMessage('Video thumbnail must be a valid URL'),
    body('videoDurationSeconds').optional().isInt({ min: 1 }).withMessage('Video duration must be a positive integer')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lessonId } = req.params;
      const facilitatorId = req.facilitator.id;
      
      const updatedLesson = await lessonContentService.updateLessonMedia(
        lessonId,
        req.body,
        facilitatorId
      );
      
      res.json({
        success: true,
        data: {
          lessonId: updatedLesson.id,
          videoUrl: updatedLesson.video_url,
          videoThumbnail: updatedLesson.video_thumbnail,
          videoDurationSeconds: updatedLesson.video_duration_seconds,
          updatedAt: updatedLesson.updated_at
        }
      });
    } catch (error) {
      logger.error('Update lesson media failed:', error);
      
      if (error.message.includes('PII') || error.message.includes('personally identifiable')) {
        return res.status(400).json({
          success: false,
          error: error.message
        });
      }
      
      res.status(500).json({
        success: false,
        error: 'Failed to update lesson media'
      });
    }
  }
);

/**
 * @route   POST /api/lesson-content/versions/:versionId/publish
 * @desc    Publish a content version (make it live)
 * @access  Private (Facilitator)
 */
router.post('/versions/:versionId/publish',
  authenticateFacilitator,
  [
    param('versionId').isUUID().withMessage('Version ID must be a valid UUID')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { versionId } = req.params;
      const facilitatorId = req.facilitator.id;
      
      const publishedVersion = await lessonContentService.publishContentVersion(
        versionId,
        facilitatorId
      );
      
      res.json({
        success: true,
        data: {
          versionId: publishedVersion.id,
          lessonId: publishedVersion.lesson_id,
          status: publishedVersion.status,
          publishedAt: publishedVersion.updated_at
        }
      });
    } catch (error) {
      logger.error('Publish content version failed:', error);
      
      if (error.message.includes('not found') || error.message.includes('already published')) {
        return res.status(400).json({
          success: false,
          error: error.message
        });
      }
      
      res.status(500).json({
        success: false,
        error: 'Failed to publish content version'
      });
    }
  }
);

/**
 * @route   GET /api/lesson-content/lessons/:lessonId/analytics
 * @desc    Get content analytics for a specific lesson
 * @access  Private (Facilitator)
 */
router.get('/lessons/:lessonId/analytics',
  authenticateFacilitator,
  [
    param('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    query('timeframe').optional().isIn(['7d', '30d', '90d']).withMessage('Invalid timeframe')
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { lessonId } = req.params;
      const { timeframe = '30d' } = req.query;
      
      const analytics = await lessonContentService.getLessonContentAnalytics(
        lessonId,
        timeframe
      );
      
      res.json({
        success: true,
        data: analytics
      });
    } catch (error) {
      logger.error('Get lesson content analytics failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to get lesson content analytics'
      });
    }
  }
);

/**
 * @route   GET /api/lesson-content/curriculum/overview
 * @desc    Get overview of all 12 lessons curriculum status
 * @access  Private (Facilitator)
 */
router.get('/curriculum/overview',
  authenticateFacilitator,
  async (req, res) => {
    try {
      const lessons = await lessonContentService.getAllLessonsWithContent();
      
      const overview = {
        totalLessons: 12,
        publishedLessons: lessons.filter(l => l.isPublished).length,
        lessonsWithContent: lessons.filter(l => l.currentContent).length,
        averageDuration: Math.round(
          lessons.reduce((sum, l) => sum + (l.durationMinutes || 0), 0) / lessons.length
        ),
        curriculum: {
          name: 'Heroes in Waiting',
          focus: 'Anti-Bullying Education',
          targetAge: 'Grades 4-6 (Elementary)',
          coppaCompliant: true
        },
        lessonBreakdown: lessons.map(lesson => ({
          number: lesson.lessonNumber,
          title: lesson.title,
          duration: lesson.durationMinutes,
          published: lesson.isPublished,
          hasContent: !!lesson.currentContent,
          lastUpdated: lesson.currentContent?.lastUpdated
        }))
      };
      
      res.json({
        success: true,
        data: overview
      });
    } catch (error) {
      logger.error('Get curriculum overview failed:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to get curriculum overview'
      });
    }
  }
);

module.exports = router;
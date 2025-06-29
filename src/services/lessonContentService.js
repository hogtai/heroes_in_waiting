const db = require('../config/database');
const logger = require('../utils/logger');
const { sanitizeContentStructure, sanitizeMetadata, detectPII } = require('../utils/contentSanitizer');

/**
 * Service for managing the 12 Heroes in Waiting lesson content versions
 * This service handles content management for the static anti-bullying curriculum
 */
class LessonContentService {
  
  /**
   * Get all lessons with their current content versions
   */
  async getAllLessonsWithContent() {
    try {
      const lessons = await db('lessons')
        .select([
          'lessons.*',
          'cv.id as content_version_id',
          'cv.version_number',
          'cv.content_structure',
          'cv.metadata',
          'cv.status as content_status',
          'cv.updated_at as content_updated_at'
        ])
        .leftJoin(
          db('content_versions')
            .select('*')
            .whereIn('id', 
              db('content_versions')
                .select(db.raw('id'))
                .whereIn(['lesson_id', 'version_number'], 
                  db('content_versions')
                    .select('lesson_id', db.raw('MAX(version_number) as version_number'))
                    .where('status', 'published')
                    .groupBy('lesson_id')
                )
            )
            .as('cv'),
          'lessons.id', 'cv.lesson_id'
        )
        .orderBy('lessons.sort_order', 'asc');

      return lessons.map(lesson => ({
        lessonId: lesson.id,
        lessonNumber: lesson.lesson_number,
        title: lesson.title,
        description: lesson.description,
        learningObjectives: lesson.learning_objectives,
        durationMinutes: lesson.duration_minutes,
        difficultyLevel: lesson.difficulty_level,
        videoUrl: lesson.video_url,
        videoThumbnail: lesson.video_thumbnail,
        videoDurationSeconds: lesson.video_duration_seconds,
        isPublished: lesson.is_published,
        sortOrder: lesson.sort_order,
        currentContent: lesson.content_version_id ? {
          versionId: lesson.content_version_id,
          versionNumber: lesson.version_number,
          contentStructure: lesson.content_structure,
          metadata: lesson.metadata,
          status: lesson.content_status,
          lastUpdated: lesson.content_updated_at
        } : null
      }));
    } catch (error) {
      logger.error('Failed to get lessons with content:', error);
      throw error;
    }
  }

  /**
   * Get content version history for a specific lesson
   */
  async getLessonContentHistory(lessonId) {
    try {
      const versions = await db('content_versions')
        .select([
          'content_versions.*',
          'facilitators.first_name',
          'facilitators.last_name'
        ])
        .leftJoin('facilitators', 'content_versions.created_by', 'facilitators.id')
        .where('content_versions.lesson_id', lessonId)
        .orderBy('content_versions.version_number', 'desc');

      return versions.map(version => ({
        id: version.id,
        versionNumber: version.version_number,
        title: version.title,
        description: version.description,
        status: version.status,
        changeSummary: version.change_summary,
        createdBy: `${version.first_name} ${version.last_name}`,
        createdAt: version.created_at,
        updatedAt: version.updated_at
      }));
    } catch (error) {
      logger.error('Failed to get lesson content history:', error);
      throw error;
    }
  }

  /**
   * Create a new content version for lesson updates
   */
  async createLessonContentVersion(lessonId, contentData, facilitatorId) {
    try {
      const { title, description, contentStructure, changeSummary, metadata } = contentData;

      // Validate lesson exists
      const lesson = await db('lessons').where('id', lessonId).first();
      if (!lesson) {
        throw new Error('Lesson not found');
      }

      // Sanitize content for educational use with anti-bullying focus
      const sanitizedContentStructure = sanitizeContentStructure(contentStructure, 'facilitator');
      const sanitizedMetadata = sanitizeMetadata({
        ...metadata,
        lessonNumber: lesson.lesson_number,
        curriculum: 'heroes-in-waiting',
        antiABullyingFocus: true,
        coppaCompliant: true
      });

      // COPPA compliance - detect any PII in content
      const piiDetections = detectPII(JSON.stringify(sanitizedContentStructure));
      if (piiDetections.length > 0) {
        logger.warn('PII detected in lesson content version:', {
          lessonId,
          lessonNumber: lesson.lesson_number,
          facilitatorId,
          piiDetections: piiDetections.map(d => ({ type: d.type, count: d.count }))
        });
        
        // For educational content with minors, we should be strict about PII
        throw new Error('Content contains personally identifiable information and cannot be saved');
      }

      // Get next version number
      const lastVersion = await db('content_versions')
        .where('lesson_id', lessonId)
        .orderBy('version_number', 'desc')
        .first();
      
      const versionNumber = lastVersion ? lastVersion.version_number + 1 : 1;

      // Create new version
      const [newVersion] = await db('content_versions')
        .insert({
          lesson_id: lessonId,
          version_number: versionNumber,
          title: title || `Lesson ${lesson.lesson_number} - Version ${versionNumber}`,
          description: description,
          content_structure: sanitizedContentStructure,
          metadata: sanitizedMetadata,
          created_by: facilitatorId,
          change_summary: changeSummary,
          status: 'draft' // New versions start as draft
        })
        .returning('*');

      logger.info('Lesson content version created:', {
        versionId: newVersion.id,
        lessonId,
        lessonNumber: lesson.lesson_number,
        versionNumber,
        facilitatorId
      });

      return newVersion;
    } catch (error) {
      logger.error('Failed to create lesson content version:', error);
      throw error;
    }
  }

  /**
   * Update lesson media content (videos, thumbnails)
   */
  async updateLessonMedia(lessonId, mediaData, facilitatorId) {
    try {
      const { videoUrl, videoThumbnail, videoDurationSeconds } = mediaData;

      // Validate URLs and detect any PII
      if (videoUrl && detectPII(videoUrl).length > 0) {
        throw new Error('Video URL contains personally identifiable information');
      }

      const updateData = {
        updated_at: new Date()
      };

      if (videoUrl) updateData.video_url = videoUrl;
      if (videoThumbnail) updateData.video_thumbnail = videoThumbnail;
      if (videoDurationSeconds) updateData.video_duration_seconds = videoDurationSeconds;

      const [updatedLesson] = await db('lessons')
        .where('id', lessonId)
        .update(updateData)
        .returning('*');

      if (!updatedLesson) {
        throw new Error('Lesson not found');
      }

      logger.info('Lesson media updated:', {
        lessonId,
        facilitatorId,
        updatedFields: Object.keys(mediaData)
      });

      return updatedLesson;
    } catch (error) {
      logger.error('Failed to update lesson media:', error);
      throw error;
    }
  }

  /**
   * Publish a content version (make it live)
   */
  async publishContentVersion(versionId, facilitatorId) {
    try {
      const version = await db('content_versions')
        .where('id', versionId)
        .first();

      if (!version) {
        throw new Error('Content version not found');
      }

      if (version.status === 'published') {
        throw new Error('Content version is already published');
      }

      // Unpublish current published version for this lesson
      await db('content_versions')
        .where('lesson_id', version.lesson_id)
        .where('status', 'published')
        .update({ status: 'archived' });

      // Publish the new version
      const [publishedVersion] = await db('content_versions')
        .where('id', versionId)
        .update({
          status: 'published',
          updated_at: new Date()
        })
        .returning('*');

      logger.info('Content version published:', {
        versionId,
        lessonId: version.lesson_id,
        facilitatorId
      });

      return publishedVersion;
    } catch (error) {
      logger.error('Failed to publish content version:', error);
      throw error;
    }
  }

  /**
   * Get analytics for lesson content usage
   */
  async getLessonContentAnalytics(lessonId, timeframe = '30d') {
    try {
      // Calculate date filter
      let dateFilter;
      switch (timeframe) {
        case '7d':
          dateFilter = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
          break;
        case '30d':
          dateFilter = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
          break;
        case '90d':
          dateFilter = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000);
          break;
        default:
          dateFilter = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
      }

      // Get lesson content analytics
      const contentVersions = await db('content_versions')
        .select('id')
        .where('lesson_id', lessonId);

      const versionIds = contentVersions.map(cv => cv.id);

      if (versionIds.length === 0) {
        return {
          lessonId,
          timeframe,
          totalEvents: 0,
          eventsByType: {},
          uniqueClassrooms: 0,
          averageEngagement: 0
        };
      }

      // Get analytics data
      const analytics = await db('content_analytics')
        .whereIn('content_version_id', versionIds)
        .where('event_timestamp', '>=', dateFilter)
        .select([
          'event_type',
          'classroom_id',
          'event_timestamp'
        ]);

      const eventsByType = {};
      const uniqueClassrooms = new Set();

      analytics.forEach(event => {
        eventsByType[event.event_type] = (eventsByType[event.event_type] || 0) + 1;
        if (event.classroom_id) {
          uniqueClassrooms.add(event.classroom_id);
        }
      });

      return {
        lessonId,
        timeframe,
        totalEvents: analytics.length,
        eventsByType,
        uniqueClassrooms: uniqueClassrooms.size,
        averageEngagement: analytics.length > 0 ? 
          (eventsByType.viewed || 0) / Math.max(uniqueClassrooms.size, 1) : 0
      };
    } catch (error) {
      logger.error('Failed to get lesson content analytics:', error);
      throw error;
    }
  }
}

module.exports = new LessonContentService();
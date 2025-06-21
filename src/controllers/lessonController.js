const db = require('../config/database');
const logger = require('../utils/logger');

/**
 * @desc    Get all published lessons
 * @route   GET /api/lessons
 * @access  Private (Student/Facilitator)
 */
async function getLessons(req, res) {
  try {
    const { sortBy = 'lesson_number', sortOrder = 'asc' } = req.query;

    const lessons = await db('lessons')
      .select([
        'id',
        'lesson_number',
        'title',
        'description',
        'learning_objectives',
        'duration_minutes',
        'difficulty_level',
        'video_thumbnail',
        'video_duration_seconds',
        'sort_order',
        'created_at'
      ])
      .where('is_published', true)
      .orderBy(sortBy, sortOrder);

    res.json({
      success: true,
      data: {
        lessons: lessons.map(lesson => ({
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          description: lesson.description,
          learningObjectives: lesson.learning_objectives,
          durationMinutes: lesson.duration_minutes,
          difficultyLevel: lesson.difficulty_level,
          videoThumbnail: lesson.video_thumbnail,
          videoDurationSeconds: lesson.video_duration_seconds,
          sortOrder: lesson.sort_order,
          createdAt: lesson.created_at
        }))
      }
    });
  } catch (error) {
    logger.error('Get lessons failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get lessons'
    });
  }
}

/**
 * @desc    Get a specific lesson with full content
 * @route   GET /api/lessons/:id
 * @access  Private (Student/Facilitator)
 */
async function getLesson(req, res) {
  try {
    const lessonId = req.params.id;

    const lesson = await db('lessons')
      .where({ id: lessonId, is_published: true })
      .first();

    if (!lesson) {
      return res.status(404).json({
        success: false,
        error: 'Lesson not found'
      });
    }

    // Log lesson access for analytics
    const userType = req.facilitator ? 'facilitator' : 'student';
    const userId = req.facilitator ? req.facilitator.id : req.student.id;
    const classroomId = req.classroom ? req.classroom.id : null;

    await db('analytics_events').insert({
      [userType + '_id']: userId,
      classroom_id: classroomId,
      lesson_id: lessonId,
      event_type: 'lesson_viewed',
      event_category: 'content_access',
      event_action: 'view',
      event_label: lesson.title,
      device_type: req.get('X-Device-Type') || 'unknown',
      platform: req.get('X-Platform') || 'unknown',
      app_version: req.get('X-App-Version') || null
    });

    res.json({
      success: true,
      data: {
        lesson: {
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          description: lesson.description,
          learningObjectives: lesson.learning_objectives,
          durationMinutes: lesson.duration_minutes,
          difficultyLevel: lesson.difficulty_level,
          contentStructure: lesson.content_structure,
          videoUrl: lesson.video_url,
          videoThumbnail: lesson.video_thumbnail,
          videoDurationSeconds: lesson.video_duration_seconds,
          downloadableResources: lesson.downloadable_resources,
          activities: lesson.activities,
          discussionQuestions: lesson.discussion_questions,
          sortOrder: lesson.sort_order,
          createdAt: lesson.created_at
        }
      }
    });
  } catch (error) {
    logger.error('Get lesson failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get lesson'
    });
  }
}

/**
 * @desc    Get lesson progress for a student
 * @route   GET /api/lessons/:id/progress
 * @access  Private (Student)
 */
async function getLessonProgress(req, res) {
  try {
    const lessonId = req.params.id;
    const studentId = req.student.id;

    // Verify lesson exists
    const lesson = await db('lessons')
      .where({ id: lessonId, is_published: true })
      .first();

    if (!lesson) {
      return res.status(404).json({
        success: false,
        error: 'Lesson not found'
      });
    }

    // Get student progress
    const progress = await db('student_progress')
      .where({ student_id: studentId, lesson_id: lessonId })
      .first();

    if (!progress) {
      // Create initial progress record
      const [newProgress] = await db('student_progress')
        .insert({
          student_id: studentId,
          lesson_id: lessonId,
          completion_status: 'not_started',
          progress_percentage: 0
        })
        .returning('*');

      return res.json({
        success: true,
        data: {
          progress: {
            id: newProgress.id,
            lessonId: newProgress.lesson_id,
            completionStatus: newProgress.completion_status,
            progressPercentage: newProgress.progress_percentage,
            timeSpentMinutes: newProgress.time_spent_minutes,
            videoWatchPercentage: newProgress.video_watch_percentage,
            activitiesCompleted: newProgress.activities_completed,
            totalActivities: newProgress.total_activities,
            startedAt: newProgress.started_at,
            completedAt: newProgress.completed_at,
            createdAt: newProgress.created_at
          }
        }
      });
    }

    res.json({
      success: true,
      data: {
        progress: {
          id: progress.id,
          lessonId: progress.lesson_id,
          completionStatus: progress.completion_status,
          progressPercentage: progress.progress_percentage,
          timeSpentMinutes: progress.time_spent_minutes,
          videoWatchPercentage: progress.video_watch_percentage,
          activitiesCompleted: progress.activities_completed,
          totalActivities: progress.total_activities,
          activityResponses: progress.activity_responses,
          startedAt: progress.started_at,
          completedAt: progress.completed_at,
          createdAt: progress.created_at,
          updatedAt: progress.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Get lesson progress failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get lesson progress'
    });
  }
}

/**
 * @desc    Create or manage lesson (Admin/Content Management)
 * @route   POST /api/lessons
 * @access  Private (Admin - for content management)
 */
async function createLesson(req, res) {
  try {
    const {
      lessonNumber,
      title,
      description,
      learningObjectives,
      durationMinutes,
      difficultyLevel,
      contentStructure,
      videoUrl,
      videoThumbnail,
      videoDurationSeconds,
      downloadableResources,
      activities,
      discussionQuestions,
      isPublished = false,
      sortOrder
    } = req.body;

    // Check if lesson number already exists
    const existingLesson = await db('lessons')
      .where('lesson_number', lessonNumber)
      .first();

    if (existingLesson) {
      return res.status(409).json({
        success: false,
        error: 'Lesson with this number already exists'
      });
    }

    const [lesson] = await db('lessons')
      .insert({
        lesson_number: lessonNumber,
        title,
        description,
        learning_objectives: learningObjectives,
        duration_minutes: durationMinutes,
        difficulty_level: difficultyLevel,
        content_structure: contentStructure,
        video_url: videoUrl,
        video_thumbnail: videoThumbnail,
        video_duration_seconds: videoDurationSeconds,
        downloadable_resources: downloadableResources,
        activities,
        discussion_questions: discussionQuestions,
        is_published: isPublished,
        sort_order: sortOrder || lessonNumber
      })
      .returning('*');

    logger.info('Lesson created:', {
      lessonId: lesson.id,
      lessonNumber: lesson.lesson_number,
      title: lesson.title
    });

    res.status(201).json({
      success: true,
      data: {
        lesson: {
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          description: lesson.description,
          learningObjectives: lesson.learning_objectives,
          durationMinutes: lesson.duration_minutes,
          difficultyLevel: lesson.difficulty_level,
          isPublished: lesson.is_published,
          createdAt: lesson.created_at
        }
      }
    });
  } catch (error) {
    logger.error('Create lesson failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create lesson'
    });
  }
}

/**
 * @desc    Update lesson
 * @route   PUT /api/lessons/:id
 * @access  Private (Admin - for content management)
 */
async function updateLesson(req, res) {
  try {
    const lessonId = req.params.id;
    const updateData = { ...req.body };
    
    // Convert camelCase to snake_case for database
    const dbUpdateData = {};
    Object.keys(updateData).forEach(key => {
      switch (key) {
        case 'lessonNumber':
          dbUpdateData.lesson_number = updateData[key];
          break;
        case 'learningObjectives':
          dbUpdateData.learning_objectives = updateData[key];
          break;
        case 'durationMinutes':
          dbUpdateData.duration_minutes = updateData[key];
          break;
        case 'difficultyLevel':
          dbUpdateData.difficulty_level = updateData[key];
          break;
        case 'contentStructure':
          dbUpdateData.content_structure = updateData[key];
          break;
        case 'videoUrl':
          dbUpdateData.video_url = updateData[key];
          break;
        case 'videoThumbnail':
          dbUpdateData.video_thumbnail = updateData[key];
          break;
        case 'videoDurationSeconds':
          dbUpdateData.video_duration_seconds = updateData[key];
          break;
        case 'downloadableResources':
          dbUpdateData.downloadable_resources = updateData[key];
          break;
        case 'discussionQuestions':
          dbUpdateData.discussion_questions = updateData[key];
          break;
        case 'isPublished':
          dbUpdateData.is_published = updateData[key];
          break;
        case 'sortOrder':
          dbUpdateData.sort_order = updateData[key];
          break;
        default:
          dbUpdateData[key] = updateData[key];
      }
    });
    
    dbUpdateData.updated_at = new Date();

    const [lesson] = await db('lessons')
      .where('id', lessonId)
      .update(dbUpdateData)
      .returning('*');

    if (!lesson) {
      return res.status(404).json({
        success: false,
        error: 'Lesson not found'
      });
    }

    logger.info('Lesson updated:', {
      lessonId: lesson.id,
      lessonNumber: lesson.lesson_number,
      title: lesson.title
    });

    res.json({
      success: true,
      data: {
        lesson: {
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          description: lesson.description,
          isPublished: lesson.is_published,
          updatedAt: lesson.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Update lesson failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update lesson'
    });
  }
}

/**
 * @desc    Get classroom lesson progress summary
 * @route   GET /api/lessons/classroom/:classroomId/progress
 * @access  Private (Facilitator)
 */
async function getClassroomLessonProgress(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const facilitatorId = req.facilitator.id;

    // Verify facilitator owns the classroom
    const classroom = await db('classrooms')
      .where({ id: classroomId, facilitator_id: facilitatorId })
      .first();

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found'
      });
    }

    // Get lesson progress summary for the classroom
    const progressSummary = await db('lessons')
      .leftJoin('student_progress', function() {
        this.on('lessons.id', '=', 'student_progress.lesson_id')
          .andOn('student_progress.student_id', 'in', 
            db('students').select('id').where('classroom_id', classroomId)
          );
      })
      .select([
        'lessons.id',
        'lessons.lesson_number',
        'lessons.title',
        'lessons.duration_minutes',
        db.raw('COUNT(DISTINCT student_progress.student_id) as students_started'),
        db.raw('COUNT(DISTINCT CASE WHEN student_progress.completion_status = \'completed\' THEN student_progress.student_id END) as students_completed'),
        db.raw('AVG(student_progress.progress_percentage) as average_progress'),
        db.raw('SUM(student_progress.time_spent_minutes) as total_time_spent')
      ])
      .where('lessons.is_published', true)
      .groupBy('lessons.id', 'lessons.lesson_number', 'lessons.title', 'lessons.duration_minutes')
      .orderBy('lessons.lesson_number');

    // Get total student count in classroom
    const [{ count: totalStudents }] = await db('students')
      .count('id as count')
      .where({ classroom_id: classroomId, is_active: true });

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          totalStudents: parseInt(totalStudents)
        },
        lessons: progressSummary.map(lesson => ({
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          durationMinutes: lesson.duration_minutes,
          studentsStarted: parseInt(lesson.students_started),
          studentsCompleted: parseInt(lesson.students_completed),
          averageProgress: parseFloat(lesson.average_progress) || 0,
          totalTimeSpent: parseInt(lesson.total_time_spent) || 0,
          completionRate: totalStudents > 0 ? (parseInt(lesson.students_completed) / parseInt(totalStudents) * 100) : 0
        }))
      }
    });
  } catch (error) {
    logger.error('Get classroom lesson progress failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom lesson progress'
    });
  }
}

module.exports = {
  getLessons,
  getLesson,
  getLessonProgress,
  createLesson,
  updateLesson,
  getClassroomLessonProgress
};
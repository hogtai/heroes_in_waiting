const db = require('../config/database');
const logger = require('../utils/logger');

/**
 * @desc    Track analytics event
 * @route   POST /api/analytics/event
 * @access  Private (Student/Facilitator)
 */
async function trackEvent(req, res) {
  try {
    const {
      eventType,
      eventCategory,
      eventAction,
      eventLabel,
      eventValue,
      eventProperties,
      deviceType,
      platform,
      appVersion,
      sessionIdLocal,
      sessionDuration
    } = req.body;

    // Determine user type and IDs
    const userType = req.facilitator ? 'facilitator' : 'student';
    const userId = req.facilitator ? req.facilitator.id : req.student.id;
    const classroomId = req.classroom ? req.classroom.id : null;

    // Insert analytics event
    const [event] = await db('analytics_events')
      .insert({
        [`${userType}_id`]: userId,
        classroom_id: classroomId,
        lesson_id: req.body.lessonId || null,
        session_id: req.body.sessionId || null,
        event_type: eventType,
        event_category: eventCategory,
        event_action: eventAction,
        event_label: eventLabel,
        event_value: eventValue,
        event_properties: eventProperties,
        device_type: deviceType || req.get('X-Device-Type') || 'unknown',
        platform: platform || req.get('X-Platform') || 'unknown',
        app_version: appVersion || req.get('X-App-Version') || null,
        session_id_local: sessionIdLocal,
        session_duration: sessionDuration
      })
      .returning(['id', 'event_timestamp']);

    res.status(201).json({
      success: true,
      data: {
        eventId: event.id,
        timestamp: event.event_timestamp
      }
    });
  } catch (error) {
    logger.error('Track analytics event failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to track event'
    });
  }
}

/**
 * @desc    Get classroom analytics dashboard
 * @route   GET /api/analytics/classroom/:classroomId/dashboard
 * @access  Private (Facilitator)
 */
async function getClassroomDashboard(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const facilitatorId = req.facilitator.id;
    const { timeframe = '30d' } = req.query;

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

    // Calculate date range
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

    // Get engagement metrics
    const engagementMetrics = await db('analytics_events')
      .select([
        db.raw('COUNT(DISTINCT student_id) as active_students'),
        db.raw('COUNT(DISTINCT lesson_id) as lessons_accessed'),
        db.raw('COUNT(*) as total_events'),
        db.raw('COUNT(CASE WHEN event_category = \'engagement\' THEN 1 END) as engagement_events'),
        db.raw('AVG(session_duration) as avg_session_duration')
      ])
      .where('classroom_id', classroomId)
      .andWhere('event_timestamp', '>=', dateFilter)
      .first();

    // Get lesson engagement breakdown
    const lessonEngagement = await db('analytics_events')
      .join('lessons', 'analytics_events.lesson_id', 'lessons.id')
      .select([
        'lessons.id',
        'lessons.lesson_number',
        'lessons.title',
        db.raw('COUNT(DISTINCT analytics_events.student_id) as unique_students'),
        db.raw('COUNT(*) as total_events'),
        db.raw('COUNT(CASE WHEN analytics_events.event_type = \'lesson_completed\' THEN 1 END) as completions')
      ])
      .where('analytics_events.classroom_id', classroomId)
      .andWhere('analytics_events.event_timestamp', '>=', dateFilter)
      .groupBy('lessons.id', 'lessons.lesson_number', 'lessons.title')
      .orderBy('lessons.lesson_number');

    // Get device/platform breakdown
    const platformBreakdown = await db('analytics_events')
      .select([
        'platform',
        'device_type',
        db.raw('COUNT(DISTINCT student_id) as unique_users'),
        db.raw('COUNT(*) as total_events')
      ])
      .where('classroom_id', classroomId)
      .andWhere('event_timestamp', '>=', dateFilter)
      .groupBy('platform', 'device_type');

    // Get daily activity trend
    const dailyActivity = await db('analytics_events')
      .select([
        db.raw('DATE(event_timestamp) as date'),
        db.raw('COUNT(DISTINCT student_id) as active_students'),
        db.raw('COUNT(*) as total_events')
      ])
      .where('classroom_id', classroomId)
      .andWhere('event_timestamp', '>=', dateFilter)
      .groupBy(db.raw('DATE(event_timestamp)'))
      .orderBy('date');

    // Get student activity summary
    const studentActivity = await db('students')
      .leftJoin('analytics_events', function() {
        this.on('students.id', '=', 'analytics_events.student_id')
          .andOn('analytics_events.event_timestamp', '>=', db.raw('?', [dateFilter]));
      })
      .select([
        'students.anonymous_id',
        'students.grade_level',
        'students.last_active_at',
        db.raw('COUNT(analytics_events.id) as total_events'),
        db.raw('COUNT(DISTINCT analytics_events.lesson_id) as lessons_accessed'),
        db.raw('MAX(analytics_events.event_timestamp) as last_event_time')
      ])
      .where('students.classroom_id', classroomId)
      .andWhere('students.is_active', true)
      .groupBy('students.id', 'students.anonymous_id', 'students.grade_level', 'students.last_active_at')
      .orderBy('total_events', 'desc')
      .limit(20);

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name
        },
        timeframe,
        summary: {
          activeStudents: parseInt(engagementMetrics.active_students) || 0,
          lessonsAccessed: parseInt(engagementMetrics.lessons_accessed) || 0,
          totalEvents: parseInt(engagementMetrics.total_events) || 0,
          engagementEvents: parseInt(engagementMetrics.engagement_events) || 0,
          avgSessionDuration: Math.round(parseFloat(engagementMetrics.avg_session_duration) || 0)
        },
        lessonEngagement: lessonEngagement.map(lesson => ({
          lessonId: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          uniqueStudents: parseInt(lesson.unique_students),
          totalEvents: parseInt(lesson.total_events),
          completions: parseInt(lesson.completions)
        })),
        platformBreakdown: platformBreakdown.map(platform => ({
          platform: platform.platform,
          deviceType: platform.device_type,
          uniqueUsers: parseInt(platform.unique_users),
          totalEvents: parseInt(platform.total_events)
        })),
        dailyActivity: dailyActivity.map(day => ({
          date: day.date,
          activeStudents: parseInt(day.active_students),
          totalEvents: parseInt(day.total_events)
        })),
        topStudents: studentActivity.map(student => ({
          anonymousId: student.anonymous_id,
          gradeLevel: student.grade_level,
          totalEvents: parseInt(student.total_events),
          lessonsAccessed: parseInt(student.lessons_accessed),
          lastActiveAt: student.last_active_at,
          lastEventTime: student.last_event_time
        }))
      }
    });
  } catch (error) {
    logger.error('Get classroom dashboard failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom dashboard'
    });
  }
}

/**
 * @desc    Get facilitator analytics overview
 * @route   GET /api/analytics/facilitator/overview
 * @access  Private (Facilitator)
 */
async function getFacilitatorOverview(req, res) {
  try {
    const facilitatorId = req.facilitator.id;
    const { timeframe = '30d' } = req.query;

    // Calculate date range
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

    // Get overall facilitator metrics
    const overallMetrics = await db('classrooms')
      .leftJoin('students', 'classrooms.id', 'students.classroom_id')
      .leftJoin('analytics_events', function() {
        this.on('students.id', '=', 'analytics_events.student_id')
          .andOn('analytics_events.event_timestamp', '>=', db.raw('?', [dateFilter]));
      })
      .select([
        db.raw('COUNT(DISTINCT classrooms.id) as total_classrooms'),
        db.raw('COUNT(DISTINCT students.id) as total_students'),
        db.raw('COUNT(DISTINCT analytics_events.student_id) as active_students'),
        db.raw('COUNT(analytics_events.id) as total_events'),
        db.raw('AVG(analytics_events.session_duration) as avg_session_duration')
      ])
      .where('classrooms.facilitator_id', facilitatorId)
      .andWhere('classrooms.is_active', true)
      .first();

    // Get classroom breakdown
    const classroomBreakdown = await db('classrooms')
      .leftJoin('students', 'classrooms.id', 'students.classroom_id')
      .leftJoin('analytics_events', function() {
        this.on('students.id', '=', 'analytics_events.student_id')
          .andOn('analytics_events.event_timestamp', '>=', db.raw('?', [dateFilter]));
      })
      .select([
        'classrooms.id',
        'classrooms.name',
        'classrooms.grade_level',
        'classrooms.created_at',
        db.raw('COUNT(DISTINCT students.id) as total_students'),
        db.raw('COUNT(DISTINCT analytics_events.student_id) as active_students'),
        db.raw('COUNT(analytics_events.id) as total_events')
      ])
      .where('classrooms.facilitator_id', facilitatorId)
      .andWhere('classrooms.is_active', true)
      .groupBy('classrooms.id', 'classrooms.name', 'classrooms.grade_level', 'classrooms.created_at')
      .orderBy('classrooms.created_at', 'desc');

    // Get lesson completion summary across all classrooms
    const lessonProgress = await db('lessons')
      .leftJoin('student_progress', function() {
        this.on('lessons.id', '=', 'student_progress.lesson_id')
          .andOn('student_progress.student_id', 'in', 
            db('students')
              .select('id')
              .whereIn('classroom_id', 
                db('classrooms').select('id').where('facilitator_id', facilitatorId)
              )
          );
      })
      .select([
        'lessons.id',
        'lessons.lesson_number',
        'lessons.title',
        db.raw('COUNT(DISTINCT student_progress.student_id) as students_started'),
        db.raw('COUNT(DISTINCT CASE WHEN student_progress.completion_status = \'completed\' THEN student_progress.student_id END) as students_completed'),
        db.raw('AVG(student_progress.progress_percentage) as avg_progress')
      ])
      .where('lessons.is_published', true)
      .groupBy('lessons.id', 'lessons.lesson_number', 'lessons.title')
      .orderBy('lessons.lesson_number');

    res.json({
      success: true,
      data: {
        timeframe,
        overview: {
          totalClassrooms: parseInt(overallMetrics.total_classrooms) || 0,
          totalStudents: parseInt(overallMetrics.total_students) || 0,
          activeStudents: parseInt(overallMetrics.active_students) || 0,
          totalEvents: parseInt(overallMetrics.total_events) || 0,
          avgSessionDuration: Math.round(parseFloat(overallMetrics.avg_session_duration) || 0)
        },
        classrooms: classroomBreakdown.map(classroom => ({
          id: classroom.id,
          name: classroom.name,
          gradeLevel: classroom.grade_level,
          createdAt: classroom.created_at,
          totalStudents: parseInt(classroom.total_students),
          activeStudents: parseInt(classroom.active_students),
          totalEvents: parseInt(classroom.total_events),
          activityRate: classroom.total_students > 0 ? 
            Math.round((parseInt(classroom.active_students) / parseInt(classroom.total_students)) * 100) : 0
        })),
        lessonProgress: lessonProgress.map(lesson => ({
          lessonId: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title,
          studentsStarted: parseInt(lesson.students_started),
          studentsCompleted: parseInt(lesson.students_completed),
          avgProgress: Math.round(parseFloat(lesson.avg_progress) || 0),
          completionRate: lesson.students_started > 0 ? 
            Math.round((parseInt(lesson.students_completed) / parseInt(lesson.students_started)) * 100) : 0
        }))
      }
    });
  } catch (error) {
    logger.error('Get facilitator overview failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get facilitator overview'
    });
  }
}

/**
 * @desc    Export classroom data for analysis
 * @route   GET /api/analytics/classroom/:classroomId/export
 * @access  Private (Facilitator)
 */
async function exportClassroomData(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const facilitatorId = req.facilitator.id;
    const { format = 'json', includeEvents = 'false' } = req.query;

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

    // Get anonymous student data
    const students = await db('students')
      .select([
        'anonymous_id',
        'grade_level',
        'gender',
        'ethnicity',
        'has_disabilities',
        'primary_language',
        'total_sessions',
        'first_joined_at',
        'last_active_at'
      ])
      .where('classroom_id', classroomId)
      .andWhere('is_active', true);

    // Get progress data
    const progressData = await db('student_progress')
      .join('students', 'student_progress.student_id', 'students.id')
      .join('lessons', 'student_progress.lesson_id', 'lessons.id')
      .select([
        'students.anonymous_id',
        'lessons.lesson_number',
        'lessons.title',
        'student_progress.completion_status',
        'student_progress.progress_percentage',
        'student_progress.time_spent_minutes',
        'student_progress.video_watch_percentage',
        'student_progress.activities_completed',
        'student_progress.total_activities',
        'student_progress.started_at',
        'student_progress.completed_at'
      ])
      .where('students.classroom_id', classroomId)
      .orderBy('students.anonymous_id')
      .orderBy('lessons.lesson_number');

    // Get feedback data
    const feedbackData = await db('student_feedback')
      .join('students', 'student_feedback.student_id', 'students.id')
      .join('lessons', 'student_feedback.lesson_id', 'lessons.id')
      .select([
        'students.anonymous_id',
        'lessons.lesson_number',
        'student_feedback.feedback_type',
        'student_feedback.feedback_category',
        'student_feedback.rating_value',
        'student_feedback.mood_indicator',
        'student_feedback.feedback_date'
      ])
      .where('students.classroom_id', classroomId)
      .orderBy('student_feedback.feedback_date', 'desc');

    const exportData = {
      classroom: {
        id: classroom.id,
        name: classroom.name,
        gradeLevel: classroom.grade_level,
        exportedAt: new Date().toISOString()
      },
      students,
      progress: progressData,
      feedback: feedbackData
    };

    // Include events if requested
    if (includeEvents === 'true') {
      const eventsData = await db('analytics_events')
        .join('students', 'analytics_events.student_id', 'students.id')
        .select([
          'students.anonymous_id',
          'analytics_events.event_type',
          'analytics_events.event_category',
          'analytics_events.event_action',
          'analytics_events.event_timestamp',
          'analytics_events.device_type',
          'analytics_events.platform'
        ])
        .where('students.classroom_id', classroomId)
        .orderBy('analytics_events.event_timestamp', 'desc')
        .limit(10000); // Limit to prevent huge exports

      exportData.events = eventsData;
    }

    // Set appropriate headers for download
    const filename = `classroom_${classroom.name.replace(/[^a-zA-Z0-9]/g, '_')}_${new Date().toISOString().split('T')[0]}`;
    
    if (format === 'csv') {
      // For CSV, we'd need to implement CSV conversion
      // For now, return JSON with CSV headers
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', `attachment; filename="${filename}.json"`);
    } else {
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', `attachment; filename="${filename}.json"`);
    }

    res.json({
      success: true,
      data: exportData
    });
  } catch (error) {
    logger.error('Export classroom data failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to export classroom data'
    });
  }
}

module.exports = {
  trackEvent,
  getClassroomDashboard,
  getFacilitatorOverview,
  exportClassroomData
};
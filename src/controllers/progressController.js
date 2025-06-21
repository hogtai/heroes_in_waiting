const db = require('../config/database');
const logger = require('../utils/logger');

/**
 * @desc    Update student progress for a lesson
 * @route   PUT /api/progress/lesson
 * @access  Private (Student)
 */
async function updateLessonProgress(req, res) {
  try {
    const {
      lessonId,
      completionStatus,
      progressPercentage,
      timeSpentMinutes,
      videoWatchPercentage,
      activitiesCompleted,
      totalActivities,
      activityResponses
    } = req.body;

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

    // Check if progress record exists
    const existingProgress = await db('student_progress')
      .where({ student_id: studentId, lesson_id: lessonId })
      .first();

    const updateData = {
      completion_status: completionStatus,
      progress_percentage: progressPercentage,
      time_spent_minutes: timeSpentMinutes,
      video_watch_percentage: videoWatchPercentage,
      activities_completed: activitiesCompleted,
      total_activities: totalActivities,
      activity_responses: activityResponses,
      updated_at: new Date()
    };

    // Set started_at if transitioning from not_started
    if (!existingProgress || (existingProgress.completion_status === 'not_started' && completionStatus !== 'not_started')) {
      updateData.started_at = new Date();
    }

    // Set completed_at if transitioning to completed
    if (completionStatus === 'completed' && (!existingProgress || existingProgress.completion_status !== 'completed')) {
      updateData.completed_at = new Date();
    }

    let progress;
    if (existingProgress) {
      // Update existing progress
      [progress] = await db('student_progress')
        .where({ student_id: studentId, lesson_id: lessonId })
        .update(updateData)
        .returning('*');
    } else {
      // Create new progress record
      [progress] = await db('student_progress')
        .insert({
          student_id: studentId,
          lesson_id: lessonId,
          ...updateData
        })
        .returning('*');
    }

    // Update student's total sessions if lesson is completed for the first time
    if (completionStatus === 'completed' && (!existingProgress || existingProgress.completion_status !== 'completed')) {
      await db('students')
        .where('id', studentId)
        .increment('total_sessions', 1);
    }

    // Log progress event for analytics
    await db('analytics_events').insert({
      student_id: studentId,
      classroom_id: req.student.classroomId,
      lesson_id: lessonId,
      event_type: 'lesson_progress_updated',
      event_category: 'progress',
      event_action: 'update',
      event_label: `Progress: ${progressPercentage}%`,
      event_value: progressPercentage,
      event_properties: {
        completion_status: completionStatus,
        time_spent: timeSpentMinutes,
        video_progress: videoWatchPercentage,
        activities_completed: activitiesCompleted
      },
      device_type: req.get('X-Device-Type') || 'unknown',
      platform: req.get('X-Platform') || 'unknown',
      app_version: req.get('X-App-Version') || null
    });

    logger.info('Student progress updated:', {
      studentId: studentId,
      lessonId: lessonId,
      completionStatus: completionStatus,
      progressPercentage: progressPercentage
    });

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
          startedAt: progress.started_at,
          completedAt: progress.completed_at,
          updatedAt: progress.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Update lesson progress failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update lesson progress'
    });
  }
}

/**
 * @desc    Get student's overall progress summary
 * @route   GET /api/progress/summary
 * @access  Private (Student)
 */
async function getProgressSummary(req, res) {
  try {
    const studentId = req.student.id;

    // Get overall progress statistics
    const overallStats = await db('student_progress')
      .select([
        db.raw('COUNT(*) as lessons_started'),
        db.raw('COUNT(CASE WHEN completion_status = \'completed\' THEN 1 END) as lessons_completed'),
        db.raw('COUNT(CASE WHEN completion_status = \'in_progress\' THEN 1 END) as lessons_in_progress'),
        db.raw('AVG(progress_percentage) as average_progress'),
        db.raw('SUM(time_spent_minutes) as total_time_spent')
      ])
      .where('student_id', studentId)
      .first();

    // Get total number of published lessons
    const [{ count: totalLessons }] = await db('lessons')
      .count('id as count')
      .where('is_published', true);

    // Get recent progress (last 5 lessons)
    const recentProgress = await db('student_progress')
      .join('lessons', 'student_progress.lesson_id', 'lessons.id')
      .select([
        'lessons.id',
        'lessons.lesson_number',
        'lessons.title',
        'student_progress.completion_status',
        'student_progress.progress_percentage',
        'student_progress.updated_at'
      ])
      .where('student_progress.student_id', studentId)
      .orderBy('student_progress.updated_at', 'desc')
      .limit(5);

    // Calculate completion rate
    const completionRate = totalLessons > 0 ? 
      (parseInt(overallStats.lessons_completed) / parseInt(totalLessons) * 100) : 0;

    res.json({
      success: true,
      data: {
        summary: {
          totalLessons: parseInt(totalLessons),
          lessonsStarted: parseInt(overallStats.lessons_started),
          lessonsCompleted: parseInt(overallStats.lessons_completed),
          lessonsInProgress: parseInt(overallStats.lessons_in_progress),
          averageProgress: parseFloat(overallStats.average_progress) || 0,
          totalTimeSpent: parseInt(overallStats.total_time_spent) || 0,
          completionRate: Math.round(completionRate * 100) / 100
        },
        recentProgress: recentProgress.map(progress => ({
          lessonId: progress.id,
          lessonNumber: progress.lesson_number,
          title: progress.title,
          completionStatus: progress.completion_status,
          progressPercentage: progress.progress_percentage,
          lastUpdated: progress.updated_at
        }))
      }
    });
  } catch (error) {
    logger.error('Get progress summary failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get progress summary'
    });
  }
}

/**
 * @desc    Submit student feedback
 * @route   POST /api/progress/feedback
 * @access  Private (Student)
 */
async function submitFeedback(req, res) {
  try {
    const {
      lessonId,
      feedbackType,
      feedbackCategory,
      ratingValue,
      textResponse,
      structuredResponse,
      moodIndicator,
      activityContext
    } = req.body;

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

    // Insert feedback
    const [feedback] = await db('student_feedback')
      .insert({
        student_id: studentId,
        lesson_id: lessonId,
        classroom_session_id: null, // Could be set if feedback is during a live session
        feedback_type: feedbackType,
        feedback_category: feedbackCategory,
        rating_value: ratingValue,
        text_response: textResponse,
        structured_response: structuredResponse,
        mood_indicator: moodIndicator,
        activity_context: activityContext
      })
      .returning('*');

    // Log feedback event for analytics
    await db('analytics_events').insert({
      student_id: studentId,
      classroom_id: req.student.classroomId,
      lesson_id: lessonId,
      event_type: 'feedback_submitted',
      event_category: 'engagement',
      event_action: 'submit',
      event_label: feedbackType,
      event_value: ratingValue,
      event_properties: {
        feedback_type: feedbackType,
        feedback_category: feedbackCategory,
        mood: moodIndicator,
        has_text_response: !!textResponse
      },
      device_type: req.get('X-Device-Type') || 'unknown',
      platform: req.get('X-Platform') || 'unknown',
      app_version: req.get('X-App-Version') || null
    });

    logger.info('Student feedback submitted:', {
      studentId: studentId,
      lessonId: lessonId,
      feedbackType: feedbackType,
      ratingValue: ratingValue
    });

    res.status(201).json({
      success: true,
      data: {
        feedback: {
          id: feedback.id,
          lessonId: feedback.lesson_id,
          feedbackType: feedback.feedback_type,
          feedbackCategory: feedback.feedback_category,
          ratingValue: feedback.rating_value,
          moodIndicator: feedback.mood_indicator,
          submittedAt: feedback.created_at
        }
      }
    });
  } catch (error) {
    logger.error('Submit feedback failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to submit feedback'
    });
  }
}

/**
 * @desc    Get student progress for a specific lesson (for facilitators)
 * @route   GET /api/progress/classroom/:classroomId/lesson/:lessonId
 * @access  Private (Facilitator)
 */
async function getClassroomLessonProgress(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const lessonId = req.params.lessonId;
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

    // Get progress for all students in the classroom for this lesson
    const progressData = await db('students')
      .leftJoin('student_progress', function() {
        this.on('students.id', '=', 'student_progress.student_id')
          .andOn('student_progress.lesson_id', '=', db.raw('?', [lessonId]));
      })
      .select([
        'students.id',
        'students.anonymous_id',
        'students.grade_level',
        'students.created_at as joined_at',
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
      .andWhere('students.is_active', true)
      .orderBy('students.created_at', 'desc');

    // Calculate summary statistics
    const totalStudents = progressData.length;
    const studentsStarted = progressData.filter(p => p.completion_status).length;
    const studentsCompleted = progressData.filter(p => p.completion_status === 'completed').length;
    const averageProgress = progressData.reduce((sum, p) => sum + (p.progress_percentage || 0), 0) / totalStudents;
    const totalTimeSpent = progressData.reduce((sum, p) => sum + (p.time_spent_minutes || 0), 0);

    res.json({
      success: true,
      data: {
        lesson: {
          id: lesson.id,
          lessonNumber: lesson.lesson_number,
          title: lesson.title
        },
        classroom: {
          id: classroom.id,
          name: classroom.name
        },
        summary: {
          totalStudents,
          studentsStarted,
          studentsCompleted,
          studentsNotStarted: totalStudents - studentsStarted,
          averageProgress: Math.round(averageProgress * 100) / 100,
          totalTimeSpent,
          completionRate: totalStudents > 0 ? Math.round((studentsCompleted / totalStudents) * 100) : 0
        },
        studentProgress: progressData.map(student => ({
          studentId: student.id,
          anonymousId: student.anonymous_id,
          gradeLevel: student.grade_level,
          joinedAt: student.joined_at,
          completionStatus: student.completion_status || 'not_started',
          progressPercentage: student.progress_percentage || 0,
          timeSpentMinutes: student.time_spent_minutes || 0,
          videoWatchPercentage: student.video_watch_percentage || 0,
          activitiesCompleted: student.activities_completed || 0,
          totalActivities: student.total_activities || 0,
          startedAt: student.started_at,
          completedAt: student.completed_at
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

/**
 * @desc    Get classroom feedback summary for a lesson
 * @route   GET /api/progress/classroom/:classroomId/lesson/:lessonId/feedback
 * @access  Private (Facilitator)
 */
async function getClassroomLessonFeedback(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const lessonId = req.params.lessonId;
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

    // Get feedback summary
    const feedbackSummary = await db('student_feedback')
      .join('students', 'student_feedback.student_id', 'students.id')
      .select([
        'student_feedback.feedback_type',
        'student_feedback.feedback_category',
        'student_feedback.rating_value',
        'student_feedback.mood_indicator',
        'student_feedback.created_at'
      ])
      .where('students.classroom_id', classroomId)
      .andWhere('student_feedback.lesson_id', lessonId)
      .orderBy('student_feedback.created_at', 'desc');

    // Aggregate feedback data
    const ratingDistribution = feedbackSummary
      .filter(f => f.rating_value)
      .reduce((acc, f) => {
        acc[f.rating_value] = (acc[f.rating_value] || 0) + 1;
        return acc;
      }, {});

    const moodDistribution = feedbackSummary
      .filter(f => f.mood_indicator)
      .reduce((acc, f) => {
        acc[f.mood_indicator] = (acc[f.mood_indicator] || 0) + 1;
        return acc;
      }, {});

    const averageRating = feedbackSummary
      .filter(f => f.rating_value)
      .reduce((sum, f, _, arr) => sum + f.rating_value / arr.length, 0);

    res.json({
      success: true,
      data: {
        lesson: {
          id: lessonId,
        },
        classroom: {
          id: classroom.id,
          name: classroom.name
        },
        summary: {
          totalFeedbackItems: feedbackSummary.length,
          averageRating: Math.round(averageRating * 100) / 100,
          ratingDistribution,
          moodDistribution
        },
        recentFeedback: feedbackSummary.slice(0, 10).map(feedback => ({
          feedbackType: feedback.feedback_type,
          feedbackCategory: feedback.feedback_category,
          ratingValue: feedback.rating_value,
          moodIndicator: feedback.mood_indicator,
          submittedAt: feedback.created_at
        }))
      }
    });
  } catch (error) {
    logger.error('Get classroom lesson feedback failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom lesson feedback'
    });
  }
}

module.exports = {
  updateLessonProgress,
  getProgressSummary,
  submitFeedback,
  getClassroomLessonProgress,
  getClassroomLessonFeedback
};
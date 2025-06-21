const db = require('../config/database');
const { generateAnonymousStudentId } = require('../utils/auth');
const logger = require('../utils/logger');

/**
 * @desc    Enroll a student in a classroom using classroom code
 * @route   POST /api/students/enroll
 * @access  Public
 */
async function enrollStudent(req, res) {
  try {
    const { 
      classroomCode, 
      gradeLevel, 
      gender, 
      ethnicity, 
      hasDisabilities, 
      primaryLanguage 
    } = req.body;

    // Find classroom by code
    const classroom = await db('classrooms')
      .where({ 
        classroom_code: classroomCode, 
        is_active: true 
      })
      .first();

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Invalid classroom code or classroom is not active'
      });
    }

    // Check if classroom code is expired
    if (classroom.code_expires_at && new Date() > new Date(classroom.code_expires_at)) {
      return res.status(400).json({
        success: false,
        error: 'Classroom code has expired'
      });
    }

    // Check classroom capacity
    const currentStudentCount = await db('students')
      .count('id as count')
      .where({ classroom_id: classroom.id, is_active: true })
      .first();

    if (parseInt(currentStudentCount.count) >= classroom.student_capacity) {
      return res.status(400).json({
        success: false,
        error: 'Classroom is at full capacity'
      });
    }

    // Generate unique anonymous student ID
    let anonymousId;
    let isIdUnique = false;
    let attempts = 0;
    const maxAttempts = 10;

    while (!isIdUnique && attempts < maxAttempts) {
      anonymousId = generateAnonymousStudentId();
      const existingStudent = await db('students')
        .where({ anonymous_id: anonymousId })
        .first();
      
      if (!existingStudent) {
        isIdUnique = true;
      }
      attempts++;
    }

    if (!isIdUnique) {
      return res.status(500).json({
        success: false,
        error: 'Failed to generate unique student ID'
      });
    }

    // Create student record
    const [student] = await db('students')
      .insert({
        classroom_id: classroom.id,
        anonymous_id: anonymousId,
        grade_level: gradeLevel,
        gender: gender,
        ethnicity: ethnicity,
        has_disabilities: hasDisabilities,
        primary_language: primaryLanguage,
        total_sessions: 0
      })
      .returning('*');

    logger.info('Student enrolled:', {
      studentId: student.id,
      anonymousId: anonymousId,
      classroomId: classroom.id,
      classroomCode: classroomCode
    });

    res.status(201).json({
      success: true,
      data: {
        student: {
          id: student.id,
          anonymousId: student.anonymous_id,
          classroomId: student.classroom_id,
          gradeLevel: student.grade_level,
          joinedAt: student.created_at
        },
        classroom: {
          id: classroom.id,
          name: classroom.name,
          gradeLevel: classroom.grade_level
        }
      }
    });
  } catch (error) {
    logger.error('Student enrollment failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to enroll student'
    });
  }
}

/**
 * @desc    Get student profile
 * @route   GET /api/students/profile
 * @access  Private (Student)
 */
async function getStudentProfile(req, res) {
  try {
    const studentId = req.student.id;

    const student = await db('students')
      .leftJoin('classrooms', 'students.classroom_id', 'classrooms.id')
      .select([
        'students.id',
        'students.anonymous_id',
        'students.grade_level',
        'students.total_sessions',
        'students.first_joined_at',
        'students.last_active_at',
        'classrooms.name as classroom_name',
        'classrooms.grade_level as classroom_grade_level'
      ])
      .where('students.id', studentId)
      .first();

    if (!student) {
      return res.status(404).json({
        success: false,
        error: 'Student not found'
      });
    }

    // Get progress summary
    const progressSummary = await db('student_progress')
      .select([
        db.raw('COUNT(*) as total_lessons_started'),
        db.raw('COUNT(CASE WHEN completion_status = \'completed\' THEN 1 END) as lessons_completed'),
        db.raw('AVG(progress_percentage) as average_progress'),
        db.raw('SUM(time_spent_minutes) as total_time_spent')
      ])
      .where('student_id', studentId)
      .first();

    res.json({
      success: true,
      data: {
        student: {
          id: student.id,
          anonymousId: student.anonymous_id,
          gradeLevel: student.grade_level,
          totalSessions: student.total_sessions,
          firstJoinedAt: student.first_joined_at,
          lastActiveAt: student.last_active_at,
          classroom: {
            name: student.classroom_name,
            gradeLevel: student.classroom_grade_level
          },
          progress: {
            totalLessonsStarted: parseInt(progressSummary.total_lessons_started),
            lessonsCompleted: parseInt(progressSummary.lessons_completed),
            averageProgress: parseFloat(progressSummary.average_progress) || 0,
            totalTimeSpent: parseInt(progressSummary.total_time_spent) || 0
          }
        }
      }
    });
  } catch (error) {
    logger.error('Get student profile failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get student profile'
    });
  }
}

/**
 * @desc    Get students in a classroom (for facilitators)
 * @route   GET /api/students/classroom/:classroomId
 * @access  Private (Facilitator)
 */
async function getClassroomStudents(req, res) {
  try {
    const classroomId = req.params.classroomId;
    const facilitatorId = req.facilitator.id;
    const { page = 1, limit = 20, sortBy = 'last_active_at', sortOrder = 'desc' } = req.query;

    const offset = (page - 1) * limit;

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

    // Get students with progress summary
    const students = await db('students')
      .leftJoin('student_progress', 'students.id', 'student_progress.student_id')
      .select([
        'students.id',
        'students.anonymous_id',
        'students.grade_level',
        'students.gender',
        'students.ethnicity',
        'students.has_disabilities',
        'students.primary_language',
        'students.total_sessions',
        'students.first_joined_at',
        'students.last_active_at',
        'students.is_active',
        db.raw('COUNT(student_progress.id) as lessons_started'),
        db.raw('COUNT(CASE WHEN student_progress.completion_status = \'completed\' THEN 1 END) as lessons_completed'),
        db.raw('AVG(student_progress.progress_percentage) as average_progress'),
        db.raw('SUM(student_progress.time_spent_minutes) as total_time_spent')
      ])
      .where('students.classroom_id', classroomId)
      .groupBy('students.id')
      .orderBy(`students.${sortBy}`, sortOrder)
      .limit(limit)
      .offset(offset);

    // Get total count for pagination
    const [{ count: totalCount }] = await db('students')
      .count('id as count')
      .where('classroom_id', classroomId);

    const totalPages = Math.ceil(totalCount / limit);

    res.json({
      success: true,
      data: {
        students: students.map(student => ({
          id: student.id,
          anonymousId: student.anonymous_id,
          gradeLevel: student.grade_level,
          gender: student.gender,
          ethnicity: student.ethnicity,
          hasDisabilities: student.has_disabilities,
          primaryLanguage: student.primary_language,
          totalSessions: student.total_sessions,
          firstJoinedAt: student.first_joined_at,
          lastActiveAt: student.last_active_at,
          isActive: student.is_active,
          progress: {
            lessonsStarted: parseInt(student.lessons_started),
            lessonsCompleted: parseInt(student.lessons_completed),
            averageProgress: parseFloat(student.average_progress) || 0,
            totalTimeSpent: parseInt(student.total_time_spent) || 0
          }
        })),
        pagination: {
          currentPage: parseInt(page),
          totalPages,
          totalCount: parseInt(totalCount),
          limit: parseInt(limit),
          hasNext: page < totalPages,
          hasPrev: page > 1
        }
      }
    });
  } catch (error) {
    logger.error('Get classroom students failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom students'
    });
  }
}

/**
 * @desc    Update student active status
 * @route   PUT /api/students/:studentId/status
 * @access  Private (Facilitator)
 */
async function updateStudentStatus(req, res) {
  try {
    const studentId = req.params.studentId;
    const facilitatorId = req.facilitator.id;
    const { isActive } = req.body;

    // Verify facilitator owns the classroom that the student belongs to
    const student = await db('students')
      .join('classrooms', 'students.classroom_id', 'classrooms.id')
      .select('students.*', 'classrooms.facilitator_id')
      .where('students.id', studentId)
      .first();

    if (!student) {
      return res.status(404).json({
        success: false,
        error: 'Student not found'
      });
    }

    if (student.facilitator_id !== facilitatorId) {
      return res.status(403).json({
        success: false,
        error: 'Access denied'
      });
    }

    // Update student status
    const [updatedStudent] = await db('students')
      .where('id', studentId)
      .update({
        is_active: isActive,
        updated_at: new Date()
      })
      .returning(['id', 'anonymous_id', 'is_active', 'updated_at']);

    logger.info('Student status updated:', {
      studentId: studentId,
      facilitatorId: facilitatorId,
      isActive: isActive
    });

    res.json({
      success: true,
      data: {
        student: {
          id: updatedStudent.id,
          anonymousId: updatedStudent.anonymous_id,
          isActive: updatedStudent.is_active,
          updatedAt: updatedStudent.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Update student status failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update student status'
    });
  }
}

/**
 * @desc    Get classroom analytics/demographics
 * @route   GET /api/students/classroom/:classroomId/analytics
 * @access  Private (Facilitator)
 */
async function getClassroomAnalytics(req, res) {
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

    // Get demographic analytics
    const demographics = await db('students')
      .select([
        'grade_level',
        'gender',
        'ethnicity',
        'has_disabilities',
        'primary_language'
      ])
      .where({ classroom_id: classroomId, is_active: true });

    // Calculate demographic breakdowns
    const gradeBreakdown = demographics.reduce((acc, student) => {
      const grade = student.grade_level || 'Unknown';
      acc[grade] = (acc[grade] || 0) + 1;
      return acc;
    }, {});

    const genderBreakdown = demographics.reduce((acc, student) => {
      const gender = student.gender || 'Not specified';
      acc[gender] = (acc[gender] || 0) + 1;
      return acc;
    }, {});

    const ethnicityBreakdown = demographics.reduce((acc, student) => {
      const ethnicity = student.ethnicity || 'Not specified';
      acc[ethnicity] = (acc[ethnicity] || 0) + 1;
      return acc;
    }, {});

    const languageBreakdown = demographics.reduce((acc, student) => {
      const language = student.primary_language || 'Not specified';
      acc[language] = (acc[language] || 0) + 1;
      return acc;
    }, {});

    const disabilityCount = demographics.filter(student => student.has_disabilities === true).length;

    // Get engagement metrics
    const engagementMetrics = await db('students')
      .select([
        db.raw('COUNT(*) as total_students'),
        db.raw('COUNT(CASE WHEN last_active_at > NOW() - INTERVAL \'7 days\' THEN 1 END) as active_last_week'),
        db.raw('AVG(total_sessions) as average_sessions'),
        db.raw('MAX(total_sessions) as max_sessions')
      ])
      .where({ classroom_id: classroomId, is_active: true })
      .first();

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          totalStudents: parseInt(engagementMetrics.total_students),
          activeLastWeek: parseInt(engagementMetrics.active_last_week)
        },
        demographics: {
          grade: gradeBreakdown,
          gender: genderBreakdown,
          ethnicity: ethnicityBreakdown,
          primaryLanguage: languageBreakdown,
          studentsWithDisabilities: disabilityCount
        },
        engagement: {
          averageSessions: parseFloat(engagementMetrics.average_sessions) || 0,
          maxSessions: parseInt(engagementMetrics.max_sessions) || 0,
          activeLastWeek: parseInt(engagementMetrics.active_last_week)
        }
      }
    });
  } catch (error) {
    logger.error('Get classroom analytics failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom analytics'
    });
  }
}

module.exports = {
  enrollStudent,
  getStudentProfile,
  getClassroomStudents,
  updateStudentStatus,
  getClassroomAnalytics
};
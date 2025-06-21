const db = require('../config/database');
const { generateClassroomCode, generateAnonymousStudentId } = require('../utils/auth');
const logger = require('../utils/logger');

/**
 * @desc    Create a new classroom
 * @route   POST /api/classrooms
 * @access  Private (Facilitator)
 */
async function createClassroom(req, res) {
  try {
    const { name, description, gradeLevel, studentCapacity } = req.body;
    const facilitatorId = req.facilitator.id;

    // Generate unique classroom code
    let classroomCode;
    let isCodeUnique = false;
    let attempts = 0;
    const maxAttempts = 10;

    while (!isCodeUnique && attempts < maxAttempts) {
      classroomCode = generateClassroomCode();
      const existingClassroom = await db('classrooms')
        .where({ classroom_code: classroomCode })
        .first();
      
      if (!existingClassroom) {
        isCodeUnique = true;
      }
      attempts++;
    }

    if (!isCodeUnique) {
      return res.status(500).json({
        success: false,
        error: 'Failed to generate unique classroom code'
      });
    }

    // Create classroom
    const [classroom] = await db('classrooms')
      .insert({
        facilitator_id: facilitatorId,
        name,
        description,
        classroom_code: classroomCode,
        grade_level: gradeLevel,
        student_capacity: studentCapacity || 30
      })
      .returning('*');

    logger.info('Classroom created:', {
      classroomId: classroom.id,
      facilitatorId: facilitatorId,
      classroomCode: classroomCode
    });

    res.status(201).json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          description: classroom.description,
          classroomCode: classroom.classroom_code,
          gradeLevel: classroom.grade_level,
          studentCapacity: classroom.student_capacity,
          isActive: classroom.is_active,
          createdAt: classroom.created_at
        }
      }
    });
  } catch (error) {
    logger.error('Create classroom failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create classroom'
    });
  }
}

/**
 * @desc    Get all classrooms for a facilitator
 * @route   GET /api/classrooms
 * @access  Private (Facilitator)
 */
async function getFacilitatorClassrooms(req, res) {
  try {
    const facilitatorId = req.facilitator.id;
    const { page = 1, limit = 10, sortBy = 'created_at', sortOrder = 'desc' } = req.query;

    const offset = (page - 1) * limit;

    // Get classrooms with student count
    const classrooms = await db('classrooms')
      .leftJoin('students', 'classrooms.id', 'students.classroom_id')
      .select([
        'classrooms.id',
        'classrooms.name',
        'classrooms.description',
        'classrooms.classroom_code',
        'classrooms.grade_level',
        'classrooms.student_capacity',
        'classrooms.is_active',
        'classrooms.created_at',
        'classrooms.updated_at'
      ])
      .count('students.id as student_count')
      .where('classrooms.facilitator_id', facilitatorId)
      .groupBy('classrooms.id')
      .orderBy(`classrooms.${sortBy}`, sortOrder)
      .limit(limit)
      .offset(offset);

    // Get total count for pagination
    const [{ count: totalCount }] = await db('classrooms')
      .count('id as count')
      .where('facilitator_id', facilitatorId);

    const totalPages = Math.ceil(totalCount / limit);

    res.json({
      success: true,
      data: {
        classrooms: classrooms.map(classroom => ({
          id: classroom.id,
          name: classroom.name,
          description: classroom.description,
          classroomCode: classroom.classroom_code,
          gradeLevel: classroom.grade_level,
          studentCapacity: classroom.student_capacity,
          studentCount: parseInt(classroom.student_count),
          isActive: classroom.is_active,
          createdAt: classroom.created_at,
          updatedAt: classroom.updated_at
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
    logger.error('Get facilitator classrooms failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classrooms'
    });
  }
}

/**
 * @desc    Get a specific classroom
 * @route   GET /api/classrooms/:id
 * @access  Private (Facilitator)
 */
async function getClassroom(req, res) {
  try {
    const classroomId = req.params.id;
    const facilitatorId = req.facilitator.id;

    // Get classroom with student count and recent activity
    const classroom = await db('classrooms')
      .leftJoin('students', 'classrooms.id', 'students.classroom_id')
      .select([
        'classrooms.id',
        'classrooms.name',
        'classrooms.description',
        'classrooms.classroom_code',
        'classrooms.grade_level',
        'classrooms.student_capacity',
        'classrooms.is_active',
        'classrooms.code_expires_at',
        'classrooms.created_at',
        'classrooms.updated_at'
      ])
      .count('students.id as student_count')
      .where('classrooms.id', classroomId)
      .andWhere('classrooms.facilitator_id', facilitatorId)
      .groupBy('classrooms.id')
      .first();

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found'
      });
    }

    // Get recent students
    const recentStudents = await db('students')
      .select(['anonymous_id', 'grade_level', 'last_active_at', 'created_at'])
      .where('classroom_id', classroomId)
      .andWhere('is_active', true)
      .orderBy('last_active_at', 'desc')
      .limit(10);

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          description: classroom.description,
          classroomCode: classroom.classroom_code,
          gradeLevel: classroom.grade_level,
          studentCapacity: classroom.student_capacity,
          studentCount: parseInt(classroom.student_count),
          isActive: classroom.is_active,
          codeExpiresAt: classroom.code_expires_at,
          createdAt: classroom.created_at,
          updatedAt: classroom.updated_at,
          recentStudents: recentStudents.map(student => ({
            anonymousId: student.anonymous_id,
            gradeLevel: student.grade_level,
            lastActiveAt: student.last_active_at,
            joinedAt: student.created_at
          }))
        }
      }
    });
  } catch (error) {
    logger.error('Get classroom failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get classroom'
    });
  }
}

/**
 * @desc    Update a classroom
 * @route   PUT /api/classrooms/:id
 * @access  Private (Facilitator)
 */
async function updateClassroom(req, res) {
  try {
    const classroomId = req.params.id;
    const facilitatorId = req.facilitator.id;
    const { name, description, gradeLevel, studentCapacity, isActive } = req.body;

    const updateData = {};
    if (name !== undefined) updateData.name = name;
    if (description !== undefined) updateData.description = description;
    if (gradeLevel !== undefined) updateData.grade_level = gradeLevel;
    if (studentCapacity !== undefined) updateData.student_capacity = studentCapacity;
    if (isActive !== undefined) updateData.is_active = isActive;
    updateData.updated_at = new Date();

    const [classroom] = await db('classrooms')
      .where({ id: classroomId, facilitator_id: facilitatorId })
      .update(updateData)
      .returning('*');

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found'
      });
    }

    logger.info('Classroom updated:', {
      classroomId: classroom.id,
      facilitatorId: facilitatorId
    });

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          description: classroom.description,
          classroomCode: classroom.classroom_code,
          gradeLevel: classroom.grade_level,
          studentCapacity: classroom.student_capacity,
          isActive: classroom.is_active,
          updatedAt: classroom.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Update classroom failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update classroom'
    });
  }
}

/**
 * @desc    Regenerate classroom code
 * @route   POST /api/classrooms/:id/regenerate-code
 * @access  Private (Facilitator)
 */
async function regenerateClassroomCode(req, res) {
  try {
    const classroomId = req.params.id;
    const facilitatorId = req.facilitator.id;

    // Generate new unique classroom code
    let newClassroomCode;
    let isCodeUnique = false;
    let attempts = 0;
    const maxAttempts = 10;

    while (!isCodeUnique && attempts < maxAttempts) {
      newClassroomCode = generateClassroomCode();
      const existingClassroom = await db('classrooms')
        .where({ classroom_code: newClassroomCode })
        .first();
      
      if (!existingClassroom) {
        isCodeUnique = true;
      }
      attempts++;
    }

    if (!isCodeUnique) {
      return res.status(500).json({
        success: false,
        error: 'Failed to generate unique classroom code'
      });
    }

    // Update classroom code
    const [classroom] = await db('classrooms')
      .where({ id: classroomId, facilitator_id: facilitatorId })
      .update({
        classroom_code: newClassroomCode,
        code_expires_at: null, // Reset expiration
        updated_at: new Date()
      })
      .returning(['id', 'name', 'classroom_code', 'updated_at']);

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found'
      });
    }

    logger.info('Classroom code regenerated:', {
      classroomId: classroom.id,
      facilitatorId: facilitatorId,
      newCode: newClassroomCode
    });

    res.json({
      success: true,
      data: {
        classroom: {
          id: classroom.id,
          name: classroom.name,
          classroomCode: classroom.classroom_code,
          updatedAt: classroom.updated_at
        }
      }
    });
  } catch (error) {
    logger.error('Regenerate classroom code failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to regenerate classroom code'
    });
  }
}

/**
 * @desc    Delete a classroom
 * @route   DELETE /api/classrooms/:id
 * @access  Private (Facilitator)
 */
async function deleteClassroom(req, res) {
  try {
    const classroomId = req.params.id;
    const facilitatorId = req.facilitator.id;

    // Check if classroom has students
    const studentCount = await db('students')
      .count('id as count')
      .where('classroom_id', classroomId)
      .first();

    if (parseInt(studentCount.count) > 0) {
      return res.status(400).json({
        success: false,
        error: 'Cannot delete classroom with active students. Please deactivate the classroom instead.'
      });
    }

    // Delete classroom
    const deletedCount = await db('classrooms')
      .where({ id: classroomId, facilitator_id: facilitatorId })
      .del();

    if (deletedCount === 0) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found'
      });
    }

    logger.info('Classroom deleted:', {
      classroomId: classroomId,
      facilitatorId: facilitatorId
    });

    res.json({
      success: true,
      message: 'Classroom deleted successfully'
    });
  } catch (error) {
    logger.error('Delete classroom failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to delete classroom'
    });
  }
}

module.exports = {
  createClassroom,
  getFacilitatorClassrooms,
  getClassroom,
  updateClassroom,
  regenerateClassroomCode,
  deleteClassroom
};
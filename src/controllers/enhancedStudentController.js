const db = require('../config/database');
const { validationResult } = require('express-validator');
const logger = require('../utils/logger');

// Enhanced classroom preview for age-appropriate mobile onboarding
const getClassroomPreview = async (req, res) => {
  try {
    const { code } = req.params;
    
    // Get classroom info with facilitator details
    const classroom = await db('classrooms')
      .join('facilitators', 'classrooms.facilitator_id', 'facilitators.id')
      .select(
        'classrooms.id',
        'classrooms.name',
        'classrooms.description',
        'classrooms.grade_level',
        'classrooms.is_active',
        'facilitators.first_name',
        'facilitators.last_name',
        'facilitators.organization'
      )
      .where({
        'classrooms.classroom_code': code,
        'classrooms.is_active': true,
        'facilitators.is_active': true
      })
      .first();

    if (!classroom) {
      return res.status(404).json({
        success: false,
        error: 'Classroom not found or inactive'
      });
    }

    // Get active student count (for "23 students are learning together!" message)
    const activeStudentCount = await db('students')
      .where({
        classroom_id: classroom.id,
        is_active: true
      })
      .count('* as count')
      .first();

    // Get current lesson info
    const currentLesson = await db('lessons')
      .where({ is_published: true })
      .orderBy('sort_order', 'asc')
      .first();

    const response = {
      success: true,
      classroom: {
        name: classroom.name,
        description: classroom.description,
        gradeLevel: classroom.grade_level,
        facilitatorName: `${classroom.first_name} ${classroom.last_name}`,
        school: classroom.organization,
        activeStudentCount: parseInt(activeStudentCount.count),
        isJoinable: true,
        welcomeMessage: `Welcome to ${classroom.name}! You're about to join ${activeStudentCount.count} other students learning to be heroes.`,
        currentLesson: currentLesson ? {
          title: currentLesson.title,
          description: currentLesson.description,
          duration: currentLesson.duration_minutes
        } : null
      }
    };

    logger.info('Classroom preview requested', {
      classroomCode: code,
      facilitatorId: classroom.facilitator_id
    });

    res.json(response);

  } catch (error) {
    logger.error('Error getting classroom preview:', error);
    res.status(500).json({
      success: false,
      error: 'Internal server error'
    });
  }
};

// Enhanced emotional check-in for age-appropriate feedback
const submitEmotionalCheckin = async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        errors: errors.array()
      });
    }

    const { studentId } = req.headers;
    const {
      checkinType,
      emotionalState,
      visualResponse,
      lessonId
    } = req.body;

    // Verify student session
    const student = await db('students')
      .where({ anonymous_id: studentId, is_active: true })
      .first();

    if (!student) {
      return res.status(404).json({
        success: false,
        error: 'Student session not found'
      });
    }

    // Store emotional check-in
    const checkinData = {
      student_session_id: student.id,
      lesson_id: lessonId || null,
      feedback_type: 'emotional_checkin',
      content: JSON.stringify({
        checkinType,
        emotionalState,
        visualResponse,
        timestamp: new Date().toISOString()
      }),
      submitted_at: new Date()
    };

    await db('student_feedback').insert(checkinData);

    // Generate age-appropriate response
    const responses = {
      excited: "🌟 Your excitement is contagious! Ready to be a hero today?",
      happy: "😊 Your smile makes our classroom brighter!",
      calm: "🌸 Feeling peaceful is perfect for learning new things.",
      curious: "🔍 Great! Curious minds discover amazing things!",
      nervous: "🤗 It's okay to feel nervous. You're brave for being here!",
      tired: "💚 Thank you for being here even when you're tired. You're strong!"
    };

    const encouragementMessage = responses[emotionalState.primary_emotion] || 
      "Thank you for sharing how you're feeling. You're doing great!";

    logger.info('Emotional check-in submitted', {
      studentSessionId: student.id,
      checkinType,
      emotion: emotionalState.primary_emotion
    });

    res.json({
      success: true,
      message: encouragementMessage,
      visualFeedback: {
        animation: "gentle_pulse",
        color: visualResponse.mood_color || "#FFD700",
        icon: emotionalState.emoji_selection || "⭐"
      }
    });

  } catch (error) {
    logger.error('Error submitting emotional check-in:', error);
    res.status(500).json({
      success: false,
      error: 'Internal server error'
    });
  }
};

// Enhanced lesson content for mobile optimization
const getMobileOptimizedLesson = async (req, res) => {
  try {
    const { id } = req.params;
    const { deviceType = 'phone' } = req.query;

    const lesson = await db('lessons')
      .where({ id, is_published: true })
      .first();

    if (!lesson) {
      return res.status(404).json({
        success: false,
        error: 'Lesson not found'
      });
    }

    // Parse content structure and optimize for mobile
    const contentStructure = JSON.parse(lesson.content_structure || '{}');
    
    // Break into micro-sessions for elementary attention spans
    const microSessions = contentStructure.sections?.map((section, index) => ({
      id: `${section.type}_${index}`,
      type: section.type,
      title: section.title,
      duration: section.duration,
      mobileOptimized: {
        maxDuration: Math.min(section.duration, 5), // Max 5 minutes for elementary
        interactionPrompts: section.duration > 3 ? ['tap_to_continue', 'think_time'] : [],
        visualCues: getVisualCuesForType(section.type)
      }
    })) || [];

    // Add visual progress milestones
    const visualProgressMilestones = [
      { 
        milestone: 'lesson_start', 
        icon: '🌟', 
        message: 'Your hero journey begins!' 
      },
      { 
        milestone: 'video_watched', 
        icon: '👀', 
        message: 'Great watching! You\'re learning so much!' 
      },
      { 
        milestone: 'activity_complete', 
        icon: '🎯', 
        message: 'Amazing thinking! You\'re becoming a real hero!' 
      },
      { 
        milestone: 'lesson_complete', 
        icon: '🏆', 
        message: 'Incredible! You completed another hero lesson!' 
      }
    ];

    const response = {
      success: true,
      lesson: {
        id: lesson.id,
        title: lesson.title,
        description: lesson.description,
        estimatedTime: lesson.duration_minutes,
        difficultyLevel: lesson.difficulty_level,
        microSessions,
        visualProgressMilestones,
        mobileFeatures: {
          deviceType,
          offlineCapable: true,
          touchOptimized: true,
          accessibilitySupport: true
        }
      }
    };

    res.json(response);

  } catch (error) {
    logger.error('Error getting mobile-optimized lesson:', error);
    res.status(500).json({
      success: false,
      error: 'Internal server error'
    });
  }
};

// Helper function for visual cues
function getVisualCuesForType(type) {
  const visualCues = {
    video: { icon: '📹', color: '#FF6B6B', instruction: 'Tap to watch' },
    activity: { icon: '🎯', color: '#4ECDC4', instruction: 'Let\'s try this!' },
    discussion: { icon: '💭', color: '#45B7D1', instruction: 'Time to share' },
    reflection: { icon: '🤔', color: '#96CEB4', instruction: 'Think about this' }
  };
  return visualCues[type] || { icon: '⭐', color: '#FFD93D', instruction: 'Let\'s go!' };
}

module.exports = {
  getClassroomPreview,
  submitEmotionalCheckin,
  getMobileOptimizedLesson
};
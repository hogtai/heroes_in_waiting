const swaggerJsdoc = require('swagger-jsdoc');

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Heroes in Waiting API',
      version: '1.0.0',
      description: 'REST API for Heroes in Waiting anti-bullying curriculum mobile app',
      contact: {
        name: 'Heroes in Waiting Team',
        email: 'support@heroesinwaiting.org'
      }
    },
    servers: [
      {
        url: process.env.API_BASE_URL || 'http://localhost:3000',
        description: 'Development server'
      }
    ],
    components: {
      securitySchemes: {
        BearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: 'JWT token for facilitator authentication'
        },
        StudentAuth: {
          type: 'apiKey',
          in: 'header',
          name: 'X-Student-Auth',
          description: 'Student authentication using classroom code and student ID'
        }
      },
      schemas: {
        Error: {
          type: 'object',
          properties: {
            success: {
              type: 'boolean',
              example: false
            },
            error: {
              type: 'string',
              example: 'Error message description'
            }
          }
        },
        Facilitator: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid',
              example: '123e4567-e89b-12d3-a456-426614174000'
            },
            email: {
              type: 'string',
              format: 'email',
              example: 'facilitator@school.edu'
            },
            firstName: {
              type: 'string',
              example: 'John'
            },
            lastName: {
              type: 'string',
              example: 'Smith'
            },
            organization: {
              type: 'string',
              example: 'Elementary School District'
            },
            role: {
              type: 'string',
              example: 'Teacher'
            },
            createdAt: {
              type: 'string',
              format: 'date-time'
            }
          }
        },
        Classroom: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            name: {
              type: 'string',
              example: 'Mrs. Smith\'s 4th Grade'
            },
            description: {
              type: 'string',
              example: 'Morning class for 4th grade students'
            },
            classroomCode: {
              type: 'string',
              example: 'ABC123'
            },
            gradeLevel: {
              type: 'integer',
              minimum: 1,
              maximum: 12,
              example: 4
            },
            studentCapacity: {
              type: 'integer',
              example: 30
            },
            studentCount: {
              type: 'integer',
              example: 25
            },
            isActive: {
              type: 'boolean',
              example: true
            },
            createdAt: {
              type: 'string',
              format: 'date-time'
            }
          }
        },
        Student: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            anonymousId: {
              type: 'string',
              example: 'student_abc12345'
            },
            classroomId: {
              type: 'string',
              format: 'uuid'
            },
            gradeLevel: {
              type: 'integer',
              minimum: 1,
              maximum: 12,
              example: 4
            },
            gender: {
              type: 'string',
              enum: ['male', 'female', 'non-binary', 'prefer-not-to-say']
            },
            ethnicity: {
              type: 'string',
              example: 'Hispanic/Latino'
            },
            hasDisabilities: {
              type: 'boolean'
            },
            primaryLanguage: {
              type: 'string',
              example: 'English'
            },
            totalSessions: {
              type: 'integer',
              example: 5
            },
            joinedAt: {
              type: 'string',
              format: 'date-time'
            },
            lastActiveAt: {
              type: 'string',
              format: 'date-time'
            }
          }
        },
        Lesson: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            lessonNumber: {
              type: 'integer',
              example: 1
            },
            title: {
              type: 'string',
              example: 'Understanding Kindness'
            },
            description: {
              type: 'string',
              example: 'Introduction to kindness and empathy concepts'
            },
            learningObjectives: {
              type: 'string',
              example: 'Students will understand the importance of kindness in building community'
            },
            durationMinutes: {
              type: 'integer',
              example: 30
            },
            difficultyLevel: {
              type: 'string',
              enum: ['beginner', 'intermediate', 'advanced'],
              example: 'beginner'
            },
            videoUrl: {
              type: 'string',
              format: 'uri',
              example: 'https://example.com/lesson1.mp4'
            },
            videoThumbnail: {
              type: 'string',
              format: 'uri',
              example: 'https://example.com/lesson1_thumb.jpg'
            },
            videoDurationSeconds: {
              type: 'integer',
              example: 600
            },
            downloadableResources: {
              type: 'array',
              items: {
                type: 'string',
                format: 'uri'
              }
            },
            activities: {
              type: 'array',
              items: {
                type: 'object'
              }
            },
            discussionQuestions: {
              type: 'array',
              items: {
                type: 'string'
              }
            },
            isPublished: {
              type: 'boolean',
              example: true
            }
          }
        },
        Progress: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            lessonId: {
              type: 'string',
              format: 'uuid'
            },
            completionStatus: {
              type: 'string',
              enum: ['not_started', 'in_progress', 'completed'],
              example: 'in_progress'
            },
            progressPercentage: {
              type: 'integer',
              minimum: 0,
              maximum: 100,
              example: 75
            },
            timeSpentMinutes: {
              type: 'integer',
              example: 20
            },
            videoWatchPercentage: {
              type: 'integer',
              minimum: 0,
              maximum: 100,
              example: 90
            },
            activitiesCompleted: {
              type: 'integer',
              example: 3
            },
            totalActivities: {
              type: 'integer',
              example: 4
            },
            startedAt: {
              type: 'string',
              format: 'date-time'
            },
            completedAt: {
              type: 'string',
              format: 'date-time',
              nullable: true
            }
          }
        },
        Feedback: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            lessonId: {
              type: 'string',
              format: 'uuid'
            },
            feedbackType: {
              type: 'string',
              enum: ['mood_checkin', 'reflection', 'activity_response', 'lesson_rating'],
              example: 'lesson_rating'
            },
            feedbackCategory: {
              type: 'string',
              example: 'bullying_awareness'
            },
            ratingValue: {
              type: 'integer',
              minimum: 1,
              maximum: 5,
              example: 4
            },
            textResponse: {
              type: 'string',
              example: 'I learned a lot about being kind to others'
            },
            moodIndicator: {
              type: 'string',
              enum: ['happy', 'sad', 'confused', 'excited', 'angry', 'worried', 'proud'],
              example: 'happy'
            },
            submittedAt: {
              type: 'string',
              format: 'date-time'
            }
          }
        }
      }
    },
    tags: [
      {
        name: 'Authentication',
        description: 'Facilitator authentication and profile management'
      },
      {
        name: 'Classrooms',
        description: 'Classroom management for facilitators'
      },
      {
        name: 'Students',
        description: 'Student enrollment and management'
      },
      {
        name: 'Lessons',
        description: 'Lesson content delivery and management'
      },
      {
        name: 'Progress',
        description: 'Student progress tracking and feedback'
      },
      {
        name: 'Analytics',
        description: 'Analytics and reporting for facilitators'
      }
    ]
  },
  apis: ['./src/routes/*.js', './src/controllers/*.js']
};

const specs = swaggerJsdoc(options);

module.exports = specs;
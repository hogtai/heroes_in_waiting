const express = require('express');
const { body, param, query } = require('express-validator');
const { authenticateFacilitator } = require('../middleware/auth');
const { validateRequest } = require('../middleware/validation');
const { upload } = require('../middleware/fileUpload');
const contentManagementController = require('../controllers/contentManagementController');
const logger = require('../utils/logger');

const router = express.Router();

// =====================================================
// CONTENT VERSION MANAGEMENT
// =====================================================

/**
 * @route   GET /api/content/versions
 * @desc    Get all content versions with filtering
 * @access  Private (Facilitator)
 */
router.get('/versions',
  authenticateFacilitator,
  [
    query('lessonId').optional().isUUID().withMessage('Lesson ID must be a valid UUID'),
    query('status').optional().isIn(['draft', 'review', 'approved', 'published']).withMessage('Invalid status'),
    query('page').optional().isInt({ min: 1 }).withMessage('Page must be a positive integer'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limit must be between 1 and 100')
  ],
  validateRequest,
  contentManagementController.getContentVersions
);

/**
 * @route   GET /api/content/versions/:id
 * @desc    Get specific content version
 * @access  Private (Facilitator)
 */
router.get('/versions/:id',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Version ID must be a valid UUID')
  ],
  validateRequest,
  contentManagementController.getContentVersion
);

/**
 * @route   POST /api/content/versions
 * @desc    Create new content version
 * @access  Private (Facilitator)
 */
router.post('/versions',
  authenticateFacilitator,
  [
    body('lessonId').isUUID().withMessage('Lesson ID must be a valid UUID'),
    body('title').trim().isLength({ min: 1, max: 255 }).withMessage('Title is required and must be less than 255 characters'),
    body('description').optional().trim().isLength({ max: 1000 }).withMessage('Description must be less than 1000 characters'),
    body('contentStructure').isObject().withMessage('Content structure is required'),
    body('changeSummary').optional().trim().isLength({ max: 500 }).withMessage('Change summary must be less than 500 characters'),
    body('metadata').optional().isObject().withMessage('Metadata must be an object')
  ],
  validateRequest,
  contentManagementController.createContentVersion
);

/**
 * @route   PUT /api/content/versions/:id
 * @desc    Update content version
 * @access  Private (Facilitator)
 */
router.put('/versions/:id',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Version ID must be a valid UUID'),
    body('title').optional().trim().isLength({ min: 1, max: 255 }).withMessage('Title must be less than 255 characters'),
    body('description').optional().trim().isLength({ max: 1000 }).withMessage('Description must be less than 1000 characters'),
    body('contentStructure').optional().isObject().withMessage('Content structure must be an object'),
    body('changeSummary').optional().trim().isLength({ max: 500 }).withMessage('Change summary must be less than 500 characters'),
    body('metadata').optional().isObject().withMessage('Metadata must be an object')
  ],
  validateRequest,
  contentManagementController.updateContentVersion
);

/**
 * @route   DELETE /api/content/versions/:id
 * @desc    Delete content version (soft delete)
 * @access  Private (Facilitator)
 */
router.delete('/versions/:id',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Version ID must be a valid UUID')
  ],
  validateRequest,
  contentManagementController.deleteContentVersion
);

// =====================================================
// MEDIA FILE MANAGEMENT
// =====================================================

/**
 * @route   POST /api/content/media/upload
 * @desc    Upload media file
 * @access  Private (Facilitator)
 */
router.post('/media/upload',
  authenticateFacilitator,
  upload.single('file'),
  [
    body('accessLevel').optional().isIn(['private', 'classroom', 'public']).withMessage('Invalid access level'),
    body('isPublic').optional().isBoolean().withMessage('isPublic must be a boolean')
  ],
  validateRequest,
  contentManagementController.uploadMediaFile
);

/**
 * @route   GET /api/content/media
 * @desc    Get media files with filtering
 * @access  Private (Facilitator)
 */
router.get('/media',
  authenticateFacilitator,
  [
    query('mediaType').optional().isIn(['video', 'audio', 'image', 'document']).withMessage('Invalid media type'),
    query('accessLevel').optional().isIn(['private', 'classroom', 'public']).withMessage('Invalid access level'),
    query('page').optional().isInt({ min: 1 }).withMessage('Page must be a positive integer'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limit must be between 1 and 100')
  ],
  validateRequest,
  contentManagementController.getMediaFiles
);

/**
 * @route   GET /api/content/media/:id
 * @desc    Get specific media file
 * @access  Private (Facilitator)
 */
router.get('/media/:id',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Media file ID must be a valid UUID')
  ],
  validateRequest,
  contentManagementController.getMediaFile
);

/**
 * @route   DELETE /api/content/media/:id
 * @desc    Delete media file
 * @access  Private (Facilitator)
 */
router.delete('/media/:id',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Media file ID must be a valid UUID')
  ],
  validateRequest,
  contentManagementController.deleteMediaFile
);

// =====================================================
// CONTENT APPROVAL WORKFLOW
// =====================================================

/**
 * @route   POST /api/content/approvals
 * @desc    Request content approval
 * @access  Private (Facilitator)
 */
router.post('/approvals',
  authenticateFacilitator,
  [
    body('contentVersionId').isUUID().withMessage('Content version ID must be a valid UUID'),
    body('assignedTo').optional().isUUID().withMessage('Assigned to must be a valid UUID'),
    body('requestNotes').optional().trim().isLength({ max: 1000 }).withMessage('Request notes must be less than 1000 characters')
  ],
  validateRequest,
  contentManagementController.requestApproval
);

/**
 * @route   GET /api/content/approvals
 * @desc    Get approval requests
 * @access  Private (Facilitator)
 */
router.get('/approvals',
  authenticateFacilitator,
  [
    query('status').optional().isIn(['pending', 'in_review', 'approved', 'rejected']).withMessage('Invalid status'),
    query('assignedTo').optional().isUUID().withMessage('Assigned to must be a valid UUID'),
    query('page').optional().isInt({ min: 1 }).withMessage('Page must be a positive integer'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limit must be between 1 and 100')
  ],
  validateRequest,
  contentManagementController.getApprovalRequests
);

/**
 * @route   PUT /api/content/approvals/:id/review
 * @desc    Review approval request
 * @access  Private (Facilitator)
 */
router.put('/approvals/:id/review',
  authenticateFacilitator,
  [
    param('id').isUUID().withMessage('Approval ID must be a valid UUID'),
    body('status').isIn(['approved', 'rejected']).withMessage('Status must be approved or rejected'),
    body('reviewNotes').optional().trim().isLength({ max: 1000 }).withMessage('Review notes must be less than 1000 characters')
  ],
  validateRequest,
  contentManagementController.reviewApproval
);

// =====================================================
// CONTENT CATEGORIES AND TAGS
// =====================================================

/**
 * @route   GET /api/content/categories
 * @desc    Get content categories
 * @access  Private (Facilitator)
 */
router.get('/categories',
  authenticateFacilitator,
  contentManagementController.getContentCategories
);

/**
 * @route   POST /api/content/categories
 * @desc    Create content category
 * @access  Private (Facilitator)
 */
router.post('/categories',
  authenticateFacilitator,
  [
    body('name').trim().isLength({ min: 1, max: 100 }).withMessage('Name is required and must be less than 100 characters'),
    body('description').optional().trim().isLength({ max: 500 }).withMessage('Description must be less than 500 characters'),
    body('color').optional().matches(/^#[0-9A-F]{6}$/i).withMessage('Color must be a valid hex color'),
    body('parentId').optional().isUUID().withMessage('Parent ID must be a valid UUID'),
    body('sortOrder').optional().isInt({ min: 0 }).withMessage('Sort order must be a non-negative integer')
  ],
  validateRequest,
  contentManagementController.createContentCategory
);

/**
 * @route   GET /api/content/tags
 * @desc    Get content tags
 * @access  Private (Facilitator)
 */
router.get('/tags',
  authenticateFacilitator,
  contentManagementController.getContentTags
);

/**
 * @route   POST /api/content/tags
 * @desc    Create content tag
 * @access  Private (Facilitator)
 */
router.post('/tags',
  authenticateFacilitator,
  [
    body('name').trim().isLength({ min: 1, max: 50 }).withMessage('Name is required and must be less than 50 characters'),
    body('description').optional().trim().isLength({ max: 200 }).withMessage('Description must be less than 200 characters')
  ],
  validateRequest,
  contentManagementController.createContentTag
);

// =====================================================
// CONTENT ANALYTICS
// =====================================================

/**
 * @route   POST /api/content/analytics/track
 * @desc    Track content usage event
 * @access  Private (Facilitator/Student)
 */
router.post('/analytics/track',
  [
    body('contentVersionId').isUUID().withMessage('Content version ID must be a valid UUID'),
    body('eventType').isIn(['viewed', 'downloaded', 'shared', 'rated']).withMessage('Invalid event type'),
    body('eventData').optional().isObject().withMessage('Event data must be an object'),
    body('classroomId').optional().isUUID().withMessage('Classroom ID must be a valid UUID')
  ],
  validateRequest,
  contentManagementController.trackContentEvent
);

/**
 * @route   GET /api/content/analytics/summary
 * @desc    Get content analytics summary
 * @access  Private (Facilitator)
 */
router.get('/analytics/summary',
  authenticateFacilitator,
  [
    query('contentVersionId').optional().isUUID().withMessage('Content version ID must be a valid UUID'),
    query('timeframe').optional().isIn(['7d', '30d', '90d']).withMessage('Invalid timeframe')
  ],
  validateRequest,
  contentManagementController.getContentAnalyticsSummary
);

module.exports = router; 
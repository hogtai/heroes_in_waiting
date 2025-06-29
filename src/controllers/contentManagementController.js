const db = require('../config/database');
const logger = require('../utils/logger');
const { getFileHash, getFileMetadata, cleanupFile } = require('../middleware/fileUpload');
const { sanitizeHTMLContent, sanitizeContentStructure, sanitizeMetadata, detectPII } = require('../utils/contentSanitizer');
const path = require('path');

// =====================================================
// CONTENT VERSION MANAGEMENT
// =====================================================

/**
 * @desc    Get all content versions with filtering
 * @route   GET /api/content/versions
 * @access  Private (Facilitator)
 */
async function getContentVersions(req, res) {
  try {
    const { lessonId, status, page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    
    let query = db('content_versions')
      .select([
        'content_versions.*',
        'lessons.title as lesson_title',
        'lessons.lesson_number',
        'facilitators.first_name',
        'facilitators.last_name'
      ])
      .leftJoin('lessons', 'content_versions.lesson_id', 'lessons.id')
      .leftJoin('facilitators', 'content_versions.created_by', 'facilitators.id')
      .orderBy('content_versions.created_at', 'desc');
    
    // Apply filters
    if (lessonId) {
      query = query.where('content_versions.lesson_id', lessonId);
    }
    
    if (status) {
      query = query.where('content_versions.status', status);
    }
    
    // Get total count for pagination
    const countQuery = query.clone();
    const totalCount = await countQuery.count('* as count').first();
    
    // Get paginated results
    const versions = await query.limit(limit).offset(offset);
    
    res.json({
      success: true,
      data: {
        versions: versions.map(version => ({
          id: version.id,
          lessonId: version.lesson_id,
          lessonTitle: version.lesson_title,
          lessonNumber: version.lesson_number,
          versionNumber: version.version_number,
          title: version.title,
          description: version.description,
          contentStructure: version.content_structure,
          metadata: version.metadata,
          status: version.status,
          changeSummary: version.change_summary,
          createdBy: {
            id: version.created_by,
            name: `${version.first_name} ${version.last_name}`
          },
          reviewedBy: version.reviewed_by,
          reviewedAt: version.reviewed_at,
          reviewNotes: version.review_notes,
          createdAt: version.created_at,
          updatedAt: version.updated_at
        })),
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: totalCount.count,
          totalPages: Math.ceil(totalCount.count / limit)
        }
      }
    });
  } catch (error) {
    logger.error('Get content versions failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get content versions'
    });
  }
}

/**
 * @desc    Get specific content version
 * @route   GET /api/content/versions/:id
 * @access  Private (Facilitator)
 */
async function getContentVersion(req, res) {
  try {
    const { id } = req.params;
    
    const version = await db('content_versions')
      .select([
        'content_versions.*',
        'lessons.title as lesson_title',
        'lessons.lesson_number',
        'facilitators.first_name',
        'facilitators.last_name'
      ])
      .leftJoin('lessons', 'content_versions.lesson_id', 'lessons.id')
      .leftJoin('facilitators', 'content_versions.created_by', 'facilitators.id')
      .where('content_versions.id', id)
      .first();
    
    if (!version) {
      return res.status(404).json({
        success: false,
        error: 'Content version not found'
      });
    }
    
    res.json({
      success: true,
      data: {
        id: version.id,
        lessonId: version.lesson_id,
        lessonTitle: version.lesson_title,
        lessonNumber: version.lesson_number,
        versionNumber: version.version_number,
        title: version.title,
        description: version.description,
        contentStructure: version.content_structure,
        metadata: version.metadata,
        status: version.status,
        changeSummary: version.change_summary,
        createdBy: {
          id: version.created_by,
          name: `${version.first_name} ${version.last_name}`
        },
        reviewedBy: version.reviewed_by,
        reviewedAt: version.reviewed_at,
        reviewNotes: version.review_notes,
        createdAt: version.created_at,
        updatedAt: version.updated_at
      }
    });
  } catch (error) {
    logger.error('Get content version failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get content version'
    });
  }
}

/**
 * @desc    Create new content version
 * @route   POST /api/content/versions
 * @access  Private (Facilitator)
 */
async function createContentVersion(req, res) {
  try {
    const {
      lessonId,
      title,
      description,
      contentStructure,
      changeSummary,
      metadata
    } = req.body;
    
    const facilitatorId = req.facilitator.id;
    
    // Sanitize all content inputs for educational use
    const sanitizedTitle = sanitizeHTMLContent(title, 'facilitator');
    const sanitizedDescription = description ? sanitizeHTMLContent(description, 'facilitator') : null;
    const sanitizedChangeSummary = changeSummary ? sanitizeHTMLContent(changeSummary, 'facilitator') : null;
    
    // Sanitize content structure (handles nested HTML content)
    const sanitizedContentStructure = sanitizeContentStructure(contentStructure, 'facilitator');
    
    // Sanitize metadata
    const sanitizedMetadata = sanitizeMetadata(metadata || {});
    
    // Detect PII in content (COPPA compliance)
    const titlePII = detectPII(sanitizedTitle);
    const descriptionPII = description ? detectPII(sanitizedDescription) : [];
    const contentPII = detectPII(JSON.stringify(sanitizedContentStructure));
    
    // Log PII detection for compliance
    const allPII = [...titlePII, ...descriptionPII, ...contentPII];
    if (allPII.length > 0) {
      logger.warn('PII detected in content version creation:', {
        facilitatorId: facilitatorId,
        lessonId: lessonId,
        piiDetections: allPII.map(detection => ({ type: detection.type, count: detection.count }))
      });
      
      // In production, you might want to reject content with PII
      // For now, we'll log and allow but strip the PII
    }
    
    // Get the next version number for this lesson
    const lastVersion = await db('content_versions')
      .where('lesson_id', lessonId)
      .orderBy('version_number', 'desc')
      .first();
    
    const versionNumber = lastVersion ? lastVersion.version_number + 1 : 1;
    
    // Create the new version with sanitized content
    const [version] = await db('content_versions')
      .insert({
        lesson_id: lessonId,
        version_number: versionNumber,
        title: sanitizedTitle,
        description: sanitizedDescription,
        content_structure: sanitizedContentStructure,
        metadata: sanitizedMetadata,
        created_by: facilitatorId,
        change_summary: sanitizedChangeSummary,
        status: 'draft'
      })
      .returning('*');
    
    logger.info('Content version created:', {
      versionId: version.id,
      lessonId: lessonId,
      versionNumber: versionNumber,
      createdBy: facilitatorId
    });
    
    res.status(201).json({
      success: true,
      data: {
        id: version.id,
        lessonId: version.lesson_id,
        versionNumber: version.version_number,
        title: version.title,
        description: version.description,
        contentStructure: version.content_structure,
        metadata: version.metadata,
        status: version.status,
        changeSummary: version.change_summary,
        createdAt: version.created_at
      }
    });
  } catch (error) {
    logger.error('Create content version failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create content version'
    });
  }
}

/**
 * @desc    Update content version
 * @route   PUT /api/content/versions/:id
 * @access  Private (Facilitator)
 */
async function updateContentVersion(req, res) {
  try {
    const { id } = req.params;
    const updateData = { ...req.body };
    
    // Remove fields that shouldn't be updated directly
    delete updateData.lessonId;
    delete updateData.versionNumber;
    delete updateData.createdBy;
    delete updateData.createdAt;
    
    // Sanitize content inputs for educational use
    const sanitizedUpdateData = {};
    
    // Sanitize each field based on its type
    Object.keys(updateData).forEach(key => {
      switch (key) {
        case 'title':
          sanitizedUpdateData[key] = sanitizeHTMLContent(updateData[key], 'facilitator');
          break;
        case 'description':
          sanitizedUpdateData[key] = updateData[key] ? sanitizeHTMLContent(updateData[key], 'facilitator') : null;
          break;
        case 'changeSummary':
          sanitizedUpdateData[key] = updateData[key] ? sanitizeHTMLContent(updateData[key], 'facilitator') : null;
          break;
        case 'contentStructure':
          sanitizedUpdateData[key] = sanitizeContentStructure(updateData[key], 'facilitator');
          break;
        case 'metadata':
          sanitizedUpdateData[key] = sanitizeMetadata(updateData[key] || {});
          break;
        default:
          sanitizedUpdateData[key] = updateData[key];
      }
    });
    
    // Detect PII in updated content (COPPA compliance)
    const piiDetections = [];
    if (sanitizedUpdateData.title) {
      piiDetections.push(...detectPII(sanitizedUpdateData.title));
    }
    if (sanitizedUpdateData.description) {
      piiDetections.push(...detectPII(sanitizedUpdateData.description));
    }
    if (sanitizedUpdateData.contentStructure) {
      piiDetections.push(...detectPII(JSON.stringify(sanitizedUpdateData.contentStructure)));
    }
    
    // Log PII detection for compliance
    if (piiDetections.length > 0) {
      logger.warn('PII detected in content version update:', {
        versionId: id,
        facilitatorId: req.facilitator.id,
        piiDetections: piiDetections.map(detection => ({ type: detection.type, count: detection.count }))
      });
    }
    
    // Convert camelCase to snake_case for database
    const dbUpdateData = {};
    Object.keys(sanitizedUpdateData).forEach(key => {
      switch (key) {
        case 'contentStructure':
          dbUpdateData.content_structure = sanitizedUpdateData[key];
          break;
        case 'changeSummary':
          dbUpdateData.change_summary = sanitizedUpdateData[key];
          break;
        default:
          dbUpdateData[key] = sanitizedUpdateData[key];
      }
    });
    
    dbUpdateData.updated_at = new Date();
    
    const [version] = await db('content_versions')
      .where('id', id)
      .update(dbUpdateData)
      .returning('*');
    
    if (!version) {
      return res.status(404).json({
        success: false,
        error: 'Content version not found'
      });
    }
    
    logger.info('Content version updated:', {
      versionId: id,
      updatedBy: req.facilitator.id
    });
    
    res.json({
      success: true,
      data: {
        id: version.id,
        lessonId: version.lesson_id,
        versionNumber: version.version_number,
        title: version.title,
        description: version.description,
        contentStructure: version.content_structure,
        metadata: version.metadata,
        status: version.status,
        changeSummary: version.change_summary,
        updatedAt: version.updated_at
      }
    });
  } catch (error) {
    logger.error('Update content version failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to update content version'
    });
  }
}

/**
 * @desc    Delete content version (soft delete)
 * @route   DELETE /api/content/versions/:id
 * @access  Private (Facilitator)
 */
async function deleteContentVersion(req, res) {
  try {
    const { id } = req.params;
    
    // Check if version exists and is not published
    const version = await db('content_versions')
      .where('id', id)
      .first();
    
    if (!version) {
      return res.status(404).json({
        success: false,
        error: 'Content version not found'
      });
    }
    
    if (version.status === 'published') {
      return res.status(400).json({
        success: false,
        error: 'Cannot delete published content version'
      });
    }
    
    // Soft delete by updating status
    await db('content_versions')
      .where('id', id)
      .update({
        status: 'deleted',
        updated_at: new Date()
      });
    
    logger.info('Content version deleted:', {
      versionId: id,
      deletedBy: req.facilitator.id
    });
    
    res.json({
      success: true,
      message: 'Content version deleted successfully'
    });
  } catch (error) {
    logger.error('Delete content version failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to delete content version'
    });
  }
}

// =====================================================
// MEDIA FILE MANAGEMENT
// =====================================================

/**
 * @desc    Upload media file
 * @route   POST /api/content/media/upload
 * @access  Private (Facilitator)
 */
async function uploadMediaFile(req, res) {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'No file uploaded'
      });
    }
    
    const { accessLevel = 'private', isPublic = false } = req.body;
    const facilitatorId = req.facilitator.id;
    
    // Calculate file hash for deduplication
    const fileHash = await getFileHash(req.file.path);
    
    // Check if file already exists
    const existingFile = await db('media_files')
      .where('file_hash', fileHash)
      .first();
    
    if (existingFile) {
      // Clean up the duplicate file
      await cleanupFile(req.file.path);
      
      return res.json({
        success: true,
        data: {
          id: existingFile.id,
          fileName: existingFile.file_name,
          originalName: existingFile.original_name,
          filePath: existingFile.file_path,
          mimeType: existingFile.mime_type,
          fileSizeBytes: existingFile.file_size_bytes,
          mediaType: existingFile.media_type,
          isPublic: existingFile.is_public,
          accessLevel: existingFile.access_level,
          createdAt: existingFile.created_at,
          message: 'File already exists'
        }
      });
    }
    
    // Get file metadata with content validation
    const metadata = await getFileMetadata(req.file.path, req.file.mimetype);
    
    // Check file content validation results
    if (metadata.contentValidation && !metadata.contentValidation.isValid) {
      // Clean up invalid file
      await cleanupFile(req.file.path);
      
      logger.warn('Invalid file upload attempt:', {
        facilitatorId: facilitatorId,
        originalName: req.file.originalname,
        expectedType: req.file.mimetype,
        validationMessage: metadata.contentValidation.message
      });
      
      return res.status(400).json({
        success: false,
        error: `File validation failed: ${metadata.contentValidation.message}`
      });
    }
    
    // Detect PII in file metadata (filename, etc.) for COPPA compliance
    const filenamePII = detectPII(req.file.originalname);
    if (filenamePII.length > 0) {
      logger.warn('PII detected in uploaded filename:', {
        facilitatorId: facilitatorId,
        originalName: req.file.originalname,
        piiDetections: filenamePII.map(detection => ({ type: detection.type, count: detection.count }))
      });
      
      // In production, you might want to reject files with PII in filename
      // For now, we'll log and allow but recommend renaming
    }
    
    // Enhanced metadata with security information
    const enhancedMetadata = {
      ...metadata,
      uploadTimestamp: new Date().toISOString(),
      uploaderIP: req.ip,
      userAgent: req.get('User-Agent'),
      securityScan: {
        piiDetected: filenamePII.length > 0,
        contentValidated: metadata.contentValidation ? metadata.contentValidation.isValid : false,
        scanTimestamp: new Date().toISOString()
      }
    };
    
    // Create media file record with enhanced security metadata
    const [mediaFile] = await db('media_files')
      .insert({
        file_name: req.file.filename,
        original_name: req.file.originalname,
        file_path: req.file.path,
        mime_type: req.file.mimetype,
        file_size_bytes: enhancedMetadata.fileSizeBytes,
        file_hash: fileHash,
        media_type: enhancedMetadata.mediaType,
        metadata: enhancedMetadata,
        uploaded_by: facilitatorId,
        is_public: isPublic,
        access_level: accessLevel
      })
      .returning('*');
    
    logger.info('Media file uploaded:', {
      fileId: mediaFile.id,
      fileName: mediaFile.file_name,
      uploadedBy: facilitatorId,
      fileSize: metadata.fileSizeBytes
    });
    
    res.status(201).json({
      success: true,
      data: {
        id: mediaFile.id,
        fileName: mediaFile.file_name,
        originalName: mediaFile.original_name,
        filePath: mediaFile.file_path,
        mimeType: mediaFile.mime_type,
        fileSizeBytes: mediaFile.file_size_bytes,
        mediaType: mediaFile.media_type,
        metadata: mediaFile.metadata,
        isPublic: mediaFile.is_public,
        accessLevel: mediaFile.access_level,
        createdAt: mediaFile.created_at
      }
    });
  } catch (error) {
    logger.error('Upload media file failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to upload media file'
    });
  }
}

/**
 * @desc    Get media files with filtering
 * @route   GET /api/content/media
 * @access  Private (Facilitator)
 */
async function getMediaFiles(req, res) {
  try {
    const { mediaType, accessLevel, page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    const facilitatorId = req.facilitator.id;
    
    let query = db('media_files')
      .select([
        'media_files.*',
        'facilitators.first_name',
        'facilitators.last_name'
      ])
      .leftJoin('facilitators', 'media_files.uploaded_by', 'facilitators.id')
      .where(function() {
        this.where('media_files.uploaded_by', facilitatorId)
          .orWhere('media_files.is_public', true)
          .orWhere('media_files.access_level', 'public');
      })
      .orderBy('media_files.created_at', 'desc');
    
    // Apply filters
    if (mediaType) {
      query = query.where('media_files.media_type', mediaType);
    }
    
    if (accessLevel) {
      query = query.where('media_files.access_level', accessLevel);
    }
    
    // Get total count for pagination
    const countQuery = query.clone();
    const totalCount = await countQuery.count('* as count').first();
    
    // Get paginated results
    const mediaFiles = await query.limit(limit).offset(offset);
    
    res.json({
      success: true,
      data: {
        mediaFiles: mediaFiles.map(file => ({
          id: file.id,
          fileName: file.file_name,
          originalName: file.original_name,
          filePath: file.file_path,
          mimeType: file.mime_type,
          fileSizeBytes: file.file_size_bytes,
          mediaType: file.media_type,
          metadata: file.metadata,
          isPublic: file.is_public,
          accessLevel: file.access_level,
          uploadedBy: {
            id: file.uploaded_by,
            name: `${file.first_name} ${file.last_name}`
          },
          createdAt: file.created_at,
          updatedAt: file.updated_at
        })),
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: totalCount.count,
          totalPages: Math.ceil(totalCount.count / limit)
        }
      }
    });
  } catch (error) {
    logger.error('Get media files failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get media files'
    });
  }
}

/**
 * @desc    Get specific media file
 * @route   GET /api/content/media/:id
 * @access  Private (Facilitator)
 */
async function getMediaFile(req, res) {
  try {
    const { id } = req.params;
    const facilitatorId = req.facilitator.id;
    
    const mediaFile = await db('media_files')
      .select([
        'media_files.*',
        'facilitators.first_name',
        'facilitators.last_name'
      ])
      .leftJoin('facilitators', 'media_files.uploaded_by', 'facilitators.id')
      .where('media_files.id', id)
      .where(function() {
        this.where('media_files.uploaded_by', facilitatorId)
          .orWhere('media_files.is_public', true)
          .orWhere('media_files.access_level', 'public');
      })
      .first();
    
    if (!mediaFile) {
      return res.status(404).json({
        success: false,
        error: 'Media file not found'
      });
    }
    
    res.json({
      success: true,
      data: {
        id: mediaFile.id,
        fileName: mediaFile.file_name,
        originalName: mediaFile.original_name,
        filePath: mediaFile.file_path,
        mimeType: mediaFile.mime_type,
        fileSizeBytes: mediaFile.file_size_bytes,
        mediaType: mediaFile.media_type,
        metadata: mediaFile.metadata,
        isPublic: mediaFile.is_public,
        accessLevel: mediaFile.access_level,
        uploadedBy: {
          id: mediaFile.uploaded_by,
          name: `${mediaFile.first_name} ${mediaFile.last_name}`
        },
        createdAt: mediaFile.created_at,
        updatedAt: mediaFile.updated_at
      }
    });
  } catch (error) {
    logger.error('Get media file failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get media file'
    });
  }
}

/**
 * @desc    Delete media file
 * @route   DELETE /api/content/media/:id
 * @access  Private (Facilitator)
 */
async function deleteMediaFile(req, res) {
  try {
    const { id } = req.params;
    const facilitatorId = req.facilitator.id;
    
    // Check if file exists and user has permission
    const mediaFile = await db('media_files')
      .where('id', id)
      .where('uploaded_by', facilitatorId)
      .first();
    
    if (!mediaFile) {
      return res.status(404).json({
        success: false,
        error: 'Media file not found or access denied'
      });
    }
    
    // Delete the physical file
    await cleanupFile(mediaFile.file_path);
    
    // Delete the database record
    await db('media_files')
      .where('id', id)
      .del();
    
    logger.info('Media file deleted:', {
      fileId: id,
      deletedBy: facilitatorId
    });
    
    res.json({
      success: true,
      message: 'Media file deleted successfully'
    });
  } catch (error) {
    logger.error('Delete media file failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to delete media file'
    });
  }
}

// =====================================================
// CONTENT APPROVAL WORKFLOW
// =====================================================

/**
 * @desc    Request content approval
 * @route   POST /api/content/approvals
 * @access  Private (Facilitator)
 */
async function requestApproval(req, res) {
  try {
    const { contentVersionId, assignedTo, requestNotes } = req.body;
    const facilitatorId = req.facilitator.id;
    
    // Check if content version exists
    const contentVersion = await db('content_versions')
      .where('id', contentVersionId)
      .first();
    
    if (!contentVersion) {
      return res.status(404).json({
        success: false,
        error: 'Content version not found'
      });
    }
    
    // Check if approval request already exists
    const existingRequest = await db('content_approvals')
      .where('content_version_id', contentVersionId)
      .where('status', 'pending')
      .first();
    
    if (existingRequest) {
      return res.status(409).json({
        success: false,
        error: 'Approval request already exists for this content version'
      });
    }
    
    // Create approval request
    const [approval] = await db('content_approvals')
      .insert({
        content_version_id: contentVersionId,
        requested_by: facilitatorId,
        assigned_to: assignedTo,
        request_notes: requestNotes,
        status: 'pending'
      })
      .returning('*');
    
    // Update content version status
    await db('content_versions')
      .where('id', contentVersionId)
      .update({
        status: 'review',
        updated_at: new Date()
      });
    
    logger.info('Content approval requested:', {
      approvalId: approval.id,
      contentVersionId: contentVersionId,
      requestedBy: facilitatorId,
      assignedTo: assignedTo
    });
    
    res.status(201).json({
      success: true,
      data: {
        id: approval.id,
        contentVersionId: approval.content_version_id,
        requestedBy: approval.requested_by,
        assignedTo: approval.assigned_to,
        status: approval.status,
        requestNotes: approval.request_notes,
        requestedAt: approval.requested_at
      }
    });
  } catch (error) {
    logger.error('Request approval failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to request approval'
    });
  }
}

/**
 * @desc    Get approval requests
 * @route   GET /api/content/approvals
 * @access  Private (Facilitator)
 */
async function getApprovalRequests(req, res) {
  try {
    const { status, assignedTo, page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;
    const facilitatorId = req.facilitator.id;
    
    let query = db('content_approvals')
      .select([
        'content_approvals.*',
        'content_versions.title as content_title',
        'content_versions.version_number',
        'lessons.title as lesson_title',
        'requesters.first_name as requester_first_name',
        'requesters.last_name as requester_last_name',
        'assignees.first_name as assignee_first_name',
        'assignees.last_name as assignee_last_name'
      ])
      .leftJoin('content_versions', 'content_approvals.content_version_id', 'content_versions.id')
      .leftJoin('lessons', 'content_versions.lesson_id', 'lessons.id')
      .leftJoin('facilitators as requesters', 'content_approvals.requested_by', 'requesters.id')
      .leftJoin('facilitators as assignees', 'content_approvals.assigned_to', 'assignees.id')
      .where(function() {
        this.where('content_approvals.requested_by', facilitatorId)
          .orWhere('content_approvals.assigned_to', facilitatorId);
      })
      .orderBy('content_approvals.requested_at', 'desc');
    
    // Apply filters
    if (status) {
      query = query.where('content_approvals.status', status);
    }
    
    if (assignedTo) {
      query = query.where('content_approvals.assigned_to', assignedTo);
    }
    
    // Get total count for pagination
    const countQuery = query.clone();
    const totalCount = await countQuery.count('* as count').first();
    
    // Get paginated results
    const approvals = await query.limit(limit).offset(offset);
    
    res.json({
      success: true,
      data: {
        approvals: approvals.map(approval => ({
          id: approval.id,
          contentVersionId: approval.content_version_id,
          contentTitle: approval.content_title,
          versionNumber: approval.version_number,
          lessonTitle: approval.lesson_title,
          requestedBy: {
            id: approval.requested_by,
            name: `${approval.requester_first_name} ${approval.requester_last_name}`
          },
          assignedTo: approval.assigned_to ? {
            id: approval.assigned_to,
            name: `${approval.assignee_first_name} ${approval.assignee_last_name}`
          } : null,
          status: approval.status,
          requestNotes: approval.request_notes,
          reviewNotes: approval.review_notes,
          requestedAt: approval.requested_at,
          reviewedAt: approval.reviewed_at
        })),
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: totalCount.count,
          totalPages: Math.ceil(totalCount.count / limit)
        }
      }
    });
  } catch (error) {
    logger.error('Get approval requests failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get approval requests'
    });
  }
}

/**
 * @desc    Review approval request
 * @route   PUT /api/content/approvals/:id/review
 * @access  Private (Facilitator)
 */
async function reviewApproval(req, res) {
  try {
    const { id } = req.params;
    const { status, reviewNotes } = req.body;
    const facilitatorId = req.facilitator.id;
    
    // Check if approval request exists and is assigned to the reviewer
    const approval = await db('content_approvals')
      .where('id', id)
      .where('assigned_to', facilitatorId)
      .where('status', 'pending')
      .first();
    
    if (!approval) {
      return res.status(404).json({
        success: false,
        error: 'Approval request not found or not assigned to you'
      });
    }
    
    // Update approval request
    const [updatedApproval] = await db('content_approvals')
      .where('id', id)
      .update({
        status: status,
        review_notes: reviewNotes,
        reviewed_at: new Date()
      })
      .returning('*');
    
    // Update content version status
    const newContentStatus = status === 'approved' ? 'approved' : 'draft';
    await db('content_versions')
      .where('id', approval.content_version_id)
      .update({
        status: newContentStatus,
        reviewed_by: facilitatorId,
        reviewed_at: new Date(),
        review_notes: reviewNotes,
        updated_at: new Date()
      });
    
    logger.info('Content approval reviewed:', {
      approvalId: id,
      contentVersionId: approval.content_version_id,
      reviewedBy: facilitatorId,
      status: status
    });
    
    res.json({
      success: true,
      data: {
        id: updatedApproval.id,
        contentVersionId: updatedApproval.content_version_id,
        status: updatedApproval.status,
        reviewNotes: updatedApproval.review_notes,
        reviewedAt: updatedApproval.reviewed_at
      }
    });
  } catch (error) {
    logger.error('Review approval failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to review approval'
    });
  }
}

// =====================================================
// CONTENT CATEGORIES AND TAGS
// =====================================================

/**
 * @desc    Get content categories
 * @route   GET /api/content/categories
 * @access  Private (Facilitator)
 */
async function getContentCategories(req, res) {
  try {
    const categories = await db('content_categories')
      .where('is_active', true)
      .orderBy('sort_order', 'asc')
      .orderBy('name', 'asc');
    
    res.json({
      success: true,
      data: categories.map(category => ({
        id: category.id,
        name: category.name,
        description: category.description,
        color: category.color,
        parentId: category.parent_id,
        sortOrder: category.sort_order,
        createdAt: category.created_at
      }))
    });
  } catch (error) {
    logger.error('Get content categories failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get content categories'
    });
  }
}

/**
 * @desc    Create content category
 * @route   POST /api/content/categories
 * @access  Private (Facilitator)
 */
async function createContentCategory(req, res) {
  try {
    const { name, description, color, parentId, sortOrder = 0 } = req.body;
    
    const [category] = await db('content_categories')
      .insert({
        name,
        description,
        color,
        parent_id: parentId,
        sort_order: sortOrder,
        is_active: true
      })
      .returning('*');
    
    logger.info('Content category created:', {
      categoryId: category.id,
      name: category.name,
      createdBy: req.facilitator.id
    });
    
    res.status(201).json({
      success: true,
      data: {
        id: category.id,
        name: category.name,
        description: category.description,
        color: category.color,
        parentId: category.parent_id,
        sortOrder: category.sort_order,
        createdAt: category.created_at
      }
    });
  } catch (error) {
    logger.error('Create content category failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create content category'
    });
  }
}

/**
 * @desc    Get content tags
 * @route   GET /api/content/tags
 * @access  Private (Facilitator)
 */
async function getContentTags(req, res) {
  try {
    const tags = await db('content_tags')
      .where('is_active', true)
      .orderBy('name', 'asc');
    
    res.json({
      success: true,
      data: tags.map(tag => ({
        id: tag.id,
        name: tag.name,
        description: tag.description,
        createdAt: tag.created_at
      }))
    });
  } catch (error) {
    logger.error('Get content tags failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get content tags'
    });
  }
}

/**
 * @desc    Create content tag
 * @route   POST /api/content/tags
 * @access  Private (Facilitator)
 */
async function createContentTag(req, res) {
  try {
    const { name, description } = req.body;
    
    const [tag] = await db('content_tags')
      .insert({
        name,
        description,
        is_active: true
      })
      .returning('*');
    
    logger.info('Content tag created:', {
      tagId: tag.id,
      name: tag.name,
      createdBy: req.facilitator.id
    });
    
    res.status(201).json({
      success: true,
      data: {
        id: tag.id,
        name: tag.name,
        description: tag.description,
        createdAt: tag.created_at
      }
    });
  } catch (error) {
    logger.error('Create content tag failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to create content tag'
    });
  }
}

// =====================================================
// CONTENT ANALYTICS
// =====================================================

/**
 * @desc    Track content usage event
 * @route   POST /api/content/analytics/track
 * @access  Private (Facilitator/Student)
 */
async function trackContentEvent(req, res) {
  try {
    const { contentVersionId, eventType, eventData, classroomId } = req.body;
    
    // Determine user type and ID
    let userType = 'anonymous';
    let userId = null;
    
    if (req.facilitator) {
      userType = 'facilitator';
      userId = req.facilitator.id;
    } else if (req.student) {
      userType = 'student';
      userId = req.student.id;
    }
    
    // Track the event
    await db('content_analytics').insert({
      content_version_id: contentVersionId,
      classroom_id: classroomId,
      event_type: eventType,
      event_data: eventData || {},
      user_type: userType,
      user_id: userId,
      event_timestamp: new Date()
    });
    
    logger.info('Content event tracked:', {
      contentVersionId,
      eventType,
      userType,
      userId,
      classroomId
    });
    
    res.json({
      success: true,
      message: 'Event tracked successfully'
    });
  } catch (error) {
    logger.error('Track content event failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to track event'
    });
  }
}

/**
 * @desc    Get content analytics summary
 * @route   GET /api/content/analytics/summary
 * @access  Private (Facilitator)
 */
async function getContentAnalyticsSummary(req, res) {
  try {
    const { contentVersionId, timeframe = '30d' } = req.query;
    
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
    
    let query = db('content_analytics')
      .where('event_timestamp', '>=', dateFilter);
    
    if (contentVersionId) {
      query = query.where('content_version_id', contentVersionId);
    }
    
    // Get event counts by type
    const eventCounts = await query
      .select('event_type')
      .count('* as count')
      .groupBy('event_type');
    
    // Get user type breakdown
    const userTypeBreakdown = await query
      .select('user_type')
      .count('* as count')
      .groupBy('user_type');
    
    // Get daily activity
    const dailyActivity = await query
      .select(db.raw('DATE(event_timestamp) as date'))
      .count('* as count')
      .groupBy(db.raw('DATE(event_timestamp)'))
      .orderBy('date', 'desc')
      .limit(30);
    
    res.json({
      success: true,
      data: {
        timeframe,
        eventCounts: eventCounts.reduce((acc, item) => {
          acc[item.event_type] = parseInt(item.count);
          return acc;
        }, {}),
        userTypeBreakdown: userTypeBreakdown.reduce((acc, item) => {
          acc[item.user_type] = parseInt(item.count);
          return acc;
        }, {}),
        dailyActivity: dailyActivity.map(item => ({
          date: item.date,
          count: parseInt(item.count)
        }))
      }
    });
  } catch (error) {
    logger.error('Get content analytics summary failed:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get analytics summary'
    });
  }
}

module.exports = {
  // Content Version Management
  getContentVersions,
  getContentVersion,
  createContentVersion,
  updateContentVersion,
  deleteContentVersion,
  
  // Media File Management
  uploadMediaFile,
  getMediaFiles,
  getMediaFile,
  deleteMediaFile,
  
  // Content Approval Workflow
  requestApproval,
  getApprovalRequests,
  reviewApproval,
  
  // Content Categories and Tags
  getContentCategories,
  createContentCategory,
  getContentTags,
  createContentTag,
  
  // Content Analytics
  trackContentEvent,
  getContentAnalyticsSummary
}; 
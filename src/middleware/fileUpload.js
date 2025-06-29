const multer = require('multer');
const path = require('path');
const crypto = require('crypto');
const fs = require('fs').promises;
const { fileTypeFromFile } = require('file-type');
const logger = require('../utils/logger');

// Configure storage
const storage = multer.diskStorage({
  destination: async (req, file, cb) => {
    try {
      // Create uploads directory if it doesn't exist
      const uploadDir = path.join(__dirname, '../uploads');
      await fs.mkdir(uploadDir, { recursive: true });
      
      // Create subdirectories by media type
      const mediaTypeDir = path.join(uploadDir, file.mimetype.split('/')[0]);
      await fs.mkdir(mediaTypeDir, { recursive: true });
      
      cb(null, mediaTypeDir);
    } catch (error) {
      logger.error('Error creating upload directory:', error);
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    // Generate unique filename with timestamp and random string
    const timestamp = Date.now();
    const randomString = crypto.randomBytes(8).toString('hex');
    const extension = path.extname(file.originalname);
    const filename = `${timestamp}_${randomString}${extension}`;
    cb(null, filename);
  }
});

// File filter function
const fileFilter = (req, file, cb) => {
  // Allowed file types
  const allowedMimeTypes = {
    // Images
    'image/jpeg': true,
    'image/png': true,
    'image/gif': true,
    'image/webp': true,
    'image/svg+xml': true,
    
    // Videos
    'video/mp4': true,
    'video/webm': true,
    'video/ogg': true,
    'video/quicktime': true,
    
    // Audio
    'audio/mpeg': true,
    'audio/wav': true,
    'audio/ogg': true,
    'audio/mp4': true,
    
    // Documents
    'application/pdf': true,
    'application/msword': true,
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': true,
    'text/plain': true,
    'text/markdown': true
  };
  
  // Check if file type is allowed
  if (allowedMimeTypes[file.mimetype]) {
    cb(null, true);
  } else {
    cb(new Error(`File type ${file.mimetype} is not allowed`), false);
  }
};

// Configure multer
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 100 * 1024 * 1024, // 100MB max file size
    files: 1 // Only allow 1 file per request
  }
});

// Middleware to handle file upload errors
const handleUploadError = (error, req, res, next) => {
  if (error instanceof multer.MulterError) {
    if (error.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({
        success: false,
        error: 'File size too large. Maximum size is 100MB.'
      });
    }
    if (error.code === 'LIMIT_FILE_COUNT') {
      return res.status(400).json({
        success: false,
        error: 'Too many files. Only one file allowed per request.'
      });
    }
    if (error.code === 'LIMIT_UNEXPECTED_FILE') {
      return res.status(400).json({
        success: false,
        error: 'Unexpected file field.'
      });
    }
  }
  
  if (error.message.includes('File type')) {
    return res.status(400).json({
      success: false,
      error: error.message
    });
  }
  
  logger.error('File upload error:', error);
  res.status(500).json({
    success: false,
    error: 'File upload failed'
  });
};

// Utility function to get file hash
const getFileHash = async (filePath) => {
  try {
    const fileBuffer = await fs.readFile(filePath);
    return crypto.createHash('sha256').update(fileBuffer).digest('hex');
  } catch (error) {
    logger.error('Error calculating file hash:', error);
    throw error;
  }
};

/**
 * Validate file using magic numbers (file signature)
 * @param {string} filePath - Path to the uploaded file
 * @param {string} expectedMimeType - Expected MIME type from multer
 * @returns {Promise<Object>} - Validation result with actual file type
 */
const validateFileContent = async (filePath, expectedMimeType) => {
  try {
    const detectedType = await fileTypeFromFile(filePath);
    
    // Mapping of allowed MIME types to their expected magic number types
    const allowedTypes = {
      'image/jpeg': ['jpg', 'jpeg'],
      'image/png': ['png'],
      'image/gif': ['gif'],
      'image/webp': ['webp'],
      'video/mp4': ['mp4'],
      'video/webm': ['webm'],
      'video/quicktime': ['mov'],
      'audio/mpeg': ['mp3'],
      'audio/wav': ['wav'],
      'audio/ogg': ['ogg'],
      'application/pdf': ['pdf']
    };

    // Check if we could detect the file type
    if (!detectedType) {
      // For text files and some documents, magic number detection may fail
      // Allow only specific text types without magic numbers
      const textTypes = ['text/plain', 'text/markdown'];
      if (textTypes.includes(expectedMimeType)) {
        return {
          isValid: true,
          detectedType: null,
          expectedType: expectedMimeType,
          message: 'Text file without magic number (allowed)'
        };
      }
      
      return {
        isValid: false,
        detectedType: null,
        expectedType: expectedMimeType,
        message: 'Could not detect file type - possible malicious file'
      };
    }

    // Check if detected type matches expected type
    const expectedExtensions = allowedTypes[expectedMimeType];
    if (!expectedExtensions) {
      return {
        isValid: false,
        detectedType: detectedType,
        expectedType: expectedMimeType,
        message: `MIME type ${expectedMimeType} not in allowed list`
      };
    }

    const isValidType = expectedExtensions.includes(detectedType.ext);
    
    return {
      isValid: isValidType,
      detectedType: detectedType,
      expectedType: expectedMimeType,
      message: isValidType ? 'File type validation successful' : 
        `File content (${detectedType.ext}) does not match expected type (${expectedMimeType})`
    };

  } catch (error) {
    logger.error('File content validation error:', error);
    return {
      isValid: false,
      detectedType: null,
      expectedType: expectedMimeType,
      message: 'File validation failed'
    };
  }
};

// Utility function to get file metadata
const getFileMetadata = async (filePath, mimeType) => {
  const stats = await fs.stat(filePath);
  
  // Validate file content using magic numbers
  const contentValidation = await validateFileContent(filePath, mimeType);
  
  const metadata = {
    fileSizeBytes: stats.size,
    createdAt: stats.birthtime,
    modifiedAt: stats.mtime,
    contentValidation: contentValidation
  };
  
  // Add media-specific metadata
  if (mimeType.startsWith('video/')) {
    metadata.mediaType = 'video';
  } else if (mimeType.startsWith('audio/')) {
    metadata.mediaType = 'audio';
  } else if (mimeType.startsWith('image/')) {
    metadata.mediaType = 'image';
  } else if (mimeType.startsWith('application/') || mimeType.startsWith('text/')) {
    metadata.mediaType = 'document';
  }
  
  return metadata;
};

// Utility function to clean up uploaded files
const cleanupFile = async (filePath) => {
  try {
    await fs.unlink(filePath);
    logger.info(`Cleaned up file: ${filePath}`);
  } catch (error) {
    logger.error(`Error cleaning up file ${filePath}:`, error);
  }
};

module.exports = {
  upload,
  handleUploadError,
  getFileHash,
  getFileMetadata,
  cleanupFile,
  validateFileContent
}; 
const express = require('express');
const router = express.Router();
const { authenticateFacilitator } = require('../middleware/auth');

// Test endpoint for JWT validation
router.get('/jwt-test', authenticateFacilitator, (req, res) => {
  res.json({
    success: true,
    message: 'JWT validation successful',
    facilitator: req.facilitator
  });
});

// Test endpoint for input validation
router.post('/input-test', (req, res) => {
  const { testInput } = req.body;
  res.json({
    success: true,
    message: 'Input received',
    sanitizedInput: testInput,
    originalLength: testInput ? testInput.length : 0
  });
});

module.exports = router; 
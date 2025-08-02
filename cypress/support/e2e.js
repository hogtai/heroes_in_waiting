// Heroes in Waiting Analytics Dashboard - Cypress Support Commands
// COPPA Compliant Testing Framework for Educational Analytics

import './commands';
import './analytics-commands';
import './coppa-commands';

// Global test configuration
Cypress.on('uncaught:exception', (err, runnable) => {
  // Ignore specific errors that don't affect test functionality
  if (err.message.includes('ResizeObserver')) {
    return false;
  }
  return true;
});

// Before each test - setup analytics monitoring
beforeEach(() => {
  // Enable analytics tracking for tests
  cy.window().then((win) => {
    win.HEROES_ANALYTICS_TEST_MODE = true;
    win.COPPA_COMPLIANCE_VALIDATION = true;
  });
  
  // Intercept and validate all API calls for COPPA compliance
  cy.intercept('POST', '**/api/analytics/**', (req) => {
    cy.task('validateCOPPACompliance', req.body).then((result) => {
      if (!result.valid) {
        throw new Error(`COPPA Violation Detected: ${result.violation}`);
      }
    });
  }).as('analyticsRequest');
  
  // Mock successful authentication
  cy.intercept('POST', '**/api/auth/facilitator/login', {
    statusCode: 200,
    body: {
      success: true,
      token: 'mock-jwt-token-for-testing',
      facilitator: {
        id: 'facilitator-123',
        email: 'test.facilitator@example.com',
        name: 'Test Facilitator',
        role: 'facilitator'
      }
    }
  }).as('loginRequest');
});

// After each test - cleanup and validation
afterEach(() => {
  // Validate no PII was exposed during test
  cy.window().then((win) => {
    if (win.COPPA_VIOLATIONS && win.COPPA_VIOLATIONS.length > 0) {
      throw new Error(`COPPA Violations detected: ${JSON.stringify(win.COPPA_VIOLATIONS)}`);
    }
  });
  
  // Performance validation
  cy.window().its('performance').then((performance) => {
    const navigationEntry = performance.getEntriesByType('navigation')[0];
    if (navigationEntry && navigationEntry.loadEventEnd > 5000) {
      cy.log(`Warning: Page load took ${navigationEntry.loadEventEnd}ms (target: <5000ms)`);
    }
  });
});
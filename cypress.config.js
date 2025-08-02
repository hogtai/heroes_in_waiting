const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3001', // Web dashboard URL
    viewportWidth: 1280,
    viewportHeight: 720,
    video: true,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    
    // Test file patterns
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.js',
    fixturesFolder: 'cypress/fixtures',
    
    // Heroes in Waiting specific configuration
    env: {
      apiUrl: 'http://localhost:3000/api',
      testFacilitatorEmail: 'test.facilitator@example.com',
      testFacilitatorPassword: 'TestPassword123!',
      coppaComplianceEnabled: true,
      analyticsDataSyncTimeout: 30000
    },
    
    setupNodeEvents(on, config) {
      // COPPA compliance testing plugin
      on('task', {
        validateCOPPACompliance(data) {
          // Validate no PII is present in analytics data
          const piiPatterns = [
            /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/, // Email
            /\b\d{3}-\d{3}-\d{4}\b/, // Phone
            /\b\d{3}-\d{2}-\d{4}\b/, // SSN
            /\b[A-Z][a-z]+ [A-Z][a-z]+\b/ // Full names
          ];
          
          const dataString = JSON.stringify(data);
          for (const pattern of piiPatterns) {
            if (pattern.test(dataString)) {
              return { valid: false, violation: pattern.toString() };
            }
          }
          return { valid: true };
        },
        
        generateMockAnalyticsData() {
          return {
            behavioral_analytics: {
              empathy_score: Math.floor(Math.random() * 5) + 1,
              confidence_level: Math.floor(Math.random() * 5) + 1,
              communication_quality: Math.floor(Math.random() * 5) + 1,
              leadership_indicator: Math.floor(Math.random() * 5) + 1,
              anonymous_student_id: require('crypto').createHash('sha256').update(`student_${Date.now()}`).digest('hex'),
              timestamp: new Date().toISOString(),
              lesson_id: 'lesson_' + Math.floor(Math.random() * 12) + 1
            }
          };
        }
      });
      
      // Performance monitoring plugin
      on('before:browser:launch', (browser = {}, launchOptions) => {
        if (browser.name === 'chrome') {
          launchOptions.args.push('--disable-web-security');
          launchOptions.args.push('--disable-features=VizDisplayCompositor');
        }
        return launchOptions;
      });
      
      return config;
    },
  },
  
  component: {
    devServer: {
      framework: 'react',
      bundler: 'webpack',
    },
  },
});
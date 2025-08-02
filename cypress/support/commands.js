// Heroes in Waiting - Custom Cypress Commands
// Educational Analytics Dashboard Testing Framework

// Authentication Commands
Cypress.Commands.add('loginAsFacilitator', (email = 'test.facilitator@example.com', password = 'TestPassword123!') => {
  cy.visit('/login');
  cy.get('[data-cy=email-input]').type(email);
  cy.get('[data-cy=password-input]').type(password);
  cy.get('[data-cy=login-button]').click();
  cy.wait('@loginRequest');
  cy.url().should('include', '/dashboard');
  cy.get('[data-cy=facilitator-welcome]').should('be.visible');
});

// Analytics Dashboard Navigation
Cypress.Commands.add('navigateToAnalytics', () => {
  cy.get('[data-cy=nav-analytics]').click();
  cy.url().should('include', '/analytics');
  cy.get('[data-cy=analytics-dashboard]').should('be.visible');
});

Cypress.Commands.add('selectClassroom', (classroomId) => {
  cy.get('[data-cy=classroom-selector]').click();
  cy.get(`[data-cy=classroom-option-${classroomId}]`).click();
  cy.get('[data-cy=classroom-analytics]').should('be.visible');
});

// Chart and Visualization Commands
Cypress.Commands.add('waitForChartLoad', (chartSelector, timeout = 10000) => {
  cy.get(chartSelector, { timeout }).should('be.visible');
  cy.get(chartSelector).find('canvas').should('exist');
  
  // Wait for Chart.js to fully render
  cy.window().then((win) => {
    return new Cypress.Promise((resolve) => {
      const checkChart = () => {
        const canvas = win.document.querySelector(`${chartSelector} canvas`);
        if (canvas && canvas.getContext('2d')) {
          resolve();
        } else {
          setTimeout(checkChart, 100);
        }
      };
      checkChart();
    });
  });
});

Cypress.Commands.add('validateChartData', (chartSelector, expectedDataPoints) => {
  cy.get(chartSelector).within(() => {
    cy.get('canvas').should('exist');
    
    // Validate chart has expected number of data points
    cy.window().then((win) => {
      const charts = win.Chart.instances;
      const chart = Object.values(charts).find(c => 
        c.canvas.closest(chartSelector)
      );
      
      if (chart && chart.data && chart.data.datasets) {
        const totalDataPoints = chart.data.datasets.reduce((sum, dataset) => 
          sum + (dataset.data ? dataset.data.length : 0), 0
        );
        expect(totalDataPoints).to.be.at.least(expectedDataPoints);
      }
    });
  });
});

// Real-time Data Testing
Cypress.Commands.add('simulateRealTimeUpdate', (updateData) => {
  cy.window().then((win) => {
    // Simulate WebSocket or EventSource update
    if (win.heroesAnalyticsEventSource) {
      const event = new MessageEvent('message', {
        data: JSON.stringify(updateData)
      });
      win.heroesAnalyticsEventSource.dispatchEvent(event);
    } else if (win.heroesAnalyticsWebSocket) {
      win.heroesAnalyticsWebSocket.onmessage({
        data: JSON.stringify(updateData)
      });
    }
  });
});

// Time Range Selection
Cypress.Commands.add('selectTimeRange', (range) => {
  cy.get('[data-cy=time-range-selector]').click();
  cy.get(`[data-cy=time-range-${range}]`).click();
  cy.get('[data-cy=analytics-loading]').should('not.exist');
});

// Export and Reporting
Cypress.Commands.add('exportAnalyticsData', (format = 'csv') => {
  cy.get('[data-cy=export-button]').click();
  cy.get(`[data-cy=export-format-${format}]`).click();
  cy.get('[data-cy=export-confirm]').click();
  
  // Validate download started (file download testing)
  cy.readFile(`cypress/downloads/analytics-export.${format}`, { timeout: 10000 })
    .should('exist');
});

// Responsive Design Testing
Cypress.Commands.add('testResponsiveDesign', (breakpoints = ['mobile', 'tablet', 'desktop']) => {
  const viewports = {
    mobile: { width: 375, height: 667 },
    tablet: { width: 768, height: 1024 },
    desktop: { width: 1280, height: 720 }
  };
  
  breakpoints.forEach(breakpoint => {
    const viewport = viewports[breakpoint];
    cy.viewport(viewport.width, viewport.height);
    cy.get('[data-cy=analytics-dashboard]').should('be.visible');
    
    if (breakpoint === 'mobile') {
      cy.get('[data-cy=mobile-nav-toggle]').should('be.visible');
    } else {
      cy.get('[data-cy=desktop-nav]').should('be.visible');
    }
  });
});

// Performance Testing Commands
Cypress.Commands.add('measurePagePerformance', () => {
  cy.window().its('performance').then((performance) => {
    const navigationEntry = performance.getEntriesByType('navigation')[0];
    const paintEntries = performance.getEntriesByType('paint');
    
    const metrics = {
      loadTime: navigationEntry.loadEventEnd - navigationEntry.loadEventStart,
      domContentLoaded: navigationEntry.domContentLoadedEventEnd - navigationEntry.domContentLoadedEventStart,
      firstPaint: paintEntries.find(entry => entry.name === 'first-paint')?.startTime || 0,
      firstContentfulPaint: paintEntries.find(entry => entry.name === 'first-contentful-paint')?.startTime || 0
    };
    
    // Performance assertions
    expect(metrics.loadTime).to.be.lessThan(5000); // 5 second load time
    expect(metrics.firstContentfulPaint).to.be.lessThan(2000); // 2 second FCP
    
    cy.log('Performance Metrics:', metrics);
    return cy.wrap(metrics);
  });
});

// Error Handling Testing
Cypress.Commands.add('simulateNetworkError', () => {
  cy.intercept('GET', '**/api/analytics/**', { statusCode: 500 }).as('networkError');
  cy.reload();
  cy.wait('@networkError');
  cy.get('[data-cy=error-message]').should('be.visible');
  cy.get('[data-cy=retry-button]').should('be.visible');
});

Cypress.Commands.add('simulateSlowNetwork', (delay = 3000) => {
  cy.intercept('GET', '**/api/analytics/**', (req) => {
    req.reply((res) => {
      return new Promise((resolve) => {
        setTimeout(() => resolve(res), delay);
      });
    });
  }).as('slowNetwork');
});

// Accessibility Testing
Cypress.Commands.add('checkAccessibility', () => {
  cy.injectAxe();
  cy.checkA11y('[data-cy=analytics-dashboard]', {
    rules: {
      'color-contrast': { enabled: true },
      'keyboard-navigation': { enabled: true },
      'focus-management': { enabled: true }
    }
  });
});
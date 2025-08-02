// Heroes in Waiting - Analytics Dashboard Core Testing
// Educational Behavioral Analytics - Chart.js Integration Testing

describe('Analytics Dashboard Core Functionality', () => {
  beforeEach(() => {
    cy.loginAsFacilitator();
    cy.navigateToAnalytics();
  });

  describe('Dashboard Layout and Navigation', () => {
    it('should display analytics dashboard with all core components', () => {
      cy.get('[data-cy=analytics-dashboard]').should('be.visible');
      cy.get('[data-cy=dashboard-header]').should('be.visible');
      cy.get('[data-cy=metrics-overview]').should('be.visible');
      cy.get('[data-cy=charts-section]').should('be.visible');
      cy.get('[data-cy=classroom-selector]').should('be.visible');
      cy.get('[data-cy=time-range-selector]').should('be.visible');
    });

    it('should navigate between different analytics views', () => {
      // Test navigation between analytics sections
      cy.get('[data-cy=nav-behavioral-analytics]').click();
      cy.url().should('include', '/analytics/behavioral');
      cy.get('[data-cy=behavioral-analytics-view]').should('be.visible');

      cy.get('[data-cy=nav-lesson-analytics]').click();
      cy.url().should('include', '/analytics/lessons');
      cy.get('[data-cy=lesson-analytics-view]').should('be.visible');

      cy.get('[data-cy=nav-classroom-overview]').click();
      cy.url().should('include', '/analytics/classroom');
      cy.get('[data-cy=classroom-overview-view]').should('be.visible');
    });

    it('should display COPPA compliance indicators throughout', () => {
      cy.validateCOPPACompliance();
      
      // Validate specific dashboard compliance elements
      cy.get('[data-cy=anonymous-data-badge]').should('be.visible');
      cy.get('[data-cy=educational-purpose-notice]').should('be.visible');
      cy.get('[data-cy=data-privacy-controls]').should('be.visible');
    });
  });

  describe('Behavioral Analytics Metrics', () => {
    it('should display all four core behavioral metrics', () => {
      cy.validateBehavioralMetrics();
      
      // Validate metric cards display correctly
      ['empathy', 'confidence', 'communication', 'leadership'].forEach(metric => {
        cy.get(`[data-cy=metric-${metric}-card]`).within(() => {
          cy.get('[data-cy=metric-title]').should('be.visible');
          cy.get('[data-cy=metric-score]').should('be.visible');
          cy.get('[data-cy=metric-trend]').should('be.visible');
          cy.get('[data-cy=metric-description]').should('be.visible');
        });
      });
    });

    it('should validate metric score ranges (1-5)', () => {
      ['empathy', 'confidence', 'communication', 'leadership'].forEach(metric => {
        cy.get(`[data-cy=metric-${metric}-score]`).invoke('text').then((scoreText) => {
          const score = parseFloat(scoreText);
          expect(score).to.be.at.least(1);
          expect(score).to.be.at.most(5);
        });
      });
    });

    it('should display behavioral trends over time', () => {
      cy.get('[data-cy=behavioral-trends-section]').should('be.visible');
      
      // Validate trend charts for each metric
      ['empathy', 'confidence', 'communication', 'leadership'].forEach(metric => {
        cy.waitForChartLoad(`[data-cy=${metric}-trend-chart]`);
        cy.validateChartData(`[data-cy=${metric}-trend-chart]`, 5);
      });
    });
  });

  describe('Chart.js Integration', () => {
    it('should load and display behavioral analytics charts', () => {
      // Validate main behavioral analytics chart
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      cy.get('[data-cy=behavioral-analytics-chart] canvas').should('be.visible');
      
      // Validate chart has data points
      cy.validateChartData('[data-cy=behavioral-analytics-chart]', 10);
    });

    it('should display lesson effectiveness charts', () => {
      cy.get('[data-cy=nav-lesson-analytics]').click();
      
      // Wait for lesson charts to load
      cy.waitForChartLoad('[data-cy=lesson-completion-chart]');
      cy.waitForChartLoad('[data-cy=lesson-engagement-chart]');
      cy.waitForChartLoad('[data-cy=learning-objectives-chart]');
      
      // Validate chart data
      cy.validateChartData('[data-cy=lesson-completion-chart]', 12); // 12 lessons
      cy.validateChartData('[data-cy=lesson-engagement-chart]', 12);
    });

    it('should support chart interactions (hover, click, zoom)', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Test hover interactions
      cy.get('[data-cy=behavioral-analytics-chart] canvas')
        .trigger('mousemove', { offsetX: 100, offsetY: 100 });
      
      cy.get('[data-cy=chart-tooltip]').should('be.visible');
      cy.get('[data-cy=chart-tooltip]').should('contain', /empathy|confidence|communication|leadership/i);
      
      // Test click interactions for drill-down
      cy.get('[data-cy=behavioral-analytics-chart] canvas')
        .click(100, 100);
      
      cy.get('[data-cy=chart-drill-down-modal]').should('be.visible');
    });

    it('should update charts when time range changes', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Change time range
      cy.selectTimeRange('last-week');
      
      // Validate chart updates
      cy.get('[data-cy=analytics-loading]').should('be.visible');
      cy.get('[data-cy=analytics-loading]').should('not.exist');
      cy.get('[data-cy=chart-updated-indicator]').should('be.visible');
    });
  });

  describe('Real-time Data Updates', () => {
    it('should handle real-time analytics updates', () => {
      cy.validateRealTimeUpdates();
      
      // Validate real-time indicator is active
      cy.get('[data-cy=real-time-status]').should('contain', 'Live');
      cy.get('[data-cy=last-update-timestamp]').should('be.visible');
    });

    it('should update charts in real-time', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Generate and send real-time update
      cy.task('generateMockAnalyticsData').then((mockData) => {
        cy.simulateRealTimeUpdate(mockData);
        
        // Validate chart updates
        cy.get('[data-cy=chart-update-animation]').should('be.visible');
        cy.get('[data-cy=new-data-indicator]').should('be.visible');
      });
    });

    it('should handle real-time connection failures gracefully', () => {
      // Simulate connection failure
      cy.window().then((win) => {
        if (win.heroesAnalyticsEventSource) {
          win.heroesAnalyticsEventSource.close();
        }
      });
      
      // Validate fallback behavior
      cy.get('[data-cy=real-time-connection-error]').should('be.visible');
      cy.get('[data-cy=polling-fallback-indicator]').should('be.visible');
      cy.get('[data-cy=reconnect-button]').should('be.visible');
    });
  });

  describe('Classroom Analytics', () => {
    it('should display classroom selection and overview', () => {
      const classroomId = 'classroom-123';
      cy.validateClassroomOverview(classroomId);
      
      // Validate classroom-specific metrics
      cy.get('[data-cy=classroom-metrics]').within(() => {
        cy.get('[data-cy=active-students]').should('be.visible');
        cy.get('[data-cy=lessons-completed]').should('be.visible');
        cy.get('[data-cy=overall-progress]').should('be.visible');
        cy.get('[data-cy=behavioral-improvement]').should('be.visible');
      });
    });

    it('should show classroom behavioral trends', () => {
      cy.selectClassroom('classroom-123');
      cy.validateClassroomTrends();
      
      // Validate class average displays
      ['empathy', 'confidence', 'communication', 'leadership'].forEach(metric => {
        cy.get(`[data-cy=${metric}-class-average]`).should('be.visible');
        cy.get(`[data-cy=${metric}-class-average]`).should('contain.text', /[1-5]\.\d/);
      });
    });

    it('should compare multiple classrooms', () => {
      cy.get('[data-cy=classroom-comparison-mode]').click();
      cy.get('[data-cy=classroom-selector]').select(['classroom-123', 'classroom-456']);
      
      // Validate comparison charts
      cy.waitForChartLoad('[data-cy=classroom-comparison-chart]');
      cy.get('[data-cy=comparison-legend]').should('be.visible');
      cy.get('[data-cy=comparison-metrics-table]').should('be.visible');
    });
  });

  describe('Performance and Loading', () => {
    it('should load analytics dashboard within performance targets', () => {
      cy.measurePagePerformance().then((metrics) => {
        expect(metrics.loadTime).to.be.lessThan(5000); // 5 seconds
        expect(metrics.firstContentfulPaint).to.be.lessThan(2000); // 2 seconds
      });
    });

    it('should handle large datasets efficiently', () => {
      // Simulate large dataset load
      cy.selectTimeRange('last-year');
      
      cy.validateAnalyticsPerformance();
      
      // Validate pagination or virtualization for large datasets
      cy.get('[data-cy=analytics-pagination]').should('be.visible');
      cy.get('[data-cy=data-virtualization-indicator]').should('exist');
    });

    it('should implement efficient chart rendering', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Test chart rendering performance
      cy.window().then((win) => {
        const canvas = win.document.querySelector('[data-cy=behavioral-analytics-chart] canvas');
        const ctx = canvas.getContext('2d');
        
        // Validate canvas is properly rendered
        expect(canvas.width).to.be.greaterThan(0);
        expect(canvas.height).to.be.greaterThan(0);
        expect(ctx).to.exist;
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle API failures gracefully', () => {
      cy.validateAnalyticsErrorHandling();
      
      // Validate error state UI
      cy.get('[data-cy=error-state-illustration]').should('be.visible');
      cy.get('[data-cy=error-recovery-options]').should('be.visible');
    });

    it('should handle network timeouts', () => {
      cy.simulateSlowNetwork(10000); // 10 second delay
      
      cy.get('[data-cy=refresh-analytics]').click();
      
      // Validate timeout handling
      cy.get('[data-cy=loading-timeout-warning]').should('be.visible');
      cy.get('[data-cy=cancel-request-button]').should('be.visible');
    });

    it('should provide meaningful error messages', () => {
      cy.simulateNetworkError();
      
      cy.get('[data-cy=analytics-error-message]').should('be.visible');
      cy.get('[data-cy=analytics-error-message]').should('not.contain', 'undefined');
      cy.get('[data-cy=analytics-error-message]').should('not.contain', '[object Object]');
      cy.get('[data-cy=error-help-link]').should('be.visible');
    });
  });

  describe('Accessibility', () => {
    it('should pass accessibility audit', () => {
      cy.checkAccessibility();
    });

    it('should support keyboard navigation for charts', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Test keyboard navigation
      cy.get('[data-cy=behavioral-analytics-chart]').focus();
      cy.get('[data-cy=behavioral-analytics-chart]').type('{rightarrow}');
      
      // Validate focus management
      cy.get('[data-cy=chart-focus-indicator]').should('be.visible');
    });

    it('should provide screen reader support for charts', () => {
      cy.waitForChartLoad('[data-cy=behavioral-analytics-chart]');
      
      // Validate ARIA labels and descriptions
      cy.get('[data-cy=behavioral-analytics-chart]').should('have.attr', 'aria-label');
      cy.get('[data-cy=chart-data-table]').should('be.visible'); // Alternative data representation
    });
  });
});
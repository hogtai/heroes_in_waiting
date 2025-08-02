// Heroes in Waiting - Authentication Flow Testing
// Educational Analytics Dashboard - COPPA Compliant Testing

describe('Analytics Dashboard Authentication', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  describe('Facilitator Login Flow', () => {
    it('should display login form with proper COPPA notices', () => {
      cy.visit('/login');
      
      // Validate login form elements
      cy.get('[data-cy=email-input]').should('be.visible');
      cy.get('[data-cy=password-input]').should('be.visible');
      cy.get('[data-cy=login-button]').should('be.visible');
      
      // Validate COPPA compliance notices
      cy.get('[data-cy=coppa-compliance-notice]').should('be.visible');
      cy.get('[data-cy=coppa-compliance-notice]').should('contain', 'educational purposes only');
      cy.get('[data-cy=data-protection-notice]').should('be.visible');
    });

    it('should successfully authenticate facilitator with valid credentials', () => {
      cy.loginAsFacilitator();
      
      // Validate successful authentication
      cy.url().should('include', '/dashboard');
      cy.get('[data-cy=facilitator-welcome]').should('be.visible');
      cy.get('[data-cy=logout-button]').should('be.visible');
      
      // Validate JWT token is stored securely
      cy.window().then((win) => {
        const token = win.localStorage.getItem('auth_token');
        expect(token).to.exist;
        expect(token).to.match(/^[\w-]+\.[\w-]+\.[\w-]+$/); // JWT format
      });
    });

    it('should handle invalid credentials gracefully', () => {
      cy.visit('/login');
      cy.get('[data-cy=email-input]').type('invalid@example.com');
      cy.get('[data-cy=password-input]').type('wrongpassword');
      cy.get('[data-cy=login-button]').click();
      
      // Validate error handling
      cy.get('[data-cy=login-error]').should('be.visible');
      cy.get('[data-cy=login-error]').should('contain', 'Invalid credentials');
      cy.url().should('include', '/login');
    });

    it('should validate email format', () => {
      cy.visit('/login');
      cy.get('[data-cy=email-input]').type('invalid-email');
      cy.get('[data-cy=password-input]').type('password123');
      cy.get('[data-cy=login-button]').click();
      
      // Validate email format validation
      cy.get('[data-cy=email-validation-error]').should('be.visible');
      cy.get('[data-cy=email-validation-error]').should('contain', 'valid email');
    });

    it('should require password', () => {
      cy.visit('/login');
      cy.get('[data-cy=email-input]').type('test@example.com');
      cy.get('[data-cy=login-button]').click();
      
      // Validate password requirement
      cy.get('[data-cy=password-validation-error]').should('be.visible');
      cy.get('[data-cy=password-validation-error]').should('contain', 'required');
    });
  });

  describe('Session Management', () => {
    beforeEach(() => {
      cy.loginAsFacilitator();
    });

    it('should maintain session across page refreshes', () => {
      cy.reload();
      
      // Should remain authenticated
      cy.get('[data-cy=facilitator-welcome]').should('be.visible');
      cy.url().should('include', '/dashboard');
    });

    it('should handle session expiration gracefully', () => {
      // Simulate expired token
      cy.window().then((win) => {
        win.localStorage.setItem('auth_token', 'expired.jwt.token');
      });
      
      cy.reload();
      
      // Should redirect to login
      cy.url().should('include', '/login');
      cy.get('[data-cy=session-expired-notice]').should('be.visible');
    });

    it('should successfully logout and clear session', () => {
      cy.get('[data-cy=logout-button]').click();
      
      // Validate logout
      cy.url().should('include', '/login');
      cy.get('[data-cy=logout-success-message]').should('be.visible');
      
      // Validate session is cleared
      cy.window().then((win) => {
        const token = win.localStorage.getItem('auth_token');
        expect(token).to.be.null;
      });
    });
  });

  describe('Security Features', () => {
    it('should implement CSRF protection', () => {
      cy.request({
        method: 'POST',
        url: '/api/auth/facilitator/login',
        failOnStatusCode: false,
        body: {
          email: 'test@example.com',
          password: 'password123'
        }
      }).then((response) => {
        // Should require CSRF token or return 403
        expect([403, 422]).to.include(response.status);
      });
    });

    it('should implement rate limiting on login attempts', () => {
      const attempts = Array.from({ length: 6 }, (_, i) => i);
      
      cy.wrap(attempts).each(() => {
        cy.request({
          method: 'POST',
          url: '/api/auth/facilitator/login',
          failOnStatusCode: false,
          body: {
            email: 'test@example.com',
            password: 'wrongpassword'
          }
        });
      });
      
      // After multiple failed attempts, should be rate limited
      cy.request({
        method: 'POST',
        url: '/api/auth/facilitator/login',
        failOnStatusCode: false,
        body: {
          email: 'test@example.com',
          password: 'wrongpassword'
        }
      }).then((response) => {
        expect(response.status).to.equal(429); // Too Many Requests
      });
    });

    it('should use secure cookies for session management', () => {
      cy.loginAsFacilitator();
      
      cy.getCookies().then((cookies) => {
        const sessionCookie = cookies.find(cookie => 
          cookie.name.includes('session') || cookie.name.includes('auth')
        );
        
        if (sessionCookie) {
          expect(sessionCookie.secure).to.be.true;
          expect(sessionCookie.httpOnly).to.be.true;
          expect(sessionCookie.sameSite).to.equal('strict');
        }
      });
    });
  });

  describe('Accessibility', () => {
    it('should be accessible with keyboard navigation', () => {
      cy.visit('/login');
      
      // Test keyboard navigation
      cy.get('body').tab();
      cy.focused().should('have.attr', 'data-cy', 'email-input');
      cy.focused().tab();
      cy.focused().should('have.attr', 'data-cy', 'password-input');
      cy.focused().tab();
      cy.focused().should('have.attr', 'data-cy', 'login-button');
    });

    it('should have proper ARIA labels and descriptions', () => {
      cy.visit('/login');
      
      cy.get('[data-cy=email-input]').should('have.attr', 'aria-label');
      cy.get('[data-cy=password-input]').should('have.attr', 'aria-label');
      cy.get('[data-cy=login-button]').should('have.attr', 'aria-describedby');
    });

    it('should pass accessibility audit', () => {
      cy.visit('/login');
      cy.checkAccessibility();
    });
  });

  describe('Responsive Design', () => {
    it('should work properly on different screen sizes', () => {
      cy.visit('/login');
      cy.testResponsiveDesign(['mobile', 'tablet', 'desktop']);
      
      // Validate login form is accessible on all breakpoints
      ['mobile', 'tablet', 'desktop'].forEach(breakpoint => {
        const viewports = {
          mobile: { width: 375, height: 667 },
          tablet: { width: 768, height: 1024 },
          desktop: { width: 1280, height: 720 }
        };
        
        const viewport = viewports[breakpoint];
        cy.viewport(viewport.width, viewport.height);
        cy.get('[data-cy=login-form]').should('be.visible');
        cy.get('[data-cy=email-input]').should('be.visible');
        cy.get('[data-cy=password-input]').should('be.visible');
        cy.get('[data-cy=login-button]').should('be.visible');
      });
    });
  });
});
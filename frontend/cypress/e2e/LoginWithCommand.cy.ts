describe('Login with Custom Command', () => {
  beforeEach(() => {
    cy.visit('http://localhost:4200/');
  });

  it('should login successfully using custom command', () => {
    // Use the custom login command
    cy.loginWithKeycloak('testuser', 'testpassword');

    // Verify we're back on the main application
    cy.url().should('include', 'localhost:4200');
    cy.url().should('not.include', 'keycloak');

    // Verify logged-in state
    cy.contains('button', /logout|sign out|my account/i).should('be.visible');
  });

  it('should be able to access protected pages after login', () => {
    cy.loginWithKeycloak('testuser', 'testpassword');

    // Navigate to a protected page (adjust the URL based on your application)
    cy.visit('http://localhost:4200/dashboard');

    // Verify page loaded (should not be redirected to login)
    cy.url().should('include', 'localhost:4200/dashboard');
  });

  it('should display user information when logged in', () => {
    cy.loginWithKeycloak('testuser', 'testpassword');

    // Look for user profile or account menu
    cy.contains('button', /profile|account|user|testuser/i).should('exist');
  });
});

/// <reference types="cypress" />
// ***********************************************
// Custom commands for testing
// ***********************************************

/**
 * Custom command to login to the application using Keycloak
 * @param username - The username for login
 * @param password - The password for login
 */
Cypress.Commands.add('loginWithKeycloak', (username: string, password: string) => {
  // Click on login button
  cy.contains('button', /login|sign in/i, { timeout: 5000 }).click({ force: true });

  // Wait for Keycloak page to load
  cy.url({ timeout: 10000 }).should('include', 'keycloak');

  // Enter username
  cy.get('input[id="username"]', { timeout: 10000 }).clear().type(username);

  // Enter password
  cy.get('input[id="password"]').clear().type(password);

  // Submit login form
  cy.contains('button', /sign in|login/i).click();

  // Wait for redirect back to application
  cy.url({ timeout: 15000 }).should('include', 'localhost:4200');
  cy.url().should('not.include', 'keycloak');

  // Verify user is authenticated
  cy.contains('button', /logout|sign out|my account/i, { timeout: 5000 }).should('exist');
});

declare global {
  namespace Cypress {
    interface Chainable {
      loginWithKeycloak(username: string, password: string): Chainable<void>
    }
  }
}
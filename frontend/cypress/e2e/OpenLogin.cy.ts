describe('template spec', () => {
  it('passes', () => {
    cy.visit('http://localhost:4200/')
    cy.get('img.not-found__image').click();
    cy.get('button.mat-accent span.mdc-button__label').click();
  })
})
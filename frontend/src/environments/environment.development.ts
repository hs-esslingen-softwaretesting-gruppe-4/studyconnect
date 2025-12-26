export const environment = {
  production: false,
  apiUrl: 'http://localhost:8088/api',
  registerUserEndpoint: 'http://localhost:8088/api/users',

  // Keycloak configuration
  identityProviderUrl: 'https://keycloakswt.duckdns.org',
  requiredRole: 'studyconnect',
  realmName: 'studyconnect-dev',
  clientID: 'studyconnect-frontend-dev',
};

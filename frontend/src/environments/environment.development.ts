export const environment = {
  production: true,
  debugAuth: true,
  apiBasePath: 'http://localhost:8088',
  apiUrl: 'http://localhost:8088/api',
  registerUserEndpoint: 'http://localhost:8088/api/users',

  // Keycloak configuration
  identityProviderUrl: 'https://keycloakswt.duckdns.org',
  requiredRole: 'studyconnect',
  realmName: 'studyconnect',
  clientID: 'studyconnect-frontend',
};

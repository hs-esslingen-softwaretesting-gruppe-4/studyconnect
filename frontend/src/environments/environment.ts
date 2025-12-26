const realmName = 'studyconnect';

export const environment = {
  production: true,
  apiUrl: 'http://localhost:8088/api',
  registerUserEndpoint: 'http://localhost:8088/api/users',

  // Keycloak configuration ,
  requiredRole: 'studyconnect',
  realmName: realmName,
  clientID: 'studyconnect-frontend',
  identityProviderUrl: `https://keycloakswt.duckdns.org`,
};

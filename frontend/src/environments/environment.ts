const realmName = 'studyconnect';

export const environment = {
  production: true,
  debugAuth: false,
  apiBasePath: '',
  apiUrl: '/api',
  registerUserEndpoint: '/api/users',

  // Keycloak configuration ,
  requiredRole: 'studyconnect',
  realmName: realmName,
  clientID: 'studyconnect-frontend',
  identityProviderUrl: `https://keycloakswt.duckdns.org`,
};

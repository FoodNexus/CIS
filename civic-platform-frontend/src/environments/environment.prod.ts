/** Production / Docker: nginx proxies /api → Spring Boot (same origin). */
export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'foodnexus',
    issuer: 'http://localhost:8180/realms/foodnexus',
    clientId: 'cis-front',
    responseType: 'code',
    scope: 'openid profile email'
  },
  giphyApiKey: ''
};

function resolveApiBaseUrl(): string {
  try {
    if (typeof localStorage !== 'undefined') {
      const o = localStorage.getItem('api_base_url');
      if (o?.trim()) {
        return o.trim().replace(/\/$/, '');
      }
    }
  } catch {
    /* ignore */
  }
  return 'http://localhost:8081/api';
}

export const environment = {
  production: false,
  /** Spring Boot with `server.servlet.context-path=/api`. Getter so `api_base_url` in localStorage applies app-wide after reload. */
  get apiUrl(): string {
    return resolveApiBaseUrl();
  },
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'foodnexus',
    issuer: 'http://localhost:8180/realms/foodnexus',
    clientId: 'cis-front',
    responseType: 'code',
    scope: 'openid profile email'
  },
  /** Optional: https://developers.giphy.com/dashboard/ — enables GIF search in post/comment composer */
  giphyApiKey: ''
};

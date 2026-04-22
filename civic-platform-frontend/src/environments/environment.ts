function resolveApiBaseUrl(): string {
  const fallback = 'http://localhost:8081/api';
  try {
    if (typeof localStorage !== 'undefined') {
      const o = localStorage.getItem('api_base_url');
      if (o?.trim()) {
        const normalized = o.trim().replace(/\/$/, '');
        // Auto-migrate the old local dev URL to the current backend port.
        if (/^http:\/\/localhost:8082\/api$/i.test(normalized)) {
          localStorage.setItem('api_base_url', fallback);
          return fallback;
        }
        // Keep only valid API base URLs ending with /api.
        if (/^https?:\/\/[^/\s]+\/api$/i.test(normalized)) {
          return normalized;
        }
      }
    }
  } catch {
    /* ignore */
  }
  return fallback;
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

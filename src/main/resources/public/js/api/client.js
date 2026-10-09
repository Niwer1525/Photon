import { state } from '../state.js';

export async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) headers.set('Authorization', `Bearer ${state.token}`);
    if (state.userToken) headers.set('X-Photon-User-Token', state.userToken);

    const method = (options.method || 'GET').toUpperCase();
    if (path.startsWith('/api/admin') && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const cookie = document.cookie
            .split(';')
            .map((c) => c.trim())
            .find((c) => c.startsWith('photon_csrf='));
        if (cookie) headers.set('X-CSRF-Token', decodeURIComponent(cookie.split('=')[1] || ''));
    }

    const isFormData = options.body instanceof FormData;
    const isURLSearch = options.body instanceof URLSearchParams;

    if (options.body && !isFormData && !isURLSearch && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

    const res = await fetch(path, { credentials: 'same-origin', ...options, headers });
    const contentType = res.headers.get('content-type') || '';
    const payload = contentType.includes('json') ? await res.json() : await res.text();

    if (!res.ok) throw new Error(typeof payload === 'string' ? payload : payload?.message || res.statusText);

    return payload;
}
import { state } from '../state.js';

export async function api(path, options = {}) {
    const retryable = !path.startsWith('/accounts/auth_account')
        && path !== '/accounts/refresh'
        && path !== '/accounts/logout';

    for (let attempt = 0; attempt < (retryable ? 2 : 1); attempt += 1) {
        const headers = new Headers(options.headers || {});
        if (state.token) headers.set('Authorization', `Bearer ${state.token}`);
        if (state.userToken) headers.set('X-Photon-User-Token', state.userToken);

        const method = (options.method || 'GET').toUpperCase();
        if (path.startsWith('/api/admin') && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            const cookie = document.cookie.split(';').map((c) => c.trim()).find((c) => c.startsWith('photon_csrf='));
            if (cookie) headers.set('X-CSRF-Token', decodeURIComponent(cookie.split('=')[1] || ''));
        }

        const isFormData = options.body instanceof FormData;
        const isURLSearch = options.body instanceof URLSearchParams;
        if (options.body && !isFormData && !isURLSearch && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

        const res = await fetch(path, { credentials: 'same-origin', ...options, headers });
        const contentType = res.headers.get('content-type') || '';
        const payload = contentType.includes('json') ? await res.json() : await res.text();

        if (res.ok) return payload;
        if (attempt === 0 && retryable && (res.status === 401 || res.status === 403)
            && state.refreshToken && await refreshSession()) continue;

        const error = new Error(typeof payload === 'string' ? payload : payload?.message || res.statusText);
        error.status = res.status;
        throw error;
    }
}

async function refreshSession() {
    try {
        const res = await fetch('/accounts/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ refreshToken: state.refreshToken })
        });
        if (!res.ok) throw new Error('Refresh failed');

        const payload = await res.json();
        state.userToken = payload.token || '';
        state.refreshToken = payload.refreshToken || '';
        state.account = payload.account || null;
        localStorage.setItem('photon-refresh-token', state.refreshToken);
        if (state.userToken) localStorage.setItem('photon-user-token', state.userToken);
        else localStorage.removeItem('photon-user-token');
        if (state.account) localStorage.setItem('photon-account', JSON.stringify(state.account));
        return true;
    } catch {
        state.token = '';
        state.userToken = '';
        state.refreshToken = '';
        state.account = null;
        localStorage.removeItem('photon-account');
        localStorage.removeItem('photon-user-token');
        localStorage.removeItem('photon-refresh-token');
        window.location.hash = 'overview';
        document.getElementById('authModal')?.classList.add('open');
        return false;
    }
}

// js/services/auth.js
import { api } from '../api/client.js';
import { state } from '../state.js';
import { showToast } from '../ui/toast.js';

export async function login(email, password) {
    const body = new URLSearchParams({ email, password });
    if (state.purchaseToken) body.set('token', state.purchaseToken);

    const res = await fetch('/accounts/auth_account', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
        credentials: 'same-origin'
    });

    if (!res.ok) throw new Error(await res.text() || 'Login failed');
    return await res.json();
}
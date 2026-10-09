import { api } from '../api/client.js';
import { state } from '../state.js';
import { toast } from '../ui/toast.js';
import { closeModal, openModal } from '../ui/modal.js';
import { updateAuthVisibility } from '../ui/views.js';
import { navigate } from '../ui/router.js';

export function clearPurchaseToken() {
    if (state.purchaseToken) {
        state.purchaseToken = '';
        window.history.replaceState({}, '', window.location.pathname);
    }
}

export async function restoreSession() {
    if (state.purchaseToken && (state.userToken || state.account || state.token)) {
        await logout();
        return;
    }

    if (state.userToken && !state.token) {
        try {
            const account = await api('/accounts/me');
            state.account = account;
            await loadEntitlements();
            localStorage.setItem('photon-account', JSON.stringify(account));
        } catch {
            // Token expired or invalid
        }
    }

    updateAuthVisibility();
}

export async function login(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
    }

    const formData = new FormData(e.target);
    const body = new URLSearchParams(formData);

    if (state.purchaseToken) {
        body.set('token', state.purchaseToken);
    }

    try {
        if (state.purchaseToken) {
            await fetch('/stripe/purchase_session', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ checkoutSessionId: state.purchaseToken })
            }).catch(() => {});
        }

        const res = await fetch('/accounts/auth_account', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString(),
            credentials: 'same-origin'
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || 'Login failed');
        }

        const payload = await res.json();

        if (payload.status === '2FA_REQUIRED' && payload.ticket) {
            state.pending2FATicket = payload.ticket;
            document.querySelectorAll('.modal-panel').forEach((p) => p.classList.remove('active'));
            document.getElementById('totpLoginForm')?.classList.add('active');
            document.getElementById('authTabs')?.classList.add('hidden');
            const input = document.getElementById('totpLoginInput');
            if (input) {
                input.value = '';
                setTimeout(() => input.focus(), 150);
            }
            return;
        }

        await handleLoginSuccessPayload(payload);
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}

export async function submit2FALogin(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
    }

    const code = new FormData(e.target).get('code');

    try {
        const res = await fetch('/accounts/auth_account/2fa', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ticket: state.pending2FATicket, code }),
            credentials: 'same-origin'
        });

        if (!res.ok) {
            const err = await res.text();
            throw new Error(err || 'Verification failed');
        }

        const payload = await res.json();
        state.pending2FATicket = '';
        document.getElementById('authTabs')?.classList.remove('hidden');
        await handleLoginSuccessPayload(payload);
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}

export async function register(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn?.innerHTML || '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
    }

    const body = new URLSearchParams(new FormData(e.target));
    if (state.purchaseToken) body.set('token', state.purchaseToken);

    try {
        if (state.purchaseToken) {
            await fetch('/stripe/purchase_session', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ checkoutSessionId: state.purchaseToken })
            }).catch(() => {});
        }

        const res = await api('/accounts/create_account', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body
        });

        state.userToken = res.token || '';
        state.account = res.account || res;
        await loadEntitlements();
        localStorage.setItem('photon-user-token', state.userToken);
        localStorage.setItem('photon-account', JSON.stringify(state.account));

        clearPurchaseToken();
        toast('Account created', 'success');
        closeModal(null);
        updateAuthVisibility();
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}

export async function handleLoginSuccessPayload(payload) {
    state.account = payload.account || payload;
    localStorage.setItem('photon-account', JSON.stringify(state.account));

    if (payload.isAdmin) {
        state.token = '';
        state.userToken = '';
        localStorage.removeItem('photon-user-token');
        toast('Signed in as admin', 'success');
    } else {
        state.userToken = payload.token || '';
        localStorage.setItem('photon-user-token', state.userToken);
        await loadEntitlements();
        toast('Signed in', 'success');
    }

    clearPurchaseToken();
    closeModal(null);
    updateAuthVisibility();
}

export async function logout() {
    await api('/accounts/logout', { method: 'POST' }).catch(() => {});
    state.token = '';
    state.userToken = '';
    state.account = null;
    state.entitlements = [];
    localStorage.removeItem('photon-account');
    localStorage.removeItem('photon-user-token');
    updateAuthVisibility();
    navigate('overview');
    toast('Logged out');
}

export async function updateProfile(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
        const payload = {
            uuid: state.account.uuid,
            currentPassword: fd.get('currentPassword'),
            username: fd.get('username'),
            email: fd.get('email')
        };

        if (fd.get('newPassword')) {
            payload.newPassword = fd.get('newPassword');
            payload.confirmPassword = fd.get('newPassword');
        }

        if (fd.get('code')) {
            payload.code = fd.get('code').trim();
        }

        const acc = await api('/accounts/update_profile', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        state.account = acc;
        localStorage.setItem('photon-account', JSON.stringify(acc));
        e.target.reset();
        updateAuthVisibility();
        closeModal(null);
        toast('Profile updated', 'success');
    } catch (err) {
        toast(err.message, 'error');
    }
}

export async function open2FASetup() {
    if (state.account?.totpEnabled) {
        if (!confirm('Two-factor authentication is already active. Do you want to disable it?')) return;

        const code = prompt('Enter a 6-digit code from your authenticator app to disable 2FA:');
        if (!code) return;

        try {
            await api('/accounts/2fa/disable', {
                method: 'POST',
                body: JSON.stringify({ code: code.trim() })
            });

            toast('2FA has been disabled', 'success');
            const updatedAccount = await api('/accounts/me');
            state.account = updatedAccount;
            localStorage.setItem('photon-account', JSON.stringify(updatedAccount));
            updateAuthVisibility();
        } catch (err) {
            toast(err.message, 'error');
        }
        return;
    }

    openModal('totpSetupModal');
    const container = document.getElementById('totpQrContainer');
    const manualKey = document.getElementById('totpManualKey');
    if (container) container.innerHTML = '<i class="fa-solid fa-spinner fa-spin fa-2x text-secondary"></i>';
    if (manualKey) manualKey.value = '';

    try {
        const data = await api('/accounts/2fa/setup');
        if (container) {
            container.innerHTML = `<img src="${data.qrCode}" alt="2FA QR Code" style="width: 180px; height: 180px; border-radius: 12px; background: white; padding: 8px;">`;
        }
        if (manualKey) manualKey.value = data.manualKey;
    } catch (err) {
        if (container) container.innerHTML = `<p class="text-danger text-sm">Failed to load QR code</p>`;
        toast(err.message, 'error');
    }
}

export async function confirm2FASetup(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn?.innerHTML || '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
    }

    const code = new FormData(e.target).get('code');

    try {
        await api('/accounts/2fa/confirm', {
            method: 'POST',
            body: JSON.stringify({ code })
        });

        toast('Two-factor authentication enabled!', 'success');
        closeModal(null);
        e.target.reset();

        const updatedAccount = await api('/accounts/me');
        state.account = updatedAccount;
        localStorage.setItem('photon-account', JSON.stringify(updatedAccount));
        updateAuthVisibility();
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}

export async function loadEntitlements() {
    try {
        const entitlements = await api('/accounts/entitlements');
        state.entitlements = Array.isArray(entitlements) ? entitlements : [];
    } catch {
        state.entitlements = [];
    }
}

export async function requestAccountDeletion(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';
    }

    const formData = new FormData(e.target);
    const payload = {
        password: formData.get('password'),
        confirmationPhrase: formData.get('confirmationPhrase'),
        code: formData.get('code') ? formData.get('code').trim() : null
    };

    try {
        // Use your API wrapper pointing to /accounts/delete_account
        await api('/accounts/delete_account', {
            method: 'DELETE',
            body: JSON.stringify(payload)
        });

        // Close the modal and reset form
        closeModal(null);
        e.target.reset();

        // Clear local credentials and session state matching logout()
        state.token = '';
        state.userToken = '';
        state.account = null;
        state.entitlements = [];
        localStorage.removeItem('photon-account');
        localStorage.removeItem('photon-user-token');

        // Update UI and route to overview
        updateAuthVisibility();
        navigate('overview');
        toast('Account scheduled for deletion. You have been logged out.', 'success');
    } catch (err) {
        toast(err.message || 'Failed to request account deletion', 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}
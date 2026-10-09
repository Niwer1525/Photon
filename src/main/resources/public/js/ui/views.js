import { state } from '../state.js';
import { escapeHTML, formatVal, formatDate, copyToClipboard } from './dom.js';

export function updateAuthVisibility() {
    const isAdmin = !!state.token || state.account?.administrator;
    const isUser = !!state.userToken || !!state.account;
    const hasAccess = isUser && state.entitlements.length > 0;

    document.querySelectorAll('.guest-only').forEach((el) => el.classList.toggle('hidden', isUser));
    document.querySelectorAll('.auth-required').forEach((el) => el.classList.toggle('hidden', !isUser));
    document.querySelectorAll('.sub-required').forEach((el) => el.classList.toggle('hidden', !isAdmin && !hasAccess));
    document.querySelectorAll('.admin-required').forEach((el) => el.classList.toggle('hidden', !isAdmin));

    if (isUser && state.account) {
        renderProfileView();
        document.getElementById('purchaseAlert')?.classList.add('hidden');
    }
}

export function renderProfileView() {
    const editUser = document.getElementById('editUsername');
    const editEmail = document.getElementById('editEmail');
    if (editUser) editUser.value = state.account.username || '';
    if (editEmail) editEmail.value = state.account.email || '';

    // Render Account Grid
    const grid = document.getElementById('profileDetailsGrid');
    if (grid) {
        const copyable = ['username', 'uuid', 'email', 'discordAuthCode'];
        const hiddenProfileFields = new Set(['entitlements', 'subscriptionexpiresat', 'accountuuid', 'subscriber', 'subscriptionstatus', 'purchases', 'subscriptions']);
        
        grid.innerHTML = Object.entries(state.account)
            .filter(([key]) => !hiddenProfileFields.has(key.toLowerCase()))
            .map(([key, val]) => {
                const v = formatVal(val);
                const isCopyable = copyable.includes(key) && val;
                return `
                    <div class="card">
                        <span class="text-secondary text-sm" style="text-transform: capitalize;">
                            <i class="fa-solid fa-tag text-accent" style="margin-right: 6px;"></i>${escapeHTML(key)}
                        </span>
                        <div class="card-body" style="margin-top: 0.5rem;">
                            <strong class="${key === 'uuid' ? 'font-mono text-sm' : ''}" style="word-break: break-all; color: var(--text-primary); font-size: 0.95rem;">${escapeHTML(v)}</strong>
                        </div>
                        ${isCopyable ? `
                        <div class="card-footer">
                            <button class="btn icon-btn copy-btn" data-copy="${escapeHTML(String(val))}" title="Copy">
                                <i class="fa-regular fa-copy"></i>
                            </button>
                        </div>
                        ` : ''}
                    </div>
                `;
            }).join('');

        grid.querySelectorAll('.copy-btn').forEach((btn) => {
            btn.addEventListener('click', () => copyToClipboard(btn.dataset.copy));
        });
    }

    // Render Entitlements
    const entitlementsGrid = document.getElementById('entitlementsGrid');
    if (entitlementsGrid) {
        entitlementsGrid.innerHTML = state.entitlements.length ? state.entitlements.map((entitlement) => {
            const isActive = String(entitlement.status || '').toUpperCase() === 'ACTIVE';
            const type = entitlement.type === 'ONE_TIME' ? 'One-time purchase' : 'Subscription';
            const expiryText = entitlement.type === 'SUBSCRIPTION' 
                ? (entitlement.expiresAt ? formatDate(entitlement.expiresAt) : 'No expiry date')
                : (entitlement.expiresAt ? formatDate(entitlement.expiresAt) : null);

            return `
                <div class="card">
                    <div class="card-header"><strong>${escapeHTML(entitlement.productId || 'Unknown product')}</strong></div>
                    <div class="card-body">
                        <span class="text-secondary text-sm">${type}</span>
                        ${expiryText ? `<p class="text-secondary text-sm" style="margin: 0.5rem 0 0;">Expires: ${escapeHTML(expiryText)}</p>` : ''}
                        <div style="margin-top: 0.5rem;">
                            <span class="badge ${isActive ? 'active' : 'inactive'}">${escapeHTML(entitlement.status || 'UNKNOWN')}</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('') : '<p class="text-secondary">No products linked to this account.</p>';
    }

    // Configure 2FA Button State
    const btn2fa = document.getElementById('btnSetup2FA');
    if (btn2fa && state.account) {
        const isEnabled = !!state.account.totpEnabled;
        btn2fa.style.color = isEnabled ? 'var(--success-color)' : 'var(--text-secondary)';
        btn2fa.title = isEnabled ? '2FA Enabled (Click to disable)' : 'Enable 2FA';
        btn2fa.innerHTML = isEnabled 
            ? '<i class="fa-solid fa-shield"></i>' 
            : '<i class="fa-solid fa-shield-halved"></i>';
    }
}
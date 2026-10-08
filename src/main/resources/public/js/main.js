/**
 * Core Application State
 */
const State = {
    token: '',
    userToken: localStorage.getItem('photon-user-token') || '',
    account: JSON.parse(localStorage.getItem('photon-account')) || null,
    pending2FATicket: '',
    purchaseToken: new URLSearchParams(window.location.search).get('token') || '',
    activePage: 'overview',
    config: null,
    licenseProducts: [],
    entitlements: [],
    configSchema: [
        { key: 'bot_activity', label: 'Bot Activity', type: 'text' },
        { key: 'discord_bot_token', label: 'Discord Bot Token', type: 'password' },
        { key: 'discord_bot_id', label: 'Discord Bot ID', type: 'text' },
        { key: 'official_discord_server_id', label: 'Official Discord Server ID', type: 'text' },
        { key: 'network_console_channel_id', label: 'Console Channel ID', type: 'text' },
        { key: 'server_creator_role_id', label: 'Server Creator Role ID', type: 'text' },
        { key: 'webserver_port', label: 'Webserver Port', type: 'number' },
        { key: 'stripe_api_key', label: 'Stripe API Key', type: 'password' },
        { key: 'stripe_webhook_secret', label: 'Stripe Webhook Secret', type: 'password' },
        { key: 'api_version', label: 'API Version', type: 'text' },
        { key: 'mod_version', label: 'Mod Version', type: 'text' },
        { key: 'launcher_version', label: 'Launcher Version', type: 'text' },
        { key: 'store_url', label: 'Store URL', type: 'url' },
        { key: 'terms_of_service_url', label: "Terms of Service", type: 'url' },
        { key: 'terms_of_sale_url', label: "Terms of Sale", type: 'url' },
        { key: 'privacy_policy_url', label: 'Privacy Policy', type: 'url' }
    ]
};

/**
 * API Client
 */
const Api = async (path, options = {}) => {
    const headers = new Headers(options.headers || {});
    if (State.token) headers.set('Authorization', `Bearer ${State.token}`);
    if (State.userToken) headers.set('X-Photon-User-Token', State.userToken);
    
    const method = (options.method || 'GET').toUpperCase();
    if (path.startsWith('/api/admin') && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const cookie = document.cookie.split(';').map(c => c.trim()).find(c => c.startsWith('photon_csrf='));
        if (cookie) headers.set('X-CSRF-Token', decodeURIComponent(cookie.split('=')[1] || ''));
    }
    
    const isFormData = options.body instanceof FormData;
    const isURLSearch = options.body instanceof URLSearchParams;
    
    // Only default to application/json if Content-Type isn't already set and body isn't Form/URLSearch data
    if (options.body && !isFormData && !isURLSearch && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

    const res = await fetch(path, { credentials: 'same-origin', ...options, headers });
    const contentType = res.headers.get('content-type') || '';
    const payload = contentType.includes('json') ? await res.json() : await res.text();

    if (!res.ok) throw new Error(typeof payload === 'string' ? payload : payload?.message || res.statusText);
    return payload;
};

/**
 * Formats
 */
const formatVal = (v) => v == null || v === '' ? '—' : (typeof v === 'object' ? JSON.stringify(v) : String(v));
const formatDate = (v) => {
    if (!v) return '—';
    const d = new Date(v);
    return isNaN(d.getTime()) ? v : d.toLocaleString();
};

/**
 * UI & DOM Manipulation
 */
const UI = {
    init() {
        // Disconnect active user session if a URL token is detected
        if (State.purchaseToken && (State.userToken || State.account || State.token)) App.logout();

        // Routing
        document.querySelectorAll('.nav-link').forEach(el => {
            el.addEventListener('click', (e) => this.navigate(e.target.dataset.target));
        });
        
        // Theme setup
        const btn = document.getElementById('themeToggle');
        const html = document.documentElement;
        const updateThemeIcon = () => {
            const isDark = html.getAttribute('data-theme') === 'dark' || (html.getAttribute('data-theme') === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
            btn.innerHTML = isDark ? '<i class="fa-solid fa-sun"></i>' : '<i class="fa-solid fa-moon"></i>';
        };
        
        btn.addEventListener('click', () => {
            const current = html.getAttribute('data-theme');
            const next = current === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-theme', next);
            localStorage.setItem('theme-preference', next);
            updateThemeIcon();
        });
        
        const savedTheme = localStorage.getItem('theme-preference');
        if(savedTheme) html.setAttribute('data-theme', savedTheme);
        updateThemeIcon();

        // Handle Purchase Token UI state
        if (State.purchaseToken && !State.account) {
            document.getElementById('purchaseAlert').classList.remove('hidden');
            document.getElementById('registerModalSubtitle').innerHTML = '<span class="text-accent"><i class="fa-solid fa-link"></i> Purchase linked automatically.</span>';
            this.switchAuthTab('register');
        }

        if (State.userToken && !State.token) {
            Api('/accounts/me')
                .then(async (account) => {
                    State.account = account;
                    await App.loadEntitlements();
                    localStorage.setItem('photon-account', JSON.stringify(account));
                    this.updateAuthVisbility();
                })
                .catch(() => {});
        }

        this.updateAuthVisbility();
    },

    toggleMobileMenu() {
        const menu = document.getElementById('navMenu');
        const btnIcon = document.querySelector('.mobile-menu-btn i');
        if (!menu) return;
        const isOpen = menu.classList.toggle('mobile-open');
        if (btnIcon) btnIcon.className = isOpen ? 'fa-solid fa-xmark' : 'fa-solid fa-bars';
    },

    navigate(pageId) {
        if(!pageId) return;
        
        // Close mobile menu if open
        const menu = document.getElementById('navMenu');
        const btnIcon = document.querySelector('.mobile-menu-btn i');
        if (menu) menu.classList.remove('mobile-open');
        if (btnIcon) btnIcon.className = 'fa-solid fa-bars';

        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        
        const targetPage = document.getElementById(`page-${pageId}`);
        if (targetPage) targetPage.classList.add('active');
        
        const targetLink = document.querySelector(`.nav-link[data-target="${pageId}"]`);
        if (targetLink) targetLink.classList.add('active');
        
        const profileBtn = document.getElementById('navProfileBtn');
        if (profileBtn) {
            if (pageId === 'user') {
                profileBtn.style.color = 'var(--accent-color)';
                profileBtn.style.borderColor = 'var(--accent-color)';
            } else {
                profileBtn.style.color = '';
                profileBtn.style.borderColor = '';
            }
        }
        
        State.activePage = pageId;

        // Lazy load logic
        const isAdmin = !!State.token || State.account?.administrator;
        if(pageId === 'overview') App.loadPublicServers();
        if(pageId === 'downloads') App.loadDownloads();
        if(pageId === 'licenses' && (State.entitlements.length || isAdmin )) App.loadLicenses(); // Allow admins to view licenses even without entitlements
        if(pageId === 'admin') App.loadTablesList();
    },

    updateAuthVisbility() {
        const isAdmin = !!State.token || State.account?.administrator;
        const isUser = !!State.userToken || !!State.account;
        const hasAccess = isUser && State.entitlements.length > 0;

        document.querySelectorAll('.guest-only').forEach(el => el.classList.toggle('hidden', isUser));
        document.querySelectorAll('.auth-required').forEach(el => el.classList.toggle('hidden', !isUser));
        document.querySelectorAll('.sub-required').forEach(el => el.classList.toggle('hidden', !isAdmin && !hasAccess)); // Allow admins to see sub-required pages even without entitlements
        document.querySelectorAll('.admin-required').forEach(el => el.classList.toggle('hidden', !isAdmin));

        if (isUser && State.account) {
            // Populate Edit Form
            const editUser = document.getElementById('editUsername');
            const editEmail = document.getElementById('editEmail');
            if(editUser) editUser.value = State.account.username || '';
            if(editEmail) editEmail.value = State.account.email || '';

            // Build Dynamic Profile Grid
            const grid = document.getElementById('profileDetailsGrid');
            if (grid) {
                const copyable = ['username', 'uuid', 'email', 'discordAuthCode'];
                const hiddenProfileFields = new Set(['entitlements', 'subscriptionexpiresat', 'accountuuid', 'subscriber', 'subscriptionstatus', 'purchases', 'subscriptions']);
                grid.innerHTML = Object.entries(State.account)
                    .filter(([key]) => !hiddenProfileFields.has(key.toLowerCase()))
                    .map(([key, val]) => {
                    const v = formatVal(val);
                    const isCopyable = copyable.includes(key) && val;
                    return `
                        <div class="card">
                            <span class="text-secondary text-sm" style="text-transform: capitalize;"><i class="fa-solid fa-tag text-accent" style="margin-right: 6px;"></i>${this.escapeHTML(key)}</span>
                            <div class="card-body" style="margin-top: 0.5rem;">
                                <strong class="${key==='uuid'?'font-mono text-sm':''}" style="word-break: break-all; color: var(--text-primary); font-size: 0.95rem;">${this.escapeHTML(v)}</strong>
                            </div>
                            ${isCopyable ? `
                            <div class="card-footer">
                                <button class="btn icon-btn" title="Copy" onclick="UI.copy('${this.escapeHTML(String(val))}')"><i class="fa-regular fa-copy"></i></button>
                            </div>
                            ` : ''}
                        </div>
                    `;
                    }).join('');
            }

            const entitlementsGrid = document.getElementById('entitlementsGrid');
            if (entitlementsGrid) {
                const entitlements = State.entitlements;
                entitlementsGrid.innerHTML = entitlements.length ? entitlements.map(entitlement => {
                    const isActive = String(entitlement.status || '').toUpperCase() === 'ACTIVE';
                    const type = entitlement.type === 'ONE_TIME' ? 'One-time purchase' : 'Subscription';
                    return `
                        <div class="card">
                            <div class="card-header"><strong>${this.escapeHTML(entitlement.productId || 'Unknown product')}</strong></div>
                            <div class="card-body">
                                <span class="text-secondary text-sm">${type}</span>
                                ${entitlement.type === 'SUBSCRIPTION' ? `<p class="text-secondary text-sm" style="margin: 0.5rem 0 0;">Expires: ${this.escapeHTML(entitlement.expiresAt ? formatDate(entitlement.expiresAt) : 'No expiry date')}</p>` : (entitlement.expiresAt ? `<p class="text-secondary text-sm" style="margin: 0.5rem 0 0;">Expires: ${this.escapeHTML(formatDate(entitlement.expiresAt))}</p>` : '')}
                                <div style="margin-top: 0.5rem;"><span class="badge ${isActive ? 'active' : 'inactive'}">${this.escapeHTML(entitlement.status || 'UNKNOWN')}</span></div>
                            </div>
                        </div>
                    `;
                }).join('') : '<p class="text-secondary">No products linked to this account.</p>';
            }

            const btn2fa = document.getElementById('btnSetup2FA');
            if (btn2fa && State.account) {
                const isEnabled = !!State.account.totpEnabled;
                btn2fa.style.color = isEnabled ? 'var(--success-color)' : 'var(--text-secondary)';
                btn2fa.title = isEnabled ? '2FA Enabled (Click to disable)' : 'Enable 2FA';
                
                // Change icon to lock or shield check when enabled
                btn2fa.innerHTML = isEnabled 
                    ? '<i class="fa-solid fa-shield-check"></i>' 
                    : '<i class="fa-solid fa-shield-halved"></i>';
            }

            document.getElementById('purchaseAlert').classList.add('hidden');
        }
    },

    openModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;

        modal.classList.add('open');
        if (id === 'createLicenseModal') App.loadLicenseProducts();

        // Toggle 2FA field in edit profile modal depending on account state
        if (id === 'editProfileModal') {
            const group = document.getElementById('profile2faGroup');
            const codeInput = document.getElementById('edit2faCode');
            const is2FA = !!State.account?.totpEnabled;
            if (group) group.classList.toggle('hidden', !is2FA);
            if (codeInput) codeInput.required = is2FA;
        }
    },
    
    closeModal(event, force=false) {
        if (force || (event && event.target && event.target.classList.contains('modal-backdrop'))) {
            document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('open'));
        }
    },

    switchAuthTab(tab) {
        document.querySelectorAll('.modal-tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.modal-panel').forEach(p => p.classList.remove('active'));
        
        const tabs = document.querySelectorAll('.modal-tab');
        if (tab === 'login' && tabs.length > 0) tabs[0].classList.add('active');
        if (tab === 'register' && tabs.length > 1) tabs[1].classList.add('active');
        
        const formId = tab === 'login' ? 'loginForm' : 'registerForm';
        const formEl = document.getElementById(formId);
        if (formEl) formEl.classList.add('active');
    },

    show2FAStep(ticket) {
        State.pending2FATicket = ticket;
        document.querySelectorAll('.modal-panel').forEach(p => p.classList.remove('active'));
        document.getElementById('totpLoginForm')?.classList.add('active');
        document.getElementById('authTabs')?.classList.add('hidden'); // Hide Login/Register tabs
        const input = document.getElementById('totpLoginInput');
        if (input) {
            input.value = '';
            setTimeout(() => input.focus(), 150);
        }
    },

    cancel2FALogin() {
        State.pending2FATicket = '';
        document.getElementById('authTabs')?.classList.remove('hidden');
        this.switchAuthTab('login');
    },

    toast(msg, type='info') {
        const container = document.getElementById('toast-container');
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        const icon = type === 'success' ? 'check-circle' : type === 'error' ? 'circle-exclamation' : 'circle-info';
        el.innerHTML = `<i class="fa-solid fa-${icon}"></i> ${this.escapeHTML(msg)}`;
        container.appendChild(el);
        setTimeout(() => { el.style.opacity = '0'; setTimeout(()=>el.remove(), 300); }, 3000);
    },

    copy(text) {
        navigator.clipboard.writeText(text)
            .then(() => this.toast('Copied to clipboard', 'success'))
            .catch(() => this.toast('Failed to copy', 'error'));
    },

    escapeHTML(str) {
        return String(str||'').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[m]));
    }
};

/**
 * App Logic & Data Fetching
 */
const App = {
    async init() {
        UI.init();
        UI.navigate(window.location.hash.replace('#','') || 'overview');
        await Promise.allSettled([this.loadPublicServers()]);
    },

    // --- Authentication ---
    clearPurchaseToken() {
        if (State.purchaseToken) {
            State.purchaseToken = '';
            window.history.replaceState({}, '', window.location.pathname);
        }
    },

    async login(e) {
        e.preventDefault();
        const btn = e.target.querySelector('button[type="submit"]');
        const originalText = btn ? btn.innerHTML : '';
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
        }

        const formData = new FormData(e.target);
        const body = new URLSearchParams(formData);

        if (State.purchaseToken) {
            body.set('token', State.purchaseToken);
        }

        try {
            if (State.purchaseToken) {
                await fetch('/stripe/purchase_session', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ checkoutSessionId: State.purchaseToken })
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

            // CHECK FOR 2FA REQUIRED STEP
            if (payload.status === '2FA_REQUIRED' && payload.ticket) {
                UI.show2FAStep(payload.ticket);
                return;
            }

            // Normal login flow
            await this.handleLoginSuccessPayload(payload);
        } catch (err) {
            UI.toast(err.message, 'error');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
    },

    async submit2FALogin(e) {
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
                body: JSON.stringify({ ticket: State.pending2FATicket, code: code }),
                credentials: 'same-origin'
            });

            if (!res.ok) {
                const err = await res.text();
                throw new Error(err || 'Verification failed');
            }

            const payload = await res.json();
            State.pending2FATicket = '';
            UI.cancel2FALogin(); // Reset form views
            await this.handleLoginSuccessPayload(payload);
        } catch (err) {
            UI.toast(err.message, 'error');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
    },

    async handleLoginSuccessPayload(payload) {
        State.account = payload.account || payload;
        localStorage.setItem('photon-account', JSON.stringify(State.account));

        if (payload.isAdmin) {
            State.token = '';
            State.userToken = '';
            localStorage.removeItem('photon-user-token');
            UI.toast('Signed in as admin', 'success');
        } else {
            State.userToken = payload.token || '';
            localStorage.setItem('photon-user-token', State.userToken);
            await this.loadEntitlements();
            UI.toast('Signed in', 'success');
        }

        this.clearPurchaseToken();
        this.onLoginSuccess();
    },

    // --- 2FA Setup Logic ---
    async open2FASetup() {
        if (State.account?.totpEnabled) {
        if (!confirm('Two-factor authentication is already active. Do you want to disable it?')) return;

        const code = prompt('Enter a 6-digit code from your authenticator app to disable 2FA:');
        if (!code) return;

        try {
            await Api('/accounts/2fa/disable', {
                method: 'POST',
                body: JSON.stringify({ code: code.trim() })
            });

            UI.toast('2FA has been disabled', 'success');
            
            // Refresh account data
            const updatedAccount = await Api('/accounts/me');
            State.account = updatedAccount;
            localStorage.setItem('photon-account', JSON.stringify(updatedAccount));
            UI.updateAuthVisbility();
        } catch (err) {
            UI.toast(err.message, 'error');
        }
        return;
    }

        UI.openModal('totpSetupModal');
        const container = document.getElementById('totpQrContainer');
        const manualKey = document.getElementById('totpManualKey');
        container.innerHTML = '<i class="fa-solid fa-spinner fa-spin fa-2x text-secondary"></i>';
        manualKey.value = '';

        try {
            const data = await Api('/accounts/2fa/setup');
            container.innerHTML = `<img src="${data.qrCode}" alt="2FA QR Code" style="width: 180px; height: 180px; border-radius: 12px; background: white; padding: 8px;">`;
            manualKey.value = data.manualKey;
        } catch (err) {
            container.innerHTML = `<p class="text-danger text-sm">Failed to load QR code</p>`;
            UI.toast(err.message, 'error');
        }
    },

    async confirm2FASetup(e) {
        e.preventDefault();
        const btn = e.target.querySelector('button[type="submit"]');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

        const code = new FormData(e.target).get('code');

        try {
            await Api('/accounts/2fa/confirm', {
                method: 'POST',
                body: JSON.stringify({ code: code })
            });

            UI.toast('Two-factor authentication enabled!', 'success');
            UI.closeModal(null, true);
            e.target.reset();

            // Refresh profile data to reflect totpEnabled = true
            const updatedAccount = await Api('/accounts/me');
            State.account = updatedAccount;
            localStorage.setItem('photon-account', JSON.stringify(updatedAccount));
            UI.updateAuthVisbility();
        } catch (err) {
            UI.toast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    },

    async register(e) {
        e.preventDefault();
        const btn = e.target.querySelector('button[type="submit"]');
        const originalText = btn.innerHTML;
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
        }

        // Build standard URL-encoded form parameters
        const body = new URLSearchParams(new FormData(e.target));
        if (State.purchaseToken) body.set('token', State.purchaseToken);

        try {
            if (State.purchaseToken) {
                await fetch('/stripe/purchase_session', {
                    method: 'POST', 
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ checkoutSessionId: State.purchaseToken })
                }).catch(()=>{});
            }

            // Pass URLSearchParams directly with explicit form header
            const res = await Api('/accounts/create_account', { 
                method: 'POST', 
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body
            });
            
            State.userToken = res.token || ''; 
            State.account = res.account || res;
            await this.loadEntitlements();
            localStorage.setItem('photon-user-token', State.userToken);
            localStorage.setItem('photon-account', JSON.stringify(State.account));
            
            if (State.purchaseToken) { 
                State.purchaseToken = ''; 
                window.history.replaceState({}, '', window.location.pathname); 
            }
            
            UI.toast('Account created', 'success');
            this.onLoginSuccess();
        } catch (err) {
            UI.toast(err.message, 'error');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
    },

    async logout() {
        await Api('/accounts/logout', { method: 'POST' });
        State.token = ''; State.userToken = ''; State.account = null; State.entitlements = [];
        localStorage.removeItem('photon-account');
        localStorage.removeItem('photon-user-token');
        UI.updateAuthVisbility();
        UI.navigate('overview');
        UI.toast('Logged out');
    },

    onLoginSuccess() {
        UI.closeModal(null, true);
        UI.updateAuthVisbility();
    },

    async updateProfile(e) {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            const payload = {
                uuid: State.account.uuid,
                currentPassword: fd.get('currentPassword'),
                username: fd.get('username'),
                email: fd.get('email')
            };
            
            if (fd.get('newPassword')) {
                payload.newPassword = fd.get('newPassword');
                payload.confirmPassword = fd.get('newPassword'); 
            }

            // Include 2FA code if provided
            if (fd.get('code')) {
                payload.code = fd.get('code').trim();
            }

            const acc = await Api('/accounts/update_profile', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            
            State.account = acc;
            localStorage.setItem('photon-account', JSON.stringify(acc));
            e.target.reset();
            UI.updateAuthVisbility();
            UI.closeModal(null, true);
            UI.toast('Profile updated', 'success');
        } catch (err) { 
            UI.toast(err.message, 'error'); 
        }
    },

    async loadEntitlements() {
        try {
            const entitlements = await Api('/accounts/entitlements');
            State.entitlements = Array.isArray(entitlements) ? entitlements : [];
        } catch (err) {
            State.entitlements = [];
        }
    },

    // --- Public ---
    async loadPublicServers() {
        try {
            const servers = await Api('/api/status/servers');
            const grid = document.getElementById('serverGrid');
            document.getElementById('serverCount').innerHTML = `${servers.length} <i class="fa-solid fa-globe"></i>`;
            
            if (!servers.length) { grid.innerHTML = '<p class="text-secondary" style="grid-column: 1/-1;">No servers online.</p>'; return; }

            grid.innerHTML = servers.map(s => `
                <div class="card">
                    <div class="card-header">
                        <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${UI.escapeHTML(s.serverName || 'Unknown')}">${UI.escapeHTML(s.serverName || 'Unknown Server')}</span>
                    </div>
                    <div class="card-body">
                        <p style="margin-bottom:0">${UI.escapeHTML(s.serverMOTD || 'No MOTD provided')}</p>
                    </div>
                    <div class="card-footer">
                        <button class="btn icon-btn" title="Copy IP" onclick="UI.copy('${s.serverIP}:${s.serverPort}')"><i class="fa-regular fa-copy"></i></button>
                        ${s.site ? `<a href="${UI.escapeHTML(s.site)}" target="_blank" class="btn icon-btn"><i class="fa-solid fa-link"></i></a>` : ''}
                        ${s.discord ? `<a href="${UI.escapeHTML(s.discord)}" target="_blank" class="btn icon-btn"><i class="fa-brands fa-discord"></i></a>` : ''}
                    </div>
                </div>
            `).join('');
        } catch (e) {
            document.getElementById('serverGrid').innerHTML = `<p class="text-secondary text-danger" style="grid-column: 1/-1;">Failed to load servers.</p>`;
        }
    },

    async loadDownloads() {
        const grid = document.getElementById('downloadsGrid');
        try {
            const repositories = await Api('/download/list');
            const sections = Object.entries(repositories || {}).map(([repository, data]) => {
                const releases = (data && data.releases) || [];
                const tags = (data && data.tags) || [];

                // Map tag metadata by tag name/commit for quick lookup
                const tagMap = new Map(tags.map(tag => [tag.tag || tag.name, tag]));

                const rows = releases.flatMap(release => {
                    const tagName = release.tagName || release.tag_name;
                    const associatedTag = tagMap.get(tagName);
                    const tagMessage = associatedTag ? (associatedTag.message || '') : '';

                    return (release.assets || [])
                        .filter(asset => asset.name)
                        .map(asset => `
                            <tr>
                                <td>${UI.escapeHTML(release.name || tagName || 'Unreleased')}</td>
                                <td><span class="font-mono text-sm">${UI.escapeHTML(release.prerelease ? 'Yes' : 'No')}</span></td>
                                <td><span class="font-mono text-sm">${UI.escapeHTML(asset.name)}</span></td>
                                <td><span class="font-mono text-sm">${UI.escapeHTML(tagMessage || release.body || asset.body || '-')}</span></td>
                                <td class="download-action">
                                    <a class="btn primary icon-btn" title="Download" href="/download?product=${encodeURIComponent(repository)}&assetName=${asset.name || 'Unknown'}&assetId=${asset.id}" target="_blank">
                                        <i class="fa-solid fa-cloud-arrow-down"></i>
                                    </a>
                                </td>
                            </tr>
                        `);
                }).join('');

                return `
                    <section class="download-repository">
                        <div class="download-repository-header">
                            <h3><i class="fa-solid fa-code-branch text-accent"></i> ${UI.escapeHTML(repository)}</h3>
                        </div>
                        <div class="table-container">
                            <table>
                                <thead><tr>
                                    <th>Release</th>
                                    <th>Pre-Release</th>
                                    <th>Asset</th>
                                    <th>Description</th>
                                    <th>Download</th>
                                </tr></thead>
                                <tbody>${rows || '<tr><td colspan="5" class="text-secondary">No downloadable assets found.</td></tr>'}</tbody>
                            </table>
                        </div>
                    </section>
                `;
            });

            grid.innerHTML = sections.length ? sections.join('') : '<p class="text-secondary">No downloadable assets found.</p>';
        } catch (e) {
            grid.innerHTML = '<p class="text-secondary text-danger">Failed to load downloads.</p>';
        }
    },

    // --- Subscriptions ---
    async loadLicenses() {
        try {
            await this.loadLicenseProducts();
            const licenses = await Api('/accounts/licenses');
            const tbody = document.getElementById('licensesTableBody');
            
            if (!licenses || !licenses.length) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-secondary text-center">No licenses found.</td></tr>';
                return;
            }

            tbody.innerHTML = licenses.map(l => {
                const status = String(l.status || l.state || 'UNKNOWN').toUpperCase();
                const isRevoked = status === 'REVOKED';
                const badgeClass = status === 'ACTIVE' ? 'active' : (isRevoked ? 'danger' : '');
                
                return `
                <tr>
                    <td>
                        <strong>${UI.escapeHTML(l.name || l.customerName || '—')}</strong><br>
                        <span class="text-secondary text-sm">${UI.escapeHTML(l.productId || l.product_id || '—')}</span>
                    </td>
                    <td>
                        <span class="text-sm">
                            <i class="fa-solid fa-arrow-right-to-bracket text-success" style="margin-right:4px;"></i> ${formatDate(l.createdAt || l.created_at).split(',')[0]}<br>
                            <i class="fa-solid fa-arrow-right-from-bracket text-danger" style="margin-right:4px;"></i> ${formatDate(l.expiresAt || l.expires_at).split(',')[0]}
                        </span>
                    </td>
                    <td><code class="font-mono text-sm" style="background: var(--surface-glass); padding: 0.3rem 0.5rem; border-radius: 6px; border: 1px solid var(--border-color);">${UI.escapeHTML(l.licenseKey || l.key || '—')}</code></td>
                    <td><span class="badge ${badgeClass}">${UI.escapeHTML(status)}</span></td>
                    <td>
                        <div style="display: flex; gap: 0.5rem;">
                            ${!isRevoked ? `
                                <button class="icon-btn" onclick="UI.copy('${l.licenseKey || l.key}')" title="Copy Key"><i class="fa-regular fa-copy"></i></button>
                                <button class="icon-btn" style="color:var(--danger-color)" onclick="App.revokeLicense('${l.licenseKey || l.key}')" title="Revoke"><i class="fa-solid fa-trash"></i></button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `}).join('');
        } catch (e) { UI.toast('Failed to load licenses', 'error'); }
    },

    async loadLicenseProducts() {
        const select = document.getElementById('licenseProductSelect');
        if (!select) return;

        select.innerHTML = '<option value="">Loading products...</option>';
        try {
            const products = await Api('/accounts/license-products');
            State.licenseProducts = Array.isArray(products) ? products : [];
            select.innerHTML = State.licenseProducts.length
                ? State.licenseProducts.map(product => `<option value="${UI.escapeHTML(product.id)}">${UI.escapeHTML(product.name || product.id)}</option>`).join('')
                : '<option value="">No products available</option>';
        } catch (error) {
            State.licenseProducts = [];
            select.innerHTML = '<option value="">Unable to load products</option>';
            UI.toast(error.message || 'Failed to load license products', 'error');
        }
    },

    async createLicense(e) {
        e.preventDefault();
        const fd = new FormData(e.target);
        const payload = { name: fd.get('name'), product_id: fd.get('product_id') };
        if (fd.get('duration_days')) payload.duration_days = Number(fd.get('duration_days'));

        try {
            await Api('/accounts/licenses', { method: 'POST', body: JSON.stringify(payload) });
            UI.closeModal(null, true);
            e.target.reset();
            UI.toast('License created', 'success');
            this.loadLicenses();
        } catch (err) { UI.toast(err.message, 'error'); }
    },

    async revokeLicense(key) {
        if(!confirm('Revoke this license?')) return;
        try {
            await Api('/accounts/licenses/revoke', { method: 'POST', body: JSON.stringify({ license_key: key }) });
            UI.toast('License revoked', 'success');
            this.loadLicenses();
        } catch (err) { UI.toast(err.message, 'error'); }
    },

    // --- Admin ---
    async loadTablesList() {
        try {
            const tables = await Api('/api/admin/tables');
            const sel = document.getElementById('tableSelector');
            
            if (!tables || !tables.length) {
                sel.innerHTML = '<option value="">No tables available</option>';
                document.getElementById('dataTableBody').innerHTML = '<tr><td class="text-secondary">No tables found on server.</td></tr>';
                return;
            }

            sel.innerHTML = tables.map(t => `<option value="${t.table}">${t.label}</option>`).join('');
            this.loadTableData();
        } catch(e) {
            const sel = document.getElementById('tableSelector');
            sel.innerHTML = '<option value="">Error loading tables</option>';
            UI.toast('Failed to load tables list (API Error)', 'error');
        }
    },

    async loadTableData() {
        const table = document.getElementById('tableSelector').value;
        const limit = document.getElementById('tableLimit').value;
        if(!table) return;
        
        try {
            const data = await Api(`/api/admin/tables/${encodeURIComponent(table)}?limit=${limit}`);
            const head = document.getElementById('dataTableHead');
            const body = document.getElementById('dataTableBody');
            
            if(!data.columns || !data.rows.length) {
                head.innerHTML = '<tr><th>Notice</th></tr>';
                body.innerHTML = '<tr><td class="text-secondary">No rows found in this table.</td></tr>';
                return;
            }

            head.innerHTML = `<tr>${data.columns.map(c => `<th>${UI.escapeHTML(c)}</th>`).join('')}</tr>`;
            body.innerHTML = data.rows.map(row => 
                `<tr>${data.columns.map(c => `<td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis;" title="${UI.escapeHTML(formatVal(row[c]))}">${UI.escapeHTML(formatVal(row[c]))}</td>`).join('')}</tr>`
            ).join('');
        } catch (e) { 
            UI.toast('Failed to load table data', 'error');
            document.getElementById('dataTableBody').innerHTML = '<tr><td class="text-danger">Failed to fetch data.</td></tr>';
        }
    }
};

document.addEventListener('DOMContentLoaded', () => App.init()); // Boot
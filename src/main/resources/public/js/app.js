// Register Custom Elements
import './components/nav.js';
import './components/footer.js';
import './components/modals/auth-modal.js';
import './components/modals/totp-setup-modal.js';
import './components/modals/create-license-modal.js';
import './components/modals/edit-profile-modal.js';

// Application Core
import { state } from './state.js';
import { initTheme } from './ui/theme.js';
import { initRouter } from './ui/router.js';
import { openModal } from './ui/modal.js';
import { restoreSession, open2FASetup } from './services/auth.js';
import { loadTableData } from './services/public.js';

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Initialize Visual Theme and Navigation Routing
    initTheme();
    initRouter();

    // 2. Bind static index.html element listeners
    document.getElementById('tableSelector')?.addEventListener('change', () => loadTableData());
    document.getElementById('tableLimit')?.addEventListener('change', () => loadTableData());
    document.getElementById('btnRefreshTable')?.addEventListener('click', () => loadTableData());
    document.getElementById('btnOpenAuthModal')?.addEventListener('click', () => openModal('authModal'));
    document.getElementById('btnSetup2FA')?.addEventListener('click', () => open2FASetup());
    document.getElementById('btnEditProfile')?.addEventListener('click', () => openModal('editProfileModal'));
    document.getElementById('btnCreateLicenseModal')?.addEventListener('click', () => openModal('createLicenseModal'));

    // 3. Handle checkout session landing state
    if (state.purchaseToken && !state.account) {
        document.getElementById('purchaseAlert')?.classList.remove('hidden');
        const subtitle = document.getElementById('registerModalSubtitle');
        if (subtitle) {
            subtitle.innerHTML = '<span class="text-accent"><i class="fa-solid fa-link"></i> Purchase linked automatically.</span>';
        }
        const authModal = document.querySelector('inc-auth-modal');
        if (authModal && typeof authModal.switchTab === 'function') {
            authModal.switchTab('register');
        }
    }

    // 4. Validate and restore session tokens
    await restoreSession();
});
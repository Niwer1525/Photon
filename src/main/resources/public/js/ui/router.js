import { state } from '../state.js';
import { loadPublicServers, loadDownloads, loadTablesList } from '../services/public.js';
import { loadLicenses } from '../services/licenses.js';

export function initRouter() {
    const initialPage = window.location.hash.replace('#', '') || 'overview';
    navigate(initialPage);

    window.addEventListener('hashchange', () => {
        const page = window.location.hash.replace('#', '') || 'overview';
        navigate(page);
    });
}

export function navigate(pageId) {
    if (!pageId) return;

    closeMobileMenu();

    document.querySelectorAll('.page').forEach((p) => p.classList.remove('active'));
    document.querySelectorAll('.nav-link').forEach((l) => l.classList.remove('active'));

    const targetPage = document.getElementById(`page-${pageId}`);
    if (targetPage) targetPage.classList.add('active');

    const targetLink = document.querySelector(`.nav-link[data-target="${pageId}"]`);
    if (targetLink) targetLink.classList.add('active');

    const profileBtn = document.getElementById('navProfileBtn');
    if (profileBtn) {
        profileBtn.style.color = pageId === 'user' ? 'var(--accent-color)' : '';
        profileBtn.style.borderColor = pageId === 'user' ? 'var(--accent-color)' : '';
    }

    state.activePage = pageId;

    // Trigger lazy loading per page
    const isAdmin = !!state.token || state.account?.administrator;
    if (pageId === 'overview') loadPublicServers();
    if (pageId === 'downloads') loadDownloads();
    if (pageId === 'licenses' && (state.entitlements.length || isAdmin)) loadLicenses();
    if (pageId === 'admin') loadTablesList();
}

export function toggleMobileMenu() {
    const menu = document.getElementById('navMenu');
    const btnIcon = document.querySelector('.mobile-menu-btn i');
    if (!menu) return;
    const isOpen = menu.classList.toggle('mobile-open');
    if (btnIcon) btnIcon.className = isOpen ? 'fa-solid fa-xmark' : 'fa-solid fa-bars';
}

export function closeMobileMenu() {
    const menu = document.getElementById('navMenu');
    const btnIcon = document.querySelector('.mobile-menu-btn i');
    if (menu) menu.classList.remove('mobile-open');
    if (btnIcon) btnIcon.className = 'fa-solid fa-bars';
}
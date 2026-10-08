import { navigate, toggleMobileMenu } from '../ui/router.js';
import { toggleTheme } from '../ui/theme.js';
import { openModal } from '../ui/modal.js';
import { logout } from '../services/auth.js';

export class IncNav extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
            <nav>
                <div class="nav-brand">
                    <img src="assets/photon_logo.png" alt="Photon" style="height: 36px; width: auto; object-fit: contain;">
                </div>
                <div class="nav-links" id="navMenu">
                    <a class="nav-link active" data-target="overview">Overview</a>
                    <a class="nav-link" data-target="downloads">Downloads</a>
                    <a class="nav-link sub-required hidden" data-target="licenses">Licenses</a>
                    <a class="nav-link admin-required hidden" data-target="admin">Admin</a>
                </div>
                <div class="nav-actions">
                    <button class="icon-btn" id="themeToggle" title="Toggle Theme">
                        <i class="fa-solid fa-moon"></i>
                    </button>
                    <button class="btn primary guest-only" id="navSignInBtn">Sign In</button>
                    <button class="icon-btn auth-required hidden" id="navProfileBtn" title="Profile">
                        <i class="fa-solid fa-user"></i>
                    </button>
                    <button class="icon-btn auth-required hidden" id="navLogoutBtn" title="Logout">
                        <i class="fa-solid fa-sign-out-alt"></i>
                    </button>
                    <button class="icon-btn mobile-menu-btn" id="navMobileMenuBtn" title="Menu">
                        <i class="fa-solid fa-bars"></i>
                    </button>
                </div>
            </nav>
        `;

        // Event Listeners
        this.querySelector('#themeToggle')?.addEventListener('click', toggleTheme);
        this.querySelector('#navMobileMenuBtn')?.addEventListener('click', toggleMobileMenu);
        this.querySelector('#navSignInBtn')?.addEventListener('click', () => openModal('authModal'));
        this.querySelector('#navProfileBtn')?.addEventListener('click', () => navigate('user'));
        this.querySelector('#navLogoutBtn')?.addEventListener('click', () => logout());

        this.querySelectorAll('.nav-link').forEach((link) => {
            link.addEventListener('click', (e) => {
                const target = e.currentTarget.dataset.target;
                if (target) navigate(target);
            });
        });
    }
}

customElements.define('inc-nav', IncNav);
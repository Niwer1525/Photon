import { wrapModal, bindModalClose } from '../modal-base.js';
import { login, register, submit2FALogin } from '../../services/auth.js';

export class IncAuthModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = wrapModal('authModal', `
            <div class="modal-tabs" id="authTabs">
                <div class="modal-tab active" data-tab="login">Login</div>
                <div class="modal-tab" data-tab="register">Register</div>
            </div>
            
            <!-- Login Form -->
            <form id="loginForm" class="modal-panel active">
                <div style="text-align: center; margin-bottom: 1.5rem;">
                    <h2><i class="fa-solid fa-fingerprint text-accent"></i> Authentication</h2>
                    <p class="text-sm text-secondary">Sign in to your account</p>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <div class="input-wrapper">
                        <i class="fa-regular fa-envelope left-icon"></i>
                        <input type="email" name="email" required placeholder="joe@gmail.com">
                    </div>
                </div>
                <div class="form-group">
                    <label>Password</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-lock left-icon"></i>
                        <input type="password" name="password" required placeholder="••••••••">
                    </div>
                </div>
                <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Login</button>
            </form>

            <!-- 2FA Verification Form -->
            <form id="totpLoginForm" class="modal-panel">
                <div style="text-align: center; margin-bottom: 1.5rem;">
                    <h2><i class="fa-solid fa-shield-halved text-accent"></i> Two-Factor Auth</h2>
                    <p class="text-sm text-secondary">Enter the 6-digit code from your authenticator app</p>
                </div>
                <div class="form-group">
                    <label>Authenticator Code</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-key left-icon"></i>
                        <input type="text" name="code" id="totpLoginInput" pattern="[0-9]{6}" maxlength="6" inputmode="numeric" autocomplete="one-time-code" required placeholder="123456" style="letter-spacing: 0.3rem; text-align: center; font-size: 1.25rem;">
                    </div>
                </div>
                <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Verify & Continue</button>
                <button type="button" class="btn icon-btn" id="btnCancel2FA" style="width: 100%; margin-top: 0.5rem; border-radius: 12px;">Back to Login</button>
            </form>

            <!-- Register Form -->
            <form id="registerForm" class="modal-panel">
                <div style="text-align: center; margin-bottom: 1.5rem;">
                    <h2><i class="fa-solid fa-user-plus text-accent"></i> Create Account</h2>
                    <p class="text-sm text-secondary" id="registerModalSubtitle">Join the network</p>
                </div>
                <div class="form-group">
                    <label>Username</label>
                    <div class="input-wrapper">
                        <i class="fa-regular fa-user left-icon"></i>
                        <input type="text" name="username" required placeholder="JoeDalton_">
                    </div>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <div class="input-wrapper">
                        <i class="fa-regular fa-envelope left-icon"></i>
                        <input type="email" name="email" required placeholder="joe@gmail.com">
                    </div>
                </div>
                <div class="form-group">
                    <label>Password</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-lock left-icon"></i>
                        <input type="password" name="password" required minlength="8" placeholder="••••••••">
                    </div>
                </div>
                <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Register</button>
            </form>
        `);

        bindModalClose(this, 'authModal');

        // Tab switching
        this.querySelectorAll('.modal-tab').forEach((tab) => {
            tab.addEventListener('click', (e) => {
                const target = e.currentTarget.dataset.tab;
                this.switchTab(target);
            });
        });

        // Form Submissions
        this.querySelector('#loginForm')?.addEventListener('submit', (e) => login(e));
        this.querySelector('#registerForm')?.addEventListener('submit', (e) => register(e));
        this.querySelector('#totpLoginForm')?.addEventListener('submit', (e) => submit2FALogin(e));
        this.querySelector('#btnCancel2FA')?.addEventListener('click', () => {
            document.getElementById('authTabs')?.classList.remove('hidden');
            this.switchTab('login');
        });
    }

    switchTab(tabName) {
        this.querySelectorAll('.modal-tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
        this.querySelectorAll('.modal-panel').forEach((p) => p.classList.remove('active'));
        this.querySelector(`#${tabName}Form`)?.classList.add('active');
    }
}

customElements.define('inc-auth-modal', IncAuthModal);
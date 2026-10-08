function createModal(id, formContent) {
    return `
        <div class="modal-backdrop" id="${id}" onclick="UI.closeModal(event)">
            <div class="modal">
                <button class="modal-close icon-btn" onclick="UI.closeModal(null, true)"><i class="fa-solid fa-xmark"></i></button>
                ${formContent}
            </div>
        </div>
    `;
}

class IncAuthModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = createModal('authModal', `
            <div class="modal-tabs" id="authTabs">
                <div class="modal-tab active" onclick="UI.switchAuthTab('login')">Login</div>
                <div class="modal-tab" onclick="UI.switchAuthTab('register')">Register</div>
            </div>
            
            <!-- Login Form -->
            <form id="loginForm" class="modal-panel active" onsubmit="App.login(event)">
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

            <!-- 2FA Verification Step Form -->
            <form id="totpLoginForm" class="modal-panel" onsubmit="App.submit2FALogin(event)">
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
                <button type="button" class="btn icon-btn" style="width: 100%; margin-top: 0.5rem; border-radius: 12px;" onclick="UI.cancel2FALogin()">Back to Login</button>
            </form>

            <!-- Register Form -->
            <form id="registerForm" class="modal-panel" onsubmit="App.register(event)">
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
    }
}
customElements.define('inc-auth-modal', IncAuthModal);

class IncTotpSetupModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = createModal('totpSetupModal', `
            <div style="text-align: center; margin-bottom: 1.5rem;">
                <h2><i class="fa-solid fa-shield-halved text-accent"></i> Enable 2FA</h2>
                <p class="text-sm text-secondary">Scan this code with Google Authenticator, Authy, or 1Password</p>
            </div>
            
            <div id="totpQrContainer" style="display: flex; justify-content: center; align-items: center; margin-bottom: 1rem; min-height: 180px;">
                <i class="fa-solid fa-spinner fa-spin fa-2x text-secondary"></i>
            </div>

            <div class="form-group">
                <label>Manual Secret Key</label>
                <div class="input-wrapper">
                    <input type="text" id="totpManualKey" readonly style="font-family: monospace; text-align: center; cursor: pointer;" onclick="UI.copy(this.value)" title="Click to copy">
                </div>
            </div>

            <form onsubmit="App.confirm2FASetup(event)">
                <div class="form-group">
                    <label>Confirm 6-Digit Code</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-key left-icon"></i>
                        <input type="text" name="code" pattern="[0-9]{6}" maxlength="6" inputmode="numeric" required placeholder="123456" style="letter-spacing: 0.3rem; text-align: center; font-size: 1.25rem;">
                    </div>
                </div>
                <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Activate 2FA</button>
            </form>
        `);
    }
}
customElements.define('inc-totp-setup-modal', IncTotpSetupModal);

class IncCreateLicenseModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = createModal('createLicenseModal', `
            <form onsubmit="App.createLicense(event)">
                <h2 style="margin-bottom: 1.5rem;"><i class="fa-solid fa-key text-accent"></i> New License</h2>
                <div class="form-group">
                    <label>License Name / Identifier</label>
                    <input type="text" name="name" required placeholder="e.g. Production Server">
                </div>
                <div class="form-group">
                    <label>Product</label>
                    <select name="product_id" id="licenseProductSelect" required>
                        <option value="">Loading products...</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Duration (Days)</label>
                    <input type="number" name="duration_days" placeholder="30" min="1">
                    <span class="text-sm text-secondary">Leave blank for default.</span>
                </div>
                <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Generate Key</button>
            </form>
        `);
    }
}
customElements.define('inc-create-license-modal', IncCreateLicenseModal); 

class IncEditProfileModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = createModal('editProfileModal', `
            <form onsubmit="App.updateProfile(event)">
                <h2 style="margin-bottom: 1.5rem;"><i class="fa-solid fa-user-pen text-accent"></i> Edit Details</h2>
                <div class="grid">
                    <div class="grid grid-cols-2">
                        <div class="form-group">
                            <label>Username</label>
                            <input type="text" name="username" id="editUsername" required>
                        </div>
                        <div class="form-group">
                            <label>Email</label>
                            <input type="email" name="email" id="editEmail" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>New Password <span class="text-secondary">(Optional)</span></label>
                        <input type="password" name="newPassword" id="editNewPassword" minlength="8" placeholder="Leave blank to keep current">
                    </div>
                    <div class="form-group">
                        <label>Current Password <span class="text-danger">*</span></label>
                        <input type="password" name="currentPassword" required placeholder="Required to save changes">
                    </div>

                    <!-- 2FA Code Input: Visible if user has 2FA active -->
                    <div class="form-group hidden" id="profile2faGroup">
                        <label>2FA Authenticator Code <span class="text-danger">*</span></label>
                        <div class="input-wrapper">
                            <i class="fa-solid fa-key left-icon"></i>
                            <input type="text" name="code" id="edit2faCode" pattern="[0-9]{6}" maxlength="6" inputmode="numeric" placeholder="123456" style="letter-spacing: 0.2rem;">
                        </div>
                        <span class="text-sm text-secondary">Required because two-factor authentication is active on your account.</span>
                    </div>

                    <button type="submit" class="btn primary" style="width: 100%; margin-top: 1rem;">Save Changes</button>
                </div>
            </form>
        `);
    }
}
customElements.define('inc-edit-profile-modal', IncEditProfileModal);
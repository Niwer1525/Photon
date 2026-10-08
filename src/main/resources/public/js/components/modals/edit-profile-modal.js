import { wrapModal, bindModalClose } from '../modal-base.js';
import { updateProfile } from '../../services/auth.js';

export class IncEditProfileModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = wrapModal('editProfileModal', `
            <form id="editProfileForm">
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

        bindModalClose(this, 'editProfileModal');
        this.querySelector('#editProfileForm')?.addEventListener('submit', (e) => updateProfile(e));
    }
}

customElements.define('inc-edit-profile-modal', IncEditProfileModal);
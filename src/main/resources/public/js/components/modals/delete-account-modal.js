import { wrapModal, bindModalClose } from '../modal-base.js';
import { requestAccountDeletion } from '../../services/auth.js';
import { state } from '../../state.js';

const CONFIRMATION_PHRASE = 'DELETE MY ACCOUNT';

export class IncDeleteAccountModal extends HTMLElement {
    connectedCallback() {
        const has2FA = Boolean(state.account?.totpEnabled);

        this.innerHTML = wrapModal('deleteAccountModal', `
            <div style="text-align: center; margin-bottom: 1.5rem;">
                <h2><i class="fa-solid fa-triangle-exclamation text-danger" style="color: var(--danger-color, #ef4444);"></i> Delete Account</h2>
                <p class="text-sm text-secondary">This action starts your grace period and revokes all active sessions immediately.</p>
            </div>

            <form id="deleteAccountForm">
                <div class="form-group">
                    <label>Current Password</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-lock left-icon"></i>
                        <input type="password" name="password" required placeholder="••••••••">
                    </div>
                </div>

                ${has2FA ? `
                <div class="form-group">
                    <label>Authenticator Code (2FA)</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-key left-icon"></i>
                        <input type="text" name="code" pattern="[0-9]{6}" maxlength="6" inputmode="numeric" autocomplete="one-time-code" required placeholder="123456" style="letter-spacing: 0.3rem; text-align: center; font-size: 1.25rem;">
                    </div>
                </div>` : ''}

                <div class="form-group">
                    <label>Type <strong style="color: var(--danger-color, #ef4444);">${CONFIRMATION_PHRASE}</strong> to confirm</label>
                    <div class="input-wrapper">
                        <i class="fa-solid fa-keyboard left-icon"></i>
                        <input type="text" id="deletePhraseInput" name="confirmationPhrase" autocomplete="off" required placeholder="${CONFIRMATION_PHRASE}">
                    </div>
                </div>

                <button type="submit" id="btnSubmitDelete" class="btn danger" style="width: 100%; margin-top: 1rem; background-color: var(--danger-color, #ef4444); color: #fff;" disabled>
                    <i class="fa-solid fa-trash-can"></i> Confirm Deletion
                </button>
            </form>
        `);

        bindModalClose(this, 'deleteAccountModal');

        const phraseInput = this.querySelector('#deletePhraseInput');
        const submitBtn = this.querySelector('#btnSubmitDelete');

        // Unlock submit strictly when the exact phrase is provided
        phraseInput?.addEventListener('input', (e) => {
            submitBtn.disabled = e.target.value.trim() !== CONFIRMATION_PHRASE;
        });

        // Delegate submission to the auth service
        this.querySelector('#deleteAccountForm')?.addEventListener('submit', (e) => requestAccountDeletion(e));
    }
}

customElements.define('inc-delete-account-modal', IncDeleteAccountModal);
import { wrapModal, bindModalClose } from '../modal-base.js';
import { copyToClipboard } from '../../ui/dom.js';
import { confirm2FASetup } from '../../services/auth.js';

export class IncTotpSetupModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = wrapModal('totpSetupModal', `
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
                    <input type="text" id="totpManualKey" readonly style="font-family: monospace; text-align: center; cursor: pointer;" title="Click to copy">
                </div>
            </div>

            <form id="confirm2FAForm">
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

        bindModalClose(this, 'totpSetupModal');

        const manualKeyInput = this.querySelector('#totpManualKey');
        manualKeyInput?.addEventListener('click', () => {
            if (manualKeyInput.value) copyToClipboard(manualKeyInput.value);
        });

        this.querySelector('#confirm2FAForm')?.addEventListener('submit', (e) => confirm2FASetup(e));
    }
}

customElements.define('inc-totp-setup-modal', IncTotpSetupModal);
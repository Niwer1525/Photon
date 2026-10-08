import { wrapModal, bindModalClose } from '../modal-base.js';
import { createLicense } from '../../services/licenses.js';

export class IncCreateLicenseModal extends HTMLElement {
    connectedCallback() {
        this.innerHTML = wrapModal('createLicenseModal', `
            <form id="createLicenseForm">
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

        bindModalClose(this, 'createLicenseModal');
        this.querySelector('#createLicenseForm')?.addEventListener('submit', (e) => createLicense(e));
    }
}

customElements.define('inc-create-license-modal', IncCreateLicenseModal);
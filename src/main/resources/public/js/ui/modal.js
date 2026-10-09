import { state } from '../state.js';
import { loadLicenseProducts } from '../services/licenses.js';

/**
 * Opens a modal with the specified ID.

 * @param {*} id - The ID of the modal to open.
 */
export function openModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;

    modal.classList.add('open');

    if (id === 'createLicenseModal') loadLicenseProducts();

    if (id === 'editProfileModal') {
        const group = document.getElementById('profile2faGroup');
        const codeInput = document.getElementById('edit2faCode');
        const is2FA = !!state.account?.totpEnabled;
        if (group) group.classList.toggle('hidden', !is2FA);
        if (codeInput) codeInput.required = is2FA;
    }
}

/**
 * Closes a modal with the specified ID. If no ID is provided, it closes all open modals.
 * 
 * @param {*} id - The ID of the modal to close. If null, all modals will be closed.
 */
export function closeModal(id = null) {
    if (id) {
        document.getElementById(id)?.classList.remove('open');
        return;
    }
    document.querySelectorAll('.modal-backdrop').forEach((m) => m.classList.remove('open'));
}
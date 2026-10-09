import { api } from '../api/client.js';
import { state } from '../state.js';
import { toast } from '../ui/toast.js';
import { closeModal } from '../ui/modal.js';
import { escapeHTML, formatDate, copyToClipboard } from '../ui/dom.js';

export async function loadLicenses() {
    const tbody = document.getElementById('licensesTableBody');
    if (!tbody) return;

    try {
        await loadLicenseProducts();
        const licenses = await api('/accounts/licenses');

        if (!licenses || !licenses.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-secondary text-center">No licenses found.</td></tr>';
            return;
        }

        tbody.innerHTML = licenses.map((l) => {
            const status = String(l.status || l.state || 'UNKNOWN').toUpperCase();
            const isRevoked = status === 'REVOKED';
            const badgeClass = status === 'ACTIVE' ? 'active' : (isRevoked ? 'danger' : '');
            const key = l.licenseKey || l.key || '';

            return `
                <tr>
                    <td>
                        <strong>${escapeHTML(l.name || l.customerName || '—')}</strong><br>
                        <span class="text-secondary text-sm">${escapeHTML(l.productId || l.product_id || '—')}</span>
                    </td>
                    <td>
                        <span class="text-sm">
                            <i class="fa-solid fa-arrow-right-to-bracket text-success" style="margin-right:4px;"></i> ${formatDate(l.createdAt || l.created_at).split(',')[0]}<br>
                            <i class="fa-solid fa-arrow-right-from-bracket text-danger" style="margin-right:4px;"></i> ${formatDate(l.expiresAt || l.expires_at).split(',')[0]}
                        </span>
                    </td>
                    <td>
                        <code class="font-mono text-sm" style="background: var(--surface-glass); padding: 0.3rem 0.5rem; border-radius: 6px; border: 1px solid var(--border-color);">
                            ${escapeHTML(key || '—')}
                        </code>
                    </td>
                    <td><span class="badge ${badgeClass}">${escapeHTML(status)}</span></td>
                    <td>
                        <div style="display: flex; gap: 0.5rem;">
                            ${!isRevoked ? `
                                <button class="icon-btn copy-key-btn" data-key="${escapeHTML(key)}" title="Copy Key">
                                    <i class="fa-regular fa-copy"></i>
                                </button>
                                <button class="icon-btn revoke-key-btn" data-key="${escapeHTML(key)}" style="color:var(--danger-color)" title="Revoke">
                                    <i class="fa-solid fa-trash"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        tbody.querySelectorAll('.copy-key-btn').forEach((btn) => {
            btn.addEventListener('click', () => copyToClipboard(btn.dataset.key));
        });

        tbody.querySelectorAll('.revoke-key-btn').forEach((btn) => {
            btn.addEventListener('click', () => revokeLicense(btn.dataset.key));
        });
    } catch {
        toast('Failed to load licenses', 'error');
    }
}

export async function loadLicenseProducts() {
    const select = document.getElementById('licenseProductSelect');
    if (!select) return;

    select.innerHTML = '<option value="">Loading products...</option>';
    try {
        const products = await api('/accounts/license-products');
        state.licenseProducts = Array.isArray(products) ? products : [];
        select.innerHTML = state.licenseProducts.length
            ? state.licenseProducts.map((p) => `<option value="${escapeHTML(p.id)}">${escapeHTML(p.name || p.id)}</option>`).join('')
            : '<option value="">No products available</option>';
    } catch (error) {
        state.licenseProducts = [];
        select.innerHTML = '<option value="">Unable to load products</option>';
        toast(error.message || 'Failed to load license products', 'error');
    }
}

export async function createLicense(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const payload = { name: fd.get('name'), product_id: fd.get('product_id') };
    if (fd.get('duration_days')) payload.duration_days = Number(fd.get('duration_days'));

    try {
        await api('/accounts/licenses', { method: 'POST', body: JSON.stringify(payload) });
        closeModal(null);
        e.target.reset();
        toast('License created', 'success');
        loadLicenses();
    } catch (err) {
        toast(err.message, 'error');
    }
}

export async function revokeLicense(key) {
    if (!confirm('Revoke this license?')) return;
    try {
        await api('/accounts/licenses/revoke', { method: 'POST', body: JSON.stringify({ license_key: key }) });
        toast('License revoked', 'success');
        loadLicenses();
    } catch (err) {
        toast(err.message, 'error');
    }
}
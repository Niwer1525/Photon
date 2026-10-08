import { escapeHTML } from './dom.js';

/**
 * Displays a toast notification with the specified message and type.
 * 
 * @param {*} msg The message to display in the toast notification. 
 * @param {*} type The type of toast notification. Can be 'success', 'error', or 'info'. Defaults to 'info'.
 * @returns {void}
 */
export function toast(msg, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const el = document.createElement('div');
    el.className = `toast ${type}`;
    const icon = type === 'success' ? 'check-circle' : type === 'error' ? 'circle-exclamation' : 'circle-info';
    
    el.innerHTML = `<i class="fa-solid fa-${icon}"></i> ${escapeHTML(msg)}`;
    container.appendChild(el);

    setTimeout(() => {
        el.style.opacity = '0';
        setTimeout(() => el.remove(), 300);
    }, 3000);
}
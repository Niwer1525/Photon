import { toast } from './toast.js';

export function escapeHTML(str) {
    return String(str || '').replace(/[&<>"']/g, (m) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m]));
}

/**
 * Formats a value for display in the UI.
 * If the value is null or empty, it returns a dash.
 * If the value is an object, it returns its JSON string representation.
 * Otherwise, it converts the value to a string.
 * 
 * @param {*} v - The value to format.
 * @returns {string} - The formatted value.
 */
export function formatVal(v) {
    if (v == null || v === '') return '—';
    return typeof v === 'object' ? JSON.stringify(v) : String(v);
}

/**
 * Formats a date value for display in the UI.
 * 
 * @param {*} v - The date value to format. 
 * @returns {string} - The formatted date string or a dash if the value is invalid.
 */
export function formatDate(v) {
    if (!v) return '—';
    const d = new Date(v);
    return isNaN(d.getTime()) ? v : d.toLocaleString();
}

/**
 * Copies the provided text to the clipboard and shows a toast notification indicating success or failure.
 * 
 * @param {*} text - The text to copy to the clipboard.
 */
export async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        toast('Copied to clipboard', 'success');
    } catch {
        toast('Failed to copy', 'error');
    }
}
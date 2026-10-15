import { api } from '../api/client.js';
import { toast } from '../ui/toast.js';
import { escapeHTML, formatVal } from '../ui/dom.js';

export async function loadTablesList() {
    const sel = document.getElementById('tableSelector');
    if (!sel) return;

    try {
        const tables = await api('/api/admin/tables');
        if (!tables || !tables.length) {
            sel.innerHTML = '<option value="">No tables available</option>';
            document.getElementById('dataTableBody').innerHTML = '<tr><td class="text-secondary">No tables found on server.</td></tr>';
            return;
        }

        sel.innerHTML = tables.map((t) => `<option value="${escapeHTML(t.table)}">${escapeHTML(t.label)}</option>`).join('');
        loadTableData();
    } catch {
        sel.innerHTML = '<option value="">Error loading tables</option>';
        toast('Failed to load tables list (API Error)', 'error');
    }
}

export async function executeAdminSql() {
    const input = document.getElementById('adminSql');
    const output = document.getElementById('adminSqlResult');
    if (!input || !output || !input.value.trim()) return;
    if (!window.confirm('Run this restricted SQL? UPDATE statements are backed up first.')) return;
    try {
        const result = await api('/api/admin/sql', { method: 'POST', body: JSON.stringify({ sql: input.value }) });
        output.textContent = JSON.stringify(result, null, 2);
    } catch (error) {
        output.textContent = error.message;
        toast(error.message, 'error');
    }
}

export async function loadTableData() {
    const table = document.getElementById('tableSelector')?.value;
    const limit = document.getElementById('tableLimit')?.value || 100;
    const head = document.getElementById('dataTableHead');
    const body = document.getElementById('dataTableBody');
    if (!table || !head || !body) return;

    try {
        const data = await api(`/api/admin/tables/${encodeURIComponent(table)}?limit=${limit}`);

        if (!data.columns || !data.rows.length) {
            head.innerHTML = '<tr><th>Notice</th></tr>';
            body.innerHTML = '<tr><td class="text-secondary">No rows found in this table.</td></tr>';
            return;
        }

        head.innerHTML = `<tr>${data.columns.map((c) => `<th>${escapeHTML(c)}</th>`).join('')}</tr>`;
        body.innerHTML = data.rows.map((row) =>
            `<tr>${data.columns.map((c) => `<td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis;" title="${escapeHTML(formatVal(row[c]))}">${escapeHTML(formatVal(row[c]))}</td>`).join('')}</tr>`
        ).join('');
    } catch {
        toast('Failed to load table data', 'error');
        body.innerHTML = '<tr><td class="text-danger">Failed to fetch data.</td></tr>';
    }
}
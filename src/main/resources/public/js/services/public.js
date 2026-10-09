import { api } from '../api/client.js';
import { toast } from '../ui/toast.js';
import { escapeHTML, formatVal, copyToClipboard } from '../ui/dom.js';

export async function loadPublicServers() {
    const grid = document.getElementById('serverGrid');
    const countEl = document.getElementById('serverCount');
    if (!grid) return;

    try {
        const servers = await api('/api/status/servers');
        if (countEl) countEl.innerHTML = `${servers.length} <i class="fa-solid fa-globe"></i>`;

        if (!servers.length) {
            grid.innerHTML = '<p class="text-secondary" style="grid-column: 1/-1;">No servers online.</p>';
            return;
        }

        grid.innerHTML = servers.map((s) => `
            <div class="card">
                <div class="card-header">
                    <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${escapeHTML(s.serverName || 'Unknown')}">
                        ${escapeHTML(s.serverName || 'Unknown Server')}
                    </span>
                </div>
                <div class="card-body">
                    <p style="margin-bottom:0">${escapeHTML(s.serverMOTD || 'No MOTD provided')}</p>
                </div>
                <div class="card-footer">
                    <button class="btn icon-btn copy-ip-btn" data-ip="${s.serverIP}:${s.serverPort}" title="Copy IP">
                        <i class="fa-regular fa-copy"></i>
                    </button>
                    ${s.site ? `<a href="${escapeHTML(s.site)}" target="_blank" rel="noopener" class="btn icon-btn"><i class="fa-solid fa-link"></i></a>` : ''}
                    ${s.discord ? `<a href="${escapeHTML(s.discord)}" target="_blank" rel="noopener" class="btn icon-btn"><i class="fa-brands fa-discord"></i></a>` : ''}
                </div>
            </div>
        `).join('');

        grid.querySelectorAll('.copy-ip-btn').forEach((btn) => {
            btn.addEventListener('click', () => copyToClipboard(btn.dataset.ip));
        });
    } catch {
        grid.innerHTML = `<p class="text-secondary text-danger" style="grid-column: 1/-1;">Failed to load servers.</p>`;
    }
}

export async function loadDownloads() {
    const grid = document.getElementById('downloadsGrid');
    if (!grid) return;

    try {
        const repositories = await api('/download/list');
        const sections = Object.entries(repositories || {}).map(([repository, data]) => {
            const releases = (data && data.releases) || [];
            const tags = (data && data.tags) || [];
            const tagMap = new Map(tags.map((t) => [t.tag || t.name, t]));

            const rows = releases.flatMap((release) => {
                const tagName = release.tagName || release.tag_name;
                const associatedTag = tagMap.get(tagName);
                const tagMessage = associatedTag ? associatedTag.message || '' : '';

                return (release.assets || [])
                    .filter((asset) => asset.name)
                    .map((asset) => `
                        <tr>
                            <td>${escapeHTML(release.name || tagName || 'Unreleased')}</td>
                            <td><span class="font-mono text-sm">${escapeHTML(release.prerelease ? 'Yes' : 'No')}</span></td>
                            <td><span class="font-mono text-sm">${escapeHTML(asset.name)}</span></td>
                            <td><span class="font-mono text-sm">${escapeHTML(tagMessage || release.body || asset.body || '-')}</span></td>
                            <td class="download-action">
                                <a class="btn primary icon-btn" title="Download" href="/download?product=${encodeURIComponent(repository)}&assetName=${encodeURIComponent(asset.name || 'Unknown')}&assetId=${asset.id}" target="_blank">
                                    <i class="fa-solid fa-cloud-arrow-down"></i>
                                </a>
                            </td>
                        </tr>
                    `);
            }).join('');

            return `
                <section class="download-repository">
                    <div class="download-repository-header">
                        <h3><i class="fa-solid fa-code-branch text-accent"></i> ${escapeHTML(repository)}</h3>
                    </div>
                    <div class="table-container">
                        <table>
                            <thead><tr>
                                <th>Release</th>
                                <th>Pre-Release</th>
                                <th>Asset</th>
                                <th>Description</th>
                                <th>Download</th>
                            </tr></thead>
                            <tbody>${rows || '<tr><td colspan="5" class="text-secondary">No downloadable assets found.</td></tr>'}</tbody>
                        </table>
                    </div>
                </section>
            `;
        });

        grid.innerHTML = sections.length ? sections.join('') : '<p class="text-secondary">No downloadable assets found.</p>';
    } catch {
        grid.innerHTML = '<p class="text-secondary text-danger">Failed to load downloads.</p>';
    }
}

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
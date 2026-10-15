import { api } from '../api/client.js';
import { toast } from '../ui/toast.js';
import { escapeHTML } from '../ui/dom.js';

/**
 * This will make the first letter of each word in the productId uppercase and replace underscores with spaces.
 * 
 * @param {*} productId The productId to format.
 */
function formatProductId(productId) {
    if (!productId) return 'Unknown';
    return productId
        .replace(/_/g, ' ')
        .split(' ')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

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

        // Group servers by productId
        const groupedServers = (servers || []).reduce((acc, server) => {
            console.log('Server:', server);
            const key = formatProductId(server.productId || server.projectId) || 'Other / Community';
            if (!acc[key]) acc[key] = [];
            acc[key].push(server);
            return acc;
        }, {});

        // Generate HTML section for each productId
        const sections = Object.entries(groupedServers).map(([productId, productServers]) => {
            const cards = productServers.map((s) => `
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
                        ${s.site ? `<a href="${escapeHTML(s.site)}" target="_blank" rel="noopener" class="btn icon-btn" title="Website"><i class="fa-solid fa-link"></i></a>` : ''}
                        ${s.discord ? `<a href="${escapeHTML(s.discord)}" target="_blank" rel="noopener" class="btn icon-btn" title="Discord"><i class="fa-brands fa-discord"></i></a>` : ''}
                    </div>
                </div>
            `).join('');

            return `
                <section class="server-group">
                    <div class="server-group-header">
                        <h3><i class="fa-solid fa-server text-accent"></i> ${escapeHTML(productId)}</h3>
                    </div>
                    <div class="server-cards-grid">
                        ${cards}
                    </div>
                </section>
            `;
        });

        grid.innerHTML = sections.length 
            ? sections.join('') 
            : '<p class="text-secondary">No servers found.</p>';
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
(function () {
    'use strict';

    const STORAGE_KEY = 'short_url_session';

    // ── Session storage ──

    function loadSessionLinks() {
        try {
            const raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function saveSessionLinks(links) {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(links));
    }

    function addSessionLink(link) {
        const links = loadSessionLinks();
        const idx = links.findIndex(l => l.id === link.id);
        if (idx >= 0) links[idx] = link;
        else links.unshift(link);
        saveSessionLinks(links);
    }

    function removeSessionLink(id) {
        const links = loadSessionLinks().filter(l => l.id !== id);
        saveSessionLinks(links);
    }

    // ── Shareable link ──

    // Builds the full, copy-ready short URL for an id.
    // Uses the current origin so it works on any deployment (dev/prod).
    function buildShortUrl(id) {
        return window.location.origin + '/api/v0/short-url/' + encodeURIComponent(id);
    }

    // ── Shorten ──

    async function shortenUrl() {
        const urlInput = document.getElementById('targetUrl');
        const nameInput = document.getElementById('linkName');
        const descInput = document.getElementById('linkDescription');

        const url = urlInput.value.trim();
        if (!url) {
            showStatus('Please enter a url to shorten', true);
            return;
        }

        const payload = { url: url };
        const name = nameInput.value.trim();
        const description = descInput.value.trim();
        if (name) payload.name = name;
        if (description) payload.description = description;

        try {
            showStatus('Shortening...');
            const resp = await fetch('/api/v0/short-url', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!resp.ok) {
                const err = await resp.text();
                throw new Error(err || 'Shorten failed: ' + resp.status);
            }

            const data = await resp.json();

            const record = {
                id: data.id,
                token: data.token,
                target: url,
                name: name || '',
                description: description || ''
            };
            addSessionLink(record);

            // Clear form
            urlInput.value = '';
            nameInput.value = '';
            descInput.value = '';

            showStatus('Short url created!');
            openTokenModal(record);
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Token modal ──

    function openTokenModal(record) {
        document.getElementById('modalLink').textContent = buildShortUrl(record.id);
        document.getElementById('modalId').textContent = record.id;
        document.getElementById('modalToken').textContent = record.token;
        document.getElementById('modalTarget').textContent = record.target;
        document.getElementById('modalName').textContent = record.name || '—';
        document.getElementById('modalCopyFeedback').textContent = '';
        document.getElementById('tokenModal').style.display = 'flex';
    }

    function closeTokenModal() {
        document.getElementById('tokenModal').style.display = 'none';
    }

    function copyModalLink() {
        const link = document.getElementById('modalLink').textContent;
        copyWithFeedback(link, 'Short link copied!');
    }

    function copyCredentials() {
        const id = document.getElementById('modalId').textContent;
        const token = document.getElementById('modalToken').textContent;
        const link = document.getElementById('modalLink').textContent;
        const text = 'Short link: ' + link + '\nID: ' + id + '\nToken: ' + token;
        copyWithFeedback(text, 'Credentials copied!');
    }

    function copyWithFeedback(text, okMsg) {
        const fb = document.getElementById('modalCopyFeedback');
        copyToClipboard(text).then(() => {
            fb.textContent = okMsg;
            fb.style.color = '#8f8';
            setTimeout(() => { fb.textContent = ''; }, 2500);
        }).catch(() => {
            fb.textContent = 'Could not copy. Select and copy manually.';
            fb.style.color = '#faa';
        });
    }

    // ── Clipboard ──

    function copyToClipboard(text) {
        return new Promise((resolve, reject) => {
            if (navigator.clipboard && window.isSecureContext) {
                navigator.clipboard.writeText(text).then(resolve).catch(() => fallbackCopy(text, resolve, reject));
            } else {
                fallbackCopy(text, resolve, reject);
            }
        });
    }

    function fallbackCopy(text, resolve, reject) {
        try {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.left = '-9999px';
            document.body.appendChild(ta);
            ta.select();
            const ok = document.execCommand('copy');
            document.body.removeChild(ta);
            ok ? resolve() : reject(new Error('execCommand failed'));
        } catch (err) {
            reject(err);
        }
    }

    // ── My Links tab ──

    function renderLinksList() {
        const links = loadSessionLinks();
        const container = document.getElementById('linksList');

        if (links.length === 0) {
            container.innerHTML = '<div class="empty-state">No links created yet in this session.</div>';
            return;
        }

        container.innerHTML = links.map(l => {
            const title = l.name ? escapeHtml(l.name) : escapeHtml(l.id);
            return `
            <div class="file-card">
                <div class="file-card-header">
                    <span class="file-card-name">${title}</span>
                </div>
                <div class="file-card-meta">
                    <span>ID: ${escapeHtml(l.id)}</span>
                </div>
                <div class="file-card-meta">
                    <span>→ ${escapeHtml(l.target)}</span>
                </div>
                <div class="file-card-actions">
                    <button onclick="openWithId('${escapeJs(l.id)}')">Open</button>
                    <button onclick="copyLinkWithId('${escapeJs(l.id)}')">Copy link</button>
                    <button class="danger" onclick="deleteWithCreds('${escapeJs(l.id)}','${escapeJs(l.token)}')">Delete</button>
                </div>
            </div>`;
        }).join('');
    }

    function openWithId(id) {
        window.open(buildShortUrl(id), '_blank');
    }

    function copyLinkWithId(id) {
        copyToClipboard(buildShortUrl(id))
            .then(() => showStatus('Short link copied!'))
            .catch(() => showStatus('Could not copy link', true));
    }

    async function deleteWithCreds(id, token) {
        if (!confirm('Delete this short url permanently?')) return;
        await doDelete(id, token, () => {
            removeSessionLink(id);
            renderLinksList();
        });
    }

    // ── Manage tab ──

    function getManageParams(requireToken) {
        const id = document.getElementById('manageId').value.trim();
        const token = document.getElementById('manageToken').value.trim();
        if (!id) {
            showStatus('Short id is required', true);
            return null;
        }
        if (requireToken && !token) {
            showStatus('Delete token is required', true);
            return null;
        }
        return { id, token };
    }

    function openShort() {
        const p = getManageParams(false);
        if (!p) return;
        window.open(buildShortUrl(p.id), '_blank');
        showStatus('Opening short link...');
    }

    function copyShortLink() {
        const p = getManageParams(false);
        if (!p) return;
        copyToClipboard(buildShortUrl(p.id))
            .then(() => showStatus('Short link copied!'))
            .catch(() => showStatus('Could not copy link', true));
    }

    async function deleteShort() {
        const p = getManageParams(true);
        if (!p) return;
        if (!confirm('Delete this short url permanently?')) return;
        await doDelete(p.id, p.token, () => {
            removeSessionLink(p.id);
            document.getElementById('manageId').value = '';
            document.getElementById('manageToken').value = '';
            renderLinksList();
        });
    }

    // ── Shared delete ──

    async function doDelete(id, token, onSuccess) {
        try {
            showStatus('Deleting...');
            const resp = await fetch('/api/v0/short-url/' + encodeURIComponent(id)
                + '?token=' + encodeURIComponent(token), { method: 'DELETE' });
            if (!resp.ok && resp.status !== 204) {
                if (resp.status === 403) throw new Error('Invalid token');
                if (resp.status === 404) throw new Error('Short url not found');
                const err = await resp.text();
                throw new Error(err || 'Delete failed: ' + resp.status);
            }
            if (onSuccess) onSuccess();
            showStatus('Short url deleted');
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Tabs ──

    function switchTab(idx) {
        document.querySelectorAll('.tab-btn').forEach((btn, i) => btn.classList.toggle('active', i === idx));
        document.getElementById('shortenTab').style.display = idx === 0 ? 'block' : 'none';
        document.getElementById('linksTab').style.display = idx === 1 ? 'block' : 'none';
        document.getElementById('manageTab').style.display = idx === 2 ? 'block' : 'none';
        document.getElementById('status').textContent = '';
        if (idx === 1) renderLinksList();
    }

    // ── Status ──

    let statusTimer;
    function showStatus(msg, isError) {
        const el = document.getElementById('status');
        el.textContent = msg;
        el.className = 'status-line' + (isError ? ' err' : msg && !msg.endsWith('...') ? ' ok' : '');
        clearTimeout(statusTimer);
        if (msg && !isError) {
            statusTimer = setTimeout(() => { el.textContent = ''; el.className = 'status-line'; }, 5000);
        }
    }

    // ── Helpers ──

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : text;
        return div.innerHTML;
    }

    // Escapes a value for safe embedding inside a single-quoted JS string in inline handlers.
    function escapeJs(text) {
        return String(text == null ? '' : text)
            .replace(/\\/g, '\\\\')
            .replace(/'/g, "\\'")
            .replace(/"/g, '\\"');
    }

    // ── Expose public API ──
    window.switchTab = switchTab;
    window.shortenUrl = shortenUrl;
    window.copyModalLink = copyModalLink;
    window.copyCredentials = copyCredentials;
    window.closeTokenModal = closeTokenModal;
    window.openShort = openShort;
    window.copyShortLink = copyShortLink;
    window.deleteShort = deleteShort;
    window.openWithId = openWithId;
    window.copyLinkWithId = copyLinkWithId;
    window.deleteWithCreds = deleteWithCreds;

    // ── Modal overlay click ──
    document.addEventListener('click', function (e) {
        if (e.target.id === 'tokenModal') closeTokenModal();
    });

    // ── Init ──
    window.addEventListener('DOMContentLoaded', function () {
        switchTab(0);
    });
})();

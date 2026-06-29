(function () {
    'use strict';

    const API_BASE = '/api/v0/repository/links';
    const STORAGE_KEY = 'link_repository_session';

    // Currently opened repository: { repositoryId, token, name, createdAt }
    let currentRepo = null;
    // Cache of the links last loaded for the open repo (used to populate the edit form).
    let currentLinks = [];

    // ── Session persistence ──

    function loadSession() {
        try {
            const raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function saveSession(repo) {
        if (repo) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(repo));
        else sessionStorage.removeItem(STORAGE_KEY);
    }

    // ── Share link ──

    // Builds a copy-ready URL that re-opens this repository automatically.
    function buildShareLink(repo) {
        return window.location.origin + '/static/v0/html/links.html'
            + '?repositoryId=' + encodeURIComponent(repo.repositoryId)
            + '&token=' + encodeURIComponent(repo.token);
    }

    // ── Create repository ──

    async function createRepository() {
        const nameInput = document.getElementById('createName');
        const name = nameInput.value.trim();

        try {
            showStatus('Creating repository...');
            const resp = await fetch(API_BASE + '/', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(name ? { name: name } : {})
            });

            if (!resp.ok) {
                const err = await resp.text();
                throw new Error(err || 'Create failed: ' + resp.status);
            }

            const data = await resp.json();
            currentRepo = {
                repositoryId: data.repositoryId,
                token: data.token,
                name: data.name,
                createdAt: data.createdAt
            };
            currentLinks = [];
            saveSession(currentRepo);

            nameInput.value = '';
            openModal(currentRepo);
            showStatus('Repository created');
            renderRepo();
            switchTab(2);
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Open repository ──

    async function openRepository() {
        const id = document.getElementById('openRepoId').value.trim();
        const token = document.getElementById('openToken').value.trim();
        if (!id) { showStatus('Repository id is required', true); return; }
        if (!token) { showStatus('Access token is required', true); return; }
        await loadRepository(id, token, true);
    }

    // Fetches the repository, and on success stores it as current and shows the links tab.
    async function loadRepository(id, token, switchToLinks) {
        try {
            showStatus('Opening repository...');
            const data = await fetchRepository(id, token);

            currentRepo = {
                repositoryId: data.repositoryId,
                token: token,
                name: data.name,
                createdAt: data.createdAt
            };
            currentLinks = data.links || [];
            saveSession(currentRepo);

            renderRepo();
            if (switchToLinks) switchTab(2);
            showStatus('Repository opened');
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // Performs the GET and returns the parsed repository, throwing a friendly error otherwise.
    async function fetchRepository(id, token) {
        const resp = await fetch(API_BASE + '/' + encodeURIComponent(id)
            + '?token=' + encodeURIComponent(token));
        if (!resp.ok) {
            if (resp.status === 404) throw new Error('Repository not found or token is invalid');
            if (resp.status === 400) throw new Error('Repository id or token missing');
            const err = await resp.text();
            throw new Error(err || 'Open failed: ' + resp.status);
        }
        return resp.json();
    }

    // Re-fetches links for the currently open repo without changing tabs.
    async function reloadLinks() {
        if (!currentRepo) return;
        try {
            showStatus('Refreshing...');
            const data = await fetchRepository(currentRepo.repositoryId, currentRepo.token);
            currentRepo.name = data.name;
            currentRepo.createdAt = data.createdAt;
            currentLinks = data.links || [];
            saveSession(currentRepo);
            renderRepo();
            showStatus('Links refreshed');
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    function closeRepository() {
        currentRepo = null;
        currentLinks = [];
        saveSession(null);
        cancelEdit();
        renderRepo();
        showStatus('Repository closed');
        switchTab(0);
    }

    async function deleteRepository() {
        if (!currentRepo) return;
        if (!confirm('Delete this repository and all its links permanently?')) return;
        try {
            showStatus('Deleting repository...');
            const resp = await fetch(API_BASE + '/' + encodeURIComponent(currentRepo.repositoryId)
                + '?token=' + encodeURIComponent(currentRepo.token), { method: 'DELETE' });
            if (!resp.ok && resp.status !== 204) {
                if (resp.status === 404) throw new Error('Repository not found or token is invalid');
                const err = await resp.text();
                throw new Error(err || 'Delete failed: ' + resp.status);
            }
            currentRepo = null;
            currentLinks = [];
            saveSession(null);
            cancelEdit();
            renderRepo();
            showStatus('Repository deleted');
            switchTab(0);
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Add / update link ──

    async function submitLink() {
        if (!currentRepo) { showStatus('No repository open', true); return; }

        const url = document.getElementById('linkUrl').value.trim();
        const name = document.getElementById('linkName').value.trim();
        const description = document.getElementById('linkDescription').value.trim();
        const editingUuid = document.getElementById('editingUuid').value;

        if (!url) { showStatus('Link url is required', true); return; }

        const payload = { url: url };
        if (name) payload.name = name;
        if (description) payload.description = description;

        try {
            let resp;
            if (editingUuid) {
                showStatus('Updating link...');
                resp = await fetch(linkUrlFor(editingUuid), {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            } else {
                showStatus('Adding link...');
                resp = await fetch(API_BASE + '/' + encodeURIComponent(currentRepo.repositoryId)
                    + '/links?token=' + encodeURIComponent(currentRepo.token), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            }

            if (!resp.ok && resp.status !== 204) {
                if (resp.status === 404) throw new Error('Repository or link not found, or token is invalid');
                if (resp.status === 400) throw new Error('Link url is required');
                const err = await resp.text();
                throw new Error(err || 'Save failed: ' + resp.status);
            }

            cancelEdit();
            showStatus(editingUuid ? 'Link updated' : 'Link added');
            await reloadLinks();
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // Builds the URL for a single link of the current repo.
    function linkUrlFor(uuid) {
        return API_BASE + '/' + encodeURIComponent(currentRepo.repositoryId)
            + '/links/' + encodeURIComponent(uuid)
            + '?token=' + encodeURIComponent(currentRepo.token);
    }

    function startEdit(uuid) {
        const link = currentLinks.find(l => l.uuid === uuid);
        if (!link) return;
        document.getElementById('editingUuid').value = uuid;
        document.getElementById('linkUrl').value = link.url || '';
        document.getElementById('linkName').value = link.name || '';
        document.getElementById('linkDescription').value = link.description || '';
        document.getElementById('submitLinkBtn').textContent = 'update link';
        document.getElementById('cancelEditBtn').style.display = 'inline-block';
        document.getElementById('linkUrl').focus();
    }

    function cancelEdit() {
        document.getElementById('editingUuid').value = '';
        document.getElementById('linkUrl').value = '';
        document.getElementById('linkName').value = '';
        document.getElementById('linkDescription').value = '';
        document.getElementById('submitLinkBtn').textContent = 'add link';
        document.getElementById('cancelEditBtn').style.display = 'none';
    }

    async function deleteLink(uuid) {
        if (!currentRepo) return;
        if (!confirm('Delete this link permanently?')) return;
        try {
            showStatus('Deleting link...');
            const resp = await fetch(linkUrlFor(uuid), { method: 'DELETE' });
            if (!resp.ok && resp.status !== 204) {
                if (resp.status === 404) throw new Error('Link not found or token is invalid');
                const err = await resp.text();
                throw new Error(err || 'Delete failed: ' + resp.status);
            }
            // If we were editing the deleted link, reset the form.
            if (document.getElementById('editingUuid').value === uuid) cancelEdit();
            showStatus('Link deleted');
            await reloadLinks();
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Rendering ──

    // Renders the links tab: either the "no repo" state or the open repo with its banner and list.
    function renderRepo() {
        const noRepo = document.getElementById('noRepoState');
        const content = document.getElementById('repoContent');

        if (!currentRepo) {
            noRepo.style.display = 'block';
            content.style.display = 'none';
            return;
        }

        noRepo.style.display = 'none';
        content.style.display = 'block';

        document.getElementById('repoName').textContent = currentRepo.name || currentRepo.repositoryId;
        const created = currentRepo.createdAt ? ' • created ' + currentRepo.createdAt : '';
        document.getElementById('repoMeta').textContent = 'id: ' + currentRepo.repositoryId + created;

        renderLinksList();
    }

    function renderLinksList() {
        const container = document.getElementById('linksList');
        if (!currentLinks || currentLinks.length === 0) {
            container.innerHTML = '<div class="empty-state">no links yet</div>';
            return;
        }

        container.innerHTML = currentLinks.map(l => {
            const title = l.name ? escapeHtml(l.name) : escapeHtml(l.url);
            const url = escapeHtml(l.url);
            const desc = l.description
                ? `<div class="file-card-desc">${escapeHtml(l.description)}</div>` : '';
            return `
            <div class="file-card">
                <div class="file-card-name">${title}</div>
                <div class="file-card-meta">
                    <a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>
                </div>
                ${desc}
                <div class="file-card-actions">
                    <button onclick="openLink('${escapeJs(l.url)}')">Open</button>
                    <button onclick="copyLink('${escapeJs(l.url)}')">Copy</button>
                    <button onclick="startEdit('${escapeJs(l.uuid)}')">Edit</button>
                    <button class="danger" onclick="deleteLink('${escapeJs(l.uuid)}')">Delete</button>
                </div>
            </div>`;
        }).join('');
    }

    function openLink(url) {
        window.open(url, '_blank', 'noopener');
    }

    function copyLink(url) {
        copyToClipboard(url)
            .then(() => showStatus('Link copied'))
            .catch(() => showStatus('Could not copy link', true));
    }

    // ── Create modal ──

    function openModal(repo) {
        document.getElementById('modalRepoId').textContent = repo.repositoryId;
        document.getElementById('modalToken').textContent = repo.token;
        document.getElementById('modalName').textContent = repo.name || '—';
        document.getElementById('modalCreated').textContent = repo.createdAt || '—';
        document.getElementById('modalFeedback').textContent = '';
        document.getElementById('repoModal').style.display = 'flex';
    }

    function closeModal() {
        document.getElementById('repoModal').style.display = 'none';
    }

    function copyShareLink() {
        if (!currentRepo) return;
        copyWithFeedback(buildShareLink(currentRepo), 'Share link copied!');
    }

    function copyCredentials() {
        if (!currentRepo) return;
        const text = 'Repository ID: ' + currentRepo.repositoryId
            + '\nToken: ' + currentRepo.token
            + '\nShare link: ' + buildShareLink(currentRepo);
        copyWithFeedback(text, 'Credentials copied!');
    }

    // Copies text and reports the result into the modal feedback line if visible, else the status line.
    function copyWithFeedback(text, okMsg) {
        const modalOpen = document.getElementById('repoModal').style.display === 'flex';
        copyToClipboard(text).then(() => {
            if (modalOpen) {
                const fb = document.getElementById('modalFeedback');
                fb.textContent = okMsg;
                fb.style.color = '#80b380';
                setTimeout(() => { fb.textContent = ''; }, 2500);
            } else {
                showStatus(okMsg);
            }
        }).catch(() => {
            if (modalOpen) {
                const fb = document.getElementById('modalFeedback');
                fb.textContent = 'Could not copy. Select and copy manually.';
                fb.style.color = '#cc8080';
            } else {
                showStatus('Could not copy', true);
            }
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

    // ── Tabs ──

    function switchTab(idx) {
        document.querySelectorAll('.tab-btn').forEach((btn, i) => btn.classList.toggle('active', i === idx));
        document.getElementById('openTab').style.display = idx === 0 ? 'block' : 'none';
        document.getElementById('createTab').style.display = idx === 1 ? 'block' : 'none';
        document.getElementById('linksTab').style.display = idx === 2 ? 'block' : 'none';
        document.getElementById('status').textContent = '';
        document.getElementById('status').className = 'status-line';
        if (idx === 2) renderRepo();
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

    function getUrlParam(name) {
        return new URLSearchParams(window.location.search).get(name) || '';
    }

    // ── Expose public API (inline handlers) ──
    window.switchTab = switchTab;
    window.createRepository = createRepository;
    window.openRepository = openRepository;
    window.reloadLinks = reloadLinks;
    window.closeRepository = closeRepository;
    window.deleteRepository = deleteRepository;
    window.submitLink = submitLink;
    window.startEdit = startEdit;
    window.cancelEdit = cancelEdit;
    window.deleteLink = deleteLink;
    window.openLink = openLink;
    window.copyLink = copyLink;
    window.copyShareLink = copyShareLink;
    window.copyCredentials = copyCredentials;
    window.closeModal = closeModal;

    // ── Modal overlay click closes ──
    document.addEventListener('click', function (e) {
        if (e.target.id === 'repoModal') closeModal();
    });

    // ── Init ──
    window.addEventListener('DOMContentLoaded', function () {
        switchTab(0);

        // Prefer credentials from the URL (share link), otherwise restore the session.
        const urlId = getUrlParam('repositoryId');
        const urlToken = getUrlParam('token') || getUrlParam('auth');
        if (urlId && urlToken) {
            document.getElementById('openRepoId').value = urlId;
            document.getElementById('openToken').value = urlToken;
            loadRepository(urlId, urlToken, true);
            return;
        }

        const saved = loadSession();
        if (saved && saved.repositoryId && saved.token) {
            loadRepository(saved.repositoryId, saved.token, false);
        }
    });
})();

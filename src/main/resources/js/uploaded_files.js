(function () {
    'use strict';

    const STORAGE_KEY = 'uploaded_files_session';

    function loadSessionFiles() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch (e) {
        return [];
    }
}

function saveSessionFiles(files) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(files));
}

function addSessionFile(file) {
    const files = loadSessionFiles();
    // replace if same uuid
    const idx = files.findIndex(f => f.uuid === file.uuid);
    if (idx >= 0) files[idx] = file;
    else files.unshift(file);
    saveSessionFiles(files);
}

function removeSessionFile(uuid) {
    const files = loadSessionFiles().filter(f => f.uuid !== uuid);
    saveSessionFiles(files);
}

// ── File selection & drag-and-drop ──

function onFileSelected() {
    const input = document.getElementById('fileInput');
    const display = document.getElementById('fileNameDisplay');
    if (input.files && input.files.length > 0) {
        display.textContent = input.files[0].name + ' (' + formatSize(input.files[0].size) + ')';
        document.getElementById('dropZoneText').style.display = 'none';
    } else {
        display.textContent = '';
        document.getElementById('dropZoneText').style.display = '';
    }
}

(function initDropZone() {
    const zone = document.getElementById('dropZone');
    if (!zone) return;
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragover'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('dragover');
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            document.getElementById('fileInput').files = files;
            onFileSelected();
        }
    });
})();

// ── Formatting ──

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function formatExpiry(epochMs) {
    if (!epochMs || epochMs <= 0) return 'Never';
    const diff = epochMs - Date.now();
    if (diff <= 0) return 'Expired';
    const secs = Math.floor(diff / 1000);
    if (secs < 60) return secs + 's';
    if (secs < 3600) return Math.floor(secs / 60) + 'm ' + (secs % 60) + 's';
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    return h + 'h ' + m + 'm';
}

// ── Upload ──

async function uploadFile() {
    const fileInput = document.getElementById('fileInput');
    const nameInput = document.getElementById('uploadName');
    const ttlInput = document.getElementById('uploadTtl');

    if (!fileInput.files || fileInput.files.length === 0) {
        showStatus('Please select a file first', true);
        return;
    }

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append('file', file);
    const name = nameInput.value.trim();
    if (name) formData.append('name', name);
    const ttl = parseInt(ttlInput.value) || 0;
    if (ttl > 0) formData.append('ttl', String(ttl));

    try {
        showStatus('Uploading...');
        const resp = await fetch('/api/v0/uploaded-files', {
            method: 'POST',
            body: formData
        });

        if (!resp.ok) {
            const err = await resp.text();
            throw new Error(err || 'Upload failed: ' + resp.status);
        }

        const data = await resp.json();

        // Store locally
        addSessionFile({
            uuid: data.uuid,
            token: data.token,
            name: data.name,
            expiredTime: data.expiredTime
        });

        // Clear form
        fileInput.value = '';
        document.getElementById('fileNameDisplay').textContent = '';
        document.getElementById('dropZoneText').style.display = '';
        nameInput.value = '';
        ttlInput.value = '0';

        showStatus('File uploaded successfully!');
        openTokenModal(data);
    } catch (err) {
        showStatus('Upload error: ' + err.message, true);
    }
}

// ── Token modal ──

function openTokenModal(data) {
    document.getElementById('modalUuid').textContent = data.uuid;
    document.getElementById('modalToken').textContent = data.token;
    document.getElementById('modalName').textContent = data.name;
    document.getElementById('modalExpires').textContent = formatExpiry(data.expiredTime);
    document.getElementById('modalCopyFeedback').textContent = '';
    document.getElementById('tokenModal').style.display = 'flex';
}

function closeTokenModal() {
    document.getElementById('tokenModal').style.display = 'none';
}

function copyCredentials() {
    const uuid = document.getElementById('modalUuid').textContent;
    const token = document.getElementById('modalToken').textContent;
    const text = 'UUID: ' + uuid + '\nToken: ' + token;

    copyToClipboard(text).then(() => {
        const fb = document.getElementById('modalCopyFeedback');
        fb.textContent = 'Credentials copied!';
        fb.style.color = '#8f8';
        setTimeout(() => { fb.textContent = ''; }, 2500);
    }).catch(() => {
        const fb = document.getElementById('modalCopyFeedback');
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

// ── My Files tab ──

function renderFilesList() {
    const files = loadSessionFiles();
    const container = document.getElementById('filesList');

    if (files.length === 0) {
        container.innerHTML = '<div class="empty-state">No files uploaded yet in this session.</div>';
        return;
    }

    container.innerHTML = files.map(f => {
        const expires = formatExpiry(f.expiredTime);
        const expired = f.expiredTime > 0 && f.expiredTime < Date.now();
        return `
        <div class="file-card" style="${expired ? 'opacity:0.5;' : ''}">
            <div class="file-card-header">
                <span class="file-card-name">${escapeHtml(f.name)}</span>
                ${expired ? '<span style="color:#f66;font-size:0.8rem;">EXPIRED</span>' : ''}
            </div>
            <div class="file-card-meta">
                <span>UUID: ${escapeHtml(f.uuid.substring(0, 8))}…</span>
                <span>Expires: ${expires}</span>
            </div>
            <div class="file-card-actions">
                <button onclick="downloadWithCreds('${f.uuid}','${escapeHtml(f.token)}')">Download</button>
                <button class="danger" onclick="deleteWithCreds('${f.uuid}','${escapeHtml(f.token)}')">Delete</button>
            </div>
        </div>`;
    }).join('');
}

function downloadWithCreds(uuid, token) {
    window.open('/api/v0/uploaded-files/' + uuid + '/download?token=' + encodeURIComponent(token), '_blank');
}

async function deleteWithCreds(uuid, token) {
    if (!confirm('Delete this file permanently?')) return;
    try {
        showStatus('Deleting...');
        const resp = await fetch('/api/v0/uploaded-files/' + uuid + '?token=' + encodeURIComponent(token), {
            method: 'DELETE'
        });
        if (!resp.ok && resp.status !== 204) {
            const err = await resp.text();
            throw new Error(err || 'Delete failed: ' + resp.status);
        }
        removeSessionFile(uuid);
        renderFilesList();
        showStatus('File deleted');
    } catch (err) {
        showStatus('Error: ' + err.message, true);
    }
}

// ── Access File tab ──

function getAccessParams() {
    const uuid = document.getElementById('accessUuid').value.trim();
    const token = document.getElementById('accessToken').value.trim();
    if (!uuid || !token) {
        showStatus('UUID and token are required', true);
        return null;
    }
    return { uuid, token };
}

async function getFileInfo() {
    const p = getAccessParams();
    if (!p) return;

    try {
        showStatus('Loading metadata...');
        const resp = await fetch('/api/v0/uploaded-files/' + p.uuid + '?token=' + encodeURIComponent(p.token));
        if (!resp.ok) {
            const err = await resp.text();
            throw new Error(err || 'Failed: ' + resp.status);
        }
        const data = await resp.json();

        document.getElementById('infoName').textContent = data.name;
        document.getElementById('infoUuid').textContent = data.uuid;
        document.getElementById('infoExpires').textContent = formatExpiry(data.expiredTime);
        document.getElementById('fileInfo').style.display = 'block';
        showStatus('');
    } catch (err) {
        showStatus('Error: ' + err.message, true);
        document.getElementById('fileInfo').style.display = 'none';
    }
}

async function downloadFile() {
    const p = getAccessParams();
    if (!p) return;
    window.open('/api/v0/uploaded-files/' + p.uuid + '/download?token=' + encodeURIComponent(p.token), '_blank');
    showStatus('Download started...');
}

async function deleteFile() {
    const p = getAccessParams();
    if (!p) return;
    if (!confirm('Delete this file permanently?')) return;

    try {
        showStatus('Deleting...');
        const resp = await fetch('/api/v0/uploaded-files/' + p.uuid + '?token=' + encodeURIComponent(p.token), {
            method: 'DELETE'
        });
        if (!resp.ok && resp.status !== 204) {
            const err = await resp.text();
            throw new Error(err || 'Delete failed: ' + resp.status);
        }
        removeSessionFile(p.uuid);
        document.getElementById('fileInfo').style.display = 'none';
        document.getElementById('accessUuid').value = '';
        document.getElementById('accessToken').value = '';
        showStatus('File deleted');
        renderFilesList();
    } catch (err) {
        showStatus('Error: ' + err.message, true);
    }
}

// ── Tabs ──

function switchTab(idx) {
    document.querySelectorAll('.tab-btn').forEach((btn, i) => btn.classList.toggle('active', i === idx));
    document.getElementById('uploadTab').style.display = idx === 0 ? 'block' : 'none';
    document.getElementById('filesTab').style.display = idx === 1 ? 'block' : 'none';
    document.getElementById('accessTab').style.display = idx === 2 ? 'block' : 'none';
    document.getElementById('status').textContent = '';
    document.getElementById('fileInfo').style.display = 'none';
    if (idx === 1) renderFilesList();
}

// ── Status ──

let statusTimer;
function showStatus(msg, isError) {
    const el = document.getElementById('status');
    el.textContent = msg;
    el.className = 'status-line' + (isError ? ' err' : msg && msg !== 'Uploading...' ? ' ok' : '');
    clearTimeout(statusTimer);
    if (msg && !isError) {
        statusTimer = setTimeout(() => { el.textContent = ''; el.className = 'status-line'; }, 5000);
    }
}

// ── Helpers ──

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ── Expose public API ──
    window.switchTab = switchTab;
    window.onFileSelected = onFileSelected;
    window.uploadFile = uploadFile;
    window.copyCredentials = copyCredentials;
    window.closeTokenModal = closeTokenModal;
    window.getFileInfo = getFileInfo;
    window.downloadFile = downloadFile;
    window.deleteFile = deleteFile;
    window.downloadWithCreds = downloadWithCreds;
    window.deleteWithCreds = deleteWithCreds;

    // ── Modal overlay click ──
    document.addEventListener('click', function(e) {
        if (e.target.id === 'tokenModal') closeTokenModal();
    });

    // ── Init ──
    window.addEventListener('DOMContentLoaded', function() {
        switchTab(0);
    });
})();

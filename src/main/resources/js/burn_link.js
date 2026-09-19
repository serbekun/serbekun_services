(function () {
    'use strict';

    const DEVICES = ['iphone', 'ipad', 'android', 'windows', 'mac', 'linux'];
    const BROWSERS = ['edge', 'opera', 'firefox', 'chrome', 'safari'];
    const MAX_TEXT_LENGTH = 100000;

    let current = null;

    // ── Option lists ──

    function renderOptions(containerId, values, prefix) {
        const container = document.getElementById(containerId);
        container.innerHTML = values.map((value, i) => `
            <label class="check-item">
                <input type="checkbox" id="${prefix}-${i}" value="${value}">
                <span class="check-box" aria-hidden="true"></span>
                <span class="check-label">${value}</span>
            </label>`).join('');
    }

    function selectedValues(containerId) {
        return Array.from(document.querySelectorAll('#' + containerId + ' input:checked'))
            .map(input => input.value);
    }

    // ── Parsing ──

    function parseList(value) {
        return value.split(/[\n,]+/).map(s => s.trim()).filter(Boolean);
    }

    // ── Create ──

    async function createBurnLink() {
        const text = document.getElementById('secretText').value;
        if (!text.trim()) {
            showStatus('Please enter the secret text', true);
            return;
        }
        if (text.length > MAX_TEXT_LENGTH) {
            showStatus('Secret is too long (max ' + MAX_TEXT_LENGTH + ' characters)', true);
            return;
        }

        const payload = {
            text: text,
            ttl: Number(document.getElementById('burnTtl').value) || 0,
            devices: selectedValues('deviceList'),
            browsers: selectedValues('browserList')
        };

        const ipWhitelist = parseList(document.getElementById('ipWhitelist').value);
        const ipBlacklist = parseList(document.getElementById('ipBlacklist').value);
        if (ipWhitelist.length) payload.ipWhitelist = ipWhitelist;
        if (ipBlacklist.length) payload.ipBlacklist = ipBlacklist;

        try {
            showStatus('Creating...');
            const resp = await fetch('/api/v0/burn', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!resp.ok) {
                let message = 'Create failed: ' + resp.status;
                try {
                    const err = await resp.json();
                    if (err && (err.error || err.message)) message = err.error || err.message;
                } catch (e) { /* not json */ }
                throw new Error(message);
            }

            current = await resp.json();
            showStatus('Burn link created!');
            openModal(current);
        } catch (err) {
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Modal ──

    function openModal(link) {
        document.getElementById('modalLink').textContent = link.url;
        document.getElementById('modalId').textContent = link.id;
        document.getElementById('modalToken').textContent = link.token;
        document.getElementById('modalExpires').textContent =
            link.expiredTime > 0 ? new Date(link.expiredTime).toLocaleString() : 'never (until opened)';
        document.getElementById('modalFeedback').textContent = '';
        document.getElementById('resultModal').style.display = 'flex';
    }

    function closeModal() {
        document.getElementById('resultModal').style.display = 'none';
    }

    function copyModalLink() {
        if (current) copyWithFeedback(current.url, 'Link copied!');
    }

    function copyCredentials() {
        if (!current) return;
        const text = 'Link: ' + current.url + '\nID: ' + current.id + '\nToken: ' + current.token;
        copyWithFeedback(text, 'Credentials copied!');
    }

    function copyWithFeedback(text, okMessage) {
        copyToClipboard(text).then(() => {
            const fb = document.getElementById('modalFeedback');
            fb.textContent = okMessage;
            fb.style.color = 'var(--ok)';
            setTimeout(() => { fb.textContent = ''; }, 2500);
        }).catch(() => {
            const fb = document.getElementById('modalFeedback');
            fb.textContent = 'Could not copy. Select and copy manually.';
            fb.style.color = 'var(--err)';
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

    // ── TTL presets ──

    // Sets the ttl input to the chosen preset and highlights its button.
    function setTtlPreset(btn, seconds) {
        document.getElementById('burnTtl').value = String(seconds);
        highlightTtlPreset(seconds);
    }

    // Highlights the preset button matching the given seconds value, if any.
    function highlightTtlPreset(seconds) {
        document.querySelectorAll('.ttl-preset').forEach(b => {
            b.classList.toggle('active', parseInt(b.dataset.ttl, 10) === seconds);
        });
    }

    // Keeps preset highlighting in sync when the ttl input is edited manually.
    function syncTtlPresets() {
        const val = parseInt(document.getElementById('burnTtl').value, 10);
        highlightTtlPreset(isNaN(val) ? null : val);
    }

    // ── Status ──

    let statusTimer;
    function showStatus(message, isError) {
        const el = document.getElementById('status');
        el.textContent = message;
        el.className = 'status-line' + (isError ? ' err' : message && !message.endsWith('...') ? ' ok' : '');
        clearTimeout(statusTimer);
        if (message && !isError) {
            statusTimer = setTimeout(() => { el.textContent = ''; el.className = 'status-line'; }, 5000);
        }
    }

    // ── Public ──

    window.createBurnLink = createBurnLink;
    window.closeModal = closeModal;
    window.copyModalLink = copyModalLink;
    window.copyCredentials = copyCredentials;
    window.setTtlPreset = setTtlPreset;
    window.syncTtlPresets = syncTtlPresets;

    document.addEventListener('click', function (e) {
        if (e.target.id === 'resultModal') closeModal();
    });

    window.addEventListener('DOMContentLoaded', function () {
        renderOptions('deviceList', DEVICES, 'device');
        renderOptions('browserList', BROWSERS, 'browser');
        highlightTtlPreset(0);

        const text = document.getElementById('secretText');
        const counter = document.getElementById('textCount');
        text.addEventListener('input', function () {
            counter.textContent = text.value.length + ' / ' + MAX_TEXT_LENGTH;
        });
    });
})();

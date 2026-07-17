(function () {
    'use strict';

    let currentIp = null;

    // ── Fetch IP ──

    async function refreshIp() {
        const ipEl = document.getElementById('ipValue');
        const copyBtn = document.getElementById('copyBtn');

        ipEl.textContent = 'loading…';
        ipEl.classList.remove('err');
        copyBtn.disabled = true;
        currentIp = null;

        try {
            const resp = await fetch('/api/v0/network/ip');
            if (!resp.ok) {
                throw new Error('Request failed: ' + resp.status);
            }
            const data = await resp.json();
            currentIp = data.ip;
            ipEl.textContent = currentIp;
            copyBtn.disabled = false;
            showStatus('IP resolved');
        } catch (err) {
            ipEl.textContent = 'could not resolve ip';
            ipEl.classList.add('err');
            showStatus('Error: ' + err.message, true);
        }
    }

    // ── Copy ──

    function copyIp() {
        if (!currentIp) return;
        copyToClipboard(currentIp)
            .then(() => showStatus('IP copied!'))
            .catch(() => showStatus('Could not copy', true));
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

    // ── Expose public API ──
    window.refreshIp = refreshIp;
    window.copyIp = copyIp;

    // ── Init ──
    window.addEventListener('DOMContentLoaded', refreshIp);
})();

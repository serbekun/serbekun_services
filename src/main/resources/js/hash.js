// hash.js — SHA-256 / SHA-512 / BLAKE3 / HMAC-SHA256 digests and integrity
// checks for the /static/v0/html/hash.html page.
(function () {
    'use strict';

    // Algorithm and key are shared across all three tabs, so they live here
    // rather than in any one panel.
    let algorithm = 'sha256';
    let textEncoding = 'utf8';
    let verifySource = 'text';

    const $ = (id) => document.getElementById(id);
    const value = (id) => $(id).value.trim();

    // ─── Requests ───

    async function postJson(path, body) {
        const res = await fetch(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        return unwrap(res);
    }

    async function postForm(path, formData) {
        const res = await fetch(path, { method: 'POST', body: formData });
        return unwrap(res);
    }

    async function unwrap(res) {
        const payload = await res.json().catch(() => null);
        if (!res.ok) {
            throw new Error((payload && (payload.error || payload.message)) || 'request failed (' + res.status + ')');
        }
        return payload;
    }

    /** The key half of a request, or an error if HMAC was picked without one. */
    function keyFields() {
        if (algorithm !== 'hmac-sha256') return {};
        const key = value('key');
        if (!key) throw new Error('hmac-sha256 needs a key');
        return { key: key };
    }

    // ─── Formatting ───

    function formatBytes(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function showResult(blockId, bodyId, text, isError) {
        const block = $(blockId);
        $(bodyId).textContent = text;
        block.classList.toggle('error', !!isError);
        block.style.display = 'block';
    }

    async function writeClip(text) {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
        } else {
            const ta = Object.assign(document.createElement('textarea'), {
                value: text,
                style: 'position:fixed;left:-9999px'
            });
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
        }
    }

    let statusTimer;
    function setStatus(msg, type) {
        const el = $('status');
        el.textContent = msg;
        el.className = 'status-line' + (type ? ' ' + type : '');
        clearTimeout(statusTimer);
        if (msg && type) statusTimer = setTimeout(() => {
            el.textContent = '';
            el.className = 'status-line';
        }, 5000);
    }

    // ─── Algorithm picker ───

    document.querySelectorAll('.algo-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.algo-btn').forEach((b) => b.classList.toggle('active', b === btn));
            algorithm = btn.dataset.algo;
            // Only HMAC takes a key, so the field appears with it.
            $('keyField').classList.toggle('hidden', algorithm !== 'hmac-sha256');
            setStatus('');
        });
    });

    // ─── Tabs ───

    document.querySelectorAll('.tab-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach((b) => b.classList.toggle('active', b === btn));
            document.querySelectorAll('.panel').forEach((p) => {
                p.classList.toggle('active', p.id === btn.dataset.panel);
            });
            setStatus('');
        });
    });

    // ─── Encoding / source switches ───

    document.querySelectorAll('.enc-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            const target = btn.dataset.target;
            document.querySelectorAll('.enc-btn[data-target="' + target + '"]')
                .forEach((b) => b.classList.toggle('active', b === btn));

            if (target === 'text') {
                textEncoding = btn.dataset.enc;
            } else {
                verifySource = btn.dataset.source;
                $('verifyTextField').classList.toggle('hidden', verifySource !== 'text');
                $('verifyFileField').classList.toggle('hidden', verifySource !== 'file');
            }
            setStatus('');
        });
    });

    // ─── File pickers, with drag and drop ───

    function wireFilePicker(inputId, labelId) {
        const input = $(inputId);
        const label = $(labelId);
        const placeholder = label.textContent;

        input.addEventListener('change', () => {
            const file = input.files[0];
            if (!file) {
                label.textContent = placeholder;
                label.classList.remove('filled');
                return;
            }
            label.textContent = file.name + '  ·  ' + formatBytes(file.size);
            label.classList.add('filled');
        });

        ['dragenter', 'dragover'].forEach((type) => {
            label.addEventListener(type, (e) => {
                e.preventDefault();
                label.classList.add('dragging');
            });
        });
        ['dragleave', 'drop'].forEach((type) => {
            label.addEventListener(type, () => label.classList.remove('dragging'));
        });
        label.addEventListener('drop', (e) => {
            e.preventDefault();
            if (!e.dataTransfer.files.length) return;
            input.files = e.dataTransfer.files;
            input.dispatchEvent(new Event('change'));
        });
    }

    wireFilePicker('fileInput', 'fileLabel');
    wireFilePicker('verifyFileInput', 'verifyFileLabel');

    // ─── Copy ───

    document.querySelectorAll('[data-copy-result]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const text = $(btn.dataset.copyResult).textContent.trim();
            if (!text) return;
            await writeClip(text);
            btn.textContent = 'copied';
            btn.classList.add('copied');
            setTimeout(() => { btn.textContent = 'copy'; btn.classList.remove('copied'); }, 2000);
        });
    });

    // ─── Hash text ───

    $('textBtn').addEventListener('click', async () => {
        try {
            const data = $('textInput').value;
            if (!data) throw new Error('input is required');

            setStatus('hashing...');
            const result = await postJson('/api/v0/hash', Object.assign({
                algorithm: algorithm,
                data: data,
                encoding: textEncoding
            }, keyFields()));

            $('textResultLabel').textContent = result.algorithm + ' digest';
            showResult('textResult', 'textResultText', result.hash, false);
            $('textResultMeta').textContent = formatBytes(result.bytes) + ' hashed · ' + result.hash.length / 2 + ' byte digest';
            setStatus('done', 'ok');
        } catch (e) {
            $('textResultLabel').textContent = 'error';
            showResult('textResult', 'textResultText', e.message, true);
            $('textResultMeta').textContent = '';
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Hash file ───

    $('fileBtn').addEventListener('click', async () => {
        try {
            const file = $('fileInput').files[0];
            if (!file) throw new Error('choose a file first');

            const form = new FormData();
            form.append('file', file);
            form.append('algorithm', algorithm);
            const keys = keyFields();
            if (keys.key) form.append('key', keys.key);

            setStatus('hashing ' + file.name + '...');
            const result = await postForm('/api/v0/hash/file', form);

            $('fileResultLabel').textContent = result.algorithm + ' digest';
            showResult('fileResult', 'fileResultText', result.hash, false);
            $('fileResultMeta').textContent = result.name + ' · ' + formatBytes(result.bytes);
            setStatus('done', 'ok');
        } catch (e) {
            $('fileResultLabel').textContent = 'error';
            showResult('fileResult', 'fileResultText', e.message, true);
            $('fileResultMeta').textContent = '';
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Verify ───

    $('verifyBtn').addEventListener('click', async () => {
        const verdict = $('verifyVerdict');
        const detail = $('verifyDetail');
        detail.classList.remove('shown');

        try {
            const expected = value('verifyHash');
            if (!expected) throw new Error('expected hash is required');

            let result;
            setStatus('verifying...');

            if (verifySource === 'file') {
                const file = $('verifyFileInput').files[0];
                if (!file) throw new Error('choose a file first');

                // /hash/file computes a digest; the comparison happens here,
                // since the file never needs to make a second trip.
                const form = new FormData();
                form.append('file', file);
                form.append('algorithm', algorithm);
                const keys = keyFields();
                if (keys.key) form.append('key', keys.key);

                const hashed = await postForm('/api/v0/hash/file', form);
                const normalized = expected.toLowerCase();
                result = {
                    valid: hashed.hash === normalized,
                    expected: normalized,
                    actual: hashed.hash
                };
            } else {
                const data = $('verifyInput').value;
                if (!data) throw new Error('input is required');
                result = await postJson('/api/v0/hash/verify', Object.assign({
                    algorithm: algorithm,
                    data: data,
                    encoding: textEncoding,
                    hash: expected
                }, keyFields()));
            }

            verdict.classList.remove('valid', 'invalid');
            verdict.classList.add('shown', result.valid ? 'valid' : 'invalid');
            $('verifyMark').textContent = result.valid ? '✓' : '✗';
            $('verifyText').textContent = result.valid
                ? 'integrity confirmed — the digest matches'
                : 'no match — the data or the expected hash is not what you think';

            if (!result.valid) {
                $('verifyExpected').textContent = result.expected;
                $('verifyActual').textContent = result.actual;
                detail.classList.add('shown');
            }
            setStatus(result.valid ? 'valid' : 'invalid', result.valid ? 'ok' : 'err');
        } catch (e) {
            verdict.classList.remove('valid');
            verdict.classList.add('shown', 'invalid');
            $('verifyMark').textContent = '!';
            $('verifyText').textContent = e.message;
            setStatus('error: ' + e.message, 'err');
        }
    });
})();

// cipher_rsa.js — RSA key pairs, encryption, signatures and hybrid
// (AES-GCM + RSA-OAEP) encryption for the /static/v0/html/cipher_rsa.html page.
(function () {
    'use strict';

    // Raw payload ceiling for the hybrid tab. Base64 inflates by 4/3 and the
    // whole envelope travels as one JSON body, so stay well under the server's
    // request limit and the browser's appetite for a single string.
    const MAX_FILE_BYTES = 32 * 1024 * 1024;

    // The decrypted hybrid payload, kept as bytes so "save" can hand back the
    // exact file that went in — not a lossy text round trip.
    let hybridDecryptedBytes = null;
    let hybridDecryptedName = 'decrypted.bin';
    let hybridEnvelope = null;

    // ─── Base64 helpers ───

    function textToBase64(text) {
        return bytesToBase64(new TextEncoder().encode(text));
    }

    function bytesToBase64(bytes) {
        // Chunked so a large file does not blow the argument limit of apply().
        let binary = '';
        const chunk = 0x8000;
        for (let i = 0; i < bytes.length; i += chunk) {
            binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
        }
        return btoa(binary);
    }

    function base64ToBytes(base64) {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return bytes;
    }

    function base64ToText(base64) {
        return new TextDecoder().decode(base64ToBytes(base64));
    }

    function isBase64(s) {
        return /^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$/.test(s);
    }

    // ─── DOM helpers ───

    const $ = (id) => document.getElementById(id);
    const value = (id) => $(id).value.trim();

    async function postJson(path, body) {
        const res = await fetch(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const payload = await res.json().catch(() => null);
        if (!res.ok) {
            throw new Error((payload && (payload.error || payload.message)) || 'request failed (' + res.status + ')');
        }
        return payload;
    }

    function showResult(blockId, bodyId, text, isError) {
        const block = $(blockId);
        $(bodyId).textContent = text;
        block.classList.toggle('error', !!isError);
        block.style.display = 'block';
    }

    function requireKey(id, label) {
        const key = value(id);
        if (!key) throw new Error(label + ' is required — generate or paste one');
        if (!isBase64(key)) throw new Error(label + ' must be valid base64');
        return key;
    }

    function download(blob, filename) {
        const a = Object.assign(document.createElement('a'), {
            href: URL.createObjectURL(blob),
            download: filename
        });
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(a.href), 0);
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

    // ─── Key pair ───

    $('genKeyBtn').addEventListener('click', async () => {
        const btn = $('genKeyBtn');
        btn.disabled = true;
        setStatus('generating 2048-bit key pair...');
        try {
            const res = await fetch('/api/v0/cipher/rsa/keypair');
            if (!res.ok) throw new Error('server error ' + res.status);
            const data = await res.json();
            $('publicKey').value = data.publicKey;
            $('privateKey').value = data.privateKey;
            setStatus('key pair generated — save the private key', 'ok');
        } catch (e) {
            setStatus('error: ' + e.message, 'err');
        } finally {
            btn.disabled = false;
        }
    });

    // ─── Copy / save / load buttons ───

    document.querySelectorAll('[data-copy]').forEach((btn) => {
        btn.addEventListener('click', async () => {
            const text = value(btn.dataset.copy);
            if (!text) { setStatus('error: nothing to copy', 'err'); return; }
            await writeClip(text);
            setStatus('copied', 'ok');
        });
    });

    document.querySelectorAll('[data-save]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const text = value(btn.dataset.save);
            if (!text) { setStatus('error: nothing to save', 'err'); return; }
            download(new Blob([text], { type: 'text/plain' }), btn.dataset.filename);
            setStatus('saved as ' + btn.dataset.filename, 'ok');
        });
    });

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

    document.querySelectorAll('[data-save-result]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const text = $(btn.dataset.saveResult).textContent.trim();
            if (!text) { setStatus('error: nothing to save', 'err'); return; }
            download(new Blob([text], { type: 'text/plain' }), btn.dataset.filename);
            setStatus('saved as ' + btn.dataset.filename, 'ok');
        });
    });

    // One hidden input serves every "load file" button; the target field is
    // remembered until the picker resolves.
    let loadTargetId = null;
    document.querySelectorAll('[data-load]').forEach((btn) => {
        btn.addEventListener('click', () => {
            loadTargetId = btn.dataset.load;
            $('fileLoader').click();
        });
    });

    $('fileLoader').addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file || !loadTargetId) return;
        try {
            $(loadTargetId).value = (await file.text()).trim();
            setStatus('loaded ' + file.name, 'ok');
        } catch (err) {
            setStatus('error: ' + err.message, 'err');
        }
        e.target.value = '';
    });

    // ─── Encrypt ───

    $('encryptBtn').addEventListener('click', async () => {
        const text = value('encryptInput');
        try {
            if (!text) throw new Error('plaintext is required');
            const publicKey = requireKey('publicKey', 'public key');

            setStatus('encrypting...');
            const result = await postJson('/api/v0/cipher/rsa/encrypt', {
                data: textToBase64(text),
                publicKey: publicKey
            });
            showResult('encryptResult', 'encryptResultText', result.data, false);
            setStatus('done', 'ok');
        } catch (e) {
            showResult('encryptResult', 'encryptResultText', e.message, true);
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Decrypt ───

    $('decryptBtn').addEventListener('click', async () => {
        try {
            const data = value('decryptInput');
            if (!data) throw new Error('ciphertext is required');
            if (!isBase64(data)) throw new Error('ciphertext must be valid base64');
            const privateKey = requireKey('privateKey', 'private key');

            setStatus('decrypting...');
            const result = await postJson('/api/v0/cipher/rsa/decrypt', {
                data: data,
                privateKey: privateKey
            });
            showResult('decryptResult', 'decryptResultText', base64ToText(result.data), false);
            setStatus('done', 'ok');
        } catch (e) {
            showResult('decryptResult', 'decryptResultText', e.message, true);
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Sign ───

    $('signBtn').addEventListener('click', async () => {
        try {
            const text = value('signInput');
            if (!text) throw new Error('message is required');
            const privateKey = requireKey('privateKey', 'private key');

            setStatus('signing...');
            const result = await postJson('/api/v0/cipher/rsa/sign', {
                data: textToBase64(text),
                privateKey: privateKey
            });
            showResult('signResult', 'signResultText', result.signature, false);
            setStatus('signed', 'ok');
        } catch (e) {
            showResult('signResult', 'signResultText', e.message, true);
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Verify ───

    $('verifyBtn').addEventListener('click', async () => {
        const verdict = $('verifyVerdict');
        try {
            const text = value('verifyInput');
            const signature = value('verifySignature');
            if (!text) throw new Error('signed message is required');
            if (!signature) throw new Error('signature is required');
            if (!isBase64(signature)) throw new Error('signature must be valid base64');
            const publicKey = requireKey('publicKey', 'public key');

            setStatus('verifying...');
            const result = await postJson('/api/v0/cipher/rsa/verify', {
                data: textToBase64(text),
                signature: signature,
                publicKey: publicKey
            });

            verdict.classList.remove('valid', 'invalid');
            verdict.classList.add('shown', result.valid ? 'valid' : 'invalid');
            $('verifyMark').textContent = result.valid ? '✓' : '✗';
            $('verifyText').textContent = result.valid
                ? 'signature is valid — signed by the holder of this key pair'
                : 'signature does not match — wrong key, wrong message, or altered data';
            setStatus(result.valid ? 'valid' : 'invalid', result.valid ? 'ok' : 'err');
        } catch (e) {
            verdict.classList.remove('valid');
            verdict.classList.add('shown', 'invalid');
            $('verifyMark').textContent = '!';
            $('verifyText').textContent = e.message;
            setStatus('error: ' + e.message, 'err');
        }
    });

    // ─── Hybrid: text / file source switch ───

    document.querySelectorAll('.source-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            const target = btn.dataset.target;
            document.querySelectorAll('.source-btn[data-target="' + target + '"]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            const useFile = btn.dataset.source === 'file';
            $(target + 'TextField').style.display = useFile ? 'none' : 'block';
            $(target + 'FileField').style.display = useFile ? 'block' : 'none';
        });
    });

    $('hybridEncryptFile').addEventListener('change', (e) => {
        const file = e.target.files[0];
        const label = $('hybridEncryptFileLabel');
        if (!file) {
            label.textContent = 'choose a file…';
            label.classList.remove('filled');
            return;
        }
        label.textContent = file.name + '  ·  ' + formatBytes(file.size);
        label.classList.add('filled');
    });

    function formatBytes(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    // ─── Hybrid encrypt ───

    $('hybridEncryptBtn').addEventListener('click', async () => {
        try {
            const useFile = document.querySelector('.source-btn[data-target="hybridEncrypt"].active')
                .dataset.source === 'file';
            const publicKey = requireKey('publicKey', 'public key');

            let dataBase64;
            let name = null;
            let type = null;

            if (useFile) {
                const file = $('hybridEncryptFile').files[0];
                if (!file) throw new Error('choose a file first');
                if (file.size > MAX_FILE_BYTES) {
                    throw new Error('file is larger than ' + formatBytes(MAX_FILE_BYTES));
                }
                setStatus('reading ' + file.name + '...');
                dataBase64 = bytesToBase64(new Uint8Array(await file.arrayBuffer()));
                name = file.name;
                type = file.type || null;
            } else {
                const text = value('hybridEncryptInput');
                if (!text) throw new Error('plaintext is required');
                dataBase64 = textToBase64(text);
            }

            setStatus('encrypting...');
            const result = await postJson('/api/v0/cipher/hybrid/encrypt', {
                data: dataBase64,
                publicKey: publicKey
            });

            // The envelope carries both halves plus the original file name, so a
            // round trip restores the file as it was. Only the payload is
            // encrypted — the name travels in the clear.
            const envelope = { encryptedKey: result.encryptedKey, data: result.data };
            if (name) envelope.name = name;
            if (type) envelope.type = type;

            hybridEnvelope = envelope;
            showResult('hybridEncryptResult', 'hybridEncryptResultText', JSON.stringify(envelope, null, 2), false);
            setStatus('done — keep the envelope and the private key', 'ok');
        } catch (e) {
            hybridEnvelope = null;
            showResult('hybridEncryptResult', 'hybridEncryptResultText', e.message, true);
            setStatus('error: ' + e.message, 'err');
        }
    });

    $('hybridEncryptSaveBtn').addEventListener('click', () => {
        if (!hybridEnvelope) { setStatus('error: nothing to save', 'err'); return; }
        const filename = (hybridEnvelope.name ? hybridEnvelope.name : 'payload') + '.enc.json';
        download(new Blob([JSON.stringify(hybridEnvelope, null, 2)], { type: 'application/json' }), filename);
        setStatus('saved as ' + filename, 'ok');
    });

    // ─── Hybrid decrypt ───

    $('hybridDecryptBtn').addEventListener('click', async () => {
        try {
            const raw = value('hybridDecryptInput');
            if (!raw) throw new Error('envelope is required');

            let envelope;
            try {
                envelope = JSON.parse(raw);
            } catch (err) {
                throw new Error('envelope is not valid json');
            }
            if (!envelope.encryptedKey || !envelope.data) {
                throw new Error('envelope needs both "encryptedKey" and "data"');
            }

            const privateKey = requireKey('privateKey', 'private key');

            setStatus('decrypting...');
            const result = await postJson('/api/v0/cipher/hybrid/decrypt', {
                data: envelope.data,
                encryptedKey: envelope.encryptedKey,
                privateKey: privateKey
            });

            hybridDecryptedBytes = base64ToBytes(result.data);
            hybridDecryptedName = envelope.name || 'decrypted.txt';

            // Show the payload when it is readable text; a binary file only gets
            // a summary, since dumping raw bytes into the page helps nobody.
            let text = null;
            try {
                text = new TextDecoder('utf-8', { fatal: true }).decode(hybridDecryptedBytes);
            } catch (err) {
                text = null;
            }

            if (text !== null) {
                $('hybridDecryptResultLabel').textContent = 'plaintext' + (envelope.name ? ' · ' + envelope.name : '');
                showResult('hybridDecryptResult', 'hybridDecryptResultText', text, false);
            } else {
                $('hybridDecryptResultLabel').textContent = 'binary · ' + hybridDecryptedName;
                showResult('hybridDecryptResult', 'hybridDecryptResultText',
                    'binary payload · ' + formatBytes(hybridDecryptedBytes.length) + ' — use save to write it out',
                    false);
            }
            setStatus('done', 'ok');
        } catch (e) {
            hybridDecryptedBytes = null;
            $('hybridDecryptResultLabel').textContent = 'error';
            showResult('hybridDecryptResult', 'hybridDecryptResultText', e.message, true);
            setStatus('error: ' + e.message, 'err');
        }
    });

    $('hybridDecryptSaveBtn').addEventListener('click', () => {
        if (!hybridDecryptedBytes) { setStatus('error: nothing to save', 'err'); return; }
        download(new Blob([hybridDecryptedBytes], { type: 'application/octet-stream' }), hybridDecryptedName);
        setStatus('saved as ' + hybridDecryptedName, 'ok');
    });
})();

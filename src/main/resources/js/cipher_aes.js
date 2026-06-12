(function () {
    window.switchTab = function (idx) {
        document.querySelectorAll('.tab-btn').forEach((b, i) => b.classList.toggle('active', i === idx));
        document.getElementById('encryptPanel').style.display = idx === 0 ? 'block' : 'none';
        document.getElementById('decryptPanel').style.display = idx === 1 ? 'block' : 'none';
        setStatus('');
    };

    window.generateKey = async function () {
        setStatus('generating key...', '');
        try {
            const res = await fetch('/api/v0/cipher/aes', { method: 'GET' });
            if (!res.ok) throw new Error('server error ' + res.status);
            const data = await res.json();
            document.getElementById('encryptKey').value = data.key;
            document.getElementById('decryptKey').value = data.key;
            setStatus('key generated', 'ok');
        } catch (e) {
            setStatus('error: ' + e.message, 'err');
        }
    };

    window.encryptData = async function () {
        const text = document.getElementById('encryptInput').value.trim();
        const key  = document.getElementById('encryptKey').value.trim();
        const resultDiv  = document.getElementById('encryptResult');
        const resultBody = document.getElementById('encryptResultText');
        if (!text || !key) { setStatus('error: both fields required', 'err'); return; }
        if (!isBase64(key)) { setStatus('error: key must be valid base64', 'err'); return; }
        setStatus('encrypting...', '');
        try {
            const b64data = btoa(unescape(encodeURIComponent(text)));
            const res = await fetch('/api/v0/cipher/aes/encrypt', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ data: b64data, key })
            });
            if (!res.ok) throw new Error(await res.text() || 'encrypt failed');
            const result = await res.json();
            resultBody.textContent = result.data;
            resultDiv.classList.remove('error');
            resultDiv.style.display = 'block';
            setStatus('done', 'ok');
        } catch (e) {
            resultBody.textContent = e.message;
            resultDiv.classList.add('error');
            resultDiv.style.display = 'block';
            setStatus('error: ' + e.message, 'err');
        }
    };

    window.decryptData = async function () {
        const data = document.getElementById('decryptInput').value.trim();
        const key  = document.getElementById('decryptKey').value.trim();
        const resultDiv  = document.getElementById('decryptResult');
        const resultBody = document.getElementById('decryptResultText');
        if (!data || !key) { setStatus('error: both fields required', 'err'); return; }
        if (!isBase64(data) || !isBase64(key)) { setStatus('error: invalid base64 input', 'err'); return; }
        setStatus('decrypting...', '');
        try {
            const res = await fetch('/api/v0/cipher/aes/decrypt', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ data, key })
            });
            if (!res.ok) throw new Error(await res.text() || 'decrypt failed');
            const result = await res.json();
            resultBody.textContent = decodeURIComponent(escape(atob(result.data)));
            resultDiv.classList.remove('error');
            resultDiv.style.display = 'block';
            setStatus('done', 'ok');
        } catch (e) {
            resultBody.textContent = e.message;
            resultDiv.classList.add('error');
            resultDiv.style.display = 'block';
            setStatus('error: ' + e.message, 'err');
        }
    };

    window.copyResult = async function (id, btn) {
        const text = document.getElementById(id).textContent.trim();
        if (!text) return;
        await writeClip(text);
        btn.textContent = 'copied';
        btn.classList.add('copied');
        setTimeout(() => { btn.textContent = 'copy'; btn.classList.remove('copied'); }, 2000);
    };

    window.copyKey = function (id) {
        const v = document.getElementById(id).value.trim();
        if (!v) { setStatus('error: no key to copy', 'err'); return; }
        writeClip(v).then(() => setStatus('key copied', 'ok'));
    };

    window.downloadKey = function (id) {
        const v = document.getElementById(id).value.trim();
        if (!v) { setStatus('error: no key to download', 'err'); return; }
        const a = Object.assign(document.createElement('a'), {
            href: URL.createObjectURL(new Blob([v], { type: 'text/plain' })),
            download: 'aes_key.txt'
        });
        document.body.appendChild(a); a.click(); document.body.removeChild(a);
        setStatus('key saved as aes_key.txt', 'ok');
    };

    function isBase64(s) {
        return /^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$/.test(s);
    }

    async function writeClip(text) {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
        } else {
            const ta = Object.assign(document.createElement('textarea'), { value: text, style: 'position:fixed;left:-9999px' });
            document.body.appendChild(ta); ta.select(); document.execCommand('copy'); document.body.removeChild(ta);
        }
    }

    let statusTimer;
    function setStatus(msg, type) {
        const el = document.getElementById('status');
        el.textContent = msg;
        el.className = 'status-line' + (type ? ' ' + type : '');
        clearTimeout(statusTimer);
        if (msg && type !== '') statusTimer = setTimeout(() => { el.textContent = ''; el.className = 'status-line'; }, 5000);
    }
})();
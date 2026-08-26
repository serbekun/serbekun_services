// qr.js — QR generation, reading and short-link codes for the
// /static/v0/html/qr.html page.
(function () {
    'use strict';

    let format = 'png';
    let errorCorrection = 'M';
    let readFile = null;

    // The last rendered code, kept so the copy and download buttons have
    // something to hand over without asking the server again.
    let generated = null;
    let shortened = null;

    const $ = (id) => document.getElementById(id);
    const value = (id) => $(id).value.trim();

    // ─── Requests ───

    async function postJson(path, body) {
        const res = await fetch(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
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

    // ─── Helpers ───

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

    function flash(btn, label) {
        const original = btn.textContent;
        btn.textContent = label || 'copied';
        btn.classList.add('copied');
        setTimeout(() => {
            btn.textContent = original;
            btn.classList.remove('copied');
        }, 1400);
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

    async function run(btn, work) {
        btn.disabled = true;
        try {
            await work();
        } catch (e) {
            setStatus(e.message, 'err');
        } finally {
            btn.disabled = false;
        }
    }

    /** Turns a data: URL back into a Blob, for the clipboard and for downloads. */
    function dataUrlToBlob(dataUrl) {
        const comma = dataUrl.indexOf(',');
        const type = dataUrl.slice(5, dataUrl.indexOf(';'));
        const bytes = atob(dataUrl.slice(comma + 1));
        const buffer = new Uint8Array(bytes.length);
        for (let i = 0; i < bytes.length; i++) buffer[i] = bytes.charCodeAt(i);
        return new Blob([buffer], { type: type });
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

    // ─── Option switches ───

    document.querySelectorAll('.enc-btn[data-format]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.enc-btn[data-format]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            format = btn.dataset.format;
        });
    });

    document.querySelectorAll('.enc-btn[data-ec]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.enc-btn[data-ec]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            errorCorrection = btn.dataset.ec;
        });
    });

    // The picker and the hex field are two views of one colour; each follows
    // the other, and only a well-formed hex value is worth following back.
    function bindColor(pickerId, hexId) {
        const picker = $(pickerId);
        const hex = $(hexId);
        picker.addEventListener('input', () => { hex.value = picker.value; });
        hex.addEventListener('input', () => {
            if (/^#[0-9a-fA-F]{6}$/.test(hex.value.trim())) picker.value = hex.value.trim();
        });
    }
    bindColor('genForeground', 'genForegroundHex');
    bindColor('genBackground', 'genBackgroundHex');

    $('genTransparent').addEventListener('change', () => {
        const off = $('genTransparent').checked;
        $('genBackground').disabled = off;
        $('genBackgroundHex').disabled = off;
    });

    // ─── Generate ───

    /** The QR options every panel shares, as the API spells them. */
    function qrOptions() {
        const size = parseInt(value('genSize'), 10);
        const margin = parseInt(value('genMargin'), 10);
        return {
            format: format,
            size: Number.isFinite(size) ? size : 512,
            errorCorrection: errorCorrection,
            foreground: value('genForegroundHex') || '#000000',
            background: $('genTransparent').checked ? '#00000000' : (value('genBackgroundHex') || '#ffffff'),
            margin: Number.isFinite(margin) ? margin : 4
        };
    }

    function showGenerated(result) {
        generated = result;
        $('genPreview').src = result.image;
        $('genMeta').textContent = result.format + ' · ' + result.size + ' px · level ' + errorCorrection;
        $('genDownload').href = result.image;
        $('genDownload').download = 'qr.' + result.format;
        $('genResult').classList.add('shown');
    }

    $('genBtn').addEventListener('click', () => run($('genBtn'), async () => {
        const data = $('genData').value;
        if (!data) throw new Error('there is nothing to encode');

        const options = qrOptions();
        options.data = data;
        showGenerated(await postJson('/api/v0/qr/generate', options));
        setStatus('code rendered', 'ok');
    }));

    $('genCopyData').addEventListener('click', async () => {
        if (!generated) return;
        await writeClip(generated.image);
        flash($('genCopyData'));
    });

    // Copying the image itself only works where the clipboard takes a Blob of
    // that type — which for now means PNG, in a secure context.
    $('genCopyImage').addEventListener('click', async () => {
        if (!generated) return;
        try {
            if (!navigator.clipboard || !window.ClipboardItem) throw new Error('unsupported');
            const blob = dataUrlToBlob(generated.image);
            await navigator.clipboard.write([new ClipboardItem({ [blob.type]: blob })]);
            flash($('genCopyImage'));
        } catch (e) {
            setStatus('this browser will not copy an image — use download instead', 'err');
        }
    });

    // ─── Read ───

    function showReadFile(file) {
        readFile = file;
        $('readLabel').textContent = file.name + ' · ' + Math.round(file.size / 1024) + ' KB';
        $('readLabel').classList.add('filled');
        $('readPreview').src = URL.createObjectURL(file);
        $('readThumb').classList.add('shown');
        $('readResult').style.display = 'none';
    }

    $('readFile').addEventListener('change', (e) => {
        if (e.target.files.length) showReadFile(e.target.files[0]);
    });

    const dropTarget = $('readLabel');
    ['dragenter', 'dragover'].forEach((type) => dropTarget.addEventListener(type, (e) => {
        e.preventDefault();
        dropTarget.classList.add('dragging');
    }));
    ['dragleave', 'drop'].forEach((type) => dropTarget.addEventListener(type, (e) => {
        e.preventDefault();
        dropTarget.classList.remove('dragging');
    }));
    dropTarget.addEventListener('drop', (e) => {
        if (e.dataTransfer.files.length) showReadFile(e.dataTransfer.files[0]);
    });

    // A screenshot of a code is usually in the clipboard, not on disk.
    document.addEventListener('paste', (e) => {
        const item = Array.from(e.clipboardData.items).find((i) => i.type.startsWith('image/'));
        if (!item) return;
        const file = item.getAsFile();
        if (!file) return;
        showReadFile(file);
        document.querySelector('.tab-btn[data-panel="readPanel"]').click();
        setStatus('image pasted', 'ok');
    });

    $('readBtn').addEventListener('click', () => run($('readBtn'), async () => {
        if (!readFile) throw new Error('choose an image first');

        const form = new FormData();
        form.append('file', readFile);
        const result = await postForm('/api/v0/qr/read', form);

        const block = $('readResult');
        block.classList.toggle('error', !result.found);
        $('readResultLabel').textContent = result.found ? 'decoded text' : 'nothing found';
        $('readResultMeta').textContent = result.found ? result.format : '';
        block.style.display = 'block';

        const body = $('readResultText');
        body.textContent = '';
        if (!result.found) {
            body.textContent = 'no QR code in this image — try a sharper or less cropped picture';
            setStatus('no code found', 'err');
            return;
        }

        // A decoded link is worth being a link.
        if (/^https?:\/\//i.test(result.text)) {
            const link = document.createElement('a');
            link.href = result.text;
            link.textContent = result.text;
            link.rel = 'noreferrer noopener';
            link.target = '_blank';
            body.appendChild(link);
        } else {
            body.textContent = result.text;
        }
        setStatus('code read', 'ok');
    }));

    $('readCopy').addEventListener('click', async () => {
        const text = $('readResultText').textContent;
        if (!text) return;
        await writeClip(text);
        flash($('readCopy'));
    });

    // ─── Short link ───

    $('shortBtn').addEventListener('click', () => run($('shortBtn'), async () => {
        const url = value('shortUrlInput');
        if (!url) throw new Error('a url is required');

        const body = qrOptions();
        body.url = url;
        body.name = value('shortName') || null;

        const result = await postJson('/api/v0/short-url/qr', body);
        shortened = result;

        $('shortPreview').src = result.qr;
        $('shortLink').textContent = result.shortUrl;
        $('shortId').textContent = result.id;
        $('shortToken').textContent = result.token;
        $('shortDownload').href = result.qr;
        $('shortDownload').download = 'qr-' + result.id + '.' + result.format;
        $('shortResult').classList.add('shown');
        setStatus('short link created', 'ok');
    }));

    $('shortCopyLink').addEventListener('click', async () => {
        if (!shortened) return;
        await writeClip(shortened.shortUrl);
        flash($('shortCopyLink'));
    });

    $('shortCopyToken').addEventListener('click', async () => {
        if (!shortened) return;
        await writeClip(shortened.token);
        flash($('shortCopyToken'));
    });
})();

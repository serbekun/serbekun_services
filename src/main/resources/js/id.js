// id.js — UUIDs, ULIDs, tokens and random bytes for the
// /static/v0/html/id.html page.
(function () {
    'use strict';

    let type = 'uuid';

    // One selection per button group, so switching kinds keeps whatever was
    // chosen for the others.
    const choice = {
        uuidVersion: 'v4',
        uuidFormat: 'canonical',
        ulidFormat: 'canonical',
        alphabet: 'base62',
        bytesFormat: 'hex'
    };

    // What each kind is actually for — the thing a picker cannot say.
    const NOTES = {
        uuid: 'v4 is 122 random bits and reveals nothing. v7 puts a millisecond timestamp in the '
            + 'leading bits, so ids sort by creation time — far kinder to a database index, at the '
            + 'cost of telling anyone who reads one when it was made.',
        ulid: '128 bits — a millisecond timestamp then 80 random — written in Crockford base32, which '
            + 'drops the characters that look alike. Sorts by time as text, and ids made in the same '
            + 'millisecond still ascend.',
        token: 'Drawn from a SecureRandom, so these are safe as session tokens and api keys. The bit '
            + 'count below is what actually says how hard one is to guess; base58 is the one to pick '
            + 'if a person will ever read it off a screen.',
        bytes: 'Raw randomness, written however you need it — a key, a salt, an iv. The bytes are the '
            + 'same in every format.'
    };

    const $ = (id) => document.getElementById(id);

    // ─── Requests ───

    async function get(path) {
        const res = await fetch(path, { headers: { 'Accept': 'application/json' } });
        const payload = await res.json().catch(() => null);
        if (!res.ok) {
            throw new Error((payload && (payload.error || payload.message)) || 'request failed (' + res.status + ')');
        }
        return payload;
    }

    /** The query string for the kind currently picked. */
    function query() {
        const params = new URLSearchParams();
        params.set('count', $('count').value.trim() || '1');

        if (type === 'uuid') {
            params.set('version', choice.uuidVersion);
            params.set('format', choice.uuidFormat);
        } else if (type === 'ulid') {
            params.set('format', choice.ulidFormat);
        } else if (type === 'token') {
            params.set('length', $('length').value.trim() || '32');
            if (choice.alphabet === 'custom') {
                params.set('chars', $('chars').value);
            } else {
                params.set('alphabet', choice.alphabet);
            }
        } else {
            params.set('length', $('length').value.trim() || '32');
            params.set('format', choice.bytesFormat);
        }

        const base = type === 'uuid' ? '/api/v0/id/uuid'
            : type === 'ulid' ? '/api/v0/id/ulid'
            : type === 'token' ? '/api/v0/random/token'
            : '/api/v0/random/bytes';
        return base + '?' + params.toString();
    }

    // ─── Rendering ───

    let values = [];

    function render(result) {
        values = result.values;

        const list = $('values');
        list.textContent = '';
        result.values.forEach((value) => {
            const row = document.createElement('button');
            row.type = 'button';
            row.className = 'value-row';
            row.textContent = value;
            row.title = 'click to copy';
            row.addEventListener('click', async () => {
                await writeClip(value);
                row.classList.add('copied');
                setTimeout(() => row.classList.remove('copied'), 900);
            });
            list.appendChild(row);
        });

        const parts = [result.count + ' × ' + result.type];
        if (result.version) parts.push(result.version);
        if (result.alphabet) parts.push(result.alphabet);
        if (result.format) parts.push(result.format);
        parts.push(result.bits + ' bits each');

        $('resultLabel').textContent = result.type;
        $('meta').textContent = parts.join(' · ');
        $('result').classList.remove('error');
        $('result').style.display = 'block';
    }

    function renderError(message) {
        values = [];
        const list = $('values');
        list.textContent = '';
        const line = document.createElement('div');
        line.className = 'error-text';
        line.textContent = message;
        list.appendChild(line);

        $('resultLabel').textContent = 'not generated';
        $('meta').textContent = '';
        $('result').classList.add('error');
        $('result').style.display = 'block';
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
    function setStatus(msg, kind) {
        const el = $('status');
        el.textContent = msg;
        el.className = 'status-line' + (kind ? ' ' + kind : '');
        clearTimeout(statusTimer);
        if (msg && kind) statusTimer = setTimeout(() => {
            el.textContent = '';
            el.className = 'status-line';
        }, 5000);
    }

    // ─── Kind switcher ───

    function showOptionsFor(kind) {
        $('uuidOptions').classList.toggle('hidden', kind !== 'uuid');
        $('ulidOptions').classList.toggle('hidden', kind !== 'ulid');
        $('tokenOptions').classList.toggle('hidden', kind !== 'token');
        $('bytesOptions').classList.toggle('hidden', kind !== 'bytes');

        // Only tokens and bytes have a length, and the two mean different units.
        const sized = kind === 'token' || kind === 'bytes';
        $('lengthField').classList.toggle('hidden', !sized);
        $('lengthLabel').textContent = kind === 'bytes' ? 'length · bytes' : 'length · characters';
        // The unit changes with the kind, so the number goes back to its default.
        if (sized) $('length').value = 32;

        $('note').textContent = NOTES[kind];
    }

    document.querySelectorAll('.tab-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach((b) => b.classList.toggle('active', b === btn));
            type = btn.dataset.type;
            showOptionsFor(type);
            setStatus('');
        });
    });

    document.querySelectorAll('.enc-btn[data-group]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const group = btn.dataset.group;
            document.querySelectorAll('.enc-btn[data-group="' + group + '"]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            choice[group] = btn.dataset.value;

            if (group === 'alphabet') {
                $('charsField').classList.toggle('hidden', btn.dataset.value !== 'custom');
            }
        });
    });

    // ─── Generate ───

    $('runBtn').addEventListener('click', async () => {
        const btn = $('runBtn');
        btn.disabled = true;
        try {
            render(await get(query()));
            setStatus('generated', 'ok');
        } catch (e) {
            renderError(e.message);
            setStatus(e.message, 'err');
        } finally {
            btn.disabled = false;
        }
    });

    $('copyAll').addEventListener('click', async () => {
        if (!values.length) return;
        await writeClip(values.join('\n'));
        const btn = $('copyAll');
        btn.textContent = 'copied';
        btn.classList.add('copied');
        setTimeout(() => {
            btn.textContent = 'copy all';
            btn.classList.remove('copied');
        }, 1400);
    });

    showOptionsFor('uuid');
})();

// encoding.js — live conversion between base64 / base64url / base32 / hex /
// url / utf-8 for the /static/v0/html/encoding.html page.
(function () {
    'use strict';

    let from = 'utf8';
    let to = 'base64';
    let lastOutput = '';

    // Every keystroke would otherwise be a request; a short pause after
    // typing stops is the moment the answer is actually wanted.
    let debounce;
    // Answers can come back out of order, so only the newest one may render.
    let pending = 0;

    const $ = (id) => document.getElementById(id);

    // ─── Requests ───

    async function convert(data) {
        const res = await fetch('/api/v0/encoding/convert', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ data: data, from: from, to: to })
        });
        const payload = await res.json().catch(() => null);
        if (!res.ok) {
            throw new Error((payload && (payload.error || payload.message)) || 'request failed (' + res.status + ')');
        }
        return payload;
    }

    // ─── Rendering ───

    function showEmpty() {
        lastOutput = '';
        const body = $('output');
        body.textContent = 'nothing to convert yet';
        body.classList.add('empty');
        $('result').classList.remove('error');
        $('resultLabel').textContent = 'output';
        $('byteCount').textContent = '';
        setStatus('');
    }

    function showResult(result) {
        lastOutput = result.data;
        const body = $('output');
        body.textContent = result.data === '' ? '(empty)' : result.data;
        body.classList.toggle('empty', result.data === '');
        $('result').classList.remove('error');
        $('resultLabel').textContent = result.to;
        $('byteCount').textContent = result.bytes + (result.bytes === 1 ? ' byte' : ' bytes');
        setStatus('');
    }

    function showError(message) {
        lastOutput = '';
        const body = $('output');
        body.textContent = message;
        body.classList.remove('empty');
        $('result').classList.add('error');
        $('resultLabel').textContent = 'cannot read as ' + from;
        $('byteCount').textContent = '';
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

    // ─── The conversion itself ───

    function schedule() {
        clearTimeout(debounce);
        debounce = setTimeout(run, 200);
    }

    async function run() {
        const data = $('input').value;
        if (data === '') {
            showEmpty();
            return;
        }

        const ticket = ++pending;
        try {
            const result = await convert(data);
            if (ticket === pending) showResult(result);
        } catch (e) {
            if (ticket === pending) showError(e.message);
        }
    }

    // ─── Format pickers ───

    document.querySelectorAll('.fmt-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            const side = btn.dataset.side;
            document.querySelectorAll('.fmt-btn[data-side="' + side + '"]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            if (side === 'from') {
                from = btn.dataset.format;
            } else {
                to = btn.dataset.format;
            }
            run();
        });
    });

    function select(side, format) {
        document.querySelectorAll('.fmt-btn[data-side="' + side + '"]')
            .forEach((b) => b.classList.toggle('active', b.dataset.format === format));
    }

    // ─── Swap ───

    // Swapping carries the output back into the input, which is what turns
    // "encode this" into "now decode it again" in one click.
    $('swapBtn').addEventListener('click', () => {
        const carried = lastOutput;
        const previousFrom = from;

        from = to;
        to = previousFrom;
        select('from', from);
        select('to', to);

        if (carried) $('input').value = carried;
        run();
    });

    // ─── Copy ───

    $('copyBtn').addEventListener('click', async () => {
        if (!lastOutput) return;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(lastOutput);
            } else {
                const ta = Object.assign(document.createElement('textarea'), {
                    value: lastOutput,
                    style: 'position:fixed;left:-9999px'
                });
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
            }
            const btn = $('copyBtn');
            btn.textContent = 'copied';
            btn.classList.add('copied');
            setTimeout(() => {
                btn.textContent = 'copy';
                btn.classList.remove('copied');
            }, 1400);
        } catch (e) {
            setStatus('could not copy', 'err');
        }
    });

    // ─── Go ───

    $('input').addEventListener('input', schedule);
    showEmpty();
})();

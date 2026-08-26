// json.js — validate, format, minify, query and diff for the
// /static/v0/html/json.html page.
(function () {
    'use strict';

    let mode = 'format';
    const choice = { indent: '2', syntax: '' };

    const NOTES = {
        format: 'Re-writes the document with indentation. Sorting the keys is what makes two '
            + 'documents comparable line by line — and what makes a diff of them readable.',
        minify: 'Strips every byte that is not part of the value. The saving is shown below the result.',
        validate: 'Says whether the document parses, and where it stops parsing if it does not. '
            + 'Duplicate keys count as invalid here: a lenient parser keeps the last one and quietly '
            + 'drops the rest, which is data loss dressed up as success.',
        query: 'A JSON Pointer (/store/book/0/title) finds at most one value and is the same syntax a '
            + 'patch uses. A JSONPath ($..book[?(@.price < 10)]) can wildcard, slice and filter, and '
            + 'finds as many as match.',
        diff: 'Compares the two documents and answers with an RFC 6902 patch — the operations that '
            + 'turn the first into the second. Numbers compare by value, so 1 and 1.0 are not a change.'
    };

    const $ = (id) => document.getElementById(id);

    // ─── Requests ───

    /** Every route takes the document as the body itself, so this posts it raw. */
    async function post(path, body, expectJson) {
        const res = await fetch(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: body
        });

        if (!res.ok) {
            const payload = await res.json().catch(() => null);
            throw new Error((payload && (payload.error || payload.message)) || 'request failed (' + res.status + ')');
        }
        return expectJson ? res.json() : res.text();
    }

    // ─── Rendering ───

    let output = '';

    function show(label, text, meta, canReuse) {
        output = text;
        $('output').textContent = text;
        $('resultLabel').textContent = label;
        $('meta').textContent = meta || '';
        $('meta').style.display = meta ? 'block' : 'none';
        $('useAsInput').classList.toggle('hidden', !canReuse);
        $('result').classList.remove('error');
        $('result').style.display = 'block';
    }

    function showError(message) {
        output = '';
        $('output').textContent = message;
        $('resultLabel').textContent = 'failed';
        $('meta').style.display = 'none';
        $('useAsInput').classList.add('hidden');
        $('result').classList.add('error');
        $('result').style.display = 'block';
    }

    function showVerdict(valid, text) {
        const verdict = $('verdict');
        verdict.className = 'verdict shown ' + (valid ? 'ok' : 'bad');
        $('verdictMark').textContent = valid ? '✓' : '✗';
        $('verdictText').textContent = text;
    }

    function hideAll() {
        $('result').style.display = 'none';
        $('verdict').className = 'verdict';
    }

    function bytes(text) {
        return new TextEncoder().encode(text).length;
    }

    function formatBytes(count) {
        return count < 1024 ? count + ' B' : (count / 1024).toFixed(1) + ' KB';
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

    // ─── The five operations ───

    async function runFormat() {
        const source = $('input').value;
        const params = new URLSearchParams({ indent: choice.indent });
        if ($('sort').checked) params.set('sort', 'true');

        const result = await post('/api/v0/json/format?' + params, source, false);
        show('formatted', result, formatBytes(bytes(result)) + ' · ' + result.split('\n').length + ' lines', true);
    }

    async function runMinify() {
        const source = $('input').value;
        const params = new URLSearchParams();
        if ($('sort').checked) params.set('sort', 'true');

        const result = await post('/api/v0/json/minify?' + params, source, false);

        // Both sides are here, so the saving can be shown without asking for it.
        const before = bytes(source);
        const after = bytes(result);
        const saved = before > 0 ? Math.round((1 - after / before) * 100) : 0;
        show('minified', result,
            formatBytes(before) + ' → ' + formatBytes(after) + ' · ' + saved + '% smaller', true);
    }

    async function runValidate() {
        const result = await post('/api/v0/json/validate', $('input').value, true);

        if (result.valid) {
            showVerdict(true, 'valid JSON · ' + formatBytes(result.bytes));
            return;
        }
        const where = result.line ? ' at line ' + result.line + ', column ' + result.column : '';
        showVerdict(false, result.error + where);
    }

    async function runQuery() {
        const expression = $('expression').value;
        if (!expression && expression !== '') throw new Error('an expression is required');

        const params = new URLSearchParams({ expression: expression });
        if (choice.syntax) params.set('syntax', choice.syntax);

        const result = await post('/api/v0/json/query?' + params, $('input').value, true);
        const meta = result.count === 0
            ? 'no matches · read as ' + result.syntax
            : result.count + (result.count === 1 ? ' match' : ' matches') + ' · read as ' + result.syntax
                + (result.paths.length ? ' · ' + result.paths.join('  ') : '');

        show(result.count === 1 ? 'match' : 'matches',
            JSON.stringify(result.matches, null, 2), meta, false);
    }

    async function runDiff() {
        const body = JSON.stringify({
            from: JSON.parse($('input').value),
            to: JSON.parse($('target').value)
        });

        const result = await post('/api/v0/json/diff', body, true);
        show('json patch', JSON.stringify(result.patch, null, 2),
            result.equal ? 'the documents are identical'
                : result.operations + (result.operations === 1 ? ' operation' : ' operations'),
            false);
    }

    const RUNNERS = {
        format: runFormat,
        minify: runMinify,
        validate: runValidate,
        query: runQuery,
        diff: runDiff
    };

    // ─── Mode switcher ───

    function showOptionsFor(next) {
        $('targetField').classList.toggle('hidden', next !== 'diff');
        $('queryOptions').classList.toggle('hidden', next !== 'query');

        // Only format and minify write a document back out.
        const writes = next === 'format' || next === 'minify';
        $('writeOptions').classList.toggle('hidden', !writes);
        $('indentRow').classList.toggle('hidden', next !== 'format');

        $('inputLabel').textContent = next === 'diff' ? 'from' : 'document';
        $('note').textContent = NOTES[next];
        $('runBtn').textContent = next;
    }

    document.querySelectorAll('.tab-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach((b) => b.classList.toggle('active', b === btn));
            mode = btn.dataset.mode;
            showOptionsFor(mode);
            hideAll();
            setStatus('');
        });
    });

    document.querySelectorAll('.enc-btn[data-group]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const group = btn.dataset.group;
            document.querySelectorAll('.enc-btn[data-group="' + group + '"]')
                .forEach((b) => b.classList.toggle('active', b === btn));
            choice[group] = btn.dataset.value;
        });
    });

    // ─── Run ───

    $('runBtn').addEventListener('click', async () => {
        const btn = $('runBtn');
        btn.disabled = true;
        hideAll();
        try {
            await RUNNERS[mode]();
            setStatus(mode + ' done', 'ok');
        } catch (e) {
            // A SyntaxError here is the browser's own parse of a diff side.
            showError(e instanceof SyntaxError ? 'that is not valid JSON: ' + e.message : e.message);
            setStatus('failed', 'err');
        } finally {
            btn.disabled = false;
        }
    });

    // Formatting then querying the result is the common second step.
    $('useAsInput').addEventListener('click', () => {
        if (!output) return;
        $('input').value = output;
        setStatus('moved into the input', 'ok');
    });

    $('copyBtn').addEventListener('click', async () => {
        if (!output) return;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(output);
            } else {
                const ta = Object.assign(document.createElement('textarea'), {
                    value: output,
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

    showOptionsFor('format');
})();

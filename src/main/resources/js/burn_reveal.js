(function () {
    'use strict';

    function burnId() {
        return document.body.dataset.burnId || '';
    }

    async function revealSecret() {
        const id = burnId();
        if (!id) {
            showStatus('This link is not valid', true);
            return;
        }

        const btn = document.getElementById('revealBtn');
        btn.disabled = true;
        btn.textContent = 'Revealing...';

        try {
            const resp = await fetch('/api/v0/burn/' + encodeURIComponent(id) + '/reveal', {
                method: 'POST'
            });

            if (!resp.ok) {
                // Missing, expired, already opened and blocked are one answer.
                showStatus('This link does not exist or has already been opened', true);
                document.getElementById('revealBox').style.display = 'none';
                return;
            }

            const data = await resp.json();
            document.getElementById('revealBox').style.display = 'none';
            document.getElementById('secretText').textContent = data.text || '';
            document.getElementById('secretBox').style.display = 'block';
        } catch (err) {
            showStatus('Error: ' + err.message, true);
            btn.disabled = false;
            btn.textContent = 'Reveal secret';
        }
    }

    function copySecret() {
        const text = document.getElementById('secretText').textContent;
        const fb = document.getElementById('copyFeedback');
        const done = () => { fb.textContent = 'Copied!'; fb.style.color = 'var(--ok)'; };
        const fail = () => { fb.textContent = 'Could not copy. Select and copy manually.'; fb.style.color = 'var(--err)'; };

        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(text).then(done).catch(fail);
        } else {
            try {
                const range = document.createRange();
                range.selectNodeContents(document.getElementById('secretText'));
                window.getSelection().removeAllRanges();
                window.getSelection().addRange(range);
                document.execCommand('copy');
                done();
            } catch (e) {
                fail();
            }
        }
        setTimeout(() => { fb.textContent = ''; }, 2500);
    }

    function showStatus(message, isError) {
        const el = document.getElementById('status');
        el.textContent = message;
        el.className = 'status-line' + (isError ? ' err' : '');
    }

    window.revealSecret = revealSecret;
    window.copySecret = copySecret;
})();

(function () {
    let debounceTimer = null;
    let currentUrl = '';
    let currentTitle = '';
    let currentExt = 'mp4';

    const urlInput     = document.getElementById('urlInput');
    const videoInfoEl  = document.getElementById('videoInfo');
    const infoLabel    = document.getElementById('infoHeaderLabel');
    const titleEl      = document.getElementById('videoTitle');
    const metaEl       = document.getElementById('videoMeta');
    const descEl       = document.getElementById('videoDescription');
    const downloadBtn  = document.getElementById('downloadBtn');

    urlInput.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        const url = this.value.trim();
        if (!url) {
            videoInfoEl.style.display = 'none';
            downloadBtn.disabled = true;
            currentUrl = '';
            setStatus('');
            return;
        }
        debounceTimer = setTimeout(() => fetchVideoInfo(url), 600);
    });

    urlInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            clearTimeout(debounceTimer);
            fetchVideoInfo(this.value.trim());
        }
    });

    async function fetchVideoInfo(url) {
        if (!url) return;
        setStatus('fetching video info...', '');
        videoInfoEl.style.display = 'none';
        downloadBtn.disabled = true;
        currentUrl = '';

        try {
            const res = await fetch('/api/v0/youtube/info?url=' + encodeURIComponent(url));
            if (!res.ok) {
                const txt = await res.text();
                let msg;
                try { msg = JSON.parse(txt).error || txt; } catch { msg = txt || 'server error ' + res.status; }
                throw new Error(msg);
            }
            const data = await res.json();
            displayInfo(data);
            currentUrl = url;
            downloadBtn.disabled = false;
            setStatus('info loaded', 'ok');
        } catch (e) {
            videoInfoEl.classList.add('error');
            videoInfoEl.style.display = 'block';
            infoLabel.textContent = 'error';
            titleEl.textContent = 'failed to load video info';
            metaEl.innerHTML = '';
            descEl.textContent = e.message;
            setStatus('error: ' + e.message, 'err');
        }
    }

    function displayInfo(data) {
        videoInfoEl.classList.remove('error');
        infoLabel.textContent = 'video info';

        currentTitle = data.title || 'video';
        currentExt   = data.ext   || 'mp4';

        titleEl.textContent = data.title || 'unknown title';

        const parts = [];
        if (data.duration !== undefined) {
            const m = Math.floor(data.duration / 60);
            const s = String(data.duration % 60).padStart(2, '0');
            parts.push('<span>' + m + ':' + s + '</span>');
        }
        if (data.uploader)              parts.push('<span>' + esc(data.uploader) + '</span>');
        if (data.view_count !== undefined) parts.push('<span>' + Number(data.view_count).toLocaleString() + ' views</span>');
        if (data.like_count !== undefined) parts.push('<span>' + Number(data.like_count).toLocaleString() + ' likes</span>');
        if (data.upload_date) {
            const d = data.upload_date;
            parts.push('<span>' + d.slice(0,4) + '-' + d.slice(4,6) + '-' + d.slice(6,8) + '</span>');
        }
        metaEl.innerHTML = parts.join('');
        descEl.textContent = data.description || 'no description available.';

        videoInfoEl.style.display = 'block';
    }

    window.downloadVideo = function () {
        if (!currentUrl) { setStatus('error: no url', 'err'); return; }
        const safe = currentTitle.replace(/[\\/:*?"<>|]/g, '_').replace(/\s+/g, ' ').trim().substring(0, 200);
        const a = Object.assign(document.createElement('a'), {
            href: '/api/v0/youtube/download?url=' + encodeURIComponent(currentUrl),
            download: safe + '.' + currentExt
        });
        document.body.appendChild(a); a.click(); document.body.removeChild(a);
        setStatus('download started', 'ok');
    };

    let statusTimer;
    function setStatus(msg, type) {
        const el = document.getElementById('status');
        el.textContent = msg;
        el.className = 'status-line' + (type ? ' ' + type : '');
        clearTimeout(statusTimer);
        if (msg && type !== '') statusTimer = setTimeout(() => { el.textContent = ''; el.className = 'status-line'; }, 5000);
    }

    function esc(t) {
        const d = document.createElement('div');
        d.textContent = t;
        return d.innerHTML;
    }
})();
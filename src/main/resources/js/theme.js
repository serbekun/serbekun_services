/* theme.js — applies saved theme before first paint and wires the toggle.
   No saved choice -> no data-theme attribute -> CSS follows the system
   preference via color-scheme: light dark. */
(function () {
    var root = document.documentElement;

    var saved = null;
    try { saved = localStorage.getItem('theme'); } catch (e) {}
    if (saved === 'light' || saved === 'dark') {
        root.dataset.theme = saved;
    }

    document.addEventListener('DOMContentLoaded', function () {
        var btn = document.querySelector('.theme-toggle');
        if (!btn) return;

        btn.addEventListener('click', function () {
            var current = root.dataset.theme ||
                (matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
            var next = current === 'dark' ? 'light' : 'dark';
            root.dataset.theme = next;
            try { localStorage.setItem('theme', next); } catch (e) {}
        });
    });
})();

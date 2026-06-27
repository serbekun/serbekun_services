// Fetches the server version from the API and renders it under the `ver` prompt.
(function () {
    const output = document.getElementById("ver-output");
    if (!output) return;

    fetch("/api/v0/version")
        .then((res) => {
            if (!res.ok) throw new Error("HTTP " + res.status);
            return res.json();
        })
        .then((data) => {
            output.textContent = "serbekun-services " + data.version;
        })
        .catch(() => {
            output.textContent = "ver: unable to read server version";
            output.classList.add("ver-error");
        });
})();

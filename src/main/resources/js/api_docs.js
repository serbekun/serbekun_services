// api_docs.js — base url substitution, copy buttons, filtering and expand-all
// for the API reference page.
(function () {
    const origin = window.location.origin;

    // ─── Base url ───
    const baseUrlEl = document.getElementById("base-url");
    if (baseUrlEl) baseUrlEl.textContent = origin;

    // Examples are written with `$BASE` so they stay readable in the markup;
    // swap it for the real origin so a copied command runs as-is.
    document.querySelectorAll("pre.code").forEach((block) => {
        block.childNodes.forEach((node) => {
            if (node.nodeType === Node.TEXT_NODE && node.nodeValue.includes("$BASE")) {
                node.nodeValue = node.nodeValue.split("$BASE").join(origin);
            }
        });
    });

    // ─── Copy buttons ───
    document.querySelectorAll(".copy-btn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            // Only the text nodes — the button itself lives inside the <pre>.
            const code = Array.from(btn.parentElement.childNodes)
                .filter((n) => n !== btn)
                .map((n) => n.textContent)
                .join("")
                .trim();
            navigator.clipboard.writeText(code).then(
                () => flash(btn, "copied"),
                () => flash(btn, "failed")
            );
        });
    });

    function flash(btn, text) {
        const original = "copy";
        btn.textContent = text;
        btn.classList.add("done");
        setTimeout(() => {
            btn.textContent = original;
            btn.classList.remove("done");
        }, 1200);
    }

    // ─── Expand / collapse all ───
    const toggleAll = document.getElementById("toggle-all");
    const endpoints = Array.from(document.querySelectorAll("details.endpoint"));

    if (toggleAll) {
        toggleAll.addEventListener("click", () => {
            const expand = toggleAll.textContent.trim() === "expand all";
            endpoints.forEach((d) => {
                if (!d.hidden) d.open = expand;
            });
            toggleAll.textContent = expand ? "collapse all" : "expand all";
        });
    }

    // ─── Filter ───
    const search = document.getElementById("search");
    const noResults = document.getElementById("no-results");
    const groups = Array.from(document.querySelectorAll("section.group"));

    if (search) {
        search.addEventListener("input", () => {
            const query = search.value.trim().toLowerCase();
            let matches = 0;

            endpoints.forEach((d) => {
                const hit = query === "" || d.textContent.toLowerCase().includes(query);
                d.hidden = !hit;
                if (hit) matches++;
                // A filtered-down list is easier to scan opened; restore the
                // collapsed default once the query is cleared.
                if (query !== "") d.open = hit;
                else d.open = false;
            });

            groups.forEach((g) => {
                g.hidden = !g.querySelector("details.endpoint:not([hidden])");
            });

            if (noResults) noResults.classList.toggle("visible", matches === 0);
            if (toggleAll) toggleAll.textContent = query === "" ? "expand all" : "collapse all";
        });
    }

    // ─── Deep links from the table of contents ───
    document.querySelectorAll(".toc a").forEach((a) => {
        a.addEventListener("click", () => {
            if (search && search.value !== "") {
                search.value = "";
                search.dispatchEvent(new Event("input"));
            }
        });
    });
})();

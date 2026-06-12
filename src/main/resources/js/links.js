let currentToken = null;
let editingUuid = null;

function getTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('token') || urlParams.get('auth') || '';
}

function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (currentToken) {
        headers['Authorization'] = 'Bearer ' + currentToken;
    }
    return headers;
}

async function loadLinks() {
    try {
        showStatus('Loading links...');
        const response = await fetch('/api/v0/catalogs/links', {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to load links: ${response.status}`);
        }

        const data = await response.json();
        displayLinks(data);
        showStatus('Links loaded successfully');
    } catch (error) {
        showStatus('Error loading links: ' + error.message);
        document.getElementById('linksList').innerHTML =
            '<div class="link-item error"><strong>Failed to load links</strong></div>';
    }
}

function displayLinks(links) {
    const container = document.getElementById('linksList');
    container.innerHTML = '';

    const linkArray = Object.values(links || {});
    if (linkArray.length === 0) {
        container.innerHTML =
            '<div class="link-item"><strong>No links found</strong><br><small>Add your first link using the "Add Link" tab</small></div>';
        return;
    }

    linkArray.forEach(link => {
        const item = document.createElement('div');
        item.className = 'link-item';

        const descHtml = escapeHtml(link.description || '');
        const hasDescription = descHtml.trim().length > 0;

        item.innerHTML = `
            <div class="link-header" onclick="toggleDescription(this)" role="button" aria-expanded="false">
                <div>
                    <div class="link-title">${escapeHtml(link.name)}</div>
                    <div class="link-url"><a href="${escapeHtml(link.url)}" target="_blank" onclick="event.stopPropagation();">${escapeHtml(link.url)}</a></div>
                </div>
                <span class="toggle-arrow ${hasDescription ? '' : 'rotated'}" style="${hasDescription ? '' : 'visibility: hidden;'}">></span>
            </div>
            <div class="link-description-wrapper ${hasDescription ? '' : 'open'}">
                <div class="link-description">${hasDescription ? descHtml : '<em style="color:#666;">No description</em>'}</div>
            </div>
            <div class="link-actions">
                <button onclick="event.stopPropagation(); editLink('${link.uuid}', '${escapeHtml(link.url)}', '${escapeHtml(link.name)}', '${escapeHtml(link.description || '')}')">Edit</button>
                <button onclick="event.stopPropagation(); deleteLink('${link.uuid}')">Delete</button>
            </div>
        `;
        container.appendChild(item);
    });
}

function toggleDescription(headerElement) {
    const wrapper = headerElement.nextElementSibling;
    const arrow = headerElement.querySelector('.toggle-arrow');
    if (!wrapper || !arrow) return;

    const isOpen = wrapper.classList.contains('open');
    if (isOpen) {
        wrapper.classList.remove('open');
        arrow.classList.remove('rotated');
    } else {
        wrapper.classList.add('open');
        arrow.classList.add('rotated');
    }
}

async function addLink() {
    const url = document.getElementById('addUrl').value.trim();
    const name = document.getElementById('addName').value.trim();
    const description = document.getElementById('addDescription').value.trim();

    if (!url) {
        showStatus('URL and Name are required');
        return;
    }

    try {
        showStatus('Adding link...');
        const response = await fetch('/api/v0/catalogs/links', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ url, name, description })
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `Failed to add link: ${response.status}`);
        }

        const result = await response.json();
        const newToken = result.token;

        // Clear form
        document.getElementById('addUrl').value = '';
        document.getElementById('addName').value = '';
        document.getElementById('addDescription').value = '';

        // Show token in modal
        if (newToken) {
            openTokenModal(newToken);
            // Optionally apply it automatically
            currentToken = newToken;
            document.getElementById('manualToken').value = newToken;
        }

        showStatus('Link added successfully');
        switchTab(0);
        loadLinks();
    } catch (error) {
        showStatus('Error adding link: ' + error.message);
    }
}

function editLink(uuid, url, name, description) {
    editingUuid = uuid;
    document.getElementById('editUuid').value = uuid;
    document.getElementById('editUrl').value = unescapeHtml(url);
    document.getElementById('editName').value = unescapeHtml(name);
    document.getElementById('editDescription').value = unescapeHtml(description);
    switchTab(2);
}

async function updateLink() {
    const uuid = document.getElementById('editUuid').value;
    const url = document.getElementById('editUrl').value.trim();
    const name = document.getElementById('editName').value.trim();
    const description = document.getElementById('editDescription').value.trim();

    if (!uuid || !url || !name) {
        showStatus('UUID, URL and Name are required');
        return;
    }

    if (!currentToken) {
        showStatus('No token available. Please paste your access token above.');
        return;
    }

    try {
        showStatus('Updating link...');
        const response = await fetch(`/api/v0/catalogs/links/${uuid}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify({ uuid, token: currentToken, url, name, description })
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `Failed to update link: ${response.status}`);
        }

        showStatus('Link updated successfully');
        switchTab(0);
        loadLinks();
    } catch (error) {
        showStatus('Error updating link: ' + error.message);
    }
}

async function deleteLink(uuid) {
    if (!confirm('Are you sure you want to delete this link?')) {
        return;
    }

    if (!currentToken) {
        showStatus('No token available. Please paste your access token above.');
        return;
    }

    try {
        showStatus('Deleting link...');
        const response = await fetch(`/api/v0/catalogs/links/${uuid}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
            body: JSON.stringify({ uuid, token: currentToken })
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `Failed to delete link: ${response.status}`);
        }

        showStatus('Link deleted successfully');
        loadLinks();
    } catch (error) {
        showStatus('Error deleting link: ' + error.message);
    }
}

function switchTab(tabIndex) {
    const tabs = document.querySelectorAll('.tab-btn');
    const viewTab = document.getElementById('viewTab');
    const addTab = document.getElementById('addTab');
    const editTab = document.getElementById('editTab');
    const editBtn = document.getElementById('editBtn');

    tabs.forEach((btn, idx) => {
        btn.classList.toggle('active', idx === tabIndex);
    });

    if (tabIndex === 0) {
        viewTab.style.display = 'block';
        addTab.style.display = 'none';
        editTab.style.display = 'none';
        editBtn.style.display = 'none';
        loadLinks();
    } else if (tabIndex === 1) {
        viewTab.style.display = 'none';
        addTab.style.display = 'block';
        editTab.style.display = 'none';
        editBtn.style.display = 'none';
    } else if (tabIndex === 2) {
        viewTab.style.display = 'none';
        addTab.style.display = 'none';
        editTab.style.display = 'block';
        editBtn.style.display = 'inline-block';
        editBtn.classList.add('active');
    }
    document.getElementById('status').textContent = '';
}

function showStatus(message) {
    const statusEl = document.getElementById('status');
    statusEl.textContent = message;
    if (message.includes('Error') || message.includes('Failed')) {
        statusEl.style.color = '#ff8888';
    } else if (message.includes('success')) {
        statusEl.style.color = '#ffffff';
    } else {
        statusEl.style.color = '#cccccc';
    }

    if (message !== 'Loading links...' && message !== 'Links loaded successfully') {
        setTimeout(() => {
            if (statusEl.textContent === message) {
                statusEl.textContent = '';
            }
        }, 5000);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function unescapeHtml(text) {
    const div = document.createElement('div');
    div.innerHTML = text;
    return div.textContent;
}

// ---- TOKEN MODAL LOGIC ----
function openTokenModal(token) {
    document.getElementById('modalTokenText').textContent = token;
    document.getElementById('tokenModal').style.display = 'flex';
    document.getElementById('modalCopyFeedback').textContent = '';
}

function closeTokenModal() {
    document.getElementById('tokenModal').style.display = 'none';
}

function copyToClipboard(text) {
    return new Promise((resolve, reject) => {
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(text).then(resolve).catch(() => {
                fallbackCopy(text, resolve, reject);
            });
        } else {
            fallbackCopy(text, resolve, reject);
        }
    });
}

function fallbackCopy(text, resolve, reject) {
    try {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.left = '-9999px';
        textarea.style.top = '-9999px';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        const successful = document.execCommand('copy');
        document.body.removeChild(textarea);
        if (successful) {
            resolve();
        } else {
            reject(new Error('execCommand failed'));
        }
    } catch (err) {
        reject(err);
    }
}

function copyModalToken() {
    const token = document.getElementById('modalTokenText').textContent;
    if (!token) return;
    copyToClipboard(token).then(() => {
        const feedback = document.getElementById('modalCopyFeedback');
        feedback.textContent = 'Token copied to clipboard!';
        setTimeout(() => {
            if (feedback.textContent === 'Token copied to clipboard!') {
                feedback.textContent = '';
            }
        }, 2500);
    }).catch(() => {
        document.getElementById('modalCopyFeedback').textContent = 
            'Could not copy automatically. Please select and copy manually.';
    });
}

// Close modal when clicking overlay background
document.addEventListener('click', function(e) {
    if (e.target.id === 'tokenModal') {
        closeTokenModal();
    }
});

// ---- MANUAL TOKEN APPLICATION ----
function applyToken() {
    const input = document.getElementById('manualToken');
    const token = input.value.trim();
    if (token) {
        currentToken = token;
        showStatus('Token applied successfully');
        // Reload links with new token
        if (document.getElementById('viewTab').style.display !== 'none') {
            loadLinks();
        }
    } else {
        showStatus('Please enter a valid token');
    }
}

// Initialize
window.addEventListener('DOMContentLoaded', function() {
    const urlToken = getTokenFromUrl();
    if (urlToken) {
        currentToken = urlToken;
        document.getElementById('manualToken').value = urlToken;
        showStatus('Token loaded from URL');
    } else {
        showStatus('No token found. You can paste one above or create a new link to get a token.');
    }
    switchTab(0); // Load links by default
});
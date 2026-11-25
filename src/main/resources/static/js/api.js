/**
 * Civic Issue Heatmap - Shared API & Frontend Utilities
 */

const API_BASE = '/api/v1';
const TOKEN_KEY = 'civicheat_token';
const USER_KEY = 'civicheat_user';

// ==========================================
// Authentication State Management
// ==========================================

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    try {
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        return null;
    }
}

function setAuth(token, user) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

function isAuthenticated() {
    return !!getToken();
}

function hasRole(role) {
    const user = getUser();
    return user && user.role === role;
}

function isAdmin() {
    return hasRole('ADMIN');
}

function isOfficer() {
    return hasRole('OFFICER');
}

// ==========================================
// API Fetch Wrapper
// ==========================================

async function apiFetch(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;
    
    const headers = options.headers || {};
    const token = getToken();

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData) && !headers['Content-Type'] && options.method && options.method !== 'GET') {
        headers['Content-Type'] = 'application/json';
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(url, config);

        if (response.status === 401) {
            clearAuth();
            if (!window.location.pathname.includes('login.html')) {
                showToast('Session expired. Please sign in.', 'warning');
                setTimeout(() => window.location.href = '/login.html', 1200);
            }
        }

        if (!response.ok) {
            let errorMsg = `HTTP ${response.status}: ${response.statusText}`;
            try {
                const errorData = await response.json();
                if (errorData.message) {
                    errorMsg = errorData.message;
                } else if (errorData.fieldErrors) {
                    errorMsg = Object.values(errorData.fieldErrors).join(', ');
                }
            } catch (e) {
                // Ignore json parse error
            }
            throw new Error(errorMsg);
        }

        // Return empty or parsed json
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        return await response.text();
    } catch (err) {
        console.error(`API Error [${options.method || 'GET'} ${url}]:`, err);
        throw err;
    }
}

// ==========================================
// Toast Notifications
// ==========================================

function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span><div>${escapeHtml(message)}</div>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ==========================================
// Formatting & Badge Helpers
// ==========================================

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.innerText = text;
    return div.innerHTML;
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    return d.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function renderSeverityBadge(score, level) {
    const lvl = level || (score >= 75 ? 'CRITICAL' : score >= 50 ? 'HIGH' : score >= 25 ? 'MEDIUM' : 'LOW');
    const badgeClass = `badge-${lvl.toLowerCase()}`;
    return `<span class="badge ${badgeClass}">${lvl} (${score || 0})</span>`;
}

function renderStatusBadge(status) {
    const safeStatus = status || 'SUBMITTED';
    const label = safeStatus.replace('_', ' ');
    return `<span class="status-badge status-${safeStatus}">${label}</span>`;
}

function renderCategoryBadge(category) {
    const icons = {
        'POTHOLE': '🕳️ Pothole',
        'GARBAGE': '🗑️ Garbage',
        'STREETLIGHT': '💡 Streetlight',
        'WATER_LEAK': '🚰 Water Leak',
        'NOISE': '📢 Noise'
    };
    return icons[category] || category;
}

// ==========================================
// Modal Control
// ==========================================

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('open');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('open');
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-backdrop')) {
        e.target.classList.remove('open');
    }
});

// ==========================================
// Route Guard & Navbar Dynamic Setup
// ==========================================

function requireAuth(allowedRoles = null) {
    if (!isAuthenticated()) {
        window.location.href = '/login.html';
        return false;
    }
    if (allowedRoles && Array.isArray(allowedRoles)) {
        const user = getUser();
        if (!user || !allowedRoles.includes(user.role)) {
            showToast('Access denied: Unauthorized role.', 'error');
            setTimeout(() => {
                if (user && user.role === 'CITIZEN') {
                    window.location.href = '/citizen-dashboard.html';
                } else if (user && (user.role === 'ADMIN' || user.role === 'OFFICER')) {
                    window.location.href = '/dashboard.html';
                } else {
                    window.location.href = '/login.html';
                }
            }, 1000);
            return false;
        }
    }
    return true;
}

function setupNavbar() {
    const userContainer = document.getElementById('nav-user-container');
    if (!userContainer) return;

    const user = getUser();
    if (user && isAuthenticated()) {
        const isStaff = user.role === 'ADMIN' || user.role === 'OFFICER';
        userContainer.innerHTML = `
            <div style="position: relative; display: inline-block;">
                <button id="notif-bell-btn" class="btn btn-secondary btn-sm" title="Notifications" style="position: relative;">
                    🔔 <span id="notif-unread-badge" style="display:none; background:#ef4444; color:#fff; border-radius:9999px; padding:0.05rem 0.4rem; font-size:0.7rem; margin-left:0.2rem;">0</span>
                </button>
                <div id="notif-dropdown" style="display:none; position:absolute; right:0; top:115%; width:320px; max-height:380px; overflow-y:auto; background:var(--bg-surface, #1e293b); border:1px solid var(--border-color, #334155); border-radius:10px; box-shadow:0 12px 30px rgba(0,0,0,0.45); z-index:2000; padding:0.75rem;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem; padding-bottom:0.5rem; border-bottom:1px solid var(--border-color, #334155);">
                        <strong style="font-size:0.85rem;">Notifications</strong>
                        <button id="notif-mark-read-btn" class="btn btn-secondary btn-sm" style="font-size:0.72rem; padding:0.2rem 0.5rem;">Mark all read</button>
                    </div>
                    <div id="notif-list" style="font-size:0.8rem; color:var(--text-secondary, #cbd5e1);">Loading...</div>
                </div>
            </div>
            <div class="user-badge">
                <span>👤 <strong>${escapeHtml(user.fullName)}</strong></span>
                <span class="role-pill role-${user.role}">${user.role}</span>
            </div>
            ${isStaff 
                ? `<a href="/dashboard.html" class="nav-link">📊 Dept Command Center</a>` 
                : `<a href="/citizen-dashboard.html" class="nav-link">📋 My Citizen Dashboard</a>`}
            <button id="logout-btn" class="btn btn-secondary btn-sm">Sign Out</button>
        `;

        loadUnreadNotificationCount();

        const bellBtn = document.getElementById('notif-bell-btn');
        const dropdown = document.getElementById('notif-dropdown');
        bellBtn?.addEventListener('click', async (e) => {
            e.stopPropagation();
            const isOpen = dropdown.style.display === 'block';
            dropdown.style.display = isOpen ? 'none' : 'block';
            if (!isOpen) await loadNotificationsDropdown();
        });

        document.getElementById('notif-mark-read-btn')?.addEventListener('click', async () => {
            try {
                await apiFetch('/notifications/mark-all-read', { method: 'POST' });
                document.getElementById('notif-unread-badge').style.display = 'none';
                await loadNotificationsDropdown();
            } catch (err) {
                showToast('Failed to mark notifications read', 'error');
            }
        });

        document.addEventListener('click', (e) => {
            if (dropdown && !dropdown.contains(e.target) && e.target !== bellBtn) {
                dropdown.style.display = 'none';
            }
        });

        document.getElementById('logout-btn')?.addEventListener('click', () => {
            clearAuth();
            showToast('Signed out successfully', 'info');
            setTimeout(() => window.location.href = '/login.html', 500);
        });
    } else {
        userContainer.innerHTML = `
            <a href="/login.html" class="btn btn-primary btn-sm">🔐 Sign In / Register</a>
        `;
    }
}

document.addEventListener('DOMContentLoaded', setupNavbar);

async function loadUnreadNotificationCount() {
    if (!isAuthenticated()) return;
    try {
        const res = await apiFetch('/notifications/unread-count');
        const badge = document.getElementById('notif-unread-badge');
        if (badge && res && typeof res.unreadCount === 'number') {
            if (res.unreadCount > 0) {
                badge.innerText = res.unreadCount;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (e) {
        // Ignore silent polling errors
    }
}

async function loadNotificationsDropdown() {
    const list = document.getElementById('notif-list');
    if (!list) return;
    try {
        const page = await apiFetch('/notifications?size=10');
        const items = page.content || [];
        if (items.length === 0) {
            list.innerHTML = '<div style="padding:0.75rem; text-align:center; color:var(--text-muted);">No notifications yet.</div>';
            return;
        }
        list.innerHTML = items.map(n => `
            <div style="padding:0.5rem; border-radius:6px; margin-bottom:0.35rem; background:${n.isRead ? 'transparent' : 'rgba(59,130,246,0.12)'}; border:1px solid rgba(255,255,255,0.06);">
                <div style="font-weight:600; font-size:0.8rem; margin-bottom:0.15rem;">${escapeHtml(n.title)}</div>
                <div style="font-size:0.75rem; color:var(--text-secondary);">${escapeHtml(n.message)}</div>
                <div style="font-size:0.7rem; color:var(--text-muted); margin-top:0.2rem;">${formatDate(n.createdAt)}</div>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<div style="color:#ef4444; padding:0.5rem;">Failed to load notifications.</div>';
    }
}


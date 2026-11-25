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
            <div class="user-badge">
                <span>👤 <strong>${escapeHtml(user.fullName)}</strong></span>
                <span class="role-pill role-${user.role}">${user.role}</span>
            </div>
            ${isStaff 
                ? `<a href="/dashboard.html" class="nav-link">📊 Dept Command Center</a>` 
                : `<a href="/citizen-dashboard.html" class="nav-link">📋 My Citizen Dashboard</a>`}
            <button id="logout-btn" class="btn btn-secondary btn-sm">Sign Out</button>
        `;

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


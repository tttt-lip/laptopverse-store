// ========================================
// API SERVICE - Laptopverse
// ========================================
// Gestione centralizzata delle chiamate API
// - Fetch wrapper con logging, error handling, auth
// - Supporto guest cart tramite header custom
// - Gestione content-type dinamico
// ========================================

const API_BASE = '/api/v1';
const API_TIMEOUT = 15000; // 15 secondi timeout

// ========================================
// EVENTS SYSTEM (Pub/Sub leggero)
// ========================================
const Events = {
    events: {},

    on(event, callback) {
        if (!this.events[event]) this.events[event] = [];
        this.events[event].push(callback);
        return () => this.off(event, callback); // unsubscribe
    },

    off(event, callback) {
        if (!this.events[event]) return;
        this.events[event] = this.events[event].filter(cb => cb !== callback);
    },

    emit(event, data) {
        if (!this.events[event]) return;
        this.events[event].forEach(callback => {
            try { callback(data); }
            catch (err) { console.error(`[Events] Error in ${event}:`, err); }
        });
    },

    clear() { this.events = {}; }
};

// ========================================
// API SERVICE
// ========================================
const ApiService = {

    // 🔧 CONFIG
    config: {
        baseURL: API_BASE,
        timeout: API_TIMEOUT,
        retries: 1,
        retryDelay: 1000
    },

    // 🔄 FETCH WRAPPER PRINCIPALE
    async fetch(endpoint, options = {}) {
        const { baseURL, timeout, retries, retryDelay } = this.config;
        const guestId = localStorage.getItem('guestCartId');
        const user = JSON.parse(localStorage.getItem('user') || 'null');

        // Headers di base
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Client-Version': '1.0.0',
            ...options.headers
        };

        // Aggiungi header guest cart se presente
        if (guestId && guestId !== 'null' && guestId !== 'undefined') {
            headers['X-Guest-Cart-Id'] = guestId;
        }

        // Aggiungi token auth se utente loggato
        if (user?.token) {
            headers['Authorization'] = `Bearer ${user.token}`;
        }

        // Logging richiesta (solo in dev)
        if (process.env?.NODE_ENV !== 'production' || window?.location?.hostname === 'localhost') {
            console.groupCollapsed(`%c📡 API ${options.method || 'GET'} ${endpoint}`, 'color: #3b82f6; font-weight: 600;');
            console.log('Headers:', headers);
            if (options.body) console.log('Body:', JSON.parse(options.body));
            console.groupEnd();
        }

        let lastError;

        // Retry logic
        for (let attempt = 0; attempt <= retries; attempt++) {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), timeout);

                const response = await fetch(`${baseURL}${endpoint}`, {
                    ...options,
                    headers,
                    credentials: 'include',
                    signal: controller.signal
                });

                clearTimeout(timeoutId);

                // Gestione 204 No Content
                if (response.status === 204) {
                    return { data: null, status: 204, ok: true };
                }

                // Parsing response in base al content-type
                let data;
                const contentType = response.headers.get('content-type') || '';

                if (contentType.includes('application/json')) {
                    data = await response.json();
                } else if (contentType.includes('text/')) {
                    data = { message: await response.text() };
                } else {
                    data = await response.blob().then(blob => ({ blob }));
                }

                // Logging risposta
                if (process.env?.NODE_ENV !== 'production' || window?.location?.hostname === 'localhost') {
                    console.groupCollapsed(`%c✅ RES ${response.status} ${endpoint}`, 'color: #10b981; font-weight: 600;');
                    console.log('Data:', data);
                    console.groupEnd();
                }

                // Gestione errori HTTP
                if (!response.ok) {
                    const error = new ApiError(
                        data?.message || data?.error || `HTTP ${response.status}`,
                        response.status,
                        data,
                        endpoint
                    );

                    // Auto-logout su 401 (tranne che sul login stesso)
                    if (response.status === 401 && !endpoint.includes('/login')) {
                        console.warn('[ApiService] Sessione scaduta o non autorizzata');
                        Events.emit('auth:unauthorized', { endpoint, status: 401 });
                    }

                    // Rate limiting (429)
                    if (response.status === 429) {
                        const retryAfter = response.headers.get('Retry-After');
                        console.warn(`[ApiService] Rate limited. Retry after: ${retryAfter}s`);
                        Events.emit('api:rateLimited', { retryAfter });
                    }

                    throw error;
                }

                // Ritorna oggetto standardizzato
                return {
                    data,
                    status: response.status,
                    headers: Object.fromEntries(response.headers.entries()),
                    ok: true
                };

            } catch (err) {
                lastError = err;

                // AbortError = timeout
                if (err.name === 'AbortError') {
                    console.error(`[ApiService] Timeout dopo ${timeout}ms per ${endpoint}`);
                    throw new ApiError('Timeout della richiesta', 408, null, endpoint);
                }

                // Network error = retry
                if (err.name === 'TypeError' && err.message.includes('fetch')) {
                    if (attempt < retries) {
                        console.warn(`[ApiService] Retry ${attempt + 1}/${retries} per ${endpoint}...`);
                        await new Promise(res => setTimeout(res, retryDelay * (attempt + 1)));
                        continue;
                    }
                }

                // Altri errori: propaga
                break;
            }
        }

        // Tutti i retry falliti
        throw lastError || new ApiError('Errore di connessione', 0, null, endpoint);
    },

    // 🎯 SHORTCUT METHODS - AUTH
    async login(email, password) {
        const res = await this.fetch('/users/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        return res;
    },

    async register(userData) {
        return this.fetch('/users/register', {
            method: 'POST',
            body: JSON.stringify({
                firstName: userData.firstName,
                lastName: userData.lastName,
                email: userData.email,
                password: userData.password
            })
        });
    },

    async logout() {
        try {
            await this.fetch('/users/logout', { method: 'POST' });
        } catch (err) {
            // Ignora errori nel logout (es. token già scaduto)
            console.warn('[ApiService] Logout API error (ignored):', err.message);
        } finally {
            // Pulizia locale garantita
            localStorage.removeItem('user');
            localStorage.removeItem('guestCartId');
        }
    },

    async getCurrentUser() {
        return this.fetch('/users/me');
    },

    async updateProfile(userData) {
        return this.fetch('/users/me', {
            method: 'PUT',
            body: JSON.stringify(userData)
        });
    },

    async changePassword(oldPassword, newPassword) {
        return this.fetch('/users/me/password', {
            method: 'PUT',
            body: JSON.stringify({ oldPassword, newPassword })
        });
    },

    // 🎯 SHORTCUT METHODS - PRODUCTS
    async getProducts(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.fetch(`/products${query ? `?${query}` : ''}`);
    },

    async getProductById(id) {
        return this.fetch(`/products/${id}`);
    },

    async getProductBySlug(slug) {
        return this.fetch(`/products/slug/${slug}`);
    },

    async searchProducts(keyword, filters = {}) {
        const params = new URLSearchParams({ keyword, ...filters });
        return this.fetch(`/products/search?${params.toString()}`);
    },

    async getProductReviews(productId, page = 1, limit = 10) {
        return this.fetch(`/products/${productId}/reviews?page=${page}&limit=${limit}`);
    },

    // 🎯 SHORTCUT METHODS - CATEGORIES
    async getCategories() {
        return this.fetch('/categories');
    },

    async getCategoryById(id) {
        return this.fetch(`/categories/${id}`);
    },

    async createCategory(name, description = '') {
        return this.fetch('/categories', {
            method: 'POST',
            body: JSON.stringify({ name, description })
        });
    },

    async updateCategory(id, data) {
        return this.fetch(`/categories/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async deleteCategory(id) {
        return this.fetch(`/categories/${id}`, { method: 'DELETE' });
    },

    // 🎯 SHORTCUT METHODS - CART
    async getCart() {
        return this.fetch('/cart');
    },

    async createGuestCart() {
        const res = await this.fetch('/cart/guest', { method: 'POST' });
        // Salva l'ID del guest cart in localStorage
        if (res.data?.cartId) {
            localStorage.setItem('guestCartId', res.data.cartId);
        }
        return res;
    },

    async addToCart(productId, quantity = 1) {
        return this.fetch('/cart/items', {
            method: 'POST',
            body: JSON.stringify({ productId, quantity })
        });
    },

    async updateCartItem(itemId, quantity) {
        return this.fetch(`/cart/items/${itemId}`, {
            method: 'PUT',
            body: JSON.stringify({ quantity })
        });
    },

    async removeCartItem(itemId) {
        return this.fetch(`/cart/items/${itemId}`, { method: 'DELETE' });
    },

    async clearCart() {
        return this.fetch('/cart/clear', { method: 'POST' });
    },

    // 🎯 SHORTCUT METHODS - ORDERS
    async getOrders(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.fetch(`/orders${query ? `?${query}` : ''}`);
    },

    async getOrderById(id) {
        return this.fetch(`/orders/${id}`);
    },

    async getAllOrders(params = {}) {
        // Endpoint admin per vedere tutti gli ordini
        const query = new URLSearchParams(params).toString();
        return this.fetch(`/orders/admin${query ? `?${query}` : ''}`);
    },

    async checkout(shippingAddress, paymentMethod = 'card') {
        return this.fetch('/orders/checkout', {
            method: 'POST',
            body: JSON.stringify({
                shippingAddress,
                paymentMethod,
                saveAddress: false // TODO: aggiungere checkbox UI
            })
        });
    },

    async cancelOrder(orderId) {
        return this.fetch(`/orders/${orderId}/cancel`, { method: 'POST' });
    },

    async updateOrderStatus(orderId, status) {
        return this.fetch(`/orders/${orderId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status })
        });
    },

    async enableOrder(orderId, enabled) {
        return this.fetch(`/orders/${orderId}/enable`, {
            method: 'PATCH',
            body: JSON.stringify({ enabled })
        });
    },

    // 🎯 SHORTCUT METHODS - USERS (ADMIN)
    async getAllUsers(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.fetch(`/users${query ? `?${query}` : ''}`);
    },

    async getUserById(id) {
        return this.fetch(`/users/${id}`);
    },

    async updateUser(id, data) {
        return this.fetch(`/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async deleteUser(id) {
        return this.fetch(`/users/${id}`, { method: 'DELETE' });
    },

    async updateUserRole(id, role) {
        return this.fetch(`/users/${id}/role`, {
            method: 'PATCH',
            body: JSON.stringify({ role })
        });
    },

    // 🎯 SHORTCUT METHODS - PRODUCTS (ADMIN)
    async createProduct(productData) {
        return this.fetch('/products', {
            method: 'POST',
            body: JSON.stringify(productData)
        });
    },

    async updateProduct(id, productData) {
        return this.fetch(`/products/${id}`, {
            method: 'PUT',
            body: JSON.stringify(productData)
        });
    },

    async deleteProduct(id) {
        return this.fetch(`/products/${id}`, { method: 'DELETE' });
    },

    async toggleProductActive(id, isActive) {
        return this.fetch(`/products/${id}/active`, {
            method: 'PATCH',
            body: JSON.stringify({ isActive })
        });
    },

    // 🎯 UTILITIES
    setAuthToken(token) {
        const user = JSON.parse(localStorage.getItem('user') || 'null');
        if (user) {
            user.token = token;
            localStorage.setItem('user', JSON.stringify(user));
        }
    },

    clearAuthToken() {
        const user = JSON.parse(localStorage.getItem('user') || 'null');
        if (user) {
            delete user.token;
            localStorage.setItem('user', JSON.stringify(user));
        }
    },

    // Reset config (utile per testing)
    resetConfig() {
        this.config = {
            baseURL: API_BASE,
            timeout: API_TIMEOUT,
            retries: 1,
            retryDelay: 1000
        };
    }
};

// ========================================
// CUSTOM ERROR CLASS
// ========================================
class ApiError extends Error {
    constructor(message, status, data, endpoint) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
        this.endpoint = endpoint;
        this.timestamp = new Date().toISOString();

        // Stack trace preservation
        if (Error.captureStackTrace) {
            Error.captureStackTrace(this, ApiError);
        }
    }

    // Helper per verificare tipi di errore
    isClientError() { return this.status >= 400 && this.status < 500; }
    isServerError() { return this.status >= 500; }
    isNotFound() { return this.status === 404; }
    isUnauthorized() { return this.status === 401; }
    isForbidden() { return this.status === 403; }
    isValidationError() { return this.status === 422 || this.status === 400; }
}

// ========================================
// EXPORTS GLOBALI
// ========================================
window.ApiService = ApiService;
window.Events = Events;
window.ApiError = ApiError;

// ========================================
// EVENT LISTENERS GLOBALI
// ========================================
// Auto-logout handler
Events.on('auth:unauthorized', async () => {
    console.log('[Events] Unauthorized detected, logging out...');

    // Mostra toast se UI è disponibile
    if (typeof UI !== 'undefined' && typeof UI.showToast === 'function') {
        UI.showToast('Sessione scaduta. Effettua nuovamente l\'accesso.', 'error');
    }

    // Pulizia e redirect
    localStorage.removeItem('user');
    localStorage.removeItem('guestCartId');

    // Redirect alla home/login dopo un breve delay
    setTimeout(() => {
        if (typeof App?.showView === 'function') {
            App.showView('view-auth');
        } else {
            window.location.href = '/';
        }
    }, 1500);
});

// Rate limit handler
Events.on('api:rateLimited', ({ retryAfter }) => {
    if (typeof UI !== 'undefined' && typeof UI.showToast === 'function') {
        UI.showToast(`Troppe richieste. Riprova tra ${retryAfter || 'pochi'} secondi.`, 'error');
    }
});

// ========================================
// DEV MODE UTILITIES
// ========================================
if (window.location.hostname === 'localhost' || process.env?.NODE_ENV === 'development') {
    window.ApiMock = {
        // Simula risposta API con delay
        async mockResponse(data, delay = 300, status = 200) {
            await new Promise(res => setTimeout(res, delay));
            if (status >= 400) {
                throw new ApiError('Mock error', status, data, '/mock');
            }
            return { data, status, ok: true };
        },

        // Simula errore di rete
        async mockNetworkError(delay = 1000) {
            await new Promise(res => setTimeout(res, delay));
            throw new TypeError('Failed to fetch (mocked)');
        },

        // Simula timeout
        async mockTimeout() {
            await new Promise(res => setTimeout(res, ApiService.config.timeout + 100));
            return { data: null, status: 200, ok: true };
        }
    };

    console.log('%c🔧 ApiDev: Mock utilities disponibili', 'color: #8b5cf6; font-weight: bold;');
    console.log('  - ApiMock.mockResponse(data, delay, status)');
    console.log('  - ApiMock.mockNetworkError(delay)');
    console.log('  - ApiMock.mockTimeout()');
}

// ========================================
// INIT LOG
// ========================================
console.log(`%c✅ ApiService loaded | Base URL: ${API_BASE}`, 'color: #10b981; font-weight: 600;');
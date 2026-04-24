const App = {
    state: {
        user: JSON.parse(localStorage.getItem('user')) || null,
        guestCartId: localStorage.getItem('guestCartId') || null,
        products: [],
        cart: null,
        orders: [],
        categories: [],
        savedAccounts: JSON.parse(localStorage.getItem('saved_accounts')) || []
    },

    async init() {
        this.setupListeners();
        this.updateQuickFillDropdown();

        // 1️⃣ VALIDAZIONE JWT - Deve essere la prima cosa assoluta
        if (this.state.user) {
            try {
                console.log("[App] Validazione JWT in corso...");
                const res = await ApiService.getCurrentUser();

                // Token valido: aggiorniamo lo stato con i dati freschi del server
                this.state.user = { ...this.state.user, ...res.data.data };
                localStorage.setItem('user', JSON.stringify(this.state.user));
                console.log("[App] Sessione validata per:", this.state.user.email);
            } catch (err) {
                console.error("[App] JWT non valido o scaduto:", err.message);
                // Il logout pulisce tutto e ricarica la pagina
                this.logout();
                return; // STOP: Non procedere con altre chiamate
            }
        }

        // 2️⃣ CARICAMENTO DATI PUBBLICI - Solo dopo la validazione (o se guest)
        try {
            // Eseguiamo in parallelo per velocità, ma solo ora che sappiamo chi è l'utente
            await Promise.all([
                this.loadCategories(),
                this.loadProducts()
            ]);
            
            // 3️⃣ SINCRONIZZAZIONE CARRELLO
            if (this.state.user || this.state.guestCartId) {
                await this.syncCart();
            }
            
            this.updateGlobalUI();
            
            if (this.state.user && this.state.user.role === 'ADMIN') {
                Admin.loadProducts();
            }
        } catch (err) {
            console.error("[App] Errore durante l'inizializzazione dei dati:", err);
        }
    },

    setupListeners() {
        Events.on('logout-required', () => this.logout());
        Events.on('view-changed', (viewId) => {
            if (viewId === 'view-orders') this.loadOrders();
            if (viewId === 'view-admin') Admin.setTab(Admin.currentTab);
            if (viewId === 'view-profile' && this.state.user) this.fillProfileForm();
        });

        // Form listeners
        const loginForm = document.getElementById('login-form');
        if (loginForm) loginForm.onsubmit = (e) => { e.preventDefault(); this.handleLogin(e); };

        const registerForm = document.getElementById('register-form');
        if (registerForm) registerForm.onsubmit = (e) => { e.preventDefault(); this.handleRegister(e); };

        const profileForm = document.getElementById('profile-form');
        if (profileForm) profileForm.onsubmit = (e) => { e.preventDefault(); this.handleUpdateProfile(e); };

        const adminProductForm = document.getElementById('admin-product-form');
        if (adminProductForm) adminProductForm.onsubmit = (e) => { e.preventDefault(); Admin.saveProduct(e); };

        const categoryForm = document.getElementById('category-form');
        if (categoryForm) categoryForm.onsubmit = (e) => { e.preventDefault(); Admin.saveCategory(e); };
    },

    async loadProducts(categoryId = null) {
        try {
            const res = categoryId 
                ? await ApiService.fetch(`/products/category/${categoryId}`)
                : await ApiService.getProducts();
                
            this.state.products = res.data.data.content || res.data.data;
            if (typeof UI !== 'undefined') {
                UI.renderProducts(this.state.products);
                UI.renderCategories(this.state.categories, categoryId);
            }
        } catch (err) {
            console.error('[App] Errore caricamento prodotti:', err);
            UI?.showToast("Errore nel caricamento dei prodotti", "error");
        }
    },

    async loadCategories() {
        try {
            const res = await ApiService.getCategories();
            this.state.categories = res.data.data || [];
            if (typeof UI !== 'undefined') UI.renderCategories(this.state.categories);
        } catch (err) {
            console.error('[App] Errore caricamento categorie:', err);
        }
    },

    async filterByCategory(categoryId) {
        await this.loadProducts(categoryId);
    },

    // ✅ NUOVO: Mostra dettaglio prodotto
    async showProductDetail(slug) {
        try {
            const res = await ApiService.getProductBySlug(slug);
            UI.renderProductDetail(res.data.data);
        } catch (err) {
            console.error('[App] Errore dettaglio prodotto:', err);
            UI.showToast("Prodotto non trovato", "error");
            this.showView('view-products');
        }
    },

    async handleSearch(keyword) {
        if (!keyword) return this.loadProducts();
        try {
            const res = await ApiService.searchProducts(keyword);
            this.state.products = res.data.data.content || res.data.data;
            UI.renderProducts(this.state.products);
        } catch (err) {
            console.error('[App] Errore ricerca:', err);
            UI.showToast("Errore nella ricerca", "error");
        }
    },

    async syncCart() {
        try {
            const res = await ApiService.getCart();
            this.state.cart = res.data.data;
            if (typeof UI !== 'undefined') {
                UI.renderCart(this.state.cart);
            }
        } catch (err) {
            if (err.status === 400 || (err.message && err.message.includes("provide X-Guest"))) {
                console.warn("[App] Carrello non trovato o header mancante. Provo a creare guest cart.");
                await this.createGuestCartIfNeeded();
            } else if (err.status === 401) {
                this.logout();
            } else {
                console.warn("[App] Carrello non sincronizzato:", err.message);
                this.state.cart = null;
                if (typeof UI !== 'undefined') UI.renderCart(null);
            }
        }
        this.updateGlobalUI();
    },

    // ✅ NUOVO: Crea carrello guest se necessario
    async createGuestCartIfNeeded() {
        try {
            const res = await ApiService.createGuestCart();
            // L'ID viene già salvato in ApiService.createGuestCart
            this.state.guestCartId = localStorage.getItem('guestCartId');
            await this.syncCart();
            return true;
        } catch (err) {
            console.error('[App] Errore creazione guest cart:', err);
            return false;
        }
    },

    async addToCart(productId, quantity = 1) {
        if (isNaN(quantity) || quantity <= 0) return UI.showToast("Quantità non valida", 'error');

        // Feedback visivo immediato (UX)
        const stockEl = document.querySelector(`[data-product-stock="${productId}"]`);
        let originalStock = 0;
        if (stockEl) {
            originalStock = parseInt(stockEl.innerText.replace('Stock: ', ''));
            if (originalStock >= quantity) {
                stockEl.innerText = `Stock: ${originalStock - quantity}`;
                stockEl.classList.add('animate-pulse', 'text-amber-500');
            }
        }

        // Se guest e non ha cart ID, crealo prima
        if (!this.state.user && !this.state.guestCartId) {
            const created = await this.createGuestCartIfNeeded();
            if (!created) {
                UI.showToast("Errore: effettua il login per continuare", "error");
                this.showView('view-auth');
                return;
            }
        }

        try {
            await ApiService.addToCart(productId, quantity);
            await this.syncCart();
            UI.showToast("Prodotto aggiunto al carrello");
        } catch (err) {
            console.error('[App] Errore addToCart:', err);
            // Revert feedback visivo in caso di errore
            if (stockEl) {
                stockEl.innerText = `Stock: ${originalStock}`;
                stockEl.classList.remove('animate-pulse', 'text-amber-500');
            }
            UI.showToast(err.message || "Errore nell'aggiunta al carrello", "error");
        }
    },

    async updateCartItem(itemId, quantity) {
        if (quantity <= 0) return this.removeCartItem(itemId);
        try {
            await ApiService.updateCartItem(itemId, quantity);
            await this.syncCart();
        } catch (err) {
            console.error('[App] Errore updateCartItem:', err);
            UI.showToast(err.message || "Errore nell'aggiornamento", "error");
        }
    },

    async removeCartItem(itemId) {
        if (!confirm("Rimuovere questo articolo dal carrello?")) return;
        try {
            await ApiService.removeCartItem(itemId);
            await this.syncCart();
            UI.showToast("Articolo rimosso");
        } catch (err) {
            console.error('[App] Errore removeCartItem:', err);
            UI.showToast(err.message || "Errore nella rimozione", "error");
        }
    },

    async clearCart() {
        if (!confirm("Svuotare completamente il carrello?")) return;
        try {
            await ApiService.clearCart();
            await this.syncCart();
            UI.showToast("Carrello svuotato");
        } catch (err) {
            console.error('[App] Errore clearCart:', err);
            UI.showToast(err.message || "Errore nello svuotamento", "error");
        }
    },

    // ✅ FIX: Gestione errori login
    async handleLogin(e) {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const pass = document.getElementById('login-password').value;

        try {
            const response = await ApiService.login(email, pass);
            this.state.user = response.data || response;
            localStorage.setItem('user', JSON.stringify(this.state.user));

            // Salva account per quick login futuro
            this.saveAccount(email, pass);

            await this.syncCart();
            UI.showView('view-products');
            this.updateGlobalUI();
            UI.showToast(`Benvenuto, ${this.state.user.firstName || 'Utente'}!`);
        } catch (err) {
            console.error('[App] Login fallito:', err);
            UI.showToast(err.message || "Credenziali non valide", "error");
        }
    },

    // ✅ NUOVO: Handler registrazione
    async handleRegister(e) {
        e.preventDefault();
        const data = {
            firstName: document.getElementById('reg-firstName').value,
            lastName: document.getElementById('reg-lastName').value,
            email: document.getElementById('reg-email').value,
            password: document.getElementById('reg-password').value
        };

        try {
            const res = await ApiService.register(data);
            
            // Salva l'account appena registrato per il login rapido
            this.saveAccount(data.email, data.password);
            
            UI.showToast("Registrazione completata! Ora puoi accedere.");
            toggleAuth('login');
            document.getElementById('login-email').value = data.email;
            document.getElementById('login-password').value = data.password;
        } catch (err) {
            console.error('[App] Registrazione fallita:', err);
            UI.showToast(err.message || "Registrazione fallita", "error");
        }
    },

    // ✅ NUOVO: Compila form profilo con dati utente
    fillProfileForm() {
        if (!this.state.user) return;
        document.getElementById('profile-firstName').value = this.state.user.firstName || '';
        document.getElementById('profile-lastName').value = this.state.user.lastName || '';
        document.getElementById('profile-email').value = this.state.user.email || '';
        const badge = document.getElementById('profile-role-badge');
        if (badge) {
            badge.innerText = this.state.user.role || 'USER';
            badge.className = `px-5 py-2 rounded-full text-xs font-bold uppercase tracking-wider border ${
                this.state.user.role === 'ADMIN'
                    ? 'bg-amber-50 text-amber-600 border-amber-100'
                    : 'bg-indigo-50 text-indigo-600 border-indigo-100'
            }`;
        }
    },

    // ✅ NUOVO: Handler aggiornamento profilo
    async handleUpdateProfile(e) {
        e.preventDefault();
        const data = {
            firstName: document.getElementById('profile-firstName').value,
            lastName: document.getElementById('profile-lastName').value,
            email: document.getElementById('profile-email').value
        };

        try {
            await ApiService.updateProfile(data);
            this.state.user = { ...this.state.user, ...data };
            localStorage.setItem('user', JSON.stringify(this.state.user));
            UI.showToast("Profilo aggiornato con successo");
            this.updateGlobalUI();
            this.fillProfileForm();
        } catch (err) {
            console.error('[App] Update profilo fallito:', err);
            UI.showToast(err.message || "Aggiornamento fallito", "error");
        }
    },

    async loadOrders() {
        if (!this.state.user) return;
        try {
            const res = await ApiService.getOrders();
            this.state.orders = res.data.data || [];
            UI.renderOrders(this.state.orders);
        } catch (err) {
            console.error('[App] Errore caricamento ordini:', err);
            UI.showToast("Errore nel caricamento degli ordini", "error");
        }
    },

    async cancelOrder(orderId) {
        if (!confirm("Sei sicuro di voler annullare questo ordine?")) return;
        try {
            if (this.state.user?.role === 'ADMIN') {
                await ApiService.updateOrderStatus(orderId, 'CANCELLED');
            } else {
                await ApiService.cancelOrder(orderId);
            }
            UI.showToast("Ordine annullato");
            await this.loadOrders();
        } catch (err) {
            console.error('[App] Errore cancellazione ordine:', err);
            UI.showToast(err.message || "Errore nell'annullamento", "error");
        }
    },

    async checkout() {
        const addr = document.getElementById('shipping-address').value.trim();
        if (!addr) {
            UI.showToast("Inserisci un indirizzo di spedizione", "error");
            return;
        }
        try {
            await ApiService.checkout(addr);
            UI.showToast("Ordine completato con successo! 🎉");
            await this.loadProducts();
            await this.syncCart();
            setTimeout(() => UI.showView('view-orders'), 1200);
        } catch (err) {
            console.error('[App] Checkout fallito:', err);
            UI.showToast(err.message || "Errore nel completamento ordine", "error");
        }
    },

    logout() {
        ApiService.logout().finally(() => {
            // ✅ FIX: Non usiamo clear() altrimenti perdiamo i profili salvati
            localStorage.removeItem('user');
            localStorage.removeItem('guestCartId');
            
            this.state.user = null;
            this.state.guestCartId = null;
            this.state.cart = null;
            location.reload();
        });
    },

    // ✅ NUOVO: Salva account per quick login
    saveAccount(email, password) {
        const exists = this.state.savedAccounts.find(a => a.email === email);
        if (exists) return; // Già salvato

        this.state.savedAccounts.push({ email, password, savedAt: new Date().toISOString() });
        localStorage.setItem('saved_accounts', JSON.stringify(this.state.savedAccounts));
        this.updateQuickFillDropdown();
        console.log('[App] Account salvato per quick login:', email);
    },

    updateQuickFillDropdown() {
        const select = document.getElementById('quick-fill-select');
        if (!select) return;

        let html = `<option value="">Seleziona Profilo...</option>`;
        html += `<option value="admin@laptopverse.com:Admin123!">🔧 Administrator</option>`;
        html += `<option value="customer@laptopverse.com:Customer123!">👤 John Doe</option>`;

        this.state.savedAccounts.forEach(acc => {
            html += `<option value="${acc.email}:${acc.password}">💾 ${acc.email}</option>`;
        });
        select.innerHTML = html;
    },

    updateGlobalUI() {
        const user = this.state.user;

        // Toggle user info / login button
        const userInfo = document.getElementById('user-info');
        const loginBtn = document.getElementById('btn-login-nav');
        if (userInfo) userInfo.classList.toggle('hidden', !user);
        if (loginBtn) loginBtn.classList.toggle('hidden', !!user);

        // Update username in nav
        const usernameNav = document.getElementById('username-nav');
        if (usernameNav && user) {
            usernameNav.innerText = `${user.firstName || 'Utente'}`;
        }

        // Show/hide admin nav link
        const adminNav = document.getElementById('nav-admin');
        const isAdmin = user && user.role === 'ADMIN';
        if (adminNav) {
            adminNav.classList.toggle('hidden', !isAdmin);
        }

        // Hide cart and orders for ADMIN
        const cartBtn = document.querySelector('button[onclick="showView(\'view-cart\')"]');
        if (cartBtn) {
            cartBtn.classList.toggle('hidden', isAdmin);
        }

        // Show/hide auth-only links (Miei Ordini, Profilo)
        document.querySelectorAll('.auth-only').forEach(el => {
            const isOrderLink = el.getAttribute('onclick')?.includes('view-orders');
            if (isAdmin && isOrderLink) {
                el.classList.add('hidden');
            } else {
                el.classList.toggle('hidden', !user);
            }
        });
    },

    // ========================================
    // DEV UTILITIES
    // ========================================
    dev: {
        fillProductForm() {
            document.getElementById('form-name').value = 'Laptop Pro X' + Math.floor(Math.random()*1000);
            document.getElementById('form-sku').value = 'LPX-' + Math.random().toString(36).substring(2, 7).toUpperCase();
            document.getElementById('form-price').value = (Math.random()*2000+500).toFixed(2);
            document.getElementById('form-stock').value = Math.floor(Math.random()*50);
            document.getElementById('form-specs').value = 'Intel i7, 16GB RAM, 512GB SSD, RTX 4060';
            UI.showToast("Form prodotto compilato con dati test");
        },
        fillCategoryForm() {
            document.getElementById('cat-name').value = 'Categoria Test ' + Math.floor(Math.random()*100);
            UI.showToast("Form categoria compilato");
        },
        fillUserForm() {
            const r = Math.floor(Math.random()*1000);
            document.getElementById('user-firstName').value = 'User' + r;
            document.getElementById('user-lastName').value = 'Test';
            document.getElementById('user-email').value = `user${r}@test.com`;
            document.getElementById('user-password').value = 'Password123!';
            document.getElementById('user-role').value = Math.random() > 0.8 ? 'ADMIN' : 'CUSTOMER';
            UI.showToast("Form utente compilato");
        },
        fillAddress() {
            const addresses = [
                'Via Roma 123, Milano, 20100',
                'Corso Venezia 45, Roma, 00100',
                'Piazza Duomo 1, Firenze, 50100'
            ];
            const input = document.getElementById('shipping-address');
            if (input) input.value = addresses[Math.floor(Math.random()*addresses.length)];
            UI.showToast("Indirizzo compilato");
        },
        async quickLogin(role) {
            const credentials = {
                admin: { e: 'admin@laptopverse.com', p: 'Admin123!' },
                customer: { e: 'customer@laptopverse.com', p: 'Customer123!' }
            }[role];
            if (!credentials) return;

            const emailInput = document.getElementById('login-email');
            const passInput = document.getElementById('login-password');
            
            if (emailInput) emailInput.value = credentials.e;
            if (passInput) passInput.value = credentials.p;

            UI.showToast(`Campi login compilati per ${role}`);
        },
        fillRegisterForm() {
            const r = Math.floor(Math.random() * 1000);
            const firstName = document.getElementById('reg-firstName');
            const lastName = document.getElementById('reg-lastName');
            const email = document.getElementById('reg-email');
            const password = document.getElementById('reg-password');

            if (firstName) firstName.value = 'User' + r;
            if (lastName) lastName.value = 'Test';
            if (email) email.value = `user${r}@test.com`;
            if (password) password.value = 'Password123!';

            UI.showToast("Form registrazione compilato");
        }
    }
};

// ========================================
// GLOBAL HELPERS
// ========================================
window.App = App;
window.showView = (id) => {
    if (typeof UI !== 'undefined') UI.showView(id);
};
window.logout = () => App.logout();
window.checkout = () => App.checkout();
window.toggleAuth = (t) => {
    const loginBox = document.getElementById('box-login');
    const registerBox = document.getElementById('box-register');
    if (loginBox) loginBox.classList.toggle('hidden', t !== 'login');
    if (registerBox) registerBox.classList.toggle('hidden', t !== 'register');
};
window.quickFillLogin = () => {
    const select = document.getElementById('quick-fill-select');
    if (!select) return;
    const v = select.value;
    if (!v) return;
    const [e, p] = v.split(':');
    const emailInput = document.getElementById('login-email');
    const passInput = document.getElementById('login-password');
    if (emailInput) emailInput.value = e;
    if (passInput) passInput.value = p;
};
window.clearStorage = () => {
    localStorage.clear();
    location.reload();
};

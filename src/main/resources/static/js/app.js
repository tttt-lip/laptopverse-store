const App = {
    state: {
        user: JSON.parse(localStorage.getItem('user')) || null,
        guestCartId: localStorage.getItem('guestCartId') || null,
        products: [],
        cart: null,
        orders: [],
        savedAccounts: JSON.parse(localStorage.getItem('saved_accounts')) || []
    },

    async init() {
        this.setupListeners();
        this.updateQuickFillDropdown();
        await this.loadProducts();
        if (this.state.user || this.state.guestCartId) await this.syncCart();
        this.updateGlobalUI();
        
        if (this.state.user && this.state.user.role === 'ADMIN') {
            this.admin.loadProducts();
        }
    },

    setupListeners() {
        Events.on('logout-required', () => this.logout());
        Events.on('view-changed', (viewId) => {
            if (viewId === 'view-orders') this.loadOrders();
            if (viewId === 'view-admin') this.admin.setTab(this.admin.currentTab);
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
        if (adminProductForm) adminProductForm.onsubmit = (e) => { e.preventDefault(); this.admin.saveProduct(e); };

        const categoryForm = document.getElementById('category-form');
        if (categoryForm) categoryForm.onsubmit = (e) => { e.preventDefault(); this.admin.saveCategory(e); };
    },

    async loadProducts() {
        try {
            const res = await ApiService.getProducts();
            this.state.products = res.data.data.content || res.data.data;
            if (typeof UI !== 'undefined') UI.renderProducts(this.state.products);
        } catch (err) {
            console.error('[App] Errore caricamento prodotti:', err);
            UI?.showToast("Errore nel caricamento dei prodotti", "error");
        }
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
            UI.showToast("Registrazione completata! Ora puoi accedere.");
            toggleAuth('login');
            document.getElementById('login-email').value = data.email;
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
            localStorage.clear();
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
        if (adminNav) {
            adminNav.classList.toggle('hidden', !user || user.role !== 'ADMIN');
        }

        // Show/hide auth-only links (Miei Ordini, Profilo) - Visibili anche per ADMIN
        document.querySelectorAll('.auth-only').forEach(el => {
            el.classList.toggle('hidden', !user);
        });
    },

    // ========================================
    // ADMIN MODULE
    // ========================================
    admin: {
        currentTab: 'products',

        setTab(tab) {
            this.currentTab = tab;

            // Hide all tabs
            document.querySelectorAll('.admin-tab').forEach(t => t.classList.add('hidden'));
            const target = document.getElementById(`tab-${tab}`);
            if(target) target.classList.remove('hidden');

            // Update button styles
            document.querySelectorAll('[id^="tab-btn-"]').forEach(b => {
                b.classList.remove('bg-indigo-600', 'text-white');
                b.classList.add('text-slate-500');
            });
            const activeBtn = document.getElementById(`tab-btn-${tab}`);
            if(activeBtn) {
                activeBtn.classList.remove('text-slate-500');
                activeBtn.classList.add('bg-indigo-600', 'text-white');
            }

            // Load data for tab
            if (tab === 'products') this.loadProducts();
            if (tab === 'categories') this.loadCategories();
            if (tab === 'orders') this.loadAllOrders();
            if (tab === 'users') this.loadUsers();
        },

        async loadUsers() {
            try {
                const res = await ApiService.getAllUsers();
                UI.renderAdminUsers(res.data.data || res.data || []);
            } catch (err) {
                console.error('[Admin] Errore loadUsers:', err);
                UI.showToast("Errore caricamento utenti", "error");
            }
        },

        async createUser(e) {
            e.preventDefault();
            const data = {
                firstName: document.getElementById('user-firstName').value,
                lastName: document.getElementById('user-lastName').value,
                email: document.getElementById('user-email').value,
                password: document.getElementById('user-password').value,
                role: document.getElementById('user-role').value
            };

            try {
                await ApiService.register(data);
                UI.showToast("Utente creato con successo");
                this.closeUserForm();
                this.loadUsers();
            } catch (err) {
                console.error('[Admin] Errore createUser:', err);
                UI.showToast(err.message || "Errore nella creazione utente", "error");
            }
        },

        openUserForm() {
            const modal = document.getElementById('user-modal');
            if (modal) modal.classList.remove('hidden');
        },

        closeUserForm() {
            const modal = document.getElementById('user-modal');
            if (modal) modal.classList.add('hidden');
        },

        async editProduct(id) {
            try {
                const res = await ApiService.getProductById(id);
                const p = res.data.data;
                this.openProductForm(id);
                
                // Pre-compila i campi
                document.getElementById('form-name').value = p.name;
                document.getElementById('form-price').value = p.price;
                document.getElementById('form-stock').value = p.stockQuantity;
                document.getElementById('form-specs').value = p.specs || '';
                
                // Imposta la categoria (dopo aver caricato le categorie nel form)
                setTimeout(() => {
                    document.getElementById('form-category').value = p.categoryId;
                }, 500);
            } catch (err) {
                console.error('[Admin] Errore editProduct:', err);
                UI.showToast("Errore caricamento dati prodotto", "error");
            }
        },

        // ✅ NUOVO: Salva prodotto (create/update)
        async saveProduct(e) {
            e.preventDefault();
            const id = document.getElementById('form-product-id').value;
            const data = {
                name: document.getElementById('form-name').value,
                categoryId: document.getElementById('form-category').value,
                price: parseFloat(document.getElementById('form-price').value),
                stockQuantity: parseInt(document.getElementById('form-stock').value),
                specs: document.getElementById('form-specs').value,
                isActive: true
            };

            try {
                if (id) {
                    await ApiService.updateProduct(id, data);
                    UI.showToast("Prodotto aggiornato con successo");
                } else {
                    await ApiService.createProduct(data);
                    UI.showToast("Prodotto creato con successo");
                }
                this.closeProductForm();
                this.loadProducts();
            } catch (err) {
                console.error('[Admin] Errore saveProduct:', err);
                UI.showToast(err.message || "Errore nel salvataggio", "error");
            }
        },

        // ✅ NUOVO: Salva categoria
        async saveCategory(e) {
            e.preventDefault();
            const name = document.getElementById('cat-name').value.trim();
            if (!name) return UI.showToast("Inserisci un nome per la categoria", "error");

            try {
                await ApiService.createCategory(name);
                document.getElementById('category-form').reset();
                UI.showToast("Categoria creata con successo");
                this.loadCategories();
            } catch (err) {
                console.error('[Admin] Errore saveCategory:', err);
                UI.showToast(err.message || "Errore creazione categoria", "error");
            }
        },

        // ✅ NUOVO: Elimina prodotto
        async deleteProduct(id) {
            if (!confirm("Sei sicuro di voler eliminare definitivamente questo prodotto?")) return;
            try {
                await ApiService.deleteProduct(id);
                UI.showToast("Prodotto eliminato");
                this.loadProducts();
            } catch (err) {
                console.error('[Admin] Errore deleteProduct:', err);
                UI.showToast(err.message || "Errore eliminazione", "error");
            }
        },

        async deleteCategory(id) {
            if (!confirm("Eliminare questa categoria?")) return;
            try {
                await ApiService.fetch(`/categories/${id}`, { method: 'DELETE' });
                UI.showToast("Categoria eliminata");
                this.loadCategories();
            } catch (err) {
                console.error('[Admin] Errore deleteCategory:', err);
                UI.showToast(err.message || "Errore eliminazione", "error");
            }
        },

        async updateOrderStatus(id, status) {
            try {
                await ApiService.updateOrderStatus(id, status);
                UI.showToast(`Stato aggiornato: ${status}`);
                this.loadAllOrders();
            } catch (err) {
                console.error('[Admin] Errore updateOrderStatus:', err);
                UI.showToast("Errore aggiornamento stato", "error");
            }
        },

        // ✅ NUOVO: Toggle enabled ordine
        async toggleOrderEnabled(id, enabled) {
            try {
                await ApiService.fetch(`/orders/${id}/enable`, {
                    method: 'PATCH',
                    body: JSON.stringify({ enabled })
                });
                UI.showToast(`Ordine ${enabled ? 'attivato' : 'disattivato'}`);
                this.loadAllOrders();
            } catch (err) {
                console.error('[Admin] Errore toggleOrderEnabled:', err);
                UI.showToast("Errore aggiornamento", "error");
            }
        },

        // ✅ NUOVO: Modifica ordine (placeholder)
        async editOrder(id) {
            UI.showToast("Modifica ordine: funzionalità in sviluppo 🔧", "error");
            // TODO: Implementare modal di editing ordine
        },

        // ✅ NUOVO: Elimina ordine
        async deleteOrder(id) {
            if (!confirm("Sei sicuro di voler eliminare questo ordine?")) return;
            try {
                await ApiService.fetch(`/orders/${id}`, { method: 'DELETE' });
                UI.showToast("Ordine eliminato");
                this.loadAllOrders();
            } catch (err) {
                console.error('[Admin] Errore deleteOrder:', err);
                UI.showToast(err.message || "Errore eliminazione", "error");
            }
        },

        async deleteUser(id) {
            if (id === App.state.user?.id) return UI.showToast("Non puoi eliminare il tuo account!", "error");
            if (!confirm("Sei sicuro di eliminare questo utente?")) return;
            try {
                await ApiService.deleteUser(id);
                UI.showToast("Utente eliminato");
                this.loadUsers();
            } catch (err) {
                console.error('[Admin] Errore deleteUser:', err);
                UI.showToast(err.message || "Errore eliminazione", "error");
            }
        },

        openProductForm(productId = null) {
            const modal = document.getElementById('product-modal');
            const title = document.getElementById('modal-title');
            const form = document.getElementById('admin-product-form');

            if (!modal || !title || !form) return;

            title.innerText = productId ? "✏️ Modifica Prodotto" : "➕ Nuovo Prodotto";
            document.getElementById('form-product-id').value = productId || '';

            // Reset form se nuovo prodotto
            if (!productId) {
                form.reset();
                document.getElementById('form-stock').value = '0';
                document.getElementById('form-price').value = '0';
            } else {
                // TODO: Caricare dati prodotto esistente per pre-fill
                // await this.loadProductForEdit(productId);
            }

            // Popola select categorie
            this.populateCategorySelect();

            modal.classList.remove('hidden');
        },

        // ✅ NUOVO: Popola select categorie nel form prodotto
        async populateCategorySelect() {
            const select = document.getElementById('form-category');
            if (!select) return;

            try {
                const res = await ApiService.getCategories();
                const categories = res.data.data || [];
                select.innerHTML = `<option value="">Seleziona categoria...</option>` +
                    categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
            } catch (err) {
                console.error('[Admin] Errore populateCategorySelect:', err);
                select.innerHTML = `<option value="">Errore caricamento</option>`;
            }
        },

        closeProductForm() {
            const modal = document.getElementById('product-modal');
            if (modal) modal.classList.add('hidden');
        }
    },

    // ========================================
    // DEV UTILITIES
    // ========================================
    dev: {
        fillProductForm() {
            document.getElementById('form-name').value = 'Laptop Pro X' + Math.floor(Math.random()*1000);
            document.getElementById('form-price').value = (Math.random()*2000+500).toFixed(2);
            document.getElementById('form-stock').value = Math.floor(Math.random()*50);
            document.getElementById('form-specs').value = 'Intel i7, 16GB RAM, 512GB SSD, RTX 4060';
            UI.showToast("Form prodotto compilato con dati test");
        },
        fillCategoryForm() {
            document.getElementById('cat-name').value = 'Categoria Test ' + Math.floor(Math.random()*100);
            UI.showToast("Form categoria compilato");
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

            document.getElementById('login-email').value = credentials.e;
            document.getElementById('login-password').value = credentials.p;

            // Simula submit
            const event = new Event('submit', { cancelable: true });
            document.getElementById('login-form')?.dispatchEvent(event);
            UI.showToast(`Quick login come ${role}...`);
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

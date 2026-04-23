/**
 * ADMIN MODULE - Laptopverse
 * Gestione dashboard amministratore (prodotti, categorie, ordini, utenti)
 */
const Admin = {
    currentTab: 'products',

    setTab(tab) {
        this.currentTab = tab;

        // Nascondi tutti i tab
        document.querySelectorAll('.admin-tab').forEach(t => t.classList.add('hidden'));
        const target = document.getElementById(`tab-${tab}`);
        if(target) target.classList.remove('hidden');

        // Aggiorna stile bottoni
        document.querySelectorAll('[id^="tab-btn-"]').forEach(b => {
            b.classList.remove('bg-indigo-600', 'text-white');
            b.classList.add('text-slate-500');
        });
        const activeBtn = document.getElementById(`tab-btn-${tab}`);
        if(activeBtn) {
            activeBtn.classList.remove('text-slate-500');
            activeBtn.classList.add('bg-indigo-600', 'text-white');
        }

        // Carica dati per il tab selezionato
        if (tab === 'products') this.loadProducts();
        if (tab === 'categories') this.loadCategories();
        if (tab === 'orders') this.loadAllOrders();
        if (tab === 'users') this.loadUsers();
    },

    async loadProducts() {
        try {
            const res = await ApiService.getProducts();
            const products = res.data.data.content || res.data.data || [];
            UI.renderAdminProducts(products);
        } catch (err) {
            console.error('[Admin] Errore loadProducts:', err);
            UI.showToast("Errore caricamento prodotti", "error");
        }
    },

    async loadCategories() {
        try {
            const res = await ApiService.getCategories();
            const categories = res.data.data || [];
            UI.renderAdminCategories(categories);
        } catch (err) {
            console.error('[Admin] Errore loadCategories:', err);
            UI.showToast("Errore caricamento categorie", "error");
        }
    },

    async loadAllOrders() {
        try {
            const res = await ApiService.getAllOrders();
            UI.renderAdminOrders(res.data.data || res.data || []);
        } catch (err) {
            console.error('[Admin] Errore loadAllOrders:', err);
            UI.showToast("Errore caricamento ordini", "error");
        }
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

    async saveUser(e) {
        e.preventDefault();
        const id = document.getElementById('form-user-id').value;
        const data = {
            firstName: document.getElementById('user-firstName').value,
            lastName: document.getElementById('user-lastName').value,
            email: document.getElementById('user-email').value,
            role: document.getElementById('user-role').value
        };

        const password = document.getElementById('user-password').value;
        if (password) data.password = password;

        try {
            if (id) {
                // Endpoint ipotetico per update utente da admin
                await ApiService.fetch(`/users/${id}`, {
                    method: 'PUT',
                    body: JSON.stringify(data)
                });
                UI.showToast("Utente aggiornato con successo");
            } else {
                if (!password) return UI.showToast("La password è obbligatoria per i nuovi utenti", "error");
                await ApiService.register(data);
                UI.showToast("Utente creato con successo");
            }
            this.closeUserForm();
            this.loadUsers();
        } catch (err) {
            console.error('[Admin] Errore saveUser:', err);
            UI.showToast(err.message || "Errore nel salvataggio utente", "error");
        }
    },

    openUserForm(userId = null) {
        const modal = document.getElementById('user-modal');
        const title = modal?.querySelector('h2');
        if (modal) {
            if (title) title.innerText = userId ? "✏️ Modifica Utente" : "➕ Crea Nuovo Utente";
            document.getElementById('form-user-id').value = userId || '';
            modal.classList.remove('hidden');
        }
    },

    closeUserForm() {
        const modal = document.getElementById('user-modal');
        if (modal) {
            modal.classList.add('hidden');
            const form = document.getElementById('admin-user-form');
            if (form) form.reset();
        }
    },

    async editUser(id) {
        try {
            const res = await ApiService.fetch(`/users/${id}`);
            const u = res.data.data || res.data;
            this.openUserForm(id);
            
            document.getElementById('user-firstName').value = u.firstName || '';
            document.getElementById('user-lastName').value = u.lastName || '';
            document.getElementById('user-email').value = u.email || '';
            document.getElementById('user-role').value = u.role || 'CUSTOMER';
            // Password non viene pre-compilata per sicurezza
            document.getElementById('user-password').value = '';
            document.getElementById('user-password').placeholder = "Lascia vuoto per non cambiare";
        } catch (err) {
            console.error('[Admin] Errore editUser:', err);
            UI.showToast("Errore caricamento dati utente", "error");
        }
    },

    async editProduct(id) {
        try {
            const res = await ApiService.getProductById(id);
            const p = res.data.data;
            this.openProductForm(id);
            
            // Pre-compila i campi
            document.getElementById('form-name').value = p.name || '';
            document.getElementById('form-sku').value = p.sku || '';
            document.getElementById('form-price').value = p.price || 0;
            document.getElementById('form-stock').value = p.stockQuantity || 0;
            document.getElementById('form-specs').value = p.specs || '';
            document.getElementById('form-image').value = p.imageUrl || '';
            
            // Imposta la categoria (dopo aver caricato le categorie nel form)
            setTimeout(() => {
                document.getElementById('form-category').value = p.categoryId;
            }, 500);
        } catch (err) {
            console.error('[Admin] Errore editProduct:', err);
            UI.showToast("Errore caricamento dati prodotto", "error");
        }
    },

    async saveProduct(e) {
        e.preventDefault();
        const id = document.getElementById('form-product-id').value;
        const data = {
            name: document.getElementById('form-name').value,
            sku: document.getElementById('form-sku').value,
            categoryId: document.getElementById('form-category').value,
            price: parseFloat(document.getElementById('form-price').value),
            stockQuantity: parseInt(document.getElementById('form-stock').value),
            specs: document.getElementById('form-specs').value,
            imageUrl: document.getElementById('form-image').value,
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

    async editOrder(id) {
        UI.showToast("Modifica ordine: funzionalità in sviluppo 🔧", "error");
    },

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

        if (!productId) {
            form.reset();
            document.getElementById('form-stock').value = '0';
            document.getElementById('form-price').value = '0';
        }

        this.populateCategorySelect();
        modal.classList.remove('hidden');
    },

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
};

// Esponi globalmente
window.Admin = Admin;

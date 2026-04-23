const UI = {
    showView(id) {
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        const target = document.getElementById(id);
        if (target) target.classList.add('active');
        window.scrollTo(0, 0);
        Events.emit('view-changed', id);
    },

    renderProducts(products) {
        const grid = document.getElementById('products-grid');
        if (!grid) return;
        grid.innerHTML = products.map(p => `
            <div class="bg-white p-5 rounded-3xl border border-slate-100 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer group" onclick="App.showProductDetail('${p.slug}')">
                <div class="relative h-48 bg-slate-50 rounded-2xl mb-5 flex items-center justify-center text-slate-200 overflow-hidden">
                    <svg class="w-12 h-12 text-slate-200 group-hover:scale-110 transition-transform duration-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="1.5" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                    <div class="absolute top-3 right-3 ${p.stockQuantity < 5 ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-600'} text-[10px] font-bold px-2.5 py-1 rounded-lg border border-current opacity-90">
                        Stock: ${p.stockQuantity}
                    </div>
                </div>
                <div class="text-[10px] font-bold text-indigo-600 uppercase tracking-wider mb-1.5">${p.categoryName || 'Senza categoria'}</div>
                <h3 class="font-bold text-slate-900 text-base mb-1 group-hover:text-indigo-600 transition-colors">${p.name}</h3>
                <p class="text-slate-400 text-[11px] mb-5 line-clamp-2 leading-relaxed">${p.specs || 'Nessuna specifica'}</p>
                <div class="flex items-center justify-between pt-4 border-t border-slate-50">
                    <span class="text-xl font-bold text-slate-900">${(p.price || 0).toFixed(2)}€</span>
                    <button onclick="event.stopPropagation(); App.addToCart('${p.id}')" class="bg-slate-900 text-white p-2.5 rounded-xl hover:bg-indigo-600 transition-colors">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2.5" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path></svg>
                    </button>
                </div>
            </div>
        `).join('');
    },

    renderProductDetail(p) {
        const container = document.getElementById('product-detail-content');
        container.innerHTML = `
            <div class="grid grid-cols-1 md:grid-cols-2 gap-12 lg:gap-20">
                <div class="bg-white rounded-[2.5rem] aspect-square flex items-center justify-center text-slate-100 border border-slate-100 shadow-sm overflow-hidden relative">
                    <svg class="w-32 h-32" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="1" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                    <div class="absolute top-6 right-6 ${p.stockQuantity < 5 ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-600'} text-xs font-bold px-4 py-1.5 rounded-xl border border-current opacity-90">
                        Disponibilità: ${p.stockQuantity} pezzi
                    </div>
                </div>
                <div class="flex flex-col justify-center">
                    <div class="text-indigo-600 font-bold uppercase tracking-widest text-xs mb-4">${p.categoryName || 'Senza categoria'}</div>
                    <h2 class="text-5xl font-bold tracking-tight mb-6 text-slate-900">${p.name}</h2>
                    <p class="text-slate-500 text-base leading-relaxed mb-10">${p.specs || 'Nessuna specifica'}</p>
                    <div class="flex items-center gap-10 mb-10 bg-white p-6 rounded-3xl border border-slate-100">
                        <div>
                            <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Prezzo</div>
                            <div class="text-3xl font-bold text-slate-900">${(p.price || 0).toFixed(2)}€</div>
                        </div>
                        <div class="h-10 w-px bg-slate-100"></div>
                        <div>
                            <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Quantità</div>
                            <input type="number" id="detail-quantity" value="1" 
                                class="bg-slate-50 border-none rounded-xl px-3 py-1.5 w-20 font-bold text-lg focus:ring-2 focus:ring-indigo-500 outline-none text-center">
                        </div>
                    </div>
                    <button onclick="App.addToCart('${p.id}', parseInt(document.getElementById('detail-quantity').value))" 
                        class="w-full bg-slate-900 text-white font-bold py-5 rounded-2xl text-sm hover:bg-indigo-600 transition shadow-lg disabled:bg-slate-100 disabled:text-slate-400"
                        ${p.stockQuantity <= 0 ? 'disabled' : ''}>
                        ${p.stockQuantity > 0 ? 'Aggiungi al carrello' : 'Prodotto Esaurito'}
                    </button>
                </div>
            </div>
        `;
        this.showView('view-product-detail');
    },

    renderCart(cart) {
        const list = document.getElementById('cart-items-list');
        const summary = document.getElementById('cart-summary');
        const countBadge = document.getElementById('cart-count');
        
        if (countBadge) countBadge.innerText = cart ? (cart.totalQuantity || 0) : '0';
        
        if (!cart || !cart.items || cart.items.length === 0) {
            if (list) list.innerHTML = `<div class="bg-white p-20 rounded-3xl text-center border border-slate-100 text-slate-400 font-medium text-sm">Il tuo carrello è vuoto</div>`;
            if (summary) summary.classList.add('hidden');
            return;
        }

        if (list) {
            list.innerHTML = cart.items.map(i => `
                <div class="bg-white p-6 rounded-3xl border border-slate-100 flex justify-between items-center shadow-sm hover:shadow-md transition-shadow">
                    <div class="flex items-center gap-6">
                        <div class="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center text-indigo-400 font-bold text-[10px] border border-slate-100">LAP</div>
                        <div>
                            <h4 class="font-bold text-slate-900 text-lg">${i.productName}</h4>
                            <p class="text-[11px] text-slate-500 mt-1">
                                Prezzo: <span class="font-semibold text-slate-900">${(parseFloat(i.priceSnapshot) || 0).toFixed(2)}€</span> 
                                <span class="mx-3 text-slate-200">|</span> 
                                Quantità: <span class="font-semibold text-indigo-600">${i.quantity}</span>
                            </p>
                        </div>
                    </div>
                    <div class="text-right">
                        <div class="text-[10px] font-bold text-slate-300 uppercase mb-1">Totale</div>
                        <div class="font-bold text-slate-900 text-xl">${((parseFloat(i.priceSnapshot) || 0) * i.quantity).toFixed(2)}€</div>
                    </div>
                </div>
            `).join('');
        }
        
        if (summary) {
            const totalEl = document.getElementById('total-price');
            if (totalEl) totalEl.innerText = `${(parseFloat(cart.totalPrice) || 0).toFixed(2)}€`;
            summary.classList.remove('hidden');
        }
    },

    renderOrders(orders) {
        const list = document.getElementById('orders-list');
        if (!list) return;
        if (!orders || orders.length === 0) {
            list.innerHTML = `<div class="bg-white p-20 rounded-3xl text-center text-slate-400 border border-slate-100 text-sm">Non hai ancora effettuato ordini</div>`;
            return;
        }
        list.innerHTML = orders.map(o => `
            <div class="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
                <div class="p-8 bg-slate-50/50 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 border-b border-slate-100">
                    <div>
                        <div class="flex items-center gap-4 mb-2">
                            <span class="font-bold text-slate-900 text-lg uppercase tracking-tight">Ordine #${o.orderNumber.split('-').pop()}</span>
                            <span class="text-[10px] font-bold px-3 py-1 rounded-lg uppercase tracking-wider ${this.getStatusClass(o.status)} border border-current opacity-90">${o.status}</span>
                        </div>
                        <p class="text-xs text-slate-500">Spedito a: ${o.shippingAddress}</p>
                    </div>
                    <div class="text-right">
                        <div class="text-[10px] text-slate-400 font-bold uppercase mb-1">Totale Ordine</div>
                        <div class="font-bold text-indigo-600 text-3xl tracking-tight">${(parseFloat(o.totalAmount) || 0).toFixed(2)}€</div>
                    </div>
                </div>
                <div class="p-8 space-y-4">
                    ${o.items.map(item => `
                        <div class="flex justify-between items-center text-sm">
                            <span class="text-slate-600 font-medium">
                                <span class="font-bold text-slate-900 mr-2">${item.quantity}x</span> ${item.productNameSnapshot}
                            </span>
                            <span class="font-bold text-slate-900">${((parseFloat(item.unitPriceSnapshot) || 0) * item.quantity).toFixed(2)}€</span>
                        </div>
                    `).join('')}
                    
                    ${o.status === 'PENDING' ? `
                        <div class="pt-6 mt-4 border-t border-slate-100 flex justify-end">
                            <button onclick="App.cancelOrder('${o.id}')" 
                                class="text-rose-500 bg-rose-50 hover:bg-rose-500 hover:text-white px-6 py-2 rounded-xl text-xs font-bold transition">
                                Annulla Ordine
                            </button>
                        </div>
                    ` : ''}
                </div>
            </div>
        `).join('');
    },

    renderAdminProducts(products) {
        const tbody = document.getElementById('admin-products-table');
        if (!tbody) return;
        tbody.innerHTML = products.map(p => `
            <tr class="text-xs font-medium text-slate-600 border-b border-slate-50 hover:bg-slate-50">
                <td class="px-8 py-5">
                    <div class="font-bold text-slate-900">${p.name}</div>
                    <div class="text-[9px] text-slate-400 uppercase tracking-wider">SKU: ${p.sku || '-'} | SLUG: ${p.slug || '-'}</div>
                </td>
                <td class="px-8 py-5">${p.categoryName || '-'}</td>
                <td class="px-8 py-5 text-center">${p.stockQuantity || 0}</td>
                <td class="px-8 py-5 text-center">
                    <span class="px-2 py-1 rounded ${p.isActive ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'} font-bold text-[9px] uppercase">
                        ${p.isActive ? 'Attivo' : 'Inattivo'}
                    </span>
                </td>
                <td class="px-8 py-5 text-right font-bold text-slate-900">${(p.price || 0).toFixed(2)}€</td>
                <td class="px-8 py-5 text-center">
                    <button onclick="App.admin.openProductForm('${p.id}')" class="text-indigo-600 hover:text-indigo-800 font-bold mr-3">Modifica</button>
                    <button onclick="App.admin.deleteProduct('${p.id}')" class="text-rose-500 hover:text-rose-700 font-bold">Elimina</button>
                </td>
            </tr>
        `).join('');
    },

    renderAdminCategories(categories) {
        const list = document.getElementById('admin-categories-list');
        if (!list) return;
        list.innerHTML = categories.map(c => `
            <div class="p-6 flex justify-between items-center hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0">
                <span class="font-bold text-slate-900 text-sm">${c.name}</span>
                <button onclick="App.admin.deleteCategory('${c.id}')" class="text-rose-500 hover:text-rose-700 font-bold text-xs">Elimina</button>
            </div>
        `).join('');
    },

    renderAdminOrders(ordersData) {
        const tbody = document.getElementById('admin-orders-table');
        if (!tbody) return;
        const orders = ordersData.content || ordersData;
        tbody.innerHTML = orders.map(o => `
            <tr class="text-xs font-medium text-slate-600 border-b border-slate-50 hover:bg-slate-50">
                <td class="px-8 py-5 text-slate-900 font-bold">#${o.orderNumber ? o.orderNumber.split('-').pop() : 'N/A'}</td>
                <td class="px-8 py-5">${o.userEmail || (o.user ? o.user.email : 'N/A')}</td>
                <td class="px-8 py-5 text-center">
                    <select onchange="App.admin.updateOrderStatus('${o.id}', this.value)" class="bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 outline-none font-bold uppercase text-[9px]">
                        ${['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map(s => 
                            `<option value="${s}" ${o.status === s ? 'selected' : ''}>${s}</option>`).join('')}
                    </select>
                </td>
                <td class="px-8 py-5 text-center">
                    <button onclick="App.admin.toggleOrderEnabled('${o.id}', ${!o.isEnabled})" class="relative inline-flex h-6 w-11 items-center rounded-full ${o.isEnabled ? 'bg-emerald-500' : 'bg-slate-300'} transition">
                        <span class="inline-block h-4 w-4 transform rounded-full bg-white transition ${o.isEnabled ? 'translate-x-6' : 'translate-x-1'}"></span>
                    </button>
                </td>
                <td class="px-8 py-5 text-center">
                    <button onclick="App.admin.editOrder('${o.id}')" class="text-indigo-600 hover:text-indigo-800 font-bold mr-3">Modifica</button>
                    <button onclick="App.admin.deleteOrder('${o.id}')" class="text-rose-500 hover:text-rose-700 font-bold">Elimina</button>
                </td>
            </tr>
        `).join('');
    },

    renderAdminUsers(users) {
        const tbody = document.getElementById('admin-users-table');
        if(!tbody) return;
        tbody.innerHTML = users.map(u => `
            <tr class="text-xs font-medium text-slate-600 border-b border-slate-50 hover:bg-slate-50">
                <td class="px-8 py-5 text-slate-900 font-bold">${u.firstName || ''} ${u.lastName || ''}</td>
                <td class="px-8 py-5">${u.email || ''}</td>
                <td class="px-8 py-5"><span class="px-3 py-1 rounded-lg ${u.role === 'ADMIN' ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-600'} font-bold text-[10px] uppercase">${u.role || 'N/A'}</span></td>
                <td class="px-8 py-5 text-center">
                    <span class="px-2 py-1 rounded ${u.isEnabled ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'} font-bold text-[9px] uppercase">
                        ${u.isEnabled ? 'Attivo' : 'Disabilitato'}
                    </span>
                </td>
                <td class="px-8 py-5 text-center">
                    <button onclick="App.admin.deleteUser('${u.id}')" class="text-rose-500 hover:text-rose-700 font-bold">Elimina</button>
                </td>
            </tr>
        `).join('');
    },

    getStatusClass(status) {
        const map = { 
            'PENDING': 'bg-amber-50 text-amber-600', 
            'PAID': 'bg-emerald-50 text-emerald-600', 
            'SHIPPED': 'bg-indigo-50 text-indigo-600', 
            'DELIVERED': 'bg-slate-100 text-slate-600', 
            'CANCELLED': 'bg-rose-50 text-rose-600' 
        };
        return map[status] || 'bg-slate-50 text-slate-400';
    },

    showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        if (!container) return;
        const toast = document.createElement('div');
        const styles = type === 'success' ? 'bg-slate-900 text-white' : 'bg-white text-rose-600 border border-rose-100';
        toast.className = `${styles} px-8 py-4 rounded-2xl shadow-xl font-bold text-xs transform transition-all duration-500 translate-y-4 opacity-0 border border-slate-800`;
        toast.innerText = message;
        container.appendChild(toast);
        setTimeout(() => toast.classList.remove('translate-y-4', 'opacity-0'), 10);
        setTimeout(() => { 
            toast.classList.add('translate-y-4', 'opacity-0');
            setTimeout(() => toast.remove(), 500);
        }, 3500);
    },

    showErrorLog(err, context = "General") {
        console.error("[ERRORE - " + context + "]", 'background: #f43f5e; color: white; font-weight: bold; padding: 2px 5px; border-radius: 4px;', err);
    }
};

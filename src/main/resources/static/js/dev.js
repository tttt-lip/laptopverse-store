const DevTools = {
    fillProductForm() {
        document.getElementById('form-name').value = "Laptop " + Math.floor(Math.random() * 1000);
        document.getElementById('form-price').value = (Math.random() * 2000).toFixed(2);
        document.getElementById('form-stock').value = Math.floor(Math.random() * 100);
        document.getElementById('form-specs').value = "CPU i7, 16GB RAM, 512GB SSD";
        document.getElementById('form-sku').value = "SKU-" + Math.random().toString(36).substring(2, 7).toUpperCase();
    },
    fillCategoryForm() {
        document.getElementById('cat-name').value = "Categoria " + Math.floor(Math.random() * 100);
    },
    fillAddress() {
        const input = document.getElementById('shipping-address');
        if (input) input.value = "Via Roma 123, Milano";
    },
    quickLogin(role) {
        const credentials = {
            admin: { e: 'admin@laptopverse.com', p: 'Admin123!' },
            customer: { e: 'customer@laptopverse.com', p: 'Customer123!' }
        }[role];
        if (!credentials) return;

        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        
        if (emailInput) emailInput.value = credentials.e;
        if (passInput) passInput.value = credentials.p;
        
        if (typeof UI !== 'undefined') UI.showToast(`Campi login compilati per ${role}`);
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

        if (typeof UI !== 'undefined') UI.showToast("Form registrazione compilato");
    }
};
window.App = { ...window.App, dev: DevTools };
